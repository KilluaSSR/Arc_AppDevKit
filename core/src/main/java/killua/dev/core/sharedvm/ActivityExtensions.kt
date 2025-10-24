package killua.dev.core.sharedvm

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Obtain a shared ViewModel by key from an Activity. The registry ensures the same instance
 * is returned across different Activities/holders for a given key.
 *
 * Behavior: each call increments internal reference count. An attached lifecycle observer
 * will call release(key) when the owner is destroyed permanently (e.g. activity.isFinishing).
 */
fun <T> ComponentActivity.getSharedViewModel(key: String, factory: () -> T): T {
    val vm = SharedViewModelRegistry.getOrCreate(key, factory)

    val owner = this
    val observer = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                val activity = source as? Activity
                try {
                    if (activity?.isFinishing == true) {
                        SharedViewModelRegistry.release(key)
                    }
                } finally {
                    source.lifecycle.removeObserver(this)
                }
            }
        }
    }

    this.lifecycle.addObserver(observer)
    return vm
}

/**
 * Obtain a shared ViewModel from a Fragment. Release when fragment is finally removed.
 */
fun <T> Fragment.getSharedViewModel(key: String, factory: () -> T): T {
    val vm = SharedViewModelRegistry.getOrCreate(key, factory)
    val lifecycleOwner = viewLifecycleOwner
    val fragmentRef = this

    val observer = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                try {
                    val isFinal = fragmentRef.isRemoving || (fragmentRef.activity?.isFinishing == true)
                    if (isFinal) SharedViewModelRegistry.release(key)
                } finally {
                    source.lifecycle.removeObserver(this)
                }
            }
        }
    }

    lifecycleOwner.lifecycle.addObserver(observer)
    return vm
}
