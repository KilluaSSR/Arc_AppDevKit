package killua.dev.core.data.cache

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import logcat.LogPriority
import logcat.logcat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for automatic cache cleanup using WorkManager
 */
@Singleton
class CacheCleanupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    private val TAG = "CacheCleanupScheduler"
    /**
     * Schedule periodic cache cleanup
     * 
     * @param intervalMillis Cleanup interval in milliseconds
     * @param requiresCharging Whether to run only when device is charging
     * @param requiresDeviceIdle Whether to run only when device is idle
     */
    fun scheduleCleanup(
        intervalMillis: Long,
        requiresCharging: Boolean = false,
        requiresDeviceIdle: Boolean = false
    ) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(requiresCharging)
            .setRequiresDeviceIdle(requiresDeviceIdle)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
            intervalMillis,
            TimeUnit.MILLISECONDS
        )
            .setConstraints(constraints)
            .setInitialDelay(intervalMillis, TimeUnit.MILLISECONDS)
            .addTag(CacheCleanupWorker.WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            CacheCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )

        logcat(TAG, LogPriority.INFO) {
            "[Scheduler] ✓ Cache cleanup scheduled: interval=${intervalMillis / 1000 / 60}min, charging=$requiresCharging, idle=$requiresDeviceIdle"
        }
    }

    /**
     * Cancel scheduled cleanup
     */
    fun cancelCleanup() {
        workManager.cancelUniqueWork(CacheCleanupWorker.WORK_NAME)
        logcat(TAG, LogPriority.INFO) {
            "[Scheduler] Cache cleanup task cancelled"
        }
    }

    /**
     * Trigger immediate cleanup (one-time)
     */
    fun triggerImmediateCleanup() {
        val cleanupRequest = OneTimeWorkRequestBuilder<CacheCleanupWorker>()
            .addTag(CacheCleanupWorker.WORK_NAME)
            .build()

        workManager.enqueue(cleanupRequest)
        
        logcat(TAG, LogPriority.INFO) {
            "[Scheduler] Immediate cleanup triggered"
        }
    }

    /**
     * Get cleanup work info
     */
    fun getCleanupWorkInfo() = workManager.getWorkInfosForUniqueWorkLiveData(
        CacheCleanupWorker.WORK_NAME
    )
}
