package killua.dev.core.init

import android.content.Context
import androidx.startup.Initializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat

class StartupTaskInitializer : Initializer<StartupTaskRunner> {
    
    override fun create(context: Context): StartupTaskRunner {
        logcat("StartupTaskInitializer", LogPriority.INFO) {
            "[Startup] Jetpack Startup: Initializing app components"
        }
        
        val runner = StartupTaskRunner(context)
        
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

/**
 * Runner for background startup tasks
 * Separate from UI-based startup flow (permissions, consent)
 */
class StartupTaskRunner(
    private val context: Context
) {
    private val completedTasks = mutableSetOf<String>()
    
    /**
     * Execute background tasks that don't require UI interaction
     * Examples: Database init, cache cleanup, network config
     */
    suspend fun executeBackgroundTasks() {
        logcat("StartupTaskRunner", LogPriority.INFO) {
            "[Startup] Executing background initialization tasks"
        }
        
        // Get tasks from DI would require Hilt integration here
        // For now, execute common non-UI tasks
        
        val tasks = getBackgroundTasks()
        val orchestrator = StartupOrchestrator()
        
        val result = orchestrator.executeTasks(
            context = context,
            tasks = tasks,
            onProgress = { progress, taskName ->
                logcat("StartupTaskRunner", LogPriority.DEBUG) {
                    "[Startup] Progress: ${(progress * 100).toInt()}% - $taskName"
                }
            }
        )
        
        when (result) {
            is OrchestratorResult.Success -> {
                completedTasks.addAll(result.completedTasks)
                logcat("StartupTaskRunner", LogPriority.INFO) {
                    "[Startup] ✓ Background initialization completed: ${result.completedTasks.size} tasks"
                }
            }
            is OrchestratorResult.Failure -> {
                logcat("StartupTaskRunner", LogPriority.ERROR) {
                    "[Startup] ✗ Background initialization failed: ${result.failedTask}"
                }
            }
        }
    }
    
    /**
     * Get background tasks that don't require user interaction
     * Override this in app module to provide custom tasks
     */
    protected open fun getBackgroundTasks(): List<StartupTask> {
        return emptyList()
    }
    
    fun isTaskCompleted(taskName: String): Boolean {
        return completedTasks.contains(taskName)
    }
}
