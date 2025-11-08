package killua.dev.core.log.provider

import android.net.Uri
import androidx.core.content.FileProvider

class LogFileProvider : FileProvider() {

    companion object {
        fun getAuthORITY(context: android.content.Context): String {
            return "${context.packageName}.fileprovider"
        }

        fun getLogFileUri(context: android.content.Context, file: java.io.File): Uri {
            val authority = getAuthORITY(context)
            return getUriForFile(context, authority, file)
        }

        fun grantReadPermission(context: android.content.Context, uri: Uri) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, context.contentResolver.getType(uri))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}