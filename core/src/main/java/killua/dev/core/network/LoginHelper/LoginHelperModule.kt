package killua.dev.core.network.LoginHelper

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import killua.dev.base.Data.account.PlatformConfig
import javax.inject.Named
import javax.inject.Singleton

/**
 * 提供登录辅助功能的默认空实现
 * 使用方可以通过提供自己的实现来覆盖这些默认值
 */
@Module
@InstallIn(SingletonComponent::class)
object LoginHelperModule {
    
    /**
     * 提供默认的 PlatformConfig 实现(空实现)
     * 使用方可以通过 @Provides @Named("platformConfig") 来覆盖
     */
    @Provides
    @Singleton
    @Named("platformConfig")
    fun providePlatformConfig(): PlatformConfig? {
        return null
    }
    
    /**
     * 提供默认的 CookieRepository 实现(空实现)
     * 使用方可以通过 @Provides @Named("cookieRepository") 来覆盖
     */
    @Provides
    @Singleton
    @Named("cookieRepository")
    fun provideCookieRepository(): CookieRepository? {
        return null
    }
}
