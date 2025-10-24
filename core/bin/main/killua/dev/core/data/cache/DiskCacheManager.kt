package killua.dev.core.data.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.core.network.GsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.logcat
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DiskCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: CacheConfig = CacheConfig.DEFAULT
) : CacheManager {
    
    private val cacheDir: File by lazy {
        (config.customCacheDir ?: File(context.cacheDir, config.rootDirName)).apply {
            if (!exists()) {
                val created = mkdirs()
                logcat { "Cache dir made: path=${absolutePath}, success=$created" }
            }
        }
    }
    
    private val metadataDir: File by lazy {
        File(cacheDir, ".metadata").apply {
            if (!exists()) mkdirs()
        }
    }
    
    // 内存缓存
    private val memoryCache = if (config.enableMemoryCache) {
        LinkedHashMap<String, CacheEntry>(config.maxMemoryCacheEntries, 0.75f, true)
    } else null
    
    // 缓存流,用于观察缓存变化
    private val cacheFlows = mutableMapOf<String, MutableStateFlow<String?>>()
    
    // 统计信息
    private var hitCount = 0L
    private var missCount = 0L
    private var lastAccessTime = 0L
    
    // 锁,保证线程安全
    private val mutex = Mutex()
    
    private val cleanupScheduler: CacheCleanupScheduler by lazy {
        CacheCleanupScheduler(context)
    }
    
    init {
        logcat("DiskCacheManager") { 
            "Cache manager initialized: path=${cacheDir.absolutePath}, memoryCache=${config.enableMemoryCache}, maxSize=${config.maxCacheSize / 1024 / 1024}MB" 
        }
        
        // Start automatic cleanup with WorkManager
        if (config.autoCleanExpired) {
            logcat("DiskCacheManager") { "Auto cleanup enabled: interval=${config.cleanupInterval}ms" }
            cleanupScheduler.scheduleCleanup(config.cleanupInterval)
        }
    }
    
    // ==================== 基础操作 ====================
    
    override suspend fun putString(key: String, value: String, expireTime: Long?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                logcat("CacheManager", LogPriority.DEBUG) { "[Write] Starting cache write: key=$key, size=${value.length} bytes" }
                
                val file = getCacheFile(key)
                file.writeText(value)
                
                // Save metadata
                val metadata = createMetadata(key, value.toByteArray().size.toLong(), expireTime)
                saveMetadata(key, metadata)
                
                // Update memory cache
                updateMemoryCache(key, value)
                
                // Notify observers
                notifyObservers(key, value)
                
                val expireInfo = if (expireTime != null && expireTime > 0) {
                    "expireTime: ${expireTime / 1000}s"
                } else if (expireTime == -1L) {
                    "never expires"
                } else {
                    "default expiry: ${config.defaultExpireTime / 1000}s"
                }
                logcat("CacheManager", LogPriority.INFO) { "[Write] ✓ Cache write successful: key=$key, size=${value.length}, $expireInfo" }
                true
            } catch (e: Exception) {
                logcat("CacheManager", LogPriority.ERROR) { "[Write] ✗ Cache write failed: key=$key, error=${e.message}" }
                false
            }
        }
    }
    
    override suspend fun getString(key: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                memoryCache?.get(key)?.let { entry ->
                    if (!entry.metadata.isExpired()) {
                        hitCount++
                        lastAccessTime = System.currentTimeMillis()
                        return@withContext entry.value
                    }
                }
                
                val metadata = loadMetadata(key)
                if (metadata == null || metadata.isExpired()) {
                    missCount++
                    if (metadata?.isExpired() == true) {
                        logcat("CacheManager", LogPriority.DEBUG) { "[Read] ⚠ Cache expired: key=$key, auto-deleted" }
                        remove(key)
                    } else {
                        logcat("CacheManager", LogPriority.DEBUG) { "[Read] ⚠ Cache miss: key=$key (not found)" }
                    }
                    return@withContext null
                }
                
                val file = getCacheFile(key)
                if (!file.exists()) {
                    missCount++
                    logcat("CacheManager", LogPriority.WARN) { "[Read] ⚠ Cache file missing: key=$key" }
                    return@withContext null
                }
                
                val value = file.readText()
                
                updateMemoryCache(key, value)
                
                hitCount++
                lastAccessTime = System.currentTimeMillis()
                
                logcat("CacheManager", LogPriority.DEBUG) { "[Read] ✓ Cache hit: key=$key, size=${value.length}, hitRate=${(hitCount.toFloat() / (hitCount + missCount) * 100).toInt()}%" }
                value
            } catch (e: Exception) {
                logcat("CacheManager", LogPriority.WARN) { "[Read] ✗ Cache read failed: key=$key, error=${e.message}" }
                missCount++
                null
            }
        }
    }
    
    override suspend fun putBytes(key: String, bytes: ByteArray, expireTime: Long?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                logcat("CacheManager", LogPriority.DEBUG) { "[Write] Starting bytes cache write: key=$key, size=${bytes.size} bytes" }
                
                val file = getCacheFile(key)
                file.writeBytes(bytes)
                
                val metadata = createMetadata(key, bytes.size.toLong(), expireTime)
                saveMetadata(key, metadata)
                
                logcat("CacheManager", LogPriority.INFO) { "[Write] ✓ Bytes cache write successful: key=$key, size=${bytes.size / 1024}KB" }
                true
            } catch (e: Exception) {
                logcat("CacheManager", LogPriority.ERROR) { "[Write] ✗ Bytes cache write failed: key=$key, error=${e.message}" }
                false
            }
        }
    }
    
    override suspend fun getBytes(key: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val metadata = loadMetadata(key)
                if (metadata == null || metadata.isExpired()) {
                    if (metadata?.isExpired() == true) {
                        logcat("CacheManager", LogPriority.DEBUG) { "[Read] ⚠ Bytes cache expired: key=$key" }
                        remove(key)
                    }
                    return@withContext null
                }
                
                val file = getCacheFile(key)
                if (!file.exists()) {
                    logcat("CacheManager", LogPriority.WARN) { "[Read] ⚠ Bytes cache file not found: key=$key" }
                    return@withContext null
                }
                
                val bytes = file.readBytes()
                logcat("CacheManager", LogPriority.DEBUG) { "[Read] ✓ Bytes cache read successful: key=$key, size=${bytes.size / 1024}KB" }
                bytes
            } catch (e: Exception) {
                logcat("CacheManager", LogPriority.ERROR) { "[Read] ✗ Bytes cache read failed: key=$key, error=${e.message}" }
                null
            }
        }
    }
    
    override suspend fun <T> putObject(key: String, value: T, expireTime: Long?): Boolean {
        logcat("CacheManager", LogPriority.DEBUG) { "[Write] Starting object serialization: key=$key, type=${value!!::class.simpleName}" }
        val json = GsonParser.toJson(value)
        return putString(key, json, expireTime)
    }
    
    override suspend fun <T> getObject(key: String, clazz: Class<T>): T? {
        val json = getString(key) ?: return null
        logcat("CacheManager", LogPriority.DEBUG) { "[Read] Starting object deserialization: key=$key, type=${clazz.simpleName}" }
        return try {
            GsonParser.fromJson(json, clazz)
        } catch (e: Exception) {
            logcat("CacheManager", LogPriority.ERROR) { "[Read] ✗ Object deserialization failed: key=$key, type=${clazz.simpleName}, error=${e.message}" }
            null
        }
    }
    
    override suspend fun putFile(key: String, inputStream: InputStream, expireTime: Long?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = getCacheFile(key)
                file.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                
                val metadata = createMetadata(key, file.length(), expireTime)
                saveMetadata(key, metadata)
                
                logcat("CacheManager", LogPriority.INFO) { "[Write] ✓ File cache write successful: key=$key, size=${file.length() / 1024}KB" }
                true
            } catch (e: Exception) {
                logcat("CacheManager", LogPriority.ERROR) { "[Write] ✗ File cache write failed: key=$key, error=${e.message}" }
                false
            }
        }
    }
    
    override suspend fun getFile(key: String): File? {
        return withContext(Dispatchers.IO) {
            val metadata = loadMetadata(key)
            if (metadata == null || metadata.isExpired()) {
                if (metadata?.isExpired() == true) {
                    logcat("CacheManager", LogPriority.DEBUG) { "[Read] ⚠ File cache expired: key=$key" }
                    remove(key)
                }
                return@withContext null
            }
            
            val file = getCacheFile(key)
            if (file.exists()) {
                logcat("CacheManager", LogPriority.DEBUG) { "[Read] ✓ File cache read successful: key=$key, path=${file.absolutePath}" }
                file
            } else {
                logcat("CacheManager", LogPriority.WARN) { "[Read] ⚠ File cache not found: key=$key" }
                null
            }
        }
    }
    
    // ==================== 查询操作 ====================
    
    override suspend fun contains(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            val metadata = loadMetadata(key)
            val exists = metadata != null && !metadata.isExpired() && getCacheFile(key).exists()
            logcat("CacheManager", LogPriority.DEBUG) { "[Query] Check cache exists: key=$key, exists=$exists" }
            exists
        }
    }
    
    override suspend fun getMetadata(key: String): CacheMetadata? {
        val metadata = loadMetadata(key)
        if (metadata != null) {
            logcat("CacheManager", LogPriority.DEBUG) { 
                "[Query] Metadata: key=$key, size=${metadata.size / 1024}KB, tags=${metadata.tags}" 
            }
        }
        return metadata
    }
    
    override suspend fun getAllKeys(): List<String> {
        return withContext(Dispatchers.IO) {
            val keys = metadataDir.listFiles()
                ?.mapNotNull { it.nameWithoutExtension }
                ?: emptyList()
            logcat("CacheManager", LogPriority.DEBUG) { "[Query] Get all cache keys: total=${keys.size}" }
            keys
        }
    }
    
    override suspend fun getKeysByTag(tag: String): List<String> {
        return withContext(Dispatchers.IO) {
            val keys = getAllKeys().filter { key ->
                loadMetadata(key)?.tags?.contains(tag) == true
            }
            logcat("CacheManager", LogPriority.DEBUG) { "[Query] Query by tag: tag=$tag, found ${keys.size} items" }
            keys
        }
    }
    
    // ==================== 删除操作 ====================
    
    override suspend fun remove(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val file = getCacheFile(key)
                    val metaFile = getMetadataFile(key)
                    
                    val deleted = file.delete() or metaFile.delete()
                    
                    // 清除内存缓存
                    memoryCache?.remove(key)
                    
                    // Notify observers
                    notifyObservers(key, null)
                    
                    if (deleted) {
                        logcat("CacheManager", LogPriority.INFO) { "[Delete] ✓ Cache delete successful: key=$key" }
                    } else {
                        logcat("CacheManager", LogPriority.WARN) { "[Delete] ⚠ Cache delete failed (may not exist): key=$key" }
                    }
                    deleted
                } catch (e: Exception) {
                    logcat("CacheManager", LogPriority.ERROR) { "[Delete] ✗ Cache delete exception: key=$key, error=${e.message}" }
                    false
                }
            }
        }
    }
    
    override suspend fun removeAll(keys: List<String>): Int {
        logcat("CacheManager", LogPriority.DEBUG) { "[Batch Delete] Starting to delete ${keys.size} caches" }
        val count = keys.count { remove(it) }
        logcat("CacheManager", LogPriority.INFO) { "[Batch Delete] ✓ Completed: successfully deleted $count/${keys.size} items" }
        return count
    }
    
    override suspend fun removeByTag(tag: String): Int {
        logcat("CacheManager", LogPriority.DEBUG) { "[Delete By Tag] Starting to delete tag: $tag" }
        val keys = getKeysByTag(tag)
        val count = removeAll(keys)
        logcat("CacheManager", LogPriority.INFO) { "[Delete By Tag] ✓ Completed: tag=$tag, deleted $count items" }
        return count
    }
    
    override suspend fun clear(): Boolean {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                    metadataDir.mkdirs()
                    
                    memoryCache?.clear()
                    cacheFlows.clear()
                    
                    hitCount = 0
                    missCount = 0
                    
                    logcat("CacheManager", LogPriority.INFO) { "[Clear] ✓ All caches cleared" }
                    true
                } catch (e: Exception) {
                    logcat("CacheManager", LogPriority.ERROR) { "[Clear] ✗ Cache clear failed: error=${e.message}" }
                    false
                }
            }
        }
    }
    
    override suspend fun clearExpired(): Int {
        return withContext(Dispatchers.IO) {
            val totalKeys = getAllKeys().size
            logcat("CacheManager", LogPriority.DEBUG) { "[Cleanup] Starting expired cache cleanup: total=$totalKeys" }
            
            var count = 0
            getAllKeys().forEach { key ->
                if (loadMetadata(key)?.isExpired() == true) {
                    if (remove(key)) count++
                }
            }
            
            if (count > 0) {
                logcat("CacheManager", LogPriority.INFO) { "[Cleanup] ✓ Expired cache cleanup completed: cleaned $count/$totalKeys items" }
            } else {
                logcat("CacheManager", LogPriority.DEBUG) { "[Cleanup] No expired caches to clean" }
            }
            count
        }
    }
    
    // ==================== 统计操作 ====================
    
    override suspend fun getCacheSize(): Long {
        return withContext(Dispatchers.IO) {
            val size = cacheDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
            logcat("CacheManager", LogPriority.DEBUG) { "[Query] Cache size: ${size / 1024}KB (${size / 1024 / 1024}MB)" }
            size
        }
    }
    
    override suspend fun getCacheCount(): Int {
        val count = getAllKeys().size
        logcat("CacheManager", LogPriority.DEBUG) { "[Query] Cache count: $count items" }
        return count
    }
    
    override suspend fun getStatistics(): CacheStatistics {
        val stats = CacheStatistics(
            totalSize = getCacheSize(),
            itemCount = getCacheCount(),
            expiredCount = getAllKeys().count { key ->
                loadMetadata(key)?.isExpired() == true
            },
            hitCount = hitCount,
            missCount = missCount,
            lastAccessTime = lastAccessTime
        )
        val hitRate = if (hitCount + missCount > 0) {
            (hitCount.toFloat() / (hitCount + missCount) * 100).toInt()
        } else 0
        logcat("CacheManager", LogPriority.DEBUG) { 
            "[Statistics] size=${stats.totalSize / 1024}KB, count=${stats.itemCount}, expired=${stats.expiredCount}, hitRate=$hitRate%" 
        }
        return stats
    }
    
    // ==================== 高级操作 ====================
    
    override suspend fun putBatch(entries: Map<String, String>, expireTime: Long?): Int {
        logcat("CacheManager", LogPriority.DEBUG) { "[Batch Write] Starting to write ${entries.size} caches" }
        val count = entries.count { (key, value) ->
            putString(key, value, expireTime)
        }
        logcat("CacheManager", LogPriority.INFO) { "[Batch Write] ✓ Completed: successfully wrote $count/${entries.size} items" }
        return count
    }
    
    override suspend fun getBatch(keys: List<String>): Map<String, String?> {
        logcat("CacheManager", LogPriority.DEBUG) { "[Batch Read] Starting to read ${keys.size} caches" }
        val result = keys.associateWith { getString(it) }
        val hitCount = result.values.count { it != null }
        logcat("CacheManager", LogPriority.DEBUG) { "[Batch Read] ✓ Completed: hit $hitCount/${keys.size} items" }
        return result
    }
    
    override fun observeKey(key: String): Flow<String?> {
        logcat("CacheManager", LogPriority.DEBUG) { "[Observe] Start observing cache changes: key=$key" }
        return cacheFlows.getOrPut(key) {
            MutableStateFlow(null)
        }
    }
    
    override suspend fun setTags(key: String, tags: Set<String>): Boolean {
        return withContext(Dispatchers.IO) {
            val metadata = loadMetadata(key) ?: return@withContext false
            val newMetadata = metadata.copy(tags = tags)
            val success = saveMetadata(key, newMetadata)
            if (success) {
                logcat("CacheManager", LogPriority.DEBUG) { "[Tag] ✓ Tags update successful: key=$key, tags=$tags" }
            }
            success
        }
    }
    
    override suspend fun updateExpireTime(key: String, expireTime: Long): Boolean {
        return withContext(Dispatchers.IO) {
            val metadata = loadMetadata(key) ?: return@withContext false
            val newMetadata = metadata.copy(expireTime = expireTime)
            val success = saveMetadata(key, newMetadata)
            if (success) {
                val remainingSeconds = (expireTime - System.currentTimeMillis()) / 1000
                logcat("CacheManager", LogPriority.INFO) { "[Update] ✓ Expire time updated: key=$key, remaining=${remainingSeconds}s" }
            }
            success
        }
    }
    

    private fun getCacheFile(key: String): File {
        val hash = hashKey(key)
        return File(cacheDir, hash)
    }
    
    private fun getMetadataFile(key: String): File {
        val hash = hashKey(key)
        return File(metadataDir, "$hash.meta")
    }
    
    private fun hashKey(key: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(key.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    private fun createMetadata(key: String, size: Long, expireTime: Long?): CacheMetadata {
        val expire = expireTime ?: config.defaultExpireTime
        val expireTimestamp = if (expire > 0) {
            System.currentTimeMillis() + expire
        } else -1
        
        return CacheMetadata(
            key = key,
            createTime = System.currentTimeMillis(),
            expireTime = expireTimestamp,
            size = size
        )
    }
    
    private fun saveMetadata(key: String, metadata: CacheMetadata): Boolean {
        return try {
            val file = getMetadataFile(key)
            val json = GsonParser.toJson(metadata)
            file.writeText(json)
            true
        } catch (e: Exception) {
            logcat("CacheManager", LogPriority.WARN) { "[Metadata] ⚠ Metadata save failed: key=$key, error=${e.message}" }
            false
        }
    }
    
    private fun loadMetadata(key: String): CacheMetadata? {
        return try {
            val file = getMetadataFile(key)
            if (!file.exists()) return null
            val json = file.readText()
            GsonParser.fromJson(json, CacheMetadata::class.java)
        } catch (e: Exception) {
            logcat("CacheManager", LogPriority.WARN) { "[Metadata] ⚠ Metadata load failed: key=$key, error=${e.message}" }
            null
        }
    }
    
    private fun updateMemoryCache(key: String, value: String) {
        memoryCache?.let { cache ->
            val metadata = loadMetadata(key) ?: return
            cache[key] = CacheEntry(value, metadata)
            
            if (cache.size > config.maxMemoryCacheEntries) {
                val iterator = cache.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }
    }
    
    private fun notifyObservers(key: String, value: String?) {
        cacheFlows[key]?.value = value
    }
    
    private data class CacheEntry(
        val value: String,
        val metadata: CacheMetadata
    )
}
