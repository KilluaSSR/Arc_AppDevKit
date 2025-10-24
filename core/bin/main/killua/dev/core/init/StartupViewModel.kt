package killua.dev.core.init

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import killua.dev.core.viewmodel.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val startupConfig: StartupConfig,
    private val orchestrator: StartupOrchestrator
) : BaseViewModel<StartupIntent, StartupState, StartupEffect>(
    state = StartupState()
) {
    
    private val _effects = Channel<StartupEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    
    companion object {
        private const val TAG = "StartupViewModel"
    }
    
    init {
        logcat(TAG, LogPriority.INFO) { "$TAG initialized" }
    }
    
    override suspend fun onEvent(state: StartupState, intent: StartupIntent) {
        when (intent) {
            StartupIntent.Initialize -> handleInitialize()
            StartupIntent.Continue -> handleContinue()
            StartupIntent.Retry -> handleRetry()
            is StartupIntent.PermissionsResult -> handlePermissionsResult(intent.granted)
            StartupIntent.ConsentFormCompleted -> handleConsentCompleted()
            StartupIntent.SkipConsent -> handleSkipConsent()
        }
    }
    
    private suspend fun handleInitialize() {
        logcat(TAG, LogPriority.INFO) { "Starting initialization sequence" }
        
        updateState { it.copy(error = null) }
        
        if (startupConfig.permissions.isNotEmpty()) {
            logcat(TAG, LogPriority.DEBUG) { 
                "Requesting ${startupConfig.permissions.size} permissions" 
            }
            val permissionsToRequest = startupConfig.permissions.map { it.permission }.toTypedArray()
            _effects.send(StartupEffect.RequestPermissions(permissionsToRequest))
            return
        }
        
        updateState { it.copy(permissionsGranted = true) }
        continueInitialization()
    }
    
    private suspend fun handlePermissionsResult(granted: Map<String, Boolean>) {
        val deniedPermissions = granted.filter { !it.value }.keys.toList()
        
        if (deniedPermissions.isEmpty()) {
            logcat(TAG, LogPriority.INFO) { "All permissions granted" }
            updateState { it.copy(permissionsGranted = true) }
            continueInitialization()
            return
        }
        
        logcat(TAG, LogPriority.WARN) { 
            "Permissions denied: ${deniedPermissions.joinToString()}" 
        }
        
        val deniedRequiredPermissions = deniedPermissions.filter { permission ->
            startupConfig.permissions.find { it.permission == permission }?.required == true
        }
        
        if (deniedRequiredPermissions.isNotEmpty()) {
            logcat(TAG, LogPriority.ERROR) {
                "Required permissions denied: ${deniedRequiredPermissions.joinToString()}" 
            }
            updateState { 
                it.copy(
                    error = StartupError.PermissionDenied(deniedRequiredPermissions)
                )
            }
            _effects.send(
                StartupEffect.ShowError(
                    StartupError.PermissionDenied(deniedRequiredPermissions),
                    canRetry = true
                )
            )
        } else {
            logcat(TAG, LogPriority.WARN) {
                "Optional permissions denied, continuing with reduced functionality" 
            }
            updateState { it.copy(permissionsGranted = true) }
            continueInitialization()
        }
    }
    
    private suspend fun continueInitialization() {
        if (startupConfig.requiresConsent) {
            logcat(TAG, LogPriority.DEBUG) { "Showing consent form" }
            _effects.send(StartupEffect.ShowConsentForm)
            return
        }
        
        updateState { it.copy(consentFormLoaded = true) }
        executeTasks()
    }
    
    private suspend fun handleConsentCompleted() {
        logcat(TAG, LogPriority.INFO) { "Consent form completed" }
        updateState { it.copy(consentFormLoaded = true) }
        executeTasks()
    }
    
    private suspend fun handleSkipConsent() {
        logcat(TAG, LogPriority.WARN) { "Consent skipped" }
        updateState { it.copy(consentFormLoaded = true) }
        executeTasks()
    }
    
    private suspend fun executeTasks() {
        logcat(TAG, LogPriority.INFO) { "Executing UI-required startup tasks" }
        
        launchOnIO {
            val context = startupConfig.applicationContext

            val uiTasks = startupConfig.tasks.filter { it.requiresUI }
            
            if (uiTasks.isEmpty()) {
                logcat(TAG, LogPriority.INFO) { 
                    "No UI tasks, initialization complete" 
                }
                updateState { 
                    it.copy(
                        initializationComplete = true,
                        progress = 1.0f
                    )
                }
                return@launchOnIO
            }
            
            logcat(TAG, LogPriority.INFO) { 
                "Executing ${uiTasks.size} UI-required tasks: ${uiTasks.joinToString { it.name }}" 
            }
            
            val result = orchestrator.executeTasks(
                context = context,
                tasks = uiTasks,
                onProgress = { progress, taskName ->
                    viewModelScope.launch {
                        updateState { state ->
                            state.copy(
                                progress = progress,
                                currentTask = taskName,
                                completedTasks = if (progress > state.progress) {
                                    state.completedTasks + taskName
                                } else {
                                    state.completedTasks
                                }
                            )
                        }
                    }
                }
            )
            
            when (result) {
                is OrchestratorResult.Success -> {
                    logcat(TAG, LogPriority.INFO) { "Initialization complete" }
                    updateState { 
                        it.copy(
                            initializationComplete = true,
                            completedTasks = result.completedTasks
                        )
                    }
                }
                is OrchestratorResult.Failure -> {
                    logcat(TAG, LogPriority.ERROR) { 
                        "Initialization failed: ${result.failedTask}" 
                    }
                    val error = StartupError.InitializationFailed(
                        message = "Failed at: ${result.failedTask}",
                        cause = result.error
                    )
                    updateState { 
                        it.copy(
                            error = error
                        )
                    }
                    _effects.send(
                        StartupEffect.ShowError(error, canRetry = result.canRetry)
                    )
                }
            }
        }
    }
    
    private suspend fun handleContinue() {
        if (uiState.value.canContinue) {
            logcat(TAG, LogPriority.INFO) { "Navigating to main screen" }
            _effects.send(StartupEffect.NavigateToMain)
        } else {
            logcat(TAG, LogPriority.WARN) { "Cannot continue - conditions not met" }
        }
    }
    
    private suspend fun handleRetry() {
        logcat(TAG, LogPriority.INFO) { "Retrying initialization" }
        updateState { StartupState() }
        handleInitialize()
    }
    
    override suspend fun onEffect(effect: StartupEffect) {
        super.onEffect(effect)
        // Effects are handled by the Activity
    }
}
