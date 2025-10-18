package killua.dev.core.data.cache

import killua.dev.core.network.GsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import logcat.logcat

/**
 * 缓存管理器扩展函数
 * 提供更便捷的 API
 */

// ==================== 类型安全的对象缓存 ====================

/**
 * 写入对象缓存
 */
suspend inline fun <reified T> CacheManager.put(
    key: String,
    value: T,
    expireTime: Long? = null
): Boolean {
    return putObject(key, value, expireTime)
}

/**
 * 读取对象缓存
 */
suspend inline fun <reified T> CacheManager.get(key: String): T? {
    return getObject(key, T::class.java)
}

/**
 * 观察对象缓存变化
 */
inline fun <reified T> CacheManager.observe(key: String): Flow<T?> {
    return observeKey(key).map { json ->
        json?.let { 
            GsonParser.fromJson(it, T::class.java)
        }
    }
}

// ==================== 带默认值的缓存操作 ====================

/**
 * 读取缓存,如果不存在则返回默认值
 */
suspend fun CacheManager.getStringOrDefault(key: String, defaultValue: String): String {
    return getString(key) ?: defaultValue
}

/**
 * 读取对象缓存,如果不存在则返回默认值
 */
suspend inline fun <reified T> CacheManager.getOrDefault(key: String, defaultValue: T): T {
    return get<T>(key) ?: defaultValue
}

/**
 * 读取缓存,如果不存在则执行 block 并缓存结果
 */
suspend inline fun <reified T> CacheManager.getOrPut(
    key: String,
    expireTime: Long? = null,
    crossinline block: suspend () -> T
): T {
    get<T>(key)?.let { return it }
    
    val value = block()
    put(key, value, expireTime)
    return value
}

// ==================== 批量操作 ====================

/**
 * 批量删除缓存 (可变参数)
 */
suspend fun CacheManager.removeAll(vararg keys: String): Int {
    return removeAll(keys.toList())
}

/**
 * 批量检查缓存是否存在
 */
suspend fun CacheManager.containsAll(keys: List<String>): Boolean {
    return keys.all { contains(it) }
}

/**
 * 批量检查缓存是否存在 (可变参数)
 */
suspend fun CacheManager.containsAll(vararg keys: String): Boolean {
    return containsAll(keys.toList())
}

// ==================== 条件操作 ====================

/**
 * 只有当缓存不存在时才写入
 */
suspend fun CacheManager.putIfAbsent(
    key: String,
    value: String,
    expireTime: Long? = null
): Boolean {
    if (contains(key)) return false
    return putString(key, value, expireTime)
}

/**
 * 只有当缓存存在时才更新
 */
suspend fun CacheManager.updateIfPresent(
    key: String,
    value: String,
    expireTime: Long? = null
): Boolean {
    if (!contains(key)) return false
    return putString(key, value, expireTime)
}

// ==================== 标签操作 ====================

/**
 * 为缓存添加单个标签
 */
suspend fun CacheManager.addTag(key: String, tag: String): Boolean {
    val metadata = getMetadata(key) ?: return false
    val newTags = metadata.tags + tag
    return setTags(key, newTags)
}

/**
 * 为缓存添加多个标签
 */
suspend fun CacheManager.addTags(key: String, vararg tags: String): Boolean {
    val metadata = getMetadata(key) ?: return false
    val newTags = metadata.tags + tags.toSet()
    return setTags(key, newTags)
}

/**
 * 移除缓存标签
 */
suspend fun CacheManager.removeTag(key: String, tag: String): Boolean {
    val metadata = getMetadata(key) ?: return false
    val newTags = metadata.tags - tag
    return setTags(key, newTags)
}

// ==================== 过期时间操作 ====================

/**
 * 延长缓存过期时间
 */
suspend fun CacheManager.extendExpireTime(key: String, extraTime: Long): Boolean {
    val metadata = getMetadata(key) ?: return false
    if (metadata.expireTime < 0) return false
    
    val newExpireTime = metadata.expireTime + extraTime
    return updateExpireTime(key, newExpireTime)
}

/**
 * 刷新缓存过期时间 (重新计算)
 */
suspend fun CacheManager.refreshExpireTime(key: String, expireTime: Long): Boolean {
    val newExpireTime = System.currentTimeMillis() + expireTime
    return updateExpireTime(key, newExpireTime)
}

// ==================== 查询操作 ====================

/**
 * 获取未过期的缓存键
 */
suspend fun CacheManager.getValidKeys(): List<String> {
    return getAllKeys().filter { key ->
        getMetadata(key)?.isExpired() == false
    }
}

/**
 * 获取已过期的缓存键
 */
suspend fun CacheManager.getExpiredKeys(): List<String> {
    return getAllKeys().filter { key ->
        getMetadata(key)?.isExpired() == true
    }
}

/**
 * 根据键前缀查询
 */
suspend fun CacheManager.getKeysByPrefix(prefix: String): List<String> {
    return getAllKeys().filter { it.startsWith(prefix) }
}

/**
 * 根据键后缀查询
 */
suspend fun CacheManager.getKeysBySuffix(suffix: String): List<String> {
    return getAllKeys().filter { it.endsWith(suffix) }
}

// ==================== 统计操作 ====================

/**
 * 获取命中率
 */
suspend fun CacheManager.getHitRate(): Float {
    return getStatistics().hitRate
}

/**
 * 打印缓存统计信息
 */
suspend fun CacheManager.printStatistics(tag: String = "CacheManager", priority: LogPriority = LogPriority.DEBUG) {
    val stats = getStatistics()
    logcat(tag, priority) {
        """
        ========== 缓存统计 ==========
        总大小: ${stats.totalSize / 1024} KB
        项数量: ${stats.itemCount}
        过期数: ${stats.expiredCount}
        命中数: ${stats.hitCount}
        未命中: ${stats.missCount}
        命中率: ${(stats.hitRate * 100).toInt()}%
        最近访问: ${stats.lastAccessTime}
        ==============================
        """.trimIndent()
    }
}

// ==================== 便捷的键构建 ====================

/**
 * 构建缓存键
 */
fun buildCacheKey(vararg parts: Any?): String {
    return parts.filterNotNull().joinToString("_")
}

/**
 * 构建带前缀的缓存键
 */
fun buildCacheKey(prefix: String, vararg parts: Any?): String {
    return listOf(prefix).plus(parts.filterNotNull()).joinToString("_")
}
