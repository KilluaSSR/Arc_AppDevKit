package killua.dev.core.init

import killua.dev.core.states.CurrentState
import killua.dev.core.viewmodel.UIEffect
import killua.dev.core.viewmodel.UIIntent
import killua.dev.core.viewmodel.UIState


data class StartupState(
    val currentState: CurrentState = CurrentState.Idle,
    val progress: Float = 0f,
    val currentTask: String = "",
    val consentFormLoaded: Boolean = false,
    val permissionsGranted: Boolean = false,
    val initializationComplete: Boolean = false,
    val error: StartupError? = null,
    val completedTasks: List<String> = emptyList()
) : UIState {
    
    val derivedCurrentState: CurrentState
        get() = when {
            error != null -> CurrentState.Error
            initializationComplete -> CurrentState.Success
            progress > 0f -> CurrentState.Processing
            else -> CurrentState.Idle
        }
    
    val canContinue: Boolean
        get() = consentFormLoaded && 
                permissionsGranted && 
                initializationComplete && 
                error == null
}


sealed interface StartupIntent : UIIntent {
    data object Initialize : StartupIntent
    data object Continue : StartupIntent
    data object Retry : StartupIntent
    data class PermissionsResult(val granted: Map<String, Boolean>) : StartupIntent
    data object ConsentFormCompleted : StartupIntent
    data object SkipConsent : StartupIntent
}


sealed interface StartupEffect : UIEffect {
    data object NavigateToMain : StartupEffect
    data class RequestPermissions(val permissions: Array<String>) : StartupEffect {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as RequestPermissions
            return permissions.contentEquals(other.permissions)
        }
        
        override fun hashCode(): Int = permissions.contentHashCode()
    }
    data object ShowConsentForm : StartupEffect
    data class ShowError(val error: StartupError, val canRetry: Boolean = true) : StartupEffect
}


sealed class StartupError {
    data class InitializationFailed(val message: String, val cause: Throwable? = null) : StartupError()
    data class PermissionDenied(val permissions: List<String>) : StartupError()
    data class ConsentRequired(val message: String) : StartupError()
    data class NetworkError(val message: String) : StartupError()
    data object Unknown : StartupError()
    
    fun toMessage(): String = when (this) {
        is InitializationFailed -> "Initialization failed: $message"
        is PermissionDenied -> "Required permissions denied: ${permissions.joinToString()}"
        is ConsentRequired -> message
        is NetworkError -> "Network error: $message"
        Unknown -> "An unknown error occurred"
    }
}
