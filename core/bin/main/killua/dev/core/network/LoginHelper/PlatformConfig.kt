package killua.dev.base.Data.account

interface PlatformConfig {
    val loginUrl: String
    val cookieDomain: String
    val cookieRuleGroups: List<CookieRuleGroup>
}