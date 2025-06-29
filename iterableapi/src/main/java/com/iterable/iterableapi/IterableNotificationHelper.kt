package com.iterable.iterableapi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import org.json.JSONException
import org.json.JSONObject

internal class IterableNotificationHelper {
    companion object {
        private const val DEFAULT_CHANNEL_NAME = "iterable channel"
        private const val NO_BADGE = "_noBadge"

        @VisibleForTesting
        var instance = IterableNotificationHelperImpl()

        /**
         * Creates and returns an instance of IterableNotification.
         *
         * @param context
         * @param extras
         * @return Returns null if the intent comes from an Iterable ghostPush or it is not an Iterable notification
         */
        @JvmStatic
        fun createNotification(context: Context, extras: Bundle): IterableNotificationBuilder? {
            return instance.createNotification(context, extras)
        }

        /**
         * Posts the notification on device.
         * Only sets the notification if it is not a ghostPush/null iterableNotification.
         *
         * @param context
         * @param iterableNotificationBuilder Function assumes that the iterableNotification is a ghostPush
         *                             if the IterableNotification passed in is null.
         */
        @JvmStatic
        fun postNotificationOnDevice(context: Context, iterableNotificationBuilder: IterableNotificationBuilder) {
            instance.postNotificationOnDevice(context, iterableNotificationBuilder)
        }

        /**
         * Gets the main activity intent - the same intent as the one used to launch the app from launcher.
         * @param context Context
         * @return Main launch intent
         */
        @JvmStatic
        fun getMainActivityIntent(context: Context): Intent {
            return instance.getMainActivityIntent(context)
        }

        @JvmStatic
        fun isIterablePush(extras: Bundle): Boolean {
            return instance.isIterablePush(extras)
        }

        /**
         * Returns if the given notification is a ghost/silent push notification
         *
         * @param extras
         * @return
         */
        @JvmStatic
        fun isGhostPush(extras: Bundle): Boolean {
            return instance.isGhostPush(extras)
        }

        /**
         * Returns if the given notification has an empty body
         * @param extras
         * @return
         */
        @JvmStatic
        fun isEmptyBody(extras: Bundle): Boolean {
            return instance.isEmptyBody(extras)
        }

        @JvmStatic
        fun mapToBundle(map: Map<String, String>): Bundle {
            val bundle = Bundle()
            for ((key, value) in map) {
                bundle.putString(key, value)
            }
            return bundle
        }

        @Nullable
        private fun getAudioAttributes(): AudioAttributes? {
            var audioAttributes: AudioAttributes? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            return audioAttributes
        }

        private fun getSoundUri(context: Context, soundName: String?, soundUrl: String?): Uri {
            var soundId = 0

            if (soundUrl != null) {
                return Uri.parse(soundUrl)
            }

            if (soundName != null) {
                soundId = context.resources.getIdentifier(soundName, IterableConstants.SOUND_FOLDER_IDENTIFIER, context.packageName)
            }

            if (soundId == 0) {
                return Settings.System.DEFAULT_NOTIFICATION_URI
            }

            return Uri.parse(IterableConstants.ANDROID_RESOURCE_PATH + context.packageName + "/" + soundId)
        }
    }

    class IterableNotificationHelperImpl {

        fun createNotification(context: Context, extras: Bundle): IterableNotificationBuilder? {
            val applicationName = context.applicationInfo.loadLabel(context.packageManager).toString()
            var title: String?
            var notificationBody: String?
            var soundName: String?
            var messageId: String?
            var pushImage: String?
            var soundUri: Uri
            //TODO: When backend supports channels, these strings needs to change (channelName, channelId, channelDescription).
            val channelDescription = ""

            if (!extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
                IterableLogger.w(IterableNotificationBuilder.TAG, "Notification doesn't have an Iterable payload. Skipping.")
                return null
            }

            if (isGhostPush(extras)) {
                IterableLogger.w(IterableNotificationBuilder.TAG, "Received a ghost push notification. Skipping.")
                return null
            }

            var iterableJson: JSONObject?
            title = extras.getString(IterableConstants.ITERABLE_DATA_TITLE, applicationName)
            notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY)
            soundName = extras.getString(IterableConstants.ITERABLE_DATA_SOUND)
            var soundUrl: String? = null

