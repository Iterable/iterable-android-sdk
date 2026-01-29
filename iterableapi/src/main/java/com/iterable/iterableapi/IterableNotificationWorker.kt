package com.iterable.iterableapi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.IOException
import java.net.URL

/**
 * WorkManager Worker to handle push notification processing.
 * This replaces the deprecated AsyncTask approach to comply with Firebase best practices.
 * 
 * The Worker handles:
 * - Downloading notification images from remote URLs
 * - Building notifications with proper styling
 * - Posting notifications to the system
 */
internal class IterableNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "IterableNotificationWorker"
        
        const val KEY_NOTIFICATION_DATA_JSON = "notification_data_json"
        const val KEY_IS_GHOST_PUSH = "is_ghost_push"
        
        /**
         * Creates input data for the Worker from a Bundle.
         * Converts the Bundle to JSON for reliable serialization.
         */
        @JvmStatic
        fun createInputData(extras: Bundle, isGhostPush: Boolean): Data {
            val jsonObject = JSONObject()
            for (key in extras.keySet()) {
                val value = extras.getString(key)
                if (value != null) {
                    jsonObject.put(key, value)
                }
            }
            
            return Data.Builder()
                .putString(KEY_NOTIFICATION_DATA_JSON, jsonObject.toString())
                .putBoolean(KEY_IS_GHOST_PUSH, isGhostPush)
                .build()
        }
    }

    @WorkerThread
    override fun doWork(): Result {
        IterableLogger.d(TAG, "========================================")
        IterableLogger.d(TAG, "Starting notification processing in Worker")
        IterableLogger.d(TAG, "Worker ID: $id")
        IterableLogger.d(TAG, "Run attempt: $runAttemptCount")
        
        try {
            val isGhostPush = inputData.getBoolean(KEY_IS_GHOST_PUSH, false)
            IterableLogger.d(TAG, "Step 1: Ghost push check - isGhostPush=$isGhostPush")
            
            if (isGhostPush) {
                IterableLogger.d(TAG, "Ghost push detected - no user-visible notification to display")
                return Result.success()
            }

            val jsonString = inputData.getString(KEY_NOTIFICATION_DATA_JSON)
            IterableLogger.d(TAG, "Step 2: Retrieved notification JSON data (length=${jsonString?.length ?: 0})")
            
            if (jsonString == null || jsonString.isEmpty()) {
                IterableLogger.e(TAG, "CRITICAL ERROR: No notification data provided to Worker")
                return Result.failure()
            }

            IterableLogger.d(TAG, "Step 3: Deserializing notification data from JSON")
            val extras = jsonToBundle(jsonString)
            val keyCount = extras.keySet().size
            IterableLogger.d(TAG, "Step 3: Deserialized $keyCount keys from notification data")
            
            if (keyCount == 0) {
                IterableLogger.e(TAG, "CRITICAL ERROR: Deserialized bundle is empty")
                return Result.failure()
            }

            IterableLogger.d(TAG, "Step 4: Creating notification builder")
            val notificationBuilder = IterableNotificationHelper.createNotification(
                applicationContext,
                extras
            )
            
            if (notificationBuilder == null) {
                IterableLogger.w(TAG, "Step 4: Notification builder is null (likely ghost push or invalid data)")
                return Result.success()
            }
            
            IterableLogger.d(TAG, "Step 4: Notification builder created successfully")
            val hasImage = extras.getString(IterableConstants.ITERABLE_DATA_PUSH_IMAGE) != null
            if (hasImage) {
                IterableLogger.d(TAG, "Step 4: Notification contains image URL: ${extras.getString(IterableConstants.ITERABLE_DATA_PUSH_IMAGE)}")
            }

            IterableLogger.d(TAG, "Step 5: Posting notification to device (this may download images)")
            IterableNotificationHelper.postNotificationOnDevice(
                applicationContext,
                notificationBuilder
            )
            
            IterableLogger.d(TAG, "Step 5: Notification posted successfully to NotificationManager")
            IterableLogger.d(TAG, "Notification processing COMPLETED successfully")
            IterableLogger.d(TAG, "========================================")
            return Result.success()
            
        } catch (e: Exception) {
            IterableLogger.e(TAG, "========================================")
            IterableLogger.e(TAG, "CRITICAL ERROR processing notification in Worker", e)
            IterableLogger.e(TAG, "Error type: ${e.javaClass.simpleName}")
            IterableLogger.e(TAG, "Error message: ${e.message}")
            IterableLogger.e(TAG, "Stack trace:", e)
            IterableLogger.e(TAG, "========================================")
            
            return Result.retry()
        }
    }

    private fun jsonToBundle(jsonString: String): Bundle {
        val bundle = Bundle()
        try {
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.getString(key)
                bundle.putString(key, value)
            }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error parsing notification JSON: ${e.message}", e)
        }
        return bundle
    }
}
