package killua.dev.core.init

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat

class HiltStartupInitializer : Initializer<HiltStartupRunner> {
    
    companion object {
        private const val TAG = "HiltStartup"
    }
    
    override fun create(context: Context): HiltStartupRunner {
        logcat(TAG, LogPriority.INFO) { "Initializing with DI" }
        
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HiltStartupEntryPoint::class.java
        )
        
        val runner = HiltStartupRunner(
            context = context,
            orchestrator = entryPoint.startupOrchestrator(),
            config = entryPoint.startupConfig()
        )
        
        // Execute background tasks
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            runner.executeBackgroundTasks()
        }
        
        return runner
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltStartupEntryPoint {
    fun startupOrchestrator(): StartupOrchestrator
    fun startupConfig(): StartupConfig
}


class HiltStartupRunner(
    private val context: Context,
    private val orchestrator: StartupOrchestrator,
    private val config: StartupConfig
) {
    private val completedTasks = mutableSetOf<String>()
    
    companion object {
        private const val TAG = "HiltStartupRunner"
    }
    
    suspend fun executeBackgroundTasks() {
        logcat(TAG, LogPriority.INFO) { "Executing background tasks from config" }
        
        val backgroundTasks = config.tasks.filter { !it.requiresUI }
        
        if (backgroundTasks.isEmpty()) {
            logcat(TAG, LogPriority.INFO) { "No background tasks to execute" }
            return
        }
        
        logcat(TAG, LogPriority.INFO) {
            "Found ${backgroundTasks.size} background tasks: ${backgroundTasks.joinToString { it.name }}"
        }
        
        val result = orchestrator.executeTasks(
            context = context,
            tasks = backgroundTasks,
            onProgress = { progress, taskName ->
                logcat(TAG, LogPriority.DEBUG) {
                    "Background progress: ${(progress * 100).toInt()}% - $taskName"
                }
            }
        )
        
        when (result) {
            is OrchestratorResult.Success -> {
                completedTasks.addAll(result.completedTasks)
                logcat(TAG, LogPriority.INFO) {
                    "Background tasks completed: ${result.completedTasks.joinToString()}"
                }
            }
            is OrchestratorResult.Failure -> {
                logcat(TAG, LogPriority.ERROR) {
                    "Background task failed: ${result.failedTask}, error=${result.error.message}"
                }
            }
        }
    }
    
    fun isTaskCompleted(taskName: String): Boolean {
        return completedTasks.contains(taskName)
    }
    
    fun getCompletedTasks(): Set<String> = completedTasks.toSet()
}
