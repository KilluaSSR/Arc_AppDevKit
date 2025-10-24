package killua.dev.core.permission

data class Permission(
    val permission: String,
    val required: Boolean = true
)

enum class PermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

/**
 * 单个权限的详情
 */
data class PermissionDetail(
    val permission: String,
    val status: PermissionStatus,
    val required: Boolean
)

/**
 * 权限请求结果
 */
sealed class PermissionResult {
    /** 所有权限都已授予 */
    data object AllGranted : PermissionResult()
    
    /** 部分或全部权限被拒绝 */
    data class Denied(
        val details: List<PermissionDetail>,
        /** 是否有必需权限被拒绝 */
        val requiredDenied: Boolean,
        /** 是否有权限被永久拒绝(需要引导到设置) */
        val hasPermanentlyDenied: Boolean
    ) : PermissionResult() {
        val deniedPermissions: List<String> = details.filter { it.status != PermissionStatus.GRANTED }.map { it.permission }
        val permanentlyDeniedPermissions: List<String> = details.filter { it.status == PermissionStatus.PERMANENTLY_DENIED }.map { it.permission }
    }
}

/**
 * 权限请求回调接口
 */
interface PermissionCallback {
    /**
     * 权限被拒绝时的回调
     * @param deniedPermissions 被拒绝的权限列表
     * @param shouldShowRationale 是否应该显示权限说明(true=用户首次拒绝,可以解释为何需要;false=可能已永久拒绝)
     * @return true=继续请求权限;false=取消
     */
    suspend fun onPermissionDenied(
        deniedPermissions: List<String>,
        shouldShowRationale: Boolean
    ): Boolean = false
    
    /**
     * 权限被永久拒绝时的回调
     * @param permanentlyDeniedPermissions 被永久拒绝的权限列表
     * @return true=引导用户到设置页面;false=取消
     */
    suspend fun onPermissionPermanentlyDenied(
        permanentlyDeniedPermissions: List<String>
    ): Boolean = false
    
    /**
     * 所有权限都已授予的回调
     */
    suspend fun onAllPermissionsGranted() {}
}

interface PermissionManager {
    /**
     * 请求权限
     * @param callback 权限请求回调
     */
    suspend fun requestPermissions(
        permissions: List<Permission>,
        callback: PermissionCallback? = null
    ): PermissionResult
    
    /**
     * 检查权限状态(不请求)
     */
    fun checkPermissions(permissions: List<Permission>): PermissionResult
    
    /**
     * 检查单个权限的详细状态
     */
    fun checkPermissionStatus(permission: String): PermissionStatus
    
    /**
     * 打开应用设置页面
     */
    fun openAppSettings()
}
