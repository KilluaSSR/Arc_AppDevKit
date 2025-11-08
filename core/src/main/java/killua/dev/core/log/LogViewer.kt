package killua.dev.core.log

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import killua.dev.core.log.domain.LogExportFormat
import killua.dev.core.log.domain.LogFilter

/**
 * 日志查看器入口
 * 提供高级API用于日志查看和管理
 */
object LogViewer {

    @Composable
    fun Show(
        modifier: Modifier = Modifier,
        onBack: (() -> Unit)? = null,
        context: Context? = null
    ) {
        val service = context?.let { LogcatCaptureServiceProxy.getInstance(it) }
        LogViewerScreen(
            modifier = modifier,
            onBack = onBack ?: {},
            logService = service
        )
    }

    fun startCapture(context: Context) {
        LogcatCaptureServiceProxy.getInstance(context).startCapture()
    }

    fun stopCapture(context: Context) {
        LogcatCaptureServiceProxy.getInstance(context).stopCapture()
    }

    suspend fun clearLogs(context: Context) {
        LogcatCaptureServiceProxy.getInstance(context).clearLogs()
    }

    fun isCapturing(context: Context): Boolean {
        return LogcatCaptureServiceProxy.getInstance(context).isCapturing()
    }

    suspend fun exportLogs(
        context: Context,
        format: LogExportFormat = LogExportFormat.TXT,
        share: Boolean = true
    ) = LogcatCaptureServiceProxy.getInstance(context).exportLogs(format, share)

    fun getFilteredLogs(
        context: Context,
        filter: LogFilter = LogFilter.ALL
    ) = LogcatCaptureServiceProxy.getInstance(context).getFilteredLogs(filter)
}
