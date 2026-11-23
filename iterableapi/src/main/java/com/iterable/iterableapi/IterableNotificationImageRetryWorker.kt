package com.iterable.iterableapi

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.S)
internal class IterableNotificationImageRetryWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "IterableNotificationImageRetryWorker"

        const val MAX_RETRY_AGE_MILLIS = 24L * 60 * 60 * 1000
        // Android caches failed DNS lookups for 2s, so retrying sooner reuses the first failure.
        const val MIN_RETRY_DELAY_AFTER_POST_MILLIS = 3_000L
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_ORIGINAL_POST_TIME = "original_post_time"

        @JvmStatic
        fun retryDelayMillis(originalPostTime: Long, now: Long): Long =
            maxOf(0L, originalPostTime + MIN_RETRY_DELAY_AFTER_POST_MILLIS - now)

        @JvmStatic
        fun createInputData(extras: Bundle, notificationId: Int, originalPostTime: Long): Data {
            return Data.Builder()
                .putAll(IterableNotificationWorker.createInputData(extras))
                .putInt(KEY_NOTIFICATION_ID, notificationId)
                .putLong(KEY_ORIGINAL_POST_TIME, originalPostTime)
                .build()
        }
    }

    @WorkerThread
    override fun doWork(): Result {
        try {
            retryImage()
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Notification image retry failed", e)
        }
        return Result.success()
    }

    private fun retryImage() {
        val originalPostTime = inputData.getLong(KEY_ORIGINAL_POST_TIME, 0L)
        if (System.currentTimeMillis() - originalPostTime > MAX_RETRY_AGE_MILLIS) {
            IterableLogger.d(TAG, "Notification is too old, skipping image retry")
            return
        }

        val jsonString = inputData.getString(IterableNotificationWorker.KEY_NOTIFICATION_DATA_JSON) ?: return
        val json = JSONObject(jsonString)
        val extras = Bundle().apply { json.keys().forEach { putString(it, json.getString(it)) } }
        val messageId = IterableNotificationData(extras).messageId
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 0)
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shownNotification = findShownNotification(notificationManager, notificationId, messageId)
        if (shownNotification == null) {
            IterableLogger.d(TAG, "Notification is no longer showing, skipping image retry")
            return
        }

        Thread.sleep(retryDelayMillis(originalPostTime, System.currentTimeMillis()))
        if (isStopped) return

        val notificationBuilder = IterableNotificationHelper.createNotification(applicationContext, extras) ?: return
        notificationBuilder.extras.putString(IterableNotificationWorker.EXTRA_MESSAGE_ID, messageId)
        notificationBuilder
            .setOnlyAlertOnce(true)
            .setWhen(shownNotification.notification.`when`)
        val notification = notificationBuilder.build()

        // The download can take a while, so the user may have dismissed the notification meanwhile.
        if (notificationBuilder.imageLoaded && !isStopped &&
            findShownNotification(notificationManager, notificationId, messageId) != null
        ) {
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun findShownNotification(notificationManager: NotificationManager, id: Int, messageId: String?) =
        notificationManager.activeNotifications.firstOrNull {
            it.id == id && it.tag == null &&
                it.notification.extras.getString(IterableNotificationWorker.EXTRA_MESSAGE_ID) == messageId
        }
}
