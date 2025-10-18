package killua.dev.core.log

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 日志查看器入口
 * 直接在 Composable 中调用即可显示日志查看器
 */
object LogViewer {
    
    /**
     * 显示日志查看器
     * 
     * @param modifier Modifier
     * @param onBack 返回按钮回调
     */
    @Composable
    fun Show(
        modifier: Modifier = Modifier,
        onBack: (() -> Unit)? = null
    ) {
        LogViewerScreen(
            modifier = modifier,
            onBack = onBack ?: {}
        )
    }
    
    /**
     * 开始捕获日志
     */
    fun startCapture() {
        LogcatCaptureService.startCapture()
    }
    
    /**
     * 停止捕获日志
     */
    fun stopCapture() {
        LogcatCaptureService.stopCapture()
    }
    
    /**
     * 清除日志
     */
    fun clearLogs() {
        LogcatCaptureService.clearLogs()
    }
    
    /**
     * 是否正在捕获
     */
    fun isCapturing(): Boolean {
        return LogcatCaptureService.isCapturing()
    }
    
    /**
     * 获取日志数量
     */
    fun getLogCount(): Int {
        return LogcatCaptureService.getLogCount()
    }
}
