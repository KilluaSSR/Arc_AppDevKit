package killua.dev.core.init

import android.content.Context
import kotlinx.coroutines.async
import logcat.LogPriority
import logcat.logcat


interface StartupTask {
    val name: String
    val priority: Int get() = 0
    val isRequired: Boolean get() = true

    val requiresUI: Boolean get() = false
    
    suspend fun execute(context: Context): TaskResult
}

sealed class TaskResult {
    data object Success : TaskResult()
    data class Failure(val error: Throwable, val canRetry: Boolean = true) : TaskResult()
    data class Skipped(val reason: String) : TaskResult()
}

interface TaskMonitor {
    fun onTaskStarted(task: StartupTask)
    fun onTaskCompleted(task: StartupTask, result: TaskResult)
    fun onProgress(progress: Float, currentTask: String)
}

class LoggingTaskMonitor : TaskMonitor {
    
    companion object {
        private const val TAG = "StartupTask"
    }
    
    override fun onTaskStarted(task: StartupTask) {
        logcat(TAG, LogPriority.INFO) { 
            "Starting task: ${task.name} (priority=${task.priority})" 
        }
    }
    
    override fun onTaskCompleted(task: StartupTask, result: TaskResult) {
        when (result) {
            is TaskResult.Success -> {
                logcat(TAG, LogPriority.INFO) { 
                    "Task completed: ${task.name}" 
                }
            }
            is TaskResult.Failure -> {
                logcat(TAG, LogPriority.ERROR) { 
                    "Task failed: ${task.name}, error=${result.error.message}" 
                }
            }
            is TaskResult.Skipped -> {
                logcat(TAG, LogPriority.WARN) { 
                    "Task skipped: ${task.name}, reason=${result.reason}" 
                }
            }
        }
    }
    
    override fun onProgress(progress: Float, currentTask: String) {
        logcat(TAG, LogPriority.DEBUG) { 
            "Progress: ${(progress * 100).toInt()}%, current=$currentTask" 
        }
    }
}

class CompositeStartupTask(
    override val name: String = "CompositeTask",
    private val tasks: List<StartupTask>
) : StartupTask {
    
    override suspend fun execute(context: Context): TaskResult {
        tasks.forEach { task ->
            val result = task.execute(context)
            if (result is TaskResult.Failure && task.isRequired) {
                return result
            }
        }
        return TaskResult.Success
    }
}

class ParallelStartupTask(
    override val name: String = "ParallelTask",
    private val tasks: List<StartupTask>
) : StartupTask {
    
    override suspend fun execute(context: Context): TaskResult {
        return kotlinx.coroutines.coroutineScope {
            val deferredResults = tasks.map { task ->
                async { task.execute(context) }
            }
            val results = deferredResults.map { it.await() }
            val failures = results.filterIsInstance<TaskResult.Failure>()
            
            if (failures.isNotEmpty()) {
                failures.first()
            } else {
                TaskResult.Success
            }
        }
    }
}
