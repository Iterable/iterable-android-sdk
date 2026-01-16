package com.iterable.iterableapi

import android.content.Context
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.view.OrientationEventListener

internal class InAppOrientationService {

    fun interface OrientationChangeCallback {
        fun onOrientationChanged()
    }

    fun createOrientationListener(
        context: Context,
        callback: OrientationChangeCallback
    ): OrientationEventListener {
        return object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            private var lastOrientation = -1

            override fun onOrientationChanged(orientation: Int) {
                val currentOrientation = roundToNearest90Degrees(orientation)

                if (currentOrientation != lastOrientation && lastOrientation != -1) {
                    lastOrientation = currentOrientation

                    Handler(Looper.getMainLooper()).postDelayed({
                        IterableLogger.d(TAG, "Orientation changed, triggering callback")
                        callback.onOrientationChanged()
                    }, ORIENTATION_CHANGE_DELAY_MS)
                } else if (lastOrientation == -1) {
                    lastOrientation = currentOrientation
                }
            }
        }
    }

    fun roundToNearest90Degrees(orientation: Int): Int {
        return ((orientation + 45) / 90 * 90) % 360
    }

    fun enableListener(listener: OrientationEventListener?) {
        if (listener != null && listener.canDetectOrientation()) {
            listener.enable()
            IterableLogger.d(TAG, "Orientation listener enabled")
        } else {
            IterableLogger.w(TAG, "Cannot enable orientation listener")
        }
    }

    fun disableListener(listener: OrientationEventListener?) {
        listener?.disable()
        IterableLogger.d(TAG, "Orientation listener disabled")
    }

    companion object {
        private const val TAG = "InAppOrientService"
        private const val ORIENTATION_CHANGE_DELAY_MS = 1500L
    }
}

