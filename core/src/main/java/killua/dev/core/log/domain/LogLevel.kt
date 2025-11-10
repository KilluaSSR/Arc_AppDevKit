package killua.dev.core.log.domain

import androidx.compose.ui.graphics.Color
import logcat.LogPriority

enum class LogLevel(val priority: String, val value: Int, val displayName: String) {
    VERBOSE("V", 2, "Verbose"),
    DEBUG("D", 3, "Debug"),
    INFO("I", 4, "Info"),
    WARN("W", 5, "Warning"),
    ERROR("E", 6, "Error"),
    ASSERT("A", 7, "Assert"),
    ALL("ALL", 1, "All");

    // 获取颜色配置
    fun getColors(): Pair<Color, Color> = when (this) {
        VERBOSE -> Color.Gray to Color(0xFFF5F5F5)
        DEBUG -> Color(0xFF2196F3) to Color(0xFFE3F2FD)
        INFO -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
        WARN -> Color(0xFFFF9800) to Color(0xFFFFF3E0)
        ERROR -> Color(0xFFF44336) to Color(0xFFFFEBEE)
        ASSERT -> Color(0xFF9C27B0) to Color(0xFFF3E5F5)
        ALL -> Color.Gray to Color(0xFFF5F5F5)
    }

    // 获取背景色
    fun getBackgroundColor(): Color = getColors().second

    // 获取前景色
    fun getForegroundColor(): Color = getColors().first

    companion object {
        fun fromPriority(priority: String): LogLevel =
            LogLevel.entries.find { it.priority == priority } ?: VERBOSE

        fun fromLogPriority(logPriority: LogPriority): LogLevel = when (logPriority) {
            LogPriority.VERBOSE -> VERBOSE
            LogPriority.DEBUG -> DEBUG
            LogPriority.INFO -> INFO
            LogPriority.WARN -> WARN
            LogPriority.ERROR -> ERROR
            LogPriority.ASSERT -> ASSERT
        }

        fun getAllPriorities(): List<String> = values().map { it.priority }

        fun getBackgroundColor(priority: String): Color =
            fromPriority(priority).getBackgroundColor()

        fun getForegroundColor(priority: String): Color =
            fromPriority(priority).getForegroundColor()

        fun getColorPair(priority: String): Pair<Color, Color> =
            fromPriority(priority).getColors()
    }
}