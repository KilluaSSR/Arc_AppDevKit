package killua.dev.core.sharedvm

import killua.dev.core.viewmodel.BaseViewModel
import killua.dev.core.viewmodel.UIEffect
import killua.dev.core.viewmodel.UIIntent
import killua.dev.core.viewmodel.UIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Shared ViewModel base class. Extends project's BaseViewModel with proper lifecycle management.
 * Subclasses may override lifecycle hooks: onFirstRef/onLastRef/onDispose.
 * Important: do NOT hold strong references to Activity/Views. Use Application-level
 * resources when needed.
 */
abstract class BaseSharedViewModel<I : UIIntent, S : UIState, E : UIEffect>(state: S) :
    BaseViewModel<I, S, E>(state), SharedViewModelRegistry.LifecycleAware {

    private var disposed = false
    private val sharedScope = CoroutineScope(SupervisorJob())

    /** Called when the first holder obtains this instance */
    override fun onFirstRef() {}

    /** Called when the last holder releases this instance (before dispose) */
    override fun onLastRef() {}

    /** Called when the registry decides to permanently dispose this instance */
    open fun onDisposeImpl() {}

    /** Public dispose invoked by registry; idempotent */
    override fun dispose() {
        if (disposed) return
        disposed = true

        try {
            onDisposeImpl()
        } catch (_: Throwable) {
            // Ignore disposal errors
        }

        try {
            sharedScope.cancel()
        } catch (_: Throwable) {
            // Ignore scope cancellation errors
        }

        // ViewModel.clear() is internal, let the system handle cleanup
        // onCleared() will be called automatically
    }

    /**
     * Get the shared coroutine scope for background operations
     * This scope is managed by the SharedViewModel lifecycle
     */
    fun getSharedScope(): CoroutineScope = sharedScope

    override fun onCleared() {
        super.onCleared()
        dispose()
    }
}
