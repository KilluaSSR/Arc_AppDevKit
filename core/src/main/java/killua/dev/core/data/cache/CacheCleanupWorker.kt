package killua.dev.core.data.cache

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import logcat.LogPriority
import logcat.logcat

/**
 * WorkManager worker for automatic cache cleanup
 * Periodically cleans up expired cache entries
 */
@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cacheManager: CacheManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            logcat(WORK_NAME, LogPriority.INFO) {
                "[Cleanup] Starting automatic cache cleanup task" 
            }
            
            val cleanedCount = cacheManager.clearExpired()
            
            logcat(WORK_NAME, LogPriority.INFO) {
                "[Cleanup] ✓ Automatic cleanup completed: removed $cleanedCount expired items" 
            }
            
            Result.success()
        } catch (e: Exception) {
            logcat(WORK_NAME, LogPriority.ERROR) {
                "[Cleanup] ✗ Automatic cleanup failed: error=${e.message}" 
            }
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "cache_cleanup_worker"
    }
}
