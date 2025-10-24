package killua.dev.core.data.cache

import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream

/**
 * 缓存管理器接口
 */
interface CacheManager {
    
    // ==================== 基础操作 ====================
    
    /**
     * 写入字符串缓存
     * @param key 缓存键
     * @param value 缓存值
     * @param expireTime 过期时间 (毫秒), null 使用默认配置
     * @return 是否成功
     */
    suspend fun putString(key: String, value: String, expireTime: Long? = null): Boolean
    
    /**
     * 读取字符串缓存
     * @param key 缓存键
     * @return 缓存值, 不存在或已过期返回 null
     */
    suspend fun getString(key: String): String?
    
    /**
     * 写入字节数组缓存
     */
    suspend fun putBytes(key: String, bytes: ByteArray, expireTime: Long? = null): Boolean
    
    /**
     * 读取字节数组缓存
     */
    suspend fun getBytes(key: String): ByteArray?
    
    /**
     * 写入对象缓存 (自动序列化为 JSON)
     */
    suspend fun <T> putObject(key: String, value: T, expireTime: Long? = null): Boolean
    
    /**
     * 读取对象缓存 (自动反序列化)
     */
    suspend fun <T> getObject(key: String, clazz: Class<T>): T?
    
    /**
     * 写入文件缓存
     */
    suspend fun putFile(key: String, inputStream: InputStream, expireTime: Long? = null): Boolean
    
    /**
     * 获取缓存文件
     */
    suspend fun getFile(key: String): File?
    
    // ==================== 查询操作 ====================
    
    /**
     * 检查缓存是否存在且未过期
     */
    suspend fun contains(key: String): Boolean
    
    /**
     * 获取缓存元数据
     */
    suspend fun getMetadata(key: String): CacheMetadata?
    
    /**
     * 获取所有缓存键
     */
    suspend fun getAllKeys(): List<String>
    
    /**
     * 根据标签查询缓存键
     */
    suspend fun getKeysByTag(tag: String): List<String>
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除指定缓存
     */
    suspend fun remove(key: String): Boolean
    
    /**
     * 删除多个缓存
     */
    suspend fun removeAll(keys: List<String>): Int
    
    /**
     * 根据标签删除缓存
     */
    suspend fun removeByTag(tag: String): Int
    
    /**
     * 清空所有缓存
     */
    suspend fun clear(): Boolean
    
    /**
     * 清理过期缓存
     */
    suspend fun clearExpired(): Int
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取缓存总大小 (字节)
     */
    suspend fun getCacheSize(): Long
    
    /**
     * 获取缓存项数量
     */
    suspend fun getCacheCount(): Int
    
    /**
     * 获取缓存统计信息
     */
    suspend fun getStatistics(): CacheStatistics
    
    // ==================== 高级操作 ====================
    
    /**
     * 批量写入缓存
     */
    suspend fun putBatch(entries: Map<String, String>, expireTime: Long? = null): Int
    
    /**
     * 批量读取缓存
     */
    suspend fun getBatch(keys: List<String>): Map<String, String?>
    
    /**
     * 观察缓存变化
     */
    fun observeKey(key: String): Flow<String?>
    
    /**
     * 设置缓存标签
     */
    suspend fun setTags(key: String, tags: Set<String>): Boolean
    
    /**
     * 更新过期时间
     */
    suspend fun updateExpireTime(key: String, expireTime: Long): Boolean
}

/**
 * 缓存统计信息
 */
data class CacheStatistics(
    /**
     * 总缓存大小 (字节)
     */
    val totalSize: Long,
    
    /**
     * 缓存项数量
     */
    val itemCount: Int,
    
    /**
     * 过期项数量
     */
    val expiredCount: Int,
    
    /**
     * 命中次数
     */
    val hitCount: Long = 0,
    
    /**
     * 未命中次数
     */
    val missCount: Long = 0,
    
    /**
     * 命中率
     */
    val hitRate: Float = if (hitCount + missCount > 0) {
        hitCount.toFloat() / (hitCount + missCount)
    } else 0f,
    
    /**
     * 最近访问时间
     */
    val lastAccessTime: Long = 0
)
