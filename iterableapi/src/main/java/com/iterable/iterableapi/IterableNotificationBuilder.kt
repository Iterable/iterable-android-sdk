package com.iterable.iterableapi

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by David Truong dt@iterable.com
 */
class IterableNotificationBuilder protected constructor(
    val context: Context,
    channelId: String
) : NotificationCompat.Builder(context, channelId) {

    private var isGhostPush = false
    private var imageUrl: String? = null
    private var expandedContent: String? = null
    var requestCode = 0
    var iterableNotificationData: IterableNotificationData? = null

    /**
     * Sets the image url
     * @param imageUrl
     */
    fun setImageUrl(imageUrl: String?) {
        this.imageUrl = imageUrl
    }

    /**
     * Sets the expanded content used for backwards compatibility up to Android API 23
     * @param content
     */
    fun setExpandedContent(content: String?) {
        this.expandedContent = content
    }

    fun setIsGhostPush(ghostPush: Boolean) {
        isGhostPush = ghostPush
    }

    fun isGhostPush(): Boolean {
        return isGhostPush
    }

    /**
     * Combine all of the options that have been set and return a new [Notification]
     * object.
     * Download any optional images
     */
    override fun build(): Notification {
        var style: NotificationCompat.Style? = null

        if (this.imageUrl != null) {
            try {
                val url = URL(this.imageUrl)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connect()
                val notificationImage = BitmapFactory.decodeStream(connection.getInputStream())
                if (notificationImage != null) {
                    style = NotificationCompat.BigPictureStyle()
                        .bigPicture(notificationImage)
                        .setSummaryText(expandedContent)
                    this.setLargeIcon(notificationImage)
                } else {
                    IterableLogger.e(TAG, "Notification image could not be loaded from url: " + this.imageUrl)
                }
            } catch (e: MalformedURLException) {
                IterableLogger.e(TAG, e.toString())
            } catch (e: IOException) {
                IterableLogger.e(TAG, e.toString())
            }
        }

        //Sets the default BigTextStyle if the imageUrl isn't set or cannot be loaded.
        if (style == null) {
            style = NotificationCompat.BigTextStyle().bigText(expandedContent)
        }

        this.setStyle(style)

        return super.build()
    }

    /**
     * Creates a notification action button for a given JSON payload
     * @param context    Context
     * @param button     `IterableNotificationData.Button` object containing button information
     * @param extras     Notification payload
     */
    fun createNotificationActionButton(context: Context, button: IterableNotificationData.Button, extras: Bundle) {
        val pendingButtonIntent = getPendingIntent(context, button, extras)
        val actionBuilder = NotificationCompat.Action
            .Builder(NotificationCompat.BADGE_ICON_NONE, button.title, pendingButtonIntent)
        if (button.buttonType == IterableNotificationData.Button.BUTTON_TYPE_TEXT_INPUT) {
            actionBuilder.addRemoteInput(RemoteInput.Builder(IterableConstants.USER_INPUT).setLabel(button.inputPlaceholder).build())
        }
        addAction(actionBuilder.build())
    }

    private fun getPendingIntent(context: Context, button: IterableNotificationData.Button, extras: Bundle): PendingIntent {
        val pendingButtonIntent: PendingIntent

        val buttonIntent = Intent(IterableConstants.ACTION_PUSH_ACTION)
        buttonIntent.putExtras(extras)
        buttonIntent.putExtra(IterableConstants.REQUEST_CODE, requestCode)
        buttonIntent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, button.identifier)
        buttonIntent.putExtra(IterableConstants.ACTION_IDENTIFIER, button.identifier)

        val pendingIntentFlag = if (button.buttonType == IterableNotificationData.Button.BUTTON_TYPE_TEXT_INPUT) 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE 
        else 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        pendingButtonIntent = if (button.openApp) {
            IterableLogger.d(TAG, "Go through TrampolineActivity")
            buttonIntent.setClass(context, IterableTrampolineActivity::class.java)
            buttonIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            PendingIntent.getActivity(
                context, buttonIntent.hashCode(),
                buttonIntent, pendingIntentFlag
            )
        } else {
            IterableLogger.d(TAG, "Go through IterablePushActionReceiver")
            buttonIntent.setClass(context, IterablePushActionReceiver::class.java)
            PendingIntent.getBroadcast(
                context, buttonIntent.hashCode(),
                buttonIntent, pendingIntentFlag
            )
        }

        return pendingButtonIntent
    }

    companion object {
        const val TAG = "IterableNotification"

        /**
         * Creates and returns an instance of IterableNotification.
         * This is kept here for backwards compatibility.
         *
         * @param context
         * @param extras
         * @return Returns null if the intent comes from an Iterable ghostPush or it is not an Iterable notification
         */
        @JvmStatic
        fun createNotification(context: Context, extras: Bundle): IterableNotificationBuilder? {
            return IterableNotificationHelper.createNotification(context, extras)
        }

        /**
         * Posts the notification on device.
         * Only sets the notification if it is not a ghostPush/null iterableNotification.
         * This is kept here for backwards compatibility.
         *
         * @param context
         * @param iterableNotificationBuilder Function assumes that the iterableNotification is a ghostPush
         *                             if the IterableNotification passed in is null.
         */
        @JvmStatic
        fun postNotificationOnDevice(context: Context, iterableNotificationBuilder: IterableNotificationBuilder) {
            IterableNotificationHelper.postNotificationOnDevice(context, iterableNotificationBuilder)
        }
    }
}