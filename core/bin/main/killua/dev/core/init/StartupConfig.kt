package killua.dev.core.init

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Permission request configuration
 */
data class PermissionRequest(
    val permission: String,
    val required: Boolean = true
)

/**
 * Startup configuration
 * Business logic should implement this to provide custom behavior
 */
interface StartupConfig {
    val applicationContext: Context
    val permissions: List<PermissionRequest>
    val requiresConsent: Boolean
    val tasks: List<StartupTask>
    fun getNextIntent(context: Context): Intent
}

/**
 * Default implementation
 */
@Singleton
class DefaultStartupConfig @Inject constructor(
    @ApplicationContext override val applicationContext: Context
) : StartupConfig {
    
    override val permissions: List<PermissionRequest> = emptyList()
    
    override val requiresConsent: Boolean = false
    
    override val tasks: List<StartupTask> = emptyList()
    
    override fun getNextIntent(context: Context): Intent {
        // Default: return to same activity
        return Intent(context, context::class.java)
    }
}

/**
 * Builder for creating custom startup configurations
 */
class StartupConfigBuilder(
    private val context: Context
) {
    private val permissions = mutableListOf<PermissionRequest>()
    private var requiresConsent: Boolean = false
    private val tasks = mutableListOf<StartupTask>()
    private var nextIntentProvider: ((Context) -> Intent)? = null
    
    /**
     * Add permissions with individual required flags
     * Example:
     * ```
     * addPermissions(
     *     PermissionRequest(CAMERA, required = false),
     *     PermissionRequest(LOCATION, required = true)
     * )
     * ```
     */
    fun addPermissions(vararg requests: PermissionRequest) = apply {
        permissions.addAll(requests)
    }
    
    /**
     * Add permissions that are all required or all optional
     * Example:
     * ```
     * requirePermissions(CAMERA, LOCATION, required = true)
     * ```
     */
    fun requirePermissions(vararg permissions: String, required: Boolean = true) = apply {
        this.permissions.addAll(
            permissions.map { PermissionRequest(it, required) }
        )
    }
    
    /**
     * Add a single permission
     */
    fun addPermission(permission: String, required: Boolean = true) = apply {
        permissions.add(PermissionRequest(permission, required))
    }
    
    fun requireConsent(required: Boolean = true) = apply {
        this.requiresConsent = required
    }
    
    fun addTask(task: StartupTask) = apply {
        tasks.add(task)
    }
    
    fun addTasks(vararg tasks: StartupTask) = apply {
        this.tasks.addAll(tasks)
    }
    
    fun setNextIntent(provider: (Context) -> Intent) = apply {
        this.nextIntentProvider = provider
    }
    
    fun build(): StartupConfig = object : StartupConfig {
        override val applicationContext: Context = context
        override val permissions: List<PermissionRequest> = this@StartupConfigBuilder.permissions.toList()
        override val requiresConsent: Boolean = this@StartupConfigBuilder.requiresConsent
        override val tasks: List<StartupTask> = this@StartupConfigBuilder.tasks.toList()
        
        override fun getNextIntent(context: Context): Intent {
            return nextIntentProvider?.invoke(context) 
                ?: Intent(context, context::class.java)
        }
    }
}

/**
 * Extension function for easy config creation
 */
fun Context.startupConfig(builder: StartupConfigBuilder.() -> Unit): StartupConfig {
    return StartupConfigBuilder(this).apply(builder).build()
}
