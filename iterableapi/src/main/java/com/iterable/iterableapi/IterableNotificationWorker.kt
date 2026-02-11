package com.iterable.iterableapi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject

internal class IterableNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "IterableNotificationWorker"
        private const val FOREGROUND_NOTIFICATION_ID = 10101

        const val KEY_NOTIFICATION_DATA_JSON = "notification_data_json"
        const val KEY_IS_GHOST_PUSH = "is_ghost_push"

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

    override fun getForegroundInfo(): ForegroundInfo {
        val channelId = applicationContext.packageName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    getChannelName(),
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(getSmallIconId())
            .setContentTitle(getAppName())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun getSmallIconId(): Int {
        var iconId = 0

        try {
            val info = applicationContext.packageManager.getApplicationInfo(
                applicationContext.packageName, PackageManager.GET_META_DATA
            )
            iconId = info.metaData?.getInt(IterableConstants.NOTIFICATION_ICON_NAME, 0) ?: 0
        } catch (e: PackageManager.NameNotFoundException) {
            IterableLogger.w(TAG, "Could not read application metadata for icon")
        }

        if (iconId == 0) {
            iconId = applicationContext.resources.getIdentifier(
                IterableApi.getNotificationIcon(applicationContext),
                IterableConstants.ICON_FOLDER_IDENTIFIER,
                applicationContext.packageName
            )
        }

        if (iconId == 0) {
            iconId = applicationContext.applicationInfo.icon
        }

        return iconId
    }

    private fun getAppName(): String {
        return applicationContext.applicationInfo
            .loadLabel(applicationContext.packageManager).toString()
    }

    private fun getChannelName(): String {
        return try {
            val info = applicationContext.packageManager.getApplicationInfo(
                applicationContext.packageName, PackageManager.GET_META_DATA
            )
            info.metaData?.getString("iterable_notification_channel_name")
                ?: "Notifications"
        } catch (e: PackageManager.NameNotFoundException) {
            "Notifications"
        }
    }

    @WorkerThread
    override fun doWork(): Result {
        IterableLogger.d(TAG, "Starting notification processing in Worker")

        try {
            val isGhostPush = inputData.getBoolean(KEY_IS_GHOST_PUSH, false)

            if (isGhostPush) {
                IterableLogger.d(TAG, "Ghost push detected, skipping notification display")
                return Result.success()
            }

            val jsonString = inputData.getString(KEY_NOTIFICATION_DATA_JSON)

            if (jsonString == null || jsonString.isEmpty()) {
                IterableLogger.e(TAG, "No notification data provided to Worker")
                return Result.failure()
            }

            val extras = jsonToBundle(jsonString)

            if (extras.keySet().size == 0) {
                IterableLogger.e(TAG, "Deserialized bundle is empty")
                return Result.failure()
            }

            val notificationBuilder = IterableNotificationHelper.createNotification(
                applicationContext,
                extras
            )

            if (notificationBuilder == null) {
                IterableLogger.w(TAG, "Notification builder is null, skipping")
                return Result.success()
            }

            IterableNotificationHelper.postNotificationOnDevice(
                applicationContext,
                notificationBuilder
            )

            IterableLogger.d(TAG, "Notification posted successfully")
            return Result.success()

        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error processing notification in Worker", e)
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
