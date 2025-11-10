package killua.dev.core.log

import android.content.Context
import android.os.Build
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    @Volatile
    private var isLoggerInstalled = false

    val logs: StateFlow<List<LogEntry>> by lazy {
        logRepository.getLogs().stateIn(
            scope = scope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            initialValue = emptyList()
        )
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

    fun isCapturing(): Boolean = LogcatLogger.isInstalled

    fun installLogger() {
        if (isLoggerInstalled) {
            android.util.Log.d("LogcatCapture", "Logger already installed, skipping")
            return
        }

        synchronized(this) {
            if (isLoggerInstalled) return


            if (!LogcatLogger.isInstalled) {
                LogcatLogger.install()
                android.util.Log.i("LogcatCapture", "LogcatLogger installed")
            }

            LogcatLogger.loggers += object : LogcatLogger {
                override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

                override fun log(priority: LogPriority, tag: String, message: String) {
                    // 将 logcat 日志转换并保存到内存 Repository
                    // 注意: 不要在这里再调用 Log.d/Log.v 等,会造成无限循环!
                    // LogcatLogger 已经会将日志输出到系统 logcat

                    scope.launch {

                        val level = LogLevel.fromLogPriority(priority)
                        val logEntry = LogEntry(
                            timestamp = java.time.LocalDateTime.now(),
                            level = level,
                            tag = tag,
                            message = message,
                            processId = android.os.Process.myPid(),
                            threadId = android.os.Process.myTid()
                        )

                        // 添加到 Repository
                        when (val repo = logRepository) {
                            is killua.dev.core.log.repository.LogcatRepository -> {
                                repo.addLogEntry(logEntry)
                            }
                        }
                    }
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