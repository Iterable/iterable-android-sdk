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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
            //TODO: When backend supports channels, these strings needs to change (channelName, channelId, channelDescription).
            String channelName = getChannelName(context);
            String channelDescription = "";

            if (!extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
                IterableLogger.w(IterableNotificationBuilder.TAG, "Notification doesn't have an Iterable payload. Skipping.");
                return null;
            }

            if (isGhostPush(extras)) {
                IterableLogger.w(IterableNotificationBuilder.TAG, "Received a ghost push notification. Skipping.");
                return null;
            }

            removeUnusedChannel(context);
            registerChannelIfEmpty(context, getChannelId(context), channelName, channelDescription);
            IterableNotificationBuilder notificationBuilder = new IterableNotificationBuilder(context, getChannelId(context));
            JSONObject iterableJson = null;
            title = extras.getString(IterableConstants.ITERABLE_DATA_TITLE, applicationName);
            notificationBody = extras.getString(IterableConstants.ITERABLE_DATA_BODY);
            soundName = extras.getString(IterableConstants.ITERABLE_DATA_SOUND);

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

            if (soundName != null) {
                //Removes the file type from the name
                String[] soundFile = soundName.split("\\.");
                soundName = soundFile[0];

                if (!soundName.equalsIgnoreCase(IterableConstants.DEFAULT_SOUND)) {
                    int soundID = context.getResources().getIdentifier(soundName, IterableConstants.SOUND_FOLDER_IDENTIFIER, context.getPackageName());
                    Uri soundUri = Uri.parse(IterableConstants.ANDROID_RESOURCE_PATH + context.getPackageName() + "/" + soundID);
                    notificationBuilder.setSound(soundUri);
                } else {
                    notifPermissions.defaults |= Notification.DEFAULT_SOUND;
                }

            } else {
                notifPermissions.defaults |= Notification.DEFAULT_SOUND;
            }

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
        private void registerChannelIfEmpty(Context context, String channelId, String channelName, String channelDescription) {
            NotificationManager mNotificationManager = (NotificationManager)
                    context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    && mNotificationManager != null) {
                NotificationChannel existingChannel = mNotificationManager.getNotificationChannel(channelId);
                if (existingChannel == null || !existingChannel.getName().equals(channelName)) {
                    IterableLogger.d(IterableNotificationBuilder.TAG, "Creating notification: channelId = " + channelId + " channelName = "
                            + channelName + " channelDescription = " + channelDescription);
                    mNotificationManager.createNotificationChannel(createNotificationChannel(channelId, channelName, channelDescription, context));
                }
            }
        }

        /**
         * Removes unused old channel if the configuration for notification badge is changed.
         */
        private void removeUnusedChannel(Context context) {
            NotificationManager mNotificationManager = (NotificationManager)
                    context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                    && mNotificationManager != null) {
                String channelIdToDelete = getOldChannelId(context);
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

        private NotificationChannel createNotificationChannel(String channelId, String channelName, String channelDescription, Context context) {
            NotificationChannel notificationChannel = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription(channelDescription);
                notificationChannel.enableLights(true);
                notificationChannel.setShowBadge(isNotificationBadgingEnabled(context));
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
                IterableLogger.e(IterableNotificationBuilder.TAG, e.getLocalizedMessage() + " Defaulting the badging to true");
            }
            return true;
        }

        private String getChannelId(Context context) {
            getChannelIdName(context, !isNotificationBadgingEnabled(context))
        }
        
        private String getOldChannelId(Context context) {
            getChannelIdName(context, isNotificationBadgingEnabled(context))
        }
        
        private String getChannelIdName(Context context, boolean badgingEnabled) {
            String channelId = context.getPackageName();
            if (!isNotificationBadgingEnabled(context)) {
                channelId = channelId + NO_BADGE;
            }
            return channelId;
        }

        private String getChannelName(Context context) {
            String channelName = null;
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                if (info.metaData != null) {
                    Object channelNameMetaData = info.metaData.get(IterableConstants.NOTIFICATION_CHANNEL_NAME);
                    if (channelNameMetaData instanceof String) {
                        // Literal string value
                        channelName = (String) channelNameMetaData;
                    } else if (channelNameMetaData instanceof Integer) {
                        // Try to read from a string resource
                        int stringId = (Integer) channelNameMetaData;
                        if (stringId != 0) {
                            channelName = context.getString(stringId);
                        }
                    }
                    IterableLogger.d(IterableNotificationBuilder.TAG, "channel name: " + channelName);
                }
            } catch (Exception e) {
                IterableLogger.e(IterableNotificationBuilder.TAG, "Error while retrieving channel name", e);
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

}
