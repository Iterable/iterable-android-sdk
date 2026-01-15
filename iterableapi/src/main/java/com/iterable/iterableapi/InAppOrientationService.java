package com.iterable.iterableapi;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.view.OrientationEventListener;

import androidx.annotation.NonNull;

/**
 * Service class for handling device orientation changes in in-app messages.
 * Centralizes orientation detection logic shared between Fragment and Dialog implementations.
 */
class InAppOrientationService {

    private static final String TAG = "InAppOrientService";
    private static final long ORIENTATION_CHANGE_DELAY_MS = 1500;

    /**
     * Callback interface for orientation change events
     */
    interface OrientationChangeCallback {
        /**
         * Called when the device orientation has changed
         */
        void onOrientationChanged();
    }

    /**
     * Creates an OrientationEventListener that detects 90-degree rotations
     * @param context The context for sensor access
     * @param callback The callback to invoke when orientation changes
     * @return Configured OrientationEventListener (caller must enable it)
     */
    @NonNull
    public OrientationEventListener createOrientationListener(
            @NonNull Context context,
            @NonNull final OrientationChangeCallback callback) {
        
        return new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            private int lastOrientation = -1;

            @Override
            public void onOrientationChanged(int orientation) {
                int currentOrientation = roundToNearest90Degrees(orientation);
                
                // Only trigger callback if orientation actually changed
                if (currentOrientation != lastOrientation && lastOrientation != -1) {
                    lastOrientation = currentOrientation;
                    
                    // Delay the callback to allow orientation change to stabilize
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            IterableLogger.d(TAG, "Orientation changed, triggering callback");
                            callback.onOrientationChanged();
                        }
                    }, ORIENTATION_CHANGE_DELAY_MS);
                } else if (lastOrientation == -1) {
                    // Initialize last orientation
                    lastOrientation = currentOrientation;
                }
            }
        };
    }

    /**
     * Rounds an orientation value to the nearest 90-degree increment
     * @param orientation The raw orientation value (0-359 degrees)
     * @return The nearest 90-degree value (0, 90, 180, or 270)
     */
    public int roundToNearest90Degrees(int orientation) {
        return ((orientation + 45) / 90 * 90) % 360;
    }

    /**
     * Safely enables an OrientationEventListener
     * @param listener The listener to enable (nullable)
     */
    public void enableListener(OrientationEventListener listener) {
        if (listener != null && listener.canDetectOrientation()) {
            listener.enable();
            IterableLogger.d(TAG, "Orientation listener enabled");
        } else {
            IterableLogger.w(TAG, "Cannot enable orientation listener");
        }
    }

    /**
     * Safely disables an OrientationEventListener
     * @param listener The listener to disable (nullable)
     */
    public void disableListener(OrientationEventListener listener) {
        if (listener != null) {
            listener.disable();
            IterableLogger.d(TAG, "Orientation listener disabled");
        }
    }
}

