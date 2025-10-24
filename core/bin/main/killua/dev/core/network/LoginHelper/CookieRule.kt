package killua.dev.base.Data.account

data class CookieRule(
    val name: String,
    val pattern: String,
)

data class CookieInfo(
    val key: String,
    val value: String
)

data class CookieRuleGroup(
    val rules: List<CookieRule>,
    val matchOne: Boolean = false
)