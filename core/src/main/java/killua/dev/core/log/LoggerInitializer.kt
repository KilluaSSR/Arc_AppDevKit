package killua.dev.core.log

import android.app.Application
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LoggerInitializerEntryPoint {
    fun logcatCaptureService(): LogcatCaptureService
}

object LoggerInitializer {

    @Volatile
    private var isInitialized = false
    private val initializationLock = Any()

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
            android.util.Log.i("LoggerInitializer", "LogcatLogger installed with AndroidLogcatLogger")
        } else if (shouldInstall && LogcatLogger.isInstalled) {
            if (LogcatLogger.loggers.none { it is AndroidLogcatLogger }) {
                LogcatLogger.loggers += AndroidLogcatLogger(config.minPriority)
                android.util.Log.i("LoggerInitializer", "AndroidLogcatLogger added to existing LogcatLogger")
            }
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
            android.util.Log.i("LoggerInitializer", "In-app log viewer initialized successfully")
        } catch (e: Exception) {
            android.util.Log.w("LoggerInitializer", "Failed to get service from Hilt, using proxy", e)
            LogcatCaptureServiceProxy.getInstance(application).installLogger()
        }
    }

    private fun isDebuggable(application: Application): Boolean {
        return application.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }


    fun isInitialized(): Boolean = isInitialized

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
        val DEFAULT = LogConfig(
            autoStartCapture = true
        )
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
            autoStartCapture = true
        )
    }
}
