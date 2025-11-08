package killua.dev.core.log.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import killua.dev.core.log.domain.LogEntry
import killua.dev.core.log.domain.LogExportFormat
import killua.dev.core.log.repository.LogRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogExportService(
    private val context: Context,
    private val logRepository: LogRepository
) {

    suspend fun exportAndShare(
        logs: List<LogEntry>,
        format: LogExportFormat,
        shareIntent: Boolean = true
    ): Result<String> {
        val fileName = generateFileName(format)
        val outputFile = File(context.getExternalFilesDir(null), "logs/$fileName")

        return logRepository.exportLogs(logs, format, outputFile.absolutePath).fold(
            onSuccess = { filePath ->
                if (shareIntent) {
                    shareFile(filePath, format.mimeType)
                }
                Result.success(filePath)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun exportToFile(
        logs: List<LogEntry>,
        format: LogExportFormat,
        customPath: String? = null
    ): Result<String> {
        val outputPath = customPath ?: run {
            val fileName = generateFileName(format)
            val outputFile = File(context.getExternalFilesDir(null), "logs/$fileName")
            outputFile.absolutePath
        }

        return logRepository.exportLogs(logs, format, outputPath)
    }

    private fun generateFileName(format: LogExportFormat): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "logs_$timestamp.${format.extension}"
    }

    private fun shareFile(filePath: String, mimeType: String) {
        val file = File(filePath)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "分享日志文件")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}