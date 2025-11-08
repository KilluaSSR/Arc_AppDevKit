package killua.dev.core.log.repository

import killua.dev.core.log.domain.LogEntry
import killua.dev.core.log.domain.LogExportFormat
import killua.dev.core.log.domain.LogFilter
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getLogs(filter: LogFilter = LogFilter.ALL): Flow<List<LogEntry>>
    suspend fun exportLogs(
        logs: List<LogEntry>,
        format: LogExportFormat,
        outputPath: String
    ): Result<String>
    suspend fun clearLogs()
    fun getLogCount(): Flow<Int>
}