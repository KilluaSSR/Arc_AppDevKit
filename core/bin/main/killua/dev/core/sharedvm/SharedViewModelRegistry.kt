package killua.dev.core.sharedvm

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import killua.dev.core.viewmodel.BaseViewModel
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Central registry managing shared ViewModel instances by key.
 * Instances are not stored in a ViewModelStore; registry keeps strong reference
 * while reference count > 0, and disposes when ref count reaches 0.
 */
object SharedViewModelRegistry {
    private class Entry(
        val vm: Any,
        initialCount: Int = 1
    ) {
        val refCount = AtomicInteger(initialCount)
        val destroyed = AtomicBoolean(false)
        val createdAt = System.currentTimeMillis()
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
            entry.refCount.incrementAndGet()
            updateDebugFlow()
            return entry.vm as T
        }

        // Create new entry atomically
        val entry = map.compute(key) { _, old ->
            if (old == null) {
                val vm = factory() as Any
                // call onFirstRef if available
                try {
                    vm::class.members.firstOrNull { it.name == "onFirstRef" }?.call(vm)
                } catch (_: Throwable) {
                }
                val e = Entry(vm, 1)
                // map updated, refresh debug flow below
                e
            } else {
                old.refCount.incrementAndGet()
                old
            }
        }!!
        updateDebugFlow()
        return entry.vm as T
    }

    fun release(key: String) {
        val entry = map[key] ?: return
        val remain = entry.refCount.decrementAndGet()
        updateDebugFlow()
        if (remain <= 0) {
            // ensure only one thread destroys
            if (entry.destroyed.compareAndSet(false, true)) {
                map.remove(key)
                // attempt to call onLastRef then dispose if available
                try {
                    val vm = entry.vm
                    vm::class.members.firstOrNull { it.name == "onLastRef" }?.call(vm)
                } catch (_: Throwable) {
                }
                try {
                    val vm = entry.vm
                    vm::class.members.firstOrNull { it.name == "dispose" }?.call(vm)
                } catch (_: Throwable) {
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
        val list = map.entries.map { (k, e) ->
            SharedVmDebugInfo(
                key = k,
                vmClass = e.vm::class.java.name,
                refCount = e.refCount.get(),
                createdAt = e.createdAt
            )
        }
        _debugFlow.value = list
    }
}
