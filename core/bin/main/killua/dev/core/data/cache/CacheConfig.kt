package killua.dev.core.data.cache

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 缓存配置
 */
data class CacheConfig(
    /**
     * 缓存根目录名称 (相对于 context.cacheDir)
     * 如果 customCacheDir 不为 null,此参数将被忽略
     */
    val rootDirName: String = "app_cache",
    
    /**
     * 自定义缓存目录的完整路径
     * 如果设置此参数,将直接使用此路径作为缓存目录,忽略 rootDirName
     * null 表示使用默认的 context.cacheDir/rootDirName
     */
    val customCacheDir: File? = null,
    
    /**
     * 默认过期时间 (毫秒)
     * -1 表示永不过期
     */
    val defaultExpireTime: Long = TimeUnit.DAYS.toMillis(7),
    
    /**
     * 最大缓存大小 (字节)
     * -1 表示不限制
     */
    val maxCacheSize: Long = 100 * 1024 * 1024, // 100MB
    
    /**
     * 是否启用内存缓存
     */
    val enableMemoryCache: Boolean = true,
    
    /**
     * 内存缓存最大条目数
     */
    val maxMemoryCacheEntries: Int = 1000,
    
    /**
     * 是否自动清理过期缓存
     */
    val autoCleanExpired: Boolean = true,
    
    /**
     * 自动清理间隔 (毫秒)
     */
    val cleanupInterval: Long = TimeUnit.HOURS.toMillis(1),
    
    /**
     * 是否启用加密
     */
    val enableEncryption: Boolean = false,
    
    /**
     * 加密密钥 (如果启用加密)
     */
    val encryptionKey: String? = null
) {
    
    companion object {
        /**
         * 默认配置
         */
        val DEFAULT = CacheConfig()
        
        /**
         * 短期缓存配置 (1小时)
         */
        val SHORT_TERM = CacheConfig(
            rootDirName = "short_cache",
            defaultExpireTime = TimeUnit.HOURS.toMillis(1)
        )
        
        /**
         * 长期缓存配置 (30天)
         */
        val LONG_TERM = CacheConfig(
            rootDirName = "long_cache",
            defaultExpireTime = TimeUnit.DAYS.toMillis(30)
        )
        
        /**
         * 永久缓存配置
         */
        val PERMANENT = CacheConfig(
            rootDirName = "permanent_cache",
            defaultExpireTime = -1,
            autoCleanExpired = false
        )
        
        /**
         * 图片缓存配置
         */
        val IMAGE_CACHE = CacheConfig(
            rootDirName = "image_cache",
            defaultExpireTime = TimeUnit.DAYS.toMillis(14),
            maxCacheSize = 200 * 1024 * 1024, // 200MB
            enableMemoryCache = true
        )
    }
}

/**
 * 缓存策略
 */
enum class CacheStrategy {
    /**
     * 只读缓存,不写入
     */
    CACHE_ONLY,
    
    /**
     * 只从网络获取,不读缓存
     */
    NETWORK_ONLY,
    
    /**
     * 先读缓存,没有则从网络获取并缓存
     */
    CACHE_FIRST,
    
    /**
     * 先从网络获取,失败则读缓存
     */
    NETWORK_FIRST,
    
    /**
     * 总是从网络获取并更新缓存
     */
    ALWAYS_REFRESH
}

/**
 * 缓存项元数据
 */
data class CacheMetadata(
    /**
     * 缓存键
     */
    val key: String,
    
    /**
     * 创建时间
     */
    val createTime: Long = System.currentTimeMillis(),
    
    /**
     * 过期时间 (-1 表示永不过期)
     */
    val expireTime: Long = -1,
    
    /**
     * 数据大小 (字节)
     */
    val size: Long = 0,
    
    /**
     * MIME 类型
     */
    val mimeType: String? = null,
    
    /**
     * 自定义标签
     */
    val tags: Set<String> = emptySet(),
    
    /**
     * 扩展属性
     */
    val extras: Map<String, String> = emptyMap()
) {
    /**
     * 是否已过期
     */
    fun isExpired(): Boolean {
        if (expireTime < 0) return false
        return System.currentTimeMillis() > expireTime
    }
    
    /**
     * 剩余有效时间 (毫秒)
     */
    fun remainingTime(): Long {
        if (expireTime < 0) return Long.MAX_VALUE
        return (expireTime - System.currentTimeMillis()).coerceAtLeast(0)
    }
}
