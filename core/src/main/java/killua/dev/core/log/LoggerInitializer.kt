package killua.dev.core.log

import android.app.Application
import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import killua.dev.core.log.domain.LogLevel
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LoggerInitializerEntryPoint {
    fun logcatCaptureService(): LogcatCaptureService
}

/**
 * 日志初始化
 * 提供安全的生产环境日志管理
 */
object LoggerInitializer {

    @Volatile
    private var isInitialized = false
    private val initializationLock = Any()

    /**
     * 安全初始化日志系统
     *
     * @param application Application 实例
     * @param config 日志配置
     */
    fun initialize(
        application: Application,
        config: LogConfig = LogConfig.DEFAULT
    ) {
        if (isInitialized) return

        synchronized(initializationLock) {
            if (isInitialized) return

            try {
                setupSystemLogging(application, config)

                if (config.enableInAppViewer) {
                    setupInAppViewer(application, config)
                }

                isInitialized = true
            } catch (e: Exception) {
                // 初始化失败时至少保证系统日志可用
                AndroidLogcatLogger.installOnDebuggableApp(application, LogPriority.ERROR)
            }
        }
    }

    private fun setupSystemLogging(
        application: Application,
        config: LogConfig
    ) {
        val shouldInstall = when {
            config.enableInRelease -> true
            isDebuggable(application) -> true
            else -> false
        }

        if (shouldInstall && !LogcatLogger.isInstalled) {
            LogcatLogger.install()
            LogcatLogger.loggers += AndroidLogcatLogger(config.minPriority)
        } else if (shouldInstall) {
            AndroidLogcatLogger.installOnDebuggableApp(application, config.minPriority)
        }
    }

    private fun setupInAppViewer(
        application: Application,
        config: LogConfig
    ) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                application,
                LoggerInitializerEntryPoint::class.java
            )
            entryPoint.logcatCaptureService().installLogger()

          if (config.autoStartCapture) {
                val priorityString = LogLevel.fromLogPriority(config.minPriority).priority
                entryPoint.logcatCaptureService().startCapture(priorityString)
            }
        } catch (e: Exception) {
            // Hilt未初始化时的备用方案
            LogcatCaptureServiceProxy.getInstance(application).installLogger()
        }
    }

    private fun isDebuggable(application: Application): Boolean {
        return application.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    /**
     * 获取初始化状态
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * 安全停止日志捕获
     */
    fun safeStopCapture(context: Context) {
        try {
            LogcatCaptureServiceProxy.getInstance(context).stopCapture()
        } catch (e: Exception) {
            // 忽略停止失败
        }
    }
}

/**
 * 日志配置类
 */
data class LogConfig(
    val minPriority: LogPriority = LogPriority.VERBOSE,
    val enableInAppViewer: Boolean = true,
    val enableInRelease: Boolean = false,
    val autoStartCapture: Boolean = true,
    val maxLogEntries: Int = 10000,
    val enableExport: Boolean = true
) {
    companion object {
        val DEFAULT = LogConfig()
        val DEBUG = LogConfig(
            enableInRelease = true,
            autoStartCapture = true
        )
        val PRODUCTION = LogConfig(
            minPriority = LogPriority.INFO,
            enableInAppViewer = false,
            enableInRelease = false,
            autoStartCapture = false
        )
        val PRODUCTION_WITH_VIEWER = LogConfig(
            minPriority = LogPriority.INFO,
            enableInAppViewer = true,
            enableInRelease = false,
            autoStartCapture = false
        )
    }
}
