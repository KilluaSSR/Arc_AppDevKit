package killua.dev.core.log

import android.content.Context
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import killua.dev.core.log.domain.LogEntry
import killua.dev.core.log.domain.LogFilter
import killua.dev.core.log.domain.LogLevel
import killua.dev.core.log.repository.LogRepository
import killua.dev.core.log.service.LogExportService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.LogcatLogger
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LogModuleEntryPoint {
    fun logRepository(): LogRepository
    fun logExportService(): LogExportService
}

@Singleton
class LogcatCaptureService @Inject constructor(
    private val logRepository: LogRepository,
    private val logExportService: LogExportService
) {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null

    val logs: StateFlow<List<LogEntry>>
        get() = logRepository.getLogs().stateIn(
            scope = scope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startCapture(minPriority: String = "V") {
        if (captureJob?.isActive == true) return

        captureJob = scope.launch {
            try {
                when (val repo = logRepository) {
                    is killua.dev.core.log.repository.LogcatRepository -> {
                        repo.startCapture(minPriority)
                    }
                }
            } catch (e: Exception) {
                Log.e("LogcatCapture", "Failed to start capture", e)
                delay(5000)
            }
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        when (val repo = logRepository) {
            is killua.dev.core.log.repository.LogcatRepository -> {
                repo.stopCapture()
            }
        }
    }

    suspend fun clearLogs() = logRepository.clearLogs()

    suspend fun exportLogs(
        format: killua.dev.core.log.domain.LogExportFormat,
        share: Boolean = true
    ) = logExportService.exportAndShare(
        logs.value,
        format,
        share
    )

    fun getFilteredLogs(filter: LogFilter): Flow<List<LogEntry>> =
        logRepository.getLogs(filter)

    fun isCapturing(): Boolean = captureJob?.isActive == true

    fun installLogger() {
        if (!LogcatLogger.isInstalled) {
            LogcatLogger.install()
        }

        // 添加自定义日志处理器，使用集中化的priority逻辑
        LogcatLogger.loggers += object : LogcatLogger {
            override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

            override fun log(priority: LogPriority, tag: String, message: String) {
                val level = LogLevel.fromLogPriority(priority)
                when (level) {
                    LogLevel.VERBOSE -> Log.v(tag, message)
                    LogLevel.DEBUG -> Log.d(tag, message)
                    LogLevel.INFO -> Log.i(tag, message)
                    LogLevel.WARN -> Log.w(tag, message)
                    LogLevel.ERROR -> Log.e(tag, message)
                    LogLevel.ASSERT -> Log.wtf(tag, message)
                    LogLevel.ALL -> Log.d(tag, message)
                }
            }
        }
    }
}

object LogcatCaptureServiceProxy {
    private var instance: LogcatCaptureService? = null

    fun getInstance(context: Context): LogcatCaptureService {
        return instance ?: synchronized(this) {
            instance ?: run {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context,
                    LogModuleEntryPoint::class.java
                )
                LogcatCaptureService(
                    entryPoint.logRepository(),
                    entryPoint.logExportService()
                ).also { instance = it }
            }
        }
    }

    fun clearInstance() {
        instance?.scope?.cancel()
        instance = null
    }
}
