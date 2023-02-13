package com.iterable.iterableapi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class IterableNotificationHelper {
    private static final String DEFAULT_CHANNEL_NAME = "iterable channel";
    private static final String NO_BADGE = "_noBadge";

    @VisibleForTesting
    static IterableNotificationHelperImpl instance = new IterableNotificationHelperImpl();

    /**
     * Creates and returns an instance of IterableNotification.
     *
     * @param context
     * @param extras
     * @return Returns null if the intent comes from an Iterable ghostPush or it is not an Iterable notification
     */
    public static IterableNotificationBuilder createNotification(Context context, Bundle extras) {
        return instance.createNotification(context, extras);
    }

    /**
     * Posts the notification on device.
     * Only sets the notification if it is not a ghostPush/null iterableNotification.
     *
     * @param context
     * @param iterableNotificationBuilder Function assumes that the iterableNotification is a ghostPush
     *                             if the IterableNotification passed in is null.
     */
    public static void postNotificationOnDevice(Context context, IterableNotificationBuilder iterableNotificationBuilder) {
        instance.postNotificationOnDevice(context, iterableNotificationBuilder);
    }

    /**
     * Gets the main activity intent - the same intent as the one used to launch the app from launcher.
     * @param context Context
     * @return Main launch intent
     */
    public static Intent getMainActivityIntent(Context context) {
        return instance.getMainActivityIntent(context);
    }

    static boolean isIterablePush(Bundle extras) {
        return instance.isIterablePush(extras);
    }

    /**
     * Returns if the given notification is a ghost/silent push notification
     *
     * @param extras
     * @return
     */
    static boolean isGhostPush(Bundle extras) {
        return instance.isGhostPush(extras);
    }

    /**
     * Returns if the given notification has an empty body
     * @param extras
     * @return
     */
    static boolean isEmptyBody(Bundle extras) {
        return instance.isEmptyBody(extras);
    }

    static Bundle mapToBundle(Map<String, String> map) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        return bundle;
    }

    static class IterableNotificationHelperImpl {

        public IterableNotificationBuilder createNotification(Context context, Bundle extras) {
            String applicationName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
            String title = null;
            String notificationBody = null;
            String soundName = null;
            String messageId = null;
            String pushImage = null;
            int soundResourceId = 0;
            //TODO: When backend supports channels, these strings needs to change (channelName, channelId, channelDescription).
            String channelDescription = "";

            if (!extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
                IterableLogger.w(IterableNotificationBuilder.TAG, "Notification doesn't have an Iterable payload. Skipping.");
                return null;
            }

            if (isGhostPush(extras)) {
                IterableLogger.w(IterableNotificationBuilder.TAG, "Received a ghost push notification. Skipping.");
                return null;
            }

            JSONObject iterableJson = null;
            title = extras.getString(IterableConstants.ITERABLE_DATA_TITLE, applicationName);
            notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY);
            soundName = extras.getString(IterableConstants.ITERABLE_DATA_SOUND);

            if (soundName != null && soundName.contains(".") && soundName.charAt(0) != '.') {
                String[] soundFile = soundName.split("\\.");
                soundName = soundFile[0];
                soundResourceId = context.getResources().getIdentifier(soundFile[0], "raw", context.getPackageName());
            }

            String channelName = getChannelName(context, soundName, soundResourceId);
            IterableNotificationBuilder notificationBuilder = new IterableNotificationBuilder(context, getChannelId(context, soundName, soundResourceId));

            String iterableData = extras.getString(IterableConstants.ITERABLE_DATA_KEY);

            try {
                iterableJson = new JSONObject(iterableData);

                if (iterableJson.has(IterableConstants.ITERABLE_DATA_PUSH_IMAGE)) {
                    pushImage = iterableJson.getString(IterableConstants.ITERABLE_DATA_PUSH_IMAGE);
                }
            } catch (JSONException e) {
                IterableLogger.w(IterableNotificationBuilder.TAG, e.toString());
            }

            IterableNotificationData notificationData = new IterableNotificationData(iterableData);
            notificationBuilder.iterableNotificationData = notificationData;
            messageId = notificationBuilder.iterableNotificationData.getMessageId();

            Notification notifPermissions = new Notification();
            notifPermissions.defaults |= Notification.DEFAULT_LIGHTS;

            notificationBuilder
                    .setSmallIcon(getIconId(context))
                    .setTicker(applicationName)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentText(notificationBody);
            if (Build.VERSION.SDK_INT >= 17) {
                notificationBuilder.setShowWhen(true);
            }
            notificationBuilder.setImageUrl(pushImage);
            notificationBuilder.setExpandedContent(notificationBody);

            // The notification doesn't cancel properly if requestCode is negative
            notificationBuilder.requestCode = Math.abs((int) System.currentTimeMillis());
            IterableLogger.d(IterableNotificationBuilder.TAG, "Request code = " + notificationBuilder.requestCode);
            if (messageId != null) {
                notificationBuilder.requestCode = Math.abs(messageId.hashCode());
                IterableLogger.d(IterableNotificationBuilder.TAG, "Request code = " + notificationBuilder.requestCode);
            }

            //Create an intent for TrampolineActivity instead of BroadcastReceiver
            Intent trampolineActivityIntent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
            trampolineActivityIntent.setClass(context, IterableTrampolineActivity.class);
            trampolineActivityIntent.putExtras(extras);
            trampolineActivityIntent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, IterableConstants.ITERABLE_ACTION_DEFAULT);
            trampolineActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Action buttons
            if (notificationData.getActionButtons() != null) {
                int buttonCount = 0;
                for (IterableNotificationData.Button button : notificationData.getActionButtons()) {
                    notificationBuilder.createNotificationActionButton(context, button, extras);
                    if (++buttonCount == 3)
                        break;
                }
            }

            PendingIntent notificationClickedIntent = PendingIntent.getActivity(context, notificationBuilder.requestCode,
                    trampolineActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            notificationBuilder.setContentIntent(notificationClickedIntent);
            notificationBuilder.setIsGhostPush(isGhostPush(extras));

            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                if (info.metaData != null) {
                    int color = info.metaData.getInt(IterableConstants.NOTIFICATION_COLOR);
                    try {
                        color = context.getResources().getColor(color);
                    } catch (Resources.NotFoundException ignored) {}
                    notificationBuilder.setColor(color);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            PackageManager pm = context.getPackageManager();
            if (pm.checkPermission(android.Manifest.permission.VIBRATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                notifPermissions.defaults |= Notification.DEFAULT_VIBRATE;
            }

            notificationBuilder.setDefaults(notifPermissions.defaults);

            removeUnusedChannel(context, soundName, soundResourceId);
            registerChannelIfEmpty(context, getChannelId(context, soundName, soundResourceId), channelName, channelDescription, soundName, soundResourceId);

            return notificationBuilder;
        }

        public Intent getMainActivityIntent(Context context) {
            Context appContext = context.getApplicationContext();
            PackageManager packageManager = appContext.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(appContext.getPackageName());
            if (intent == null) {
                intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(appContext.getPackageName());
            }
            return intent;
        }

        public void postNotificationOnDevice(Context context, IterableNotificationBuilder iterableNotificationBuilder) {
            if (!iterableNotificationBuilder.isGhostPush()) {
                NotificationManager mNotificationManager = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(iterableNotificationBuilder.requestCode, iterableNotificationBuilder.build());
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
        private void registerChannelIfEmpty(Context context, String channelId, String channelName, String channelDescription, String soundName, int soundResourceId) {
            NotificationManager mNotificationManager = (NotificationManager)
                    context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    && mNotificationManager != null) {
                NotificationChannel existingChannel = mNotificationManager.getNotificationChannel(channelId);
                if (existingChannel == null || !existingChannel.getName().equals(channelName)) {
                    IterableLogger.d(IterableNotificationBuilder.TAG, "Creating notification: channelId = " + channelId + " channelName = "
                            + channelName + " channelDescription = " + channelDescription);
                    mNotificationManager.createNotificationChannel(createNotificationChannel(channelId, channelName, channelDescription, context, soundName, soundResourceId));
                }
            }
        }

        /**
         * Safely removes unused and old channel if the configuration for notification badge is changed.
         */
        private void removeUnusedChannel(Context context, String soundName, int soundResourceId) {
            NotificationManager mNotificationManager = (NotificationManager)
                    context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    && mNotificationManager != null) {
                String channelIdToDelete = getOldChannelId(context, soundName, soundResourceId);
                NotificationChannel unusedChannel = mNotificationManager.getNotificationChannel(channelIdToDelete);
                if (unusedChannel != null) {
                    for (StatusBarNotification activeNotification : mNotificationManager.getActiveNotifications()) {
                        if (activeNotification.getNotification().getChannelId() == channelIdToDelete) {
                            IterableLogger.d(IterableNotificationBuilder.TAG, "Not Deleting the channel as there are active notification for old channel");
                            return;
                        }
                    }
                    mNotificationManager.deleteNotificationChannel(channelIdToDelete);
                }
            }
        }

        private NotificationChannel createNotificationChannel(String channelId, String channelName, String channelDescription, Context context, String soundName, int soundResourceId) {
            NotificationChannel notificationChannel = null;
            Uri soundUri = null;
            AudioAttributes audioAttributes = null;

            soundUri = getSoundUri(context, soundName, soundResourceId);
            audioAttributes = getAudioAttributes(audioAttributes);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription(channelDescription);
                notificationChannel.enableLights(true);
                notificationChannel.setShowBadge(isNotificationBadgingEnabled(context));
                notificationChannel.setSound(soundUri, audioAttributes);
            }

            return notificationChannel;
        }

        private static boolean isNotificationBadgingEnabled(Context context) {
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                if (info.metaData != null) {
                    return info.metaData.getBoolean(IterableConstants.NOTIFICAION_BADGING, true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                IterableLogger.e(IterableNotificationBuilder.TAG, e.getLocalizedMessage() + " Failed to read notification badge settings. Setting to defaults - true");
            }
            return true;
        }

        private String getChannelId(Context context, String soundName, int soundResourceId) {
            return getChannelIdName(context, true, soundName, soundResourceId);
        }

        private String getOldChannelId(Context context, String soundName, int soundResourceId) {
            return getChannelIdName(context, false, soundName, soundResourceId);
        }

        private String getChannelIdName(Context context, boolean isActive, String soundName, int soundResourceId) {
            String channelId = context.getPackageName();

            if (soundResourceId != 0) {
                channelId = soundName;
            }

            if (isActive) {
                if (!isNotificationBadgingEnabled(context)) {
                    channelId = channelId + NO_BADGE;
                }
            } else {
                if (isNotificationBadgingEnabled(context)) {
                    channelId = channelId + NO_BADGE;
                }
            }
            return channelId;
        }

        private String getChannelName(Context context, String soundName, int soundResourceId) {
            String channelName = "Default";

            if (soundResourceId != 0) {
                channelName = soundName;
            }

            if (channelName != null) {
                return channelName;
            } else {
                return DEFAULT_CHANNEL_NAME;
            }
        }

        /**
         * Returns the iconId from potential resource locations
         *
         * @param context
         * @return
         */
        private int getIconId(Context context) {
            int iconId = 0;

            //Get the iconId set in the AndroidManifest.xml
            if (iconId == 0) {
                try {
                    ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                    if (info.metaData != null) {
                        iconId = info.metaData.getInt(IterableConstants.NOTIFICATION_ICON_NAME, 0);
                        IterableLogger.d(IterableNotificationBuilder.TAG, "iconID: " + info.metaData.get(IterableConstants.NOTIFICATION_ICON_NAME));
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }

            //Get the iconId set in code
            if (iconId == 0) {
                iconId = context.getResources().getIdentifier(
                        IterableApi.getNotificationIcon(context),
                        IterableConstants.ICON_FOLDER_IDENTIFIER,
                        context.getPackageName());
            }

            //Get id from the default app settings
            if (iconId == 0) {
                if (context.getApplicationInfo().icon != 0) {
                    IterableLogger.d(IterableNotificationBuilder.TAG, "No Notification Icon defined - defaulting to app icon");
                    iconId = context.getApplicationInfo().icon;
                } else {
                    IterableLogger.w(IterableNotificationBuilder.TAG, "No Notification Icon defined - push notifications will not be displayed");
                }
            }

            return iconId;
        }

        boolean isIterablePush(Bundle extras) {
            return extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY);
        }

        boolean isGhostPush(Bundle extras) {
            boolean isGhostPush = false;
            if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
                String iterableData = extras.getString(IterableConstants.ITERABLE_DATA_KEY);
                IterableNotificationData data = new IterableNotificationData(iterableData);
                isGhostPush = data.getIsGhostPush();
            }

            return isGhostPush;
        }

        boolean isEmptyBody(Bundle extras) {
            String notificationBody = "";
            if (extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
                notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY, "");
            }

            return notificationBody.isEmpty();
        }
    }

    private static AudioAttributes getAudioAttributes(AudioAttributes audioAttributes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
        }
        return audioAttributes;
    }

    private static Uri getSoundUri(Context context, String soundName, int soundResourceId) {
        if (soundResourceId == 0 || soundName == null) {
            return Settings.System.DEFAULT_NOTIFICATION_URI;
        }

        int soundID = context.getResources().getIdentifier(soundName, IterableConstants.SOUND_FOLDER_IDENTIFIER, context.getPackageName());
        return Uri.parse(IterableConstants.ANDROID_RESOURCE_PATH + context.getPackageName() + "/" + soundID);
    }

}
