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

    val vm = remember { SharedViewModelRegistry.getOrCreate(key, factory) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                val shouldRelease = activity?.isFinishing ?: true
                if (shouldRelease) SharedViewModelRegistry.release(key)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            SharedViewModelRegistry.release(key)
        }
    }

    return vm
}