            //Check if soundName is a remote sound file
            if (soundName != null) {

                // If soundname contains remote link, store it as a soundUrl and continue to trim soundName to for channel Id and name
                if (soundName.contains("https")) {
                    soundUrl = soundName
                    soundName = soundName.substring(soundName.lastIndexOf('/') + 1)
                }

                // Remove extension of sound file
                soundName = soundName.replaceFirst("[.][^.]+$".toRegex(), "")
            }

            soundUri = getSoundUri(context, soundName, soundUrl)

            val channelName = if (soundUri == Settings.System.DEFAULT_NOTIFICATION_URI) 
                getChannelName(context) 
            else 
                soundName

            val channelId = if (soundUri == Settings.System.DEFAULT_NOTIFICATION_URI) 
                context.packageName 
            else 
                getCurrentChannelId(context, soundName)

            val notificationBuilder = IterableNotificationBuilder(context, channelId)

            val iterableData = extras.getString(IterableConstants.ITERABLE_DATA_KEY)

            try {
                iterableJson = JSONObject(iterableData!!)

                if (iterableJson.has(IterableConstants.ITERABLE_DATA_PUSH_IMAGE)) {
                    pushImage = iterableJson.getString(IterableConstants.ITERABLE_DATA_PUSH_IMAGE)
                }
            } catch (e: JSONException) {
                IterableLogger.w(IterableNotificationBuilder.TAG, e.toString())
            }

            val notificationData = IterableNotificationData(iterableData)
            notificationBuilder.iterableNotificationData = notificationData
            messageId = notificationBuilder.iterableNotificationData!!.messageId

            val notifPermissions = Notification()
            notifPermissions.defaults = notifPermissions.defaults or Notification.DEFAULT_LIGHTS

            notificationBuilder
                .setSmallIcon(getIconId(context))
                .setTicker(applicationName)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(notificationBody)
            if (Build.VERSION.SDK_INT >= 17) {
                notificationBuilder.setShowWhen(true)
            }
            notificationBuilder.setImageUrl(pushImage)
            notificationBuilder.setExpandedContent(notificationBody)

            // The notification doesn't cancel properly if requestCode is negative
            notificationBuilder.requestCode = Math.abs(System.currentTimeMillis().toInt())
            IterableLogger.d(IterableNotificationBuilder.TAG, "Request code = " + notificationBuilder.requestCode)
            if (messageId != null) {
                notificationBuilder.requestCode = Math.abs(messageId.hashCode())
                IterableLogger.d(IterableNotificationBuilder.TAG, "Request code = " + notificationBuilder.requestCode)
            }

            //Create an intent for TrampolineActivity instead of BroadcastReceiver
            val trampolineActivityIntent = Intent(IterableConstants.ACTION_PUSH_ACTION)
            trampolineActivityIntent.setClass(context, IterableTrampolineActivity::class.java)
            trampolineActivityIntent.putExtras(extras)
            trampolineActivityIntent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, IterableConstants.ITERABLE_ACTION_DEFAULT)
            trampolineActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Action buttons
            if (notificationData.actionButtons != null) {
                var buttonCount = 0
                for (button in notificationData.actionButtons!!) {
                    notificationBuilder.createNotificationActionButton(context, button, extras)
                    if (++buttonCount == 3) break
                }
            }

