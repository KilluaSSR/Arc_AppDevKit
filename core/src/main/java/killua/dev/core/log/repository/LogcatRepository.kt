package killua.dev.core.log.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import killua.dev.core.log.domain.LogEntry
import killua.dev.core.log.domain.LogExportFormat
import killua.dev.core.log.domain.LogFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@RequiresApi(Build.VERSION_CODES.O)
class LogcatRepository(
    private val context: Context
) : LogRepository {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    private val _logCount = MutableStateFlow(0)
    private val mutex = Mutex()
    private var captureJob: Job? = null
    private var process: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val maxLogs = 10000

    private val packageName = context.packageName

    override fun getLogs(filter: LogFilter): Flow<List<LogEntry>> {
        return _logs.asStateFlow().map { logs ->
            logs.filter { filter.matches(it) }
        }
    }

    override suspend fun exportLogs(
        logs: List<LogEntry>,
        format: LogExportFormat,
        outputPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(outputPath)
            file.parentFile?.mkdirs()

            when (format) {
                LogExportFormat.TXT -> exportAsTxt(logs, file)
                LogExportFormat.CSV -> exportAsCsv(logs, file)
                LogExportFormat.JSON -> exportAsJson(logs, file)
                LogExportFormat.HTML -> exportAsHtml(logs, file)
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearLogs() = mutex.withLock {
        _logs.value = emptyList()
        _logCount.value = 0
    }

    override fun getLogCount(): Flow<Int> = _logCount.asStateFlow()

    fun startCapture(minPriority: String = "V") {
        if (captureJob?.isActive == true) return

        captureJob = scope.launch {
            try {
                val command = buildList {
                    add("logcat")
                    add("-v")
                    add("time")
                    add("--pid=${android.os.Process.myPid()}")
                    add("*:$minPriority")
                }

                process = Runtime.getRuntime().exec(command.toTypedArray())
                val reader = BufferedReader(InputStreamReader(process!!.inputStream))

                while (isActive) {
                    val line = reader.readLine() ?: break
                    parseAndAddLog(line)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        process?.destroy()
        process = null
    }

    private suspend fun parseAndAddLog(line: String) = mutex.withLock {
        val logEntry = LogEntry.parseFromLogcatLine(line) ?: return@withLock

        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(logEntry)

        if (currentLogs.size > maxLogs) {
            currentLogs.removeAt(0)
        }

        _logs.value = currentLogs
        _logCount.value = currentLogs.size
    }

    private fun exportAsTxt(logs: List<LogEntry>, file: File) {
        file.writeText(logs.joinToString("\n") { "${it.displayTimestamp} ${it.level.priority}/${it.tag}: ${it.message}" })
    }

    private fun exportAsCsv(logs: List<LogEntry>, file: File) {
        val header = "Timestamp,Level,Tag,Message,ProcessId\n"
        val content = logs.joinToString("\n") { log ->
            "${log.displayTimestamp},${log.level.priority},${log.tag},${log.message.replace(",", ";")},${log.processId}"
        }
        file.writeText(header + content)
    }

    private fun exportAsJson(logs: List<LogEntry>, file: File) {
        val jsonContent = logs.joinToString(",\n") { log ->
            """{
                "timestamp": "${log.displayTimestamp}",
                "level": "${log.level.priority}",
                "tag": "${log.tag}",
                "message": "${log.message.replace("\"", "\\\"")}",
                "processId": ${log.processId}
            }"""
        }
        file.writeText("[$jsonContent]")
    }

    private fun exportAsHtml(logs: List<LogEntry>, file: File) {
        val htmlContent = buildString {
            appendLine("<html><head><title>Log Export</title>")
            appendLine("<style>")
            appendLine("body { font-family: monospace; }")
            appendLine(".verbose { color: gray; }")
            appendLine(".debug { color: blue; }")
            appendLine(".info { color: green; }")
            appendLine(".warn { color: orange; }")
            appendLine(".error { color: red; }")
            appendLine("</style></head><body>")
            appendLine("<h1>Log Export</h1>")

            logs.forEach { log ->
                appendLine("<div class=\"${log.level.priority.lowercase()}\">")
                appendLine("<b>${log.displayTimestamp}</b> <b>${log.level.priority}/${log.tag}:</b><br>")
                appendLine(log.message.replace("<", "&lt;").replace(">", "&gt;"))
                appendLine("<br></div>")
            }

            appendLine("</body></html>")
        }
        file.writeText(htmlContent)
    }

    fun isCapturing(): Boolean = captureJob?.isActive == true
}