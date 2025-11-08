package killua.dev.core.log.domain

data class LogFilter(
    val query: String = "",
    val selectedLevel: LogLevel = LogLevel.ALL,
    val tags: Set<String> = emptySet(),
    val packageNames: Set<String> = emptySet()
) {
    fun matches(logEntry: LogEntry): Boolean {
        if (query.isNotEmpty()) {
            val matchesQuery = logEntry.message.contains(query, ignoreCase = true) ||
                    logEntry.tag.contains(query, ignoreCase = true)
            if (!matchesQuery) return false
        }

        if (selectedLevel != LogLevel.ALL && logEntry.level != selectedLevel) {
            return false
        }

        if (tags.isNotEmpty() && logEntry.tag !in tags) {
            return false
        }

        if (packageNames.isNotEmpty()) {
            val packageName = logEntry.packageName
            if (packageName == null || packageName !in packageNames) {
                return false
            }
        }

        return true
    }

    companion object {
        val ALL = LogFilter()
        val ERROR_ONLY = LogFilter(selectedLevel = LogLevel.ERROR)
        val WARN_AND_ERROR = LogFilter(selectedLevel = LogLevel.WARN) // WARN及以上级别
    }
}