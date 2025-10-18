package killua.dev.core.log

import android.app.Application
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger

/**
 * 日志初始化
 * 在 Application 中调用 initialize() 即可
 */
object LoggerInitializer {
    
    private var isInitialized = false
    
    /**
     * 初始化
     * 在 Application.onCreate() 中调用
     * 
     * @param application Application 实例
     * @param minPriority 最小日志优先级,默认 VERBOSE
     * @param enableInAppViewer 是否启用应用内日志查看器,默认 true
     * @param enableInRelease 是否在 release 包中也启用日志,默认 true
     */
    fun initialize(
        application: Application,
        minPriority: LogPriority = LogPriority.VERBOSE,
        enableInAppViewer: Boolean = true,
        enableInRelease: Boolean = true
    ) {
        if (isInitialized) {
            return
        }
        
        if (enableInRelease && !LogcatLogger.isInstalled) {
            LogcatLogger.install()
            LogcatLogger.loggers += AndroidLogcatLogger(minPriority)
        } else {
            AndroidLogcatLogger.installOnDebuggableApp(application, minPriority)
        }
        
        if (enableInAppViewer) {
            LogcatCaptureService.installLogger()
        }
        
        isInitialized = true
    }
    
    /**
     * 是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
}
