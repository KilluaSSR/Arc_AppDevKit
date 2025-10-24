package killua.dev.core.init

import android.content.Context
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates startup tasks execution
 */
@Singleton
class StartupOrchestrator @Inject constructor(
    private val taskMonitor: TaskMonitor = LoggingTaskMonitor()
) {
    
    companion object {
        private const val TAG = "StartupOrchestrator"
    }
    
    suspend fun executeTasks(
        context: Context,
        tasks: List<StartupTask>,
        onProgress: ((Float, String) -> Unit)? = null
    ): OrchestratorResult {
        logcat(TAG, LogPriority.INFO) { 
            "Starting initialization with ${tasks.size} tasks" 
        }
        
        val sortedTasks = tasks.sortedByDescending { it.priority }
        val totalTasks = sortedTasks.size
        val completedTasks = mutableListOf<String>()
        
        sortedTasks.forEachIndexed { index, task ->
            val currentProgress = index.toFloat() / totalTasks
            onProgress?.invoke(currentProgress, task.name)
            
            taskMonitor.onTaskStarted(task)
            
            val result = try {
                task.execute(context)
            } catch (e: Exception) {
                logcat(TAG, LogPriority.ERROR) { 
                    "Task crashed: ${task.name}, exception=${e.message}" 
                }
                TaskResult.Failure(e)
            }
            
            taskMonitor.onTaskCompleted(task, result)
            
            when (result) {
                is TaskResult.Success -> {
                    completedTasks.add(task.name)
                }
                is TaskResult.Failure -> {
                    if (task.isRequired) {
                        logcat(TAG, LogPriority.ERROR) { 
                            "Required task failed, aborting initialization" 
                        }
                        return OrchestratorResult.Failure(
                            failedTask = task.name,
                            error = result.error,
                            canRetry = result.canRetry
                        )
                    }
                }
                is TaskResult.Skipped -> {}
            }
            
            val progress = (index + 1).toFloat() / totalTasks
            onProgress?.invoke(progress, task.name)
            taskMonitor.onProgress(progress, task.name)
        }
        
        logcat(TAG, LogPriority.INFO) { "All tasks completed successfully" }
        onProgress?.invoke(1.0f, "Completed")
        
        return OrchestratorResult.Success(
            completedTasks = completedTasks
        )
    }
}

sealed class OrchestratorResult {
    data class Success(val completedTasks: List<String>) : OrchestratorResult()
    data class Failure(
        val failedTask: String,
        val error: Throwable,
        val canRetry: Boolean
    ) : OrchestratorResult()
}
