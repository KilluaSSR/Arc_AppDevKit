package killua.dev.core.sharedvm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Central registry managing shared ViewModel instances by key.
 * Proper lifecycle management with reference counting and ViewModel integration.
 */
object SharedViewModelRegistry {
    // Interface for lifecycle callbacks to avoid reflection
    interface LifecycleAware {
        fun onFirstRef()
        fun onLastRef()
        fun dispose()
    }

    private class Entry(
        private val vmRef: WeakReference<Any>,
        initialCount: Int = 1
    ) {
        val refCount = AtomicInteger(initialCount)
        val destroyed = AtomicBoolean(false)
        val createdAt = System.currentTimeMillis()

        // Get VM reference safely
        fun getVm(): Any? = vmRef.get()

        // Cleanup VM safely
        fun cleanup() {
            val vm = getVm()
            if (vm is LifecycleAware) {
                vm.dispose()
            }
        }
    }

    private val map = ConcurrentHashMap<String, Entry>()

    /**
     * Debug info exposed for debugging UI: list of active shared VMs with metadata.
     * Observers (debug page) can collect this StateFlow to show current state.
     */
    data class SharedVmDebugInfo(
        val key: String,
        val vmClass: String,
        val refCount: Int,
        val createdAt: Long
    )

    private val _debugFlow = MutableStateFlow<List<SharedVmDebugInfo>>(emptyList())
    val debugFlow: StateFlow<List<SharedVmDebugInfo>> = _debugFlow

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrCreate(key: String, factory: () -> T): T {
        // Try fast path
        map[key]?.let { entry ->
            val vm = entry.getVm()
            if (vm != null) {
                entry.refCount.incrementAndGet()
                updateDebugFlow()
                return vm as T
            } else {
                // VM was GC'd, remove entry
                map.remove(key)
            }
        }

        // Create new entry atomically
        val entry = map.compute(key) { _, old ->
            if (old == null) {
                val vm = factory() as Any
                val vmRef = WeakReference(vm)

                // Call onFirstRef if available
                try {
                    if (vm is LifecycleAware) {
                        vm.onFirstRef()
                    }
                } catch (_: Throwable) {
                    // Ignore lifecycle callback errors
                }

                Entry(vmRef, 1)
            } else {
                // Existing entry found
                val existingVm = old.getVm()
                if (existingVm != null) {
                    old.refCount.incrementAndGet()
                    old
                } else {
                    // Old entry was GC'd, create new one
                    val vm = factory() as Any
                    val vmRef = WeakReference(vm)
                    if (vm is LifecycleAware) {
                        vm.onFirstRef()
                    }
                    Entry(vmRef, 1)
                }
            }
        }!!
        updateDebugFlow()

        val resultVm = entry.getVm()
        return resultVm as T
    }

    fun release(key: String) {
        val entry = map[key] ?: return
        val remain = entry.refCount.decrementAndGet()
        updateDebugFlow()

        if (remain <= 0) {
            // ensure only one thread destroys
            if (entry.destroyed.compareAndSet(false, true)) {
                map.remove(key)

                // Call lifecycle hooks before cleanup
                val vm = entry.getVm()
                if (vm != null) {
                    try {
                        if (vm is LifecycleAware) {
                            vm.onLastRef()
                        }
                    } catch (_: Throwable) {
                        // Ignore lifecycle callback errors
                    }

                    // Cleanup ViewModel properly
                    try {
                        entry.cleanup()
                    } catch (_: Throwable) {
                        // Ignore cleanup errors
                    }
                }

                updateDebugFlow()
            }
        }
    }

    fun size(): Int = map.size

    /** Return the latest snapshot of debug info (non-blocking). */
    fun debugSnapshot(): List<SharedVmDebugInfo> = _debugFlow.value

    private fun updateDebugFlow() {
        // Snapshot current map into a list
        val list = map.entries.mapNotNull { (k, e) ->
            val vm = e.getVm()
            if (vm != null) {
                SharedVmDebugInfo(
                    key = k,
                    vmClass = vm::class.java.name,
                    refCount = e.refCount.get(),
                    createdAt = e.createdAt
                )
            } else {
                // VM was GC'd, clean up
                map.remove(k)
                null
            }
        }
        _debugFlow.value = list
    }
}
