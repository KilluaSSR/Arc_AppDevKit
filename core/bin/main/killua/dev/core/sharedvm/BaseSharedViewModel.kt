package killua.dev.core.sharedvm

import killua.dev.core.viewmodel.BaseViewModel
import killua.dev.core.viewmodel.UIEffect
import killua.dev.core.viewmodel.UIIntent
import killua.dev.core.viewmodel.UIState
import kotlinx.coroutines.CoroutineScope

/**
 * Shared ViewModel base class. Extends project's BaseViewModel.
 * Subclasses may override lifecycle hooks: onFirstRef/onLastRef/onDispose.
 * Important: do NOT hold strong references to Activity/Views. Use Application-level
 * resources when needed.
 */
abstract class BaseSharedViewModel<I : UIIntent, S : UIState, E : UIEffect>(state: S) :
    BaseViewModel<I, S, E>(state) {

    private var disposed = false

    /** Called when the first holder obtains this instance */
    open fun onFirstRef() {}

    /** Called when the last holder releases this instance (before dispose) */
    open fun onLastRef() {}

    /** Called when the registry decides to permanently dispose this instance */
    open fun onDispose() {}

    /** Public dispose invoked by registry; idempotent */
    fun dispose() {
        if (disposed) return
        disposed = true
        try {
            onDispose()
        } catch (_: Throwable) {
        }
    }
}
