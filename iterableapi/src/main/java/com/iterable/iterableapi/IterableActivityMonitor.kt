package com.iterable.iterableapi

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.iterable.iterableapi.util.DeviceInfoUtils
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

class IterableActivityMonitor {
    private val handler = Handler(Looper.getMainLooper())
    private var currentActivity: WeakReference<Activity>? = null
    private var numStartedActivities = 0
    private var inForeground = false
    private val callbacks: MutableList<WeakReference<AppStateCallback>> = CopyOnWriteArrayList()
    
    private val backgroundTransitionRunnable = Runnable {
        inForeground = false
        for (callback in callbacks) {
            callback.get()?.onSwitchToBackground()
        }
    }

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // Empty implementation
        }

        override fun onActivityStarted(activity: Activity) {
            handler.removeCallbacks(backgroundTransitionRunnable)
            numStartedActivities++
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivity = WeakReference(activity)

            if (!inForeground || DeviceInfoUtils.isFireTV(activity.packageManager)) {
                inForeground = true
                for (callback in callbacks) {
                    callback.get()?.onSwitchToForeground()
                }
            }
        }

        override fun onActivityPaused(activity: Activity) {
            if (getCurrentActivity() == activity) {
                currentActivity = null
            }
        }

        override fun onActivityStopped(activity: Activity) {
            if (numStartedActivities > 0) {
                numStartedActivities--
            }

            if (numStartedActivities == 0 && inForeground) {
                handler.postDelayed(backgroundTransitionRunnable, BACKGROUND_DELAY_MS.toLong())
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            // Empty implementation
        }

        override fun onActivityDestroyed(activity: Activity) {
            // Empty implementation
        }
    }

    fun registerLifecycleCallbacks(@NonNull context: Context) {
        if (!initialized) {
            initialized = true
            (context.applicationContext as Application).registerActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }

    fun unregisterLifecycleCallbacks(@NonNull context: Context) {
        if (initialized) {
            initialized = false
            (context.applicationContext as Application).unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }

    @Nullable
    fun getCurrentActivity(): Activity? {
        return currentActivity?.get()
    }

    fun isInForeground(): Boolean {
        return getCurrentActivity() != null
    }

    fun addCallback(@NonNull callback: AppStateCallback) {
        // Don't insert again if the same callback already exists
        for (existingCallback in callbacks) {
            if (existingCallback.get() == callback) {
                return
            }
        }
        callbacks.add(WeakReference(callback))
    }

    fun removeCallback(@NonNull callback: AppStateCallback) {
        for (callbackRef in callbacks) {
            if (callbackRef.get() == callback) {
                callbacks.remove(callbackRef)
            }
        }
    }

    interface AppStateCallback {
        fun onSwitchToForeground()
        fun onSwitchToBackground()
    }

    companion object {
        private var initialized = false
        private const val BACKGROUND_DELAY_MS = 1000
        val instance = IterableActivityMonitor()

        @NonNull
        @JvmStatic
        fun getInstance(): IterableActivityMonitor {
            return instance
        }
    }
}