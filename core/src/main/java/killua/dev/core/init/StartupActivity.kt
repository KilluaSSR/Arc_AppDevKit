package killua.dev.core.init

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject

@AndroidEntryPoint
abstract class StartupActivity : ComponentActivity() {

    @Inject
    lateinit var startupConfig: StartupConfig
    
    private val viewModel: StartupViewModel by viewModels()
    
    companion object {
        private const val TAG = "StartupActivity"
    }
    
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            logcat(TAG, LogPriority.DEBUG) { 
                "Permission result: ${result.entries.joinToString { "${it.key}=${it.value}" }}" 
            }
            lifecycleScope.launch {
                viewModel.emitIntent(StartupIntent.PermissionsResult(result))
            }
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        logcat(TAG, LogPriority.INFO) { "Activity created" }
        
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    handleEffect(effect)
                }
            }
        }
        
        setupUI()
        
        lifecycleScope.launch {
            viewModel.emitIntent(StartupIntent.Initialize)
        }
    }
    
    private fun handleEffect(effect: StartupEffect) {
        when (effect) {
            StartupEffect.NavigateToMain -> navigateToNext()
            is StartupEffect.RequestPermissions -> requestPermissions(effect.permissions)
            StartupEffect.ShowConsentForm -> showConsentForm()
            is StartupEffect.ShowError -> onStartupError(effect.error, effect.canRetry)
        }
    }
    
    private fun requestPermissions(permissions: Array<String>) {
        logcat(TAG, LogPriority.INFO) { 
            "Requesting permissions: ${permissions.joinToString()}" 
        }
        permissionLauncher.launch(permissions)
    }
    
    private fun showConsentForm() {
        logcat(TAG, LogPriority.INFO) { "Showing consent form" }
        onConsentRequired()
    }
    
    private fun navigateToNext() {
        logcat(TAG, LogPriority.INFO) { "Navigating to next screen" }
        val intent = startupConfig.getNextIntent(this)
        startActivity(intent)
        finish()
    }
    
    protected abstract fun setupUI()
    
    protected abstract fun onConsentRequired()
    
    protected abstract fun onStartupError(error: StartupError, canRetry: Boolean)
    
    protected fun notifyConsentCompleted() {
        lifecycleScope.launch {
            viewModel.emitIntent(StartupIntent.ConsentFormCompleted)
        }
    }
    
    protected fun notifyRetry() {
        lifecycleScope.launch {
            viewModel.emitIntent(StartupIntent.Retry)
        }
    }
}