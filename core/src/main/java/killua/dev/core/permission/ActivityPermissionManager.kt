package killua.dev.core.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import logcat.LogPriority
import logcat.logcat
import kotlin.coroutines.resume

class ActivityPermissionManager(
    private val activity: ComponentActivity
) : PermissionManager {
    
    companion object {
        private const val TAG = "PermissionManager"
    }
    
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var currentCallback: ((Map<String, Boolean>) -> Unit)? = null
    
    fun initialize() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            logcat(TAG, LogPriority.DEBUG) {
                "Permission result: ${result.entries.joinToString { "${it.key}=${it.value}" }}"
            }
            currentCallback?.invoke(result)
            currentCallback = null
        }
    }
    
    override suspend fun requestPermissions(
        permissions: List<Permission>,
        callback: PermissionCallback?
    ): PermissionResult {
        if (permissions.isEmpty()) {
            callback?.onAllPermissionsGranted()
            return PermissionResult.AllGranted
        }
        
        val currentResult = checkPermissions(permissions)
        if (currentResult is PermissionResult.AllGranted) {
            callback?.onAllPermissionsGranted()
            return currentResult
        }
        
        val deniedPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission.permission
            ) != PackageManager.PERMISSION_GRANTED
        }
        
        val shouldShowRationale = deniedPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission.permission
            )
        }
        
        if (shouldShowRationale && callback != null) {
            logcat(TAG, LogPriority.INFO) {
                "Should show permission rationale for: ${deniedPermissions.map { it.permission }}"
            }
            val shouldContinue = callback.onPermissionDenied(
                deniedPermissions.map { it.permission },
                shouldShowRationale = true
            )
            if (!shouldContinue) {
                return checkPermissions(permissions)
            }
        }
        
        val permissionStrings = permissions.map { it.permission }.toTypedArray()
        
        logcat(TAG, LogPriority.INFO) {
            "Requesting ${permissions.size} permissions"
        }
        
        val result = suspendCancellableCoroutine { continuation ->
            currentCallback = { grantResults ->
                val details = permissions.map { permission ->
                    val granted = grantResults[permission.permission] == true
                    val status = when {
                        granted -> PermissionStatus.GRANTED
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            permission.permission
                        ) -> PermissionStatus.DENIED
                        else -> PermissionStatus.PERMANENTLY_DENIED
                    }
                    PermissionDetail(
                        permission = permission.permission,
                        status = status,
                        required = permission.required
                    )
                }
                
                val allGranted = details.all { it.status == PermissionStatus.GRANTED }
                val permResult = if (allGranted) {
                    PermissionResult.AllGranted
                } else {
                    val requiredDenied = details.any { 
                        it.required && it.status != PermissionStatus.GRANTED 
                    }
                    val hasPermanentlyDenied = details.any { 
                        it.status == PermissionStatus.PERMANENTLY_DENIED 
                    }
                    PermissionResult.Denied(
                        details = details,
                        requiredDenied = requiredDenied,
                        hasPermanentlyDenied = hasPermanentlyDenied
                    )
                }
                
                continuation.resume(permResult)
            }
            
            permissionLauncher?.launch(permissionStrings)
        }
        
        // 处理结果回调
        when (result) {
            is PermissionResult.AllGranted -> {
                callback?.onAllPermissionsGranted()
            }
            is PermissionResult.Denied -> {
                if (result.hasPermanentlyDenied && callback != null) {
                    logcat(TAG, LogPriority.WARN) {
                        "Permanently denied: ${result.permanentlyDeniedPermissions}"
                    }
                    val shouldOpenSettings = callback.onPermissionPermanentlyDenied(
                        result.permanentlyDeniedPermissions
                    )
                    if (shouldOpenSettings) {
                        openAppSettings()
                    }
                } else if (callback != null) {
                    callback.onPermissionDenied(
                        result.deniedPermissions,
                        shouldShowRationale = false
                    )
                }
            }
        }
        
        return result
    }
    
    override fun checkPermissions(permissions: List<Permission>): PermissionResult {
        if (permissions.isEmpty()) return PermissionResult.AllGranted
        
        val details = permissions.map { permission ->
            val status = checkPermissionStatus(permission.permission)
            PermissionDetail(
                permission = permission.permission,
                status = status,
                required = permission.required
            )
        }
        
        val allGranted = details.all { it.status == PermissionStatus.GRANTED }
        return if (allGranted) {
            PermissionResult.AllGranted
        } else {
            val requiredDenied = details.any { 
                it.required && it.status != PermissionStatus.GRANTED 
            }
            val hasPermanentlyDenied = details.any { 
                it.status == PermissionStatus.PERMANENTLY_DENIED 
            }
            PermissionResult.Denied(
                details = details,
                requiredDenied = requiredDenied,
                hasPermanentlyDenied = hasPermanentlyDenied
            )
        }
    }
    
    override fun checkPermissionStatus(permission: String): PermissionStatus {
        val granted = ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
        
        return when {
            granted -> PermissionStatus.GRANTED
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) -> {
                PermissionStatus.DENIED
            }
            else -> {
                // 注意:首次请求前也会返回 false,但这里我们只在检查时调用
                // 如果从未请求过,视为 DENIED 状态
                PermissionStatus.DENIED
            }
        }
    }
    
    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        logcat(TAG, LogPriority.INFO) {
            "Opening app settings"
        }
        
        activity.startActivity(intent)
    }
}

fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasPermissions(vararg permissions: String): Boolean {
    return permissions.all { hasPermission(it) }
}
