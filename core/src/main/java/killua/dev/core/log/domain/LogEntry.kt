package killua.dev.core.log.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LogEntry(
    val timestamp: LocalDateTime,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val processId: Int,
    val threadId: Int,
    val packageName: String? = null
) {
    val displayTimestamp: String

        get() = timestamp.format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS"))

    val priority: String
        get() = level.priority

    val fullLog: String

        get() = "$displayTimestamp $priority/$tag($processId): $message"

    companion object {

        fun parseFromLogcatLine(line: String): LogEntry? {
            return try {
                // 匹配 logcat -v time 格式: MM-DD HH:MM:SS.mmm I/TAG(PID): message
                // 或者 MM-DD HH:MM:SS.mmm I/TAG( PID): message (带空格)
                val regex = """^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+([VDIWEA])/(.+?)\(\s*(\d+)\):\s+(.*)$""".toRegex()
                val match = regex.find(line) ?: return null

                val (timestamp, priority, tag, processId, message) = match.destructured

                val currentYear = java.time.Year.now().value
                val dateTime = LocalDateTime.parse(
                    "$currentYear-$timestamp",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                )

                LogEntry(
                    timestamp = dateTime,
                    level = LogLevel.fromPriority(priority),
                    tag = tag.trim(),
                    message = message,
                    processId = processId.toInt(),
                    threadId = 0
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}