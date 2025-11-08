package killua.dev.core.sharedvm

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Compose helper to remember a shared ViewModel by key.
 * Increments registry reference on composition and releases on dispose.
 */
@Composable
fun <T> rememberSharedViewModel(key: String, factory: () -> T): T {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember { context as? Activity }

    // Get or create VM with reference counting
    val vm = remember(key) { SharedViewModelRegistry.getOrCreate(key, factory) }

    DisposableEffect(lifecycleOwner, key) {
        val observer = LifecycleEventObserver { source, event ->
            when (event) {
                Lifecycle.Event.ON_DESTROY -> {
                    // Only release if the activity is finishing (not just configuration change)
                    val shouldRelease = when {
                        activity == null -> true // No activity reference, release on destroy
                        activity.isFinishing || activity.isDestroyed -> true
                        else -> false
                    }
                    if (shouldRelease) {
                        SharedViewModelRegistry.release(key)
                    }
                }
                else -> {} // Handle other lifecycle events if needed
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Release when composable is disposed (but don't double-release if already released)
            if (activity == null || activity.isFinishing) {
                SharedViewModelRegistry.release(key)
            }
        }
    }

    return vm
}
