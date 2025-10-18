package killua.dev.core.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * 全局单例 Gson 解析器
 * 提供统一的 JSON 序列化和反序列化功能
 */
object GsonParser {
    
    /**
     * 默认 Gson 实例
     * 配置:
     * - 序列化 null 值
     * - 美化输出
     * - 禁用 HTML 转义
     */
    val default: Gson by lazy {
        GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }
    
    /**
     * 紧凑模式 Gson 实例
     */
    val compact: Gson by lazy {
        GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create()
    }
    
    /**
     * 严格模式 Gson 实例 (不序列化 null)
     */
    val strict: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
    }
    
    /**
     * 将对象转换为 JSON 字符串
     */
    fun toJson(obj: Any?): String {
        return default.toJson(obj)
    }
    
    /**
     * 将对象转换为 JSON 字符串 (紧凑模式)
     */
    fun toJsonCompact(obj: Any?): String {
        return compact.toJson(obj)
    }
    
    /**
     * 将 JSON 字符串解析为对象
     */
    inline fun <reified T> fromJson(json: String): T? {
        return try {
            default.fromJson(json, T::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将 JSON 字符串解析为对象 (带类型)
     */
    fun <T> fromJson(json: String, type: Type): T? {
        return try {
            default.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将 JSON 字符串解析为对象 (带类型)
     */
    fun <T> fromJson(json: String, typeToken: TypeToken<T>): T? {
        return try {
            default.fromJson(json, typeToken.type)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将 JSON 字符串解析为 List
     */
    inline fun <reified T> fromJsonList(json: String): List<T>? {
        return try {
            val type = object : TypeToken<List<T>>() {}.type
            default.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将 JSON 字符串解析为 Map
     */
    inline fun <reified K, reified V> fromJsonMap(json: String): Map<K, V>? {
        return try {
            val type = object : TypeToken<Map<K, V>>() {}.type
            default.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 检查字符串是否为有效的 JSON
     */
    fun isValidJson(json: String): Boolean {
        return try {
            JsonParser.parseString(json)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 美化 JSON 字符串
     */
    fun prettify(json: String): String? {
        return try {
            val jsonElement = JsonParser.parseString(json)
            default.toJson(jsonElement)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 压缩 JSON 字符串 (移除空格和换行)
     */
    fun minify(json: String): String? {
        return try {
            val jsonElement = JsonParser.parseString(json)
            compact.toJson(jsonElement)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 扩展函数: 将对象转换为 JSON 字符串
 */
fun Any.toJson(): String = GsonParser.toJson(this)

/**
 * 扩展函数: 将对象转换为 JSON 字符串 (紧凑模式)
 */
fun Any.toJsonCompact(): String = GsonParser.toJsonCompact(this)

/**
 * 扩展函数: 将 JSON 字符串解析为对象
 */
inline fun <reified T> String.fromJson(): T? = GsonParser.fromJson(this)

/**
 * 扩展函数: 将 JSON 字符串解析为 List
 */
inline fun <reified T> String.fromJsonList(): List<T>? = GsonParser.fromJsonList(this)

/**
 * 扩展函数: 将 JSON 字符串解析为 Map
 */
inline fun <reified K, reified V> String.fromJsonMap(): Map<K, V>? = GsonParser.fromJsonMap(this)

/**
 * 扩展函数: 检查字符串是否为有效的 JSON
 */
fun String.isValidJson(): Boolean = GsonParser.isValidJson(this)

/**
 * 扩展函数: 美化 JSON 字符串
 */
fun String.prettifyJson(): String? = GsonParser.prettify(this)

/**
 * 扩展函数: 压缩 JSON 字符串
 */
fun String.minifyJson(): String? = GsonParser.minify(this)
