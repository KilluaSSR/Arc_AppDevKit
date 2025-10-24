package killua.dev.core.log

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.LogcatLogger
import java.io.BufferedReader
import java.io.InputStreamReader

data class LogEntry(
    val timestamp: String,
    val priority: String,
    val tag: String,
    val message: String,
    val fullLog: String
)


object LogcatCaptureService {
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()
    
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val maxLogs = 50000
    
    private var process: Process? = null
    
    /**
     * 开始捕获日志
     * @param filterTag 可选的标签过滤器,只捕获特定标签的日志
     * @param filterPriority 最小优先级过滤 (V, D, I, W, E, A)
     */
    fun startCapture(
        filterTag: String? = null,
        filterPriority: String = "V"
    ) {
        if (captureJob?.isActive == true) {
            Log.w("LogcatCapture", "LogcatCapture is Running!")
            return
        }
        
        captureJob = scope.launch {
            try {
                val command = buildList {
                    add("logcat")
                    add("-v") 
                    add("time")
                    add("*:$filterPriority")
                    if (filterTag != null) {
                        add("$filterTag:$filterPriority")
                    }
                }
                
                process = Runtime.getRuntime().exec(command.toTypedArray())
                val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                
                val logList = mutableListOf<LogEntry>()
                
                while (isActive) {
                    val line = reader.readLine() ?: break
                    
                    val logEntry = parseLogLine(line)
                    if (logEntry != null) {
                        logList.add(logEntry)
                        
                        if (logList.size > maxLogs) {
                            logList.removeAt(0)
                        }
                        
                        _logs.value = logList.toList()
                    }
                }
            } catch (e: Exception) {
                Log.e("LogcatCapture", "LogcatCapture ERROR", e)
            }
        }
    }
    
    /**
     * 停止捕获日志
     */
    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        process?.destroy()
        process = null
    }
    
    /**
     * 清除已捕获的日志
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }
    

    private fun parseLogLine(line: String): LogEntry? {
        try {
            val regex = """^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+([VDIWEA])/(.+?)\(\s*\d+\):\s+(.*)$""".toRegex()
            val match = regex.find(line) ?: return null
            
            val (timestamp, priority, tag, message) = match.destructured
            
            return LogEntry(
                timestamp = timestamp,
                priority = priority,
                tag = tag.trim(),
                message = message,
                fullLog = line
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 获取当前捕获的日志数量
     */
    fun getLogCount(): Int = _logs.value.size
    
    /**
     * 根据标签筛选日志
     */
    fun filterByTag(tag: String): List<LogEntry> {
        return _logs.value.filter { it.tag.contains(tag, ignoreCase = true) }
    }
    
    /**
     * 根据优先级筛选日志
     */
    fun filterByPriority(priority: String): List<LogEntry> {
        return _logs.value.filter { it.priority == priority }
    }
    
    /**
     * 搜索日志内容
     */
    fun searchLogs(query: String): List<LogEntry> {
        return _logs.value.filter { 
            it.message.contains(query, ignoreCase = true) ||
            it.tag.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * 安装自定义的 Logcat Logger
     */
    fun installLogger() {
        LogcatLogger.install(object : LogcatLogger {
            override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

            override fun log(priority: LogPriority, tag: String, message: String) {
                when (priority) {
                    LogPriority.VERBOSE -> Log.v(tag, message)
                    LogPriority.DEBUG -> Log.d(tag, message)
                    LogPriority.INFO -> Log.i(tag, message)
                    LogPriority.WARN -> Log.w(tag, message)
                    LogPriority.ERROR -> Log.e(tag, message)
                    LogPriority.ASSERT -> Log.wtf(tag, message)
                }
            }
        })
    }
    
    /**
     * 是否正在捕获
     */
    fun isCapturing(): Boolean = captureJob?.isActive == true
}
