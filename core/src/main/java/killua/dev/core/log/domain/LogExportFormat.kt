package killua.dev.core.log.domain

enum class LogExportFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
    HTML("html", "text/html");

    companion object {
        fun fromExtension(extension: String): LogExportFormat? =
            LogExportFormat.entries.find { it.extension.equals(extension, ignoreCase = true) }
    }
}