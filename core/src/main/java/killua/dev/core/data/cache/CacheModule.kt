package killua.dev.core.data.cache

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object CacheModule {
    
    /**
     * 提供默认缓存管理器
     */
    @Provides
    @Singleton
    @DefaultCache
    fun provideDefaultCacheManager(
        @ApplicationContext context: Context
    ): CacheManager {
        return DiskCacheManager(context, CacheConfig.DEFAULT)
    }
    
    /**
     * 提供短期缓存管理器
     */
    @Provides
    @Singleton
    @ShortTermCache
    fun provideShortTermCacheManager(
        @ApplicationContext context: Context
    ): CacheManager {
        return DiskCacheManager(context, CacheConfig.SHORT_TERM)
    }
    
    /**
     * 提供长期缓存管理器
     */
    @Provides
    @Singleton
    @LongTermCache
    fun provideLongTermCacheManager(
        @ApplicationContext context: Context
    ): CacheManager {
        return DiskCacheManager(context, CacheConfig.LONG_TERM)
    }
    
    /**
     * 提供永久缓存管理器
     */
    @Provides
    @Singleton
    @PermanentCache
    fun providePermanentCacheManager(
        @ApplicationContext context: Context
    ): CacheManager {
        return DiskCacheManager(context, CacheConfig.PERMANENT)
    }
    
    /**
     * 提供图片缓存管理器
     */
    @Provides
    @Singleton
    @ImageCache
    fun provideImageCacheManager(
        @ApplicationContext context: Context
    ): CacheManager {
        return DiskCacheManager(context, CacheConfig.IMAGE_CACHE)
    }
}

// ==================== 缓存限定符 ====================

/**
 * 默认缓存
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultCache

/**
 * 短期缓存
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ShortTermCache

/**
 * 长期缓存
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LongTermCache

/**
 * 永久缓存
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PermanentCache

/**
 * 图片缓存
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageCache

/**
 * 自定义缓存
 * 用于业务方自定义配置
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CustomCache(val name: String)
