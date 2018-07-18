package com.iterable.iterableapi;

/**
 * Created by David Truong dt@iterable.com
 *
 * IterableConstants contains a list of constants used with the Iterable mobile SDK.
 */
public final class IterableConstants {
    public static final String ACTION_NOTIF_OPENED = "com.iterable.push.ACTION_NOTIF_OPENED";
    public static final String ACTION_PUSH_ACTION = "com.iterable.push.ACTION_PUSH_ACTION";
    public static final String ACTION_PUSH_REGISTRATION = "com.iterable.push.ACTION_PUSH_REGISTRATION";

    //API Fields
    public static final String KEY_API_KEY              = "api_key";
    public static final String KEY_APPLICATION_NAME     = "applicationName";
    public static final String KEY_CAMPAIGN_ID          = "campaignId";
    public static final String KEY_CURRENT_EMAIL        = "currentEmail";
    public static final String KEY_DATA_FIELDS          = "dataFields";
    public static final String KEY_DEVICE               = "device";
    public static final String KEY_EMAIL                = "email";
    public static final String KEY_EMAIL_LIST_IDS       = "emailListIds";
    public static final String KEY_EVENT_NAME           = "eventName";
    public static final String KEY_ITEMS                = "items";
    public static final String KEY_NEW_EMAIL            = "newEmail";
    public static final String KEY_PLATFORM             = "platform";
    public static final String KEY_RECIPIENT_EMAIL      = "recipientEmail";
    public static final String KEY_SEND_AT              = "sendAt";
    public static final String KEY_TEMPLATE_ID          = "templateId";
    public static final String KEY_MESSAGE_ID           = "messageId";
    public static final String KEY_TOKEN                = "token";
    public static final String KEY_TOTAL                = "total";
    public static final String KEY_UNSUB_CHANNEL        = "unsubscribedChannelIds";
    public static final String KEY_UNSUB_MESSAGE        = "unsubscribedMessageTypeIds";
    public static final String KEY_USER_ID              = "userId";
    public static final String KEY_USER                 = "user";
    public static final String KEY_USER_TEXT            = "userText";

    //API Endpoint Key Constants
    public static final String ENDPOINT_DISABLE_DEVICE          = "users/disableDevice";
    public static final String ENDPOINT_GET_INAPP_MESSAGES      = "inApp/getMessages";
    public static final String ENDPOINT_INAPP_CONSUME           = "events/inAppConsume";
    public static final String ENDPOINT_PUSH_TARGET             = "push/target";
    public static final String ENDPOINT_REGISTER_DEVICE_TOKEN   = "users/registerDeviceToken";
    public static final String ENDPOINT_TRACK                   = "events/track";
    public static final String ENDPOINT_TRACK_INAPP_CLICK       = "events/trackInAppClick";
    public static final String ENDPOINT_TRACK_INAPP_OPEN        = "events/trackInAppOpen";
    public static final String ENDPOINT_TRACK_PURCHASE          = "commerce/trackPurchase";
    public static final String ENDPOINT_TRACK_PUSH_OPEN         = "events/trackPushOpen";
    public static final String ENDPOINT_UPDATE_USER             = "users/update";
    public static final String ENDPOINT_UPDATE_EMAIL            = "users/updateEmail";
    public static final String ENDPOINT_UPDATE_USER_SUBS        = "users/updateSubscriptions";

    public static final String PUSH_APP_ID                      = "IterableAppId";
    public static final String PUSH_GCM_PROJECT_NUMBER          = "GCMProjectNumber";
    public static final String PUSH_DISABLE_AFTER_REGISTRATION  = "DisableAfterRegistration";

    public static final String MESSAGING_PUSH_SERVICE_PLATFORM  = "PushServicePlatform";
    static final String MESSAGING_PLATFORM_GOOGLE               = "GCM"; // Deprecated, only used internally
    public static final String MESSAGING_PLATFORM_FIREBASE      = "FCM";
    public static final String MESSAGING_PLATFORM_AMAZON        = "ADM";

    public static final String IS_GHOST_PUSH        = "isGhostPush";
    public static final String ITERABLE_DATA_ACTION_IDENTIFIER = "actionIdentifier";
    public static final String ITERABLE_ACTION_DEFAULT = "default";
    public static final String ITERABLE_DATA_BADGE  = "badge";
    public static final String ITERABLE_DATA_BODY   = "body";
    public static final String ITERABLE_DATA_KEY    = "itbl";
    public static final String ITERABLE_DATA_DEEP_LINK_URL = "uri";
    public static final String ITERABLE_DATA_PUSH_IMAGE  = "attachment-url";
    public static final String ITERABLE_DATA_SOUND  = "sound";
    public static final String ITERABLE_DATA_TITLE  = "title";
    public static final String ITERABLE_DATA_ACTION_BUTTONS  = "actionButtons";
    public static final String ITERABLE_DATA_DEFAULT_ACTION  = "defaultAction";