            val notificationClickedIntent = PendingIntent.getActivity(
                context, notificationBuilder.requestCode,
                trampolineActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder.setContentIntent(notificationClickedIntent)
            notificationBuilder.setIsGhostPush(isGhostPush(extras))

            try {
                val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                if (info.metaData != null) {
                    var color = info.metaData.getInt(IterableConstants.NOTIFICATION_COLOR)
                    try {
                        color = context.resources.getColor(color)
                    } catch (ignored: Resources.NotFoundException) {
                    }
                    notificationBuilder.setColor(color)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            val pm = context.packageManager
            if (pm.checkPermission(android.Manifest.permission.VIBRATE, context.packageName) == PackageManager.PERMISSION_GRANTED) {
                notifPermissions.defaults = notifPermissions.defaults or Notification.DEFAULT_VIBRATE
            }

            notificationBuilder.setDefaults(notifPermissions.defaults)

            removeUnusedChannel(context, soundName)
            registerChannelIfEmpty(context, channelId, channelName, channelDescription, soundUri)

            return notificationBuilder
        }

        fun getMainActivityIntent(context: Context): Intent {
            val appContext = context.applicationContext
            val packageManager = appContext.packageManager
            var intent = packageManager.getLaunchIntentForPackage(appContext.packageName)
            if (intent == null) {
                intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.setPackage(appContext.packageName)
            }
            return intent
        }

        fun postNotificationOnDevice(context: Context, iterableNotificationBuilder: IterableNotificationBuilder) {
            if (!iterableNotificationBuilder.isGhostPush()) {
                val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(iterableNotificationBuilder.requestCode, iterableNotificationBuilder.build())
            }
        }

        /**
         * Creates the notification channel on device.
         * Only creates the notification channel if application does not have notification channel created.
         *
         * @param context
         * @param channelId          Determines the channel Id. This distinguishes if the app has different channel or not.
         * @param channelName        Sets the channel name that is shown to the user.
         * @param channelDescription Sets the channel description that is shown to the user.
         */
        private fun registerChannelIfEmpty(context: Context, channelId: String, channelName: String?, channelDescription: String, soundUri: Uri) {
            val mNotificationManager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mNotificationManager != null) {
                val existingChannel = mNotificationManager.getNotificationChannel(channelId)
                if (existingChannel == null || existingChannel.name != channelName) {
                    IterableLogger.d(
                        IterableNotificationBuilder.TAG, "Creating notification: channelId = " + channelId + " channelName = " +
                                channelName + " channelDescription = " + channelDescription
                    )
                    mNotificationManager.createNotificationChannel(createNotificationChannel(channelId, channelName, channelDescription, context, soundUri))
                }
            }
        }

        /**
         * Safely removes unused and old channel if the configuration for notification badge is changed.
         */
        private fun removeUnusedChannel(context: Context, soundName: String?) {
            val mNotificationManager = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mNotificationManager != null) {
                val channelIdToDelete = getOldChannelId(context, soundName)
                val unusedChannel = mNotificationManager.getNotificationChannel(channelIdToDelete)
                if (unusedChannel != null) {
                    for (activeNotification in mNotificationManager.activeNotifications) {
                        if (activeNotification.notification.channelId == channelIdToDelete) {
                            IterableLogger.d(IterableNotificationBuilder.TAG, "Not Deleting the channel as there are active notification for old channel")
                            return
                        }
                    }
                    mNotificationManager.deleteNotificationChannel(channelIdToDelete)
                }
            }
        }

        private fun createNotificationChannel(channelId: String, channelName: String?, channelDescription: String, context: Context, soundUri: Uri): NotificationChannel? {
            var notificationChannel: NotificationChannel? = null
            val audioAttributes = getAudioAttributes()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.description = channelDescription
                notificationChannel.enableLights(true)
                notificationChannel.setShowBadge(isNotificationBadgingEnabled(context))
                notificationChannel.setSound(soundUri, audioAttributes)
            }

