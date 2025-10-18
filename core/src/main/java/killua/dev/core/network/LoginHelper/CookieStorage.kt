package killua.dev.core.network.LoginHelper

import killua.dev.base.Data.account.CookieInfo

interface CookieRepository {
    suspend fun saveCookie(cookieInfo: CookieInfo)
}