    //SharedPreferences keys
    public static final String SHARED_PREFS_FILE = "com.iterable.iterableapi";
    public static final String SHARED_PREFS_EMAIL_KEY = "itbl_email";
    public static final String SHARED_PREFS_USERID_KEY = "itbl_userid";
    public static final String SHARED_PREFS_EXPIRATION_SUFFIX = "_expiration";
    public static final String SHARED_PREFS_OBJECT_SUFFIX = "_object";
    public static final String SHARED_PREFS_PAYLOAD_KEY = "itbl_payload";
    public static final int    SHARED_PREFS_PAYLOAD_EXPIRATION_HOURS = 24;
    public static final String SHARED_PREFS_ATTRIBUTION_INFO_KEY = "itbl_attribution_info";
    public static final int    SHARED_PREFS_ATTRIBUTION_INFO_EXPIRATION_HOURS = 24;
    public static final String SHARED_PREFS_FCM_MIGRATION_DONE_KEY = "itbl_fcm_migration_done";

    //Action buttons
    public static final String ITBL_BUTTON_IDENTIFIER        = "identifier";
    public static final String ITBL_BUTTON_TYPE              = "buttonType";
    public static final String ITBL_BUTTON_TITLE             = "title";
    public static final String ITBL_BUTTON_OPEN_APP          = "openApp";
    public static final String ITBL_BUTTON_REQUIRES_UNLOCK   = "requiresUnlock";
    public static final String ITBL_BUTTON_ICON              = "icon";
    public static final String ITBL_BUTTON_INPUT_TITLE       = "inputTitle";
    public static final String ITBL_BUTTON_INPUT_PLACEHOLDER = "inputPlaceholder";
    public static final String ITBL_BUTTON_ACTION            = "action";

    //Device
    public static final String DEVICE_BRAND             = "brand";
    public static final String DEVICE_MANUFACTURER      = "manufacturer";
    public static final String DEVICE_ADID              = "advertisingId";
    public static final String DEVICE_SYSTEM_NAME       = "systemName";
    public static final String DEVICE_SYSTEM_VERSION    = "systemVersion";
    public static final String DEVICE_MODEL             = "model";
    public static final String DEVICE_SDK_VERSION       = "sdkVersion";

    public static final String INSTANCE_ID_CLASS        = "com.google.android.gms.iid.InstanceID";
    public static final String ICON_FOLDER_IDENTIFIER   = "drawable";
    public static final String NOTIFICATION_ICON_NAME   = "iterable_notification_icon";
    public static final String NOTIFICATION_COLOR       = "iterable_notification_color";
    public static final String DEFAULT_SOUND            = "default";
    public static final String SOUND_FOLDER_IDENTIFIER  = "raw";
    public static final String ANDROID_RESOURCE_PATH    = "android.resource://";
    public static final String ANDROID_STRING           = "string";
    public static final String MAIN_CLASS               = "mainClass";
    public static final String REQUEST_CODE             = "requestCode";
    public static final String ACTION_IDENTIFIER        = "actionIdentifier";
    public static final String USER_INPUT               = "userInput";

    //Firebase
    public static final String FIREBASE_RESOURCE_ID     = "firebase_database_url";
    public static final String FIREBASE_SENDER_ID       = "gcm_defaultSenderId";
    public static final String FIREBASE_MESSAGING_CLASS = "com.google.firebase.messaging.FirebaseMessaging";
    public static final String FIREBASE_COMPATIBLE      = "firebaseCompatible";
    public static final String FIREBASE_TOKEN_TYPE      = "tokenRegistrationType";
    public static final String FIREBASE_INITIAL_UPGRADE = "initialFirebaseUpgrade";

    public static final String ITBL_DEEPLINK_IDENTIFIER = "/a/[A-Za-z0-9]+";
    public static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String PICASSO_CLASS = "com.squareup.picasso.Picasso";
    public static final String LOCATION_HEADER_FIELD = "Location";

    //In-App Constants
    public static final String ITERABLE_IN_APP_BACKGROUND_COLOR = "backgroundColor";
    public static final String ITERABLE_IN_APP_BODY             = "body";
    public static final String ITERABLE_IN_APP_BUTTON_ACTION    = "action";
    public static final String ITERABLE_IN_APP_BUTTON_INDEX     = "buttonIndex";
    public static final String ITERABLE_IN_APP_BUTTONS          = "buttons";
    public static final String ITERABLE_IN_APP_COLOR            = "color";
    public static final String ITERABLE_IN_APP_CONTENT          = "content";
    public static final String ITERABLE_IN_APP_COUNT            = "count";
    public static final String ITERABLE_IN_APP_MAIN_IMAGE       = "mainImage";
    public static final String ITERABLE_IN_APP_MESSAGE          = "inAppMessages";
    public static final String ITERABLE_IN_APP_TEXT             = "text";
    public static final String ITERABLE_IN_APP_TITLE            = "title";
    public static final String ITERABLE_IN_APP_TYPE             = "displayType";
    public static final String ITERABLE_IN_APP_URL_CLICK        = "urlClick";

    public static final String ITERABLE_IN_APP_TYPE_BOTTOM  = "BOTTOM";
    public static final String ITERABLE_IN_APP_TYPE_CENTER  = "MIDDLE";
    public static final String ITERABLE_IN_APP_TYPE_FULL    = "FULL";
    public static final String ITERABLE_IN_APP_TYPE_TOP     = "TOP";

    public static final String ITBL_KEY_SDK_VERSION = "SDKVersion";
    public static final String ITBL_PLATFORM_ANDROID = "Android";
    public static final String ITBL_KEY_SDK_VERSION_NUMBER = "0.0.0";
}