            return notificationChannel
        }

        private fun getCurrentChannelId(context: Context, soundName: String?): String {
            return getChannelIdName(context, true, soundName)
        }

        private fun getOldChannelId(context: Context, soundName: String?): String {
            return getChannelIdName(context, false, soundName)
        }

        private fun getChannelIdName(context: Context, isActive: Boolean, soundName: String?): String {
            var channelId = context.packageName

            if (soundName != null) {
                channelId = soundName
            }

            if (isActive) {
                if (!isNotificationBadgingEnabled(context)) {
                    channelId = channelId + NO_BADGE
                }
            } else {
                if (isNotificationBadgingEnabled(context)) {
                    channelId = channelId + NO_BADGE
                }
            }
            return channelId
        }

        private fun getChannelName(context: Context): String? {
            var channelName: String? = null
            try {
                val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                if (info.metaData != null) {
                    val channelNameMetaData = info.metaData.get(IterableConstants.NOTIFICATION_CHANNEL_NAME)
                    if (channelNameMetaData is String) {
                        // Literal string value
                        channelName = channelNameMetaData
                    } else if (channelNameMetaData is Int) {
                        // Try to read from a string resource
                        val stringId = channelNameMetaData
                        if (stringId != 0) {
                            channelName = context.getString(stringId)
                        }
                    }
                    IterableLogger.d(IterableNotificationBuilder.TAG, "channel name: $channelName")
                }
            } catch (e: Exception) {
                IterableLogger.e(IterableNotificationBuilder.TAG, "Error while retrieving channel name", e)
            }

            return if (channelName != null) {
                channelName
            } else {
                DEFAULT_CHANNEL_NAME
            }
        }

        /**
         * Returns the iconId from potential resource locations
         *
         * @param context
         * @return
         */
        private fun getIconId(context: Context): Int {
            var iconId = 0

            //Get the iconId set in the AndroidManifest.xml
            if (iconId == 0) {
                try {
                    val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                    if (info.metaData != null) {
                        iconId = info.metaData.getInt(IterableConstants.NOTIFICATION_ICON_NAME, 0)
                        IterableLogger.d(IterableNotificationBuilder.TAG, "iconID: " + info.metaData.get(IterableConstants.NOTIFICATION_ICON_NAME))
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }

            //Get the iconId set in code
            if (iconId == 0) {
                iconId = context.resources.getIdentifier(
                    IterableApi.getNotificationIcon(context),
                    IterableConstants.ICON_FOLDER_IDENTIFIER,
                    context.packageName
                )
            }

            //Get id from the default app settings
            if (iconId == 0) {
                if (context.applicationInfo.icon != 0) {
                    IterableLogger.d(IterableNotificationBuilder.TAG, "No Notification Icon defined - defaulting to app icon")
                    iconId = context.applicationInfo.icon
                } else {
                    IterableLogger.w(IterableNotificationBuilder.TAG, "No Notification Icon defined - push notifications will not be displayed")
                }
            }

            return iconId
        }

        fun isIterablePush(extras: Bundle?): Boolean {
            return extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)
        }

        fun isGhostPush(extras: Bundle): Boolean {
            var isGhostPush = false
            if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
                val iterableData = extras.getString(IterableConstants.ITERABLE_DATA_KEY)
                val data = IterableNotificationData(iterableData)
                isGhostPush = data.isGhostPush
            }

            return isGhostPush
        }

        fun isEmptyBody(extras: Bundle): Boolean {
            var notificationBody = ""
            if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
                notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY, "")
            }

            return notificationBody.isEmpty()
        }

        companion object {
            private fun isNotificationBadgingEnabled(context: Context): Boolean {
                try {
                    val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                    if (info.metaData != null) {
                        return info.metaData.getBoolean(IterableConstants.NOTIFICAION_BADGING, true)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    IterableLogger.e(IterableNotificationBuilder.TAG, e.localizedMessage + " Failed to read notification badge settings. Setting to defaults - true")
                }
                return true
            }
        }
    }
}