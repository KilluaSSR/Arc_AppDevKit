package killua.dev.core.log

import logcat.LogPriority
import logcat.logcat

/**
 * 便捷的日志扩展函数
 * 使用示例:
 * ```
 * logd("MyTag") { "Debug message" }
 * logi("MyTag") { "Info message" }
 * loge("MyTag") { "Error message" }
 * ```
 */

inline fun logv(tag: String = "App", message: () -> String) {
    logcat(tag, LogPriority.VERBOSE, message)
}

inline fun logd(tag: String = "App", message: () -> String) {
    logcat(tag, LogPriority.DEBUG, message)
}

inline fun logi(tag: String = "App", message: () -> String) {
    logcat(tag, LogPriority.INFO, message)
}

inline fun logw(tag: String = "App", message: () -> String) {
    logcat(tag, LogPriority.WARN, message)
}

inline fun loge(tag: String = "App", message: () -> String) {
    logcat(tag, LogPriority.ERROR, message)
}

inline fun logwtf(tag: String = "App", message: () -> String) {
    logcat(tag, LogPriority.ASSERT, message)
}

/**
 * 带异常的日志扩展
 */
inline fun loge(tag: String = "App", throwable: Throwable? = null, message: () -> String) {
    logcat(tag, LogPriority.ERROR) {
        if (throwable != null) {
            "${message()}\n${throwable.stackTraceToString()}"
        } else {
            message()
        }
    }
}

inline fun logw(tag: String = "App", throwable: Throwable? = null, message: () -> String) {
    logcat(tag, LogPriority.WARN) {
        if (throwable != null) {
            "${message()}\n${throwable.stackTraceToString()}"
        } else {
            message()
        }
    }
}
