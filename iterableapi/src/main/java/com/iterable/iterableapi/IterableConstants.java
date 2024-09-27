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

    //Hosts
    public static final String BASE_URL_API = "https://api.iterable.com/api/";
    public static final String BASE_URL_LINKS = "https://links.iterable.com/";

    //API Fields
    public static final String HEADER_API_KEY           = "Api-Key";
    public static final String HTTP_STATUS_CODE         = "httpStatusCode";
    public static final String HEADER_SDK_PLATFORM      = "SDK-Platform";
    public static final String HEADER_SDK_VERSION       = "SDK-Version";
    public static final String HEADER_SDK_AUTHORIZATION = "Authorization";
    public static final String HEADER_SDK_AUTH_FORMAT   = "Bearer ";
    public static final String HEADER_SDK_PROCESSOR_TYPE = "SDK-Request-Processor";
    public static final String KEY_APPLICATION_NAME     = "applicationName";
    public static final String KEY_CAMPAIGN_ID          = "campaignId";
    public static final String KEY_CURRENT_EMAIL        = "currentEmail";
    public static final String KEY_CURRENT_USERID       = "currentUserId";
    public static final String KEY_DATA_FIELDS          = "dataFields";
    public static final String KEY_MERGE_NESTED_OBJECTS = "mergeNestedObjects";
    public static final String KEY_DEVICE               = "device";
    public static final String KEY_DEVICE_INFO          = "deviceInfo";
    public static final String KEY_EMAIL                = "email";
    public static final String KEY_EMAIL_LIST_IDS       = "emailListIds";
    public static final String KEY_EVENT_NAME           = "eventName";
    public static final String KEY_ITEMS                = "items";
    public static final String KEY_NEW_EMAIL            = "newEmail";
    public static final String KEY_PACKAGE_NAME         = "packageName";
    public static final String KEY_PLATFORM             = "platform";
    public static final String KEY_PREFER_USER_ID       = "preferUserId";
    public static final String KEY_RECIPIENT_EMAIL      = "recipientEmail";
    public static final String KEY_SEND_AT              = "sendAt";
    public static final String KEY_CREATED_AT           = "createdAt";
    public static final String KEY_SENT_AT              = "Sent-At";
    public static final String KEY_TEMPLATE_ID          = "templateId";
    public static final String KEY_MESSAGE_CONTEXT      = "messageContext";
    public static final String KEY_MESSAGE_ID           = "messageId";
    public static final String KEY_TOKEN                = "token";
    public static final String KEY_TOTAL                = "total";
    public static final String KEY_UNSUB_CHANNEL        = "unsubscribedChannelIds";
    public static final String KEY_UNSUB_MESSAGE        = "unsubscribedMessageTypeIds";
    public static final String KEY_SUB_MESSAGE          = "subscribedMessageTypeIds";
    public static final String KEY_USER_ID              = "userId";
    public static final String KEY_USER                 = "user";
    public static final String KEY_USER_TEXT            = "userText";
    public static final String KEY_INBOX_SESSION_ID     = "inboxSessionId";
    public static final String KEY_EMBEDDED_SESSION_ID     = "id";
    public static final String KEY_OFFLINE_MODE         = "offlineMode";
    public static final String KEY_FIRETV = "FireTV";
    public static final String KEY_CREATE_NEW_FIELDS    = "createNewFields";
    public static final String KEY_ANON_SESSION_CONTEXT = "anonSessionContext";

    //API Endpoint Key Constants
    public static final String ENDPOINT_DISABLE_DEVICE          = "users/disableDevice";
    public static final String ENDPOINT_GET_INAPP_MESSAGES      = "inApp/getMessages";
    public static final String ENDPOINT_INAPP_CONSUME           = "events/inAppConsume";
    public static final String ENDPOINT_PUSH_TARGET             = "push/target";
    public static final String ENDPOINT_REGISTER_DEVICE_TOKEN   = "users/registerDeviceToken";
    public static final String ENDPOINT_TRACK                   = "events/track";
    public static final String ENDPOINT_TRACK_INAPP_CLICK       = "events/trackInAppClick";
    public static final String ENDPOINT_TRACK_INAPP_OPEN        = "events/trackInAppOpen";
    public static final String ENDPOINT_TRACK_INAPP_DELIVERY    = "events/trackInAppDelivery";
    public static final String ENDPOINT_TRACK_INBOX_SESSION     = "events/trackInboxSession";
    public static final String ENDPOINT_UPDATE_CART             = "commerce/updateCart";
    public static final String ENDPOINT_TRACK_PURCHASE          = "commerce/trackPurchase";
    public static final String ENDPOINT_TRACK_PUSH_OPEN         = "events/trackPushOpen";
    public static final String ENDPOINT_UPDATE_USER             = "users/update";
    public static final String ENDPOINT_UPDATE_EMAIL            = "users/updateEmail";
    public static final String ENDPOINT_UPDATE_USER_SUBS        = "users/updateSubscriptions";
    public static final String ENDPOINT_TRACK_INAPP_CLOSE       = "events/trackInAppClose";
    public static final String ENDPOINT_GET_REMOTE_CONFIGURATION = "mobile/getRemoteConfiguration";
    public static final String ENDPOINT_GET_EMBEDDED_MESSAGES   = "embedded-messaging/messages";
    public static final String ENDPOINT_TRACK_EMBEDDED_RECEIVED   = "embedded-messaging/events/received";
    public static final String ENDPOINT_TRACK_EMBEDDED_CLICK   = "embedded-messaging/events/click";
    public static final String ENDPOINT_TRACK_EMBEDDED_SESSION   = "embedded-messaging/events/session";
    public static final String ENDPOINT_GET_USER_BY_USERID      = "users/byUserId";
    public static final String ENDPOINT_GET_USER_BY_EMAIL       = "users/getByEmail";
    public static final String ENDPOINT_MERGE_USER              = "users/merge";
    public static final String ENDPOINT_CRITERIA_LIST           = "anonymoususer/list";
    public static final String ENDPOINT_TRACK_ANON_SESSION      = "anonymoususer/events/session";

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
    public static final String SHARED_PREFS_USERIDANON_KEY = "itbl_userid_anon";
    public static final String SHARED_PREFS_DEVICEID_KEY = "itbl_deviceid";
    public static final String SHARED_PREFS_AUTH_TOKEN_KEY = "itbl_authtoken";
    public static final String SHARED_PREFS_EXPIRATION_SUFFIX = "_expiration";
    public static final String SHARED_PREFS_OBJECT_SUFFIX = "_object";
    public static final String SHARED_PREFS_PAYLOAD_KEY = "itbl_payload";
    public static final int    SHARED_PREFS_PAYLOAD_EXPIRATION_HOURS = 24;
    public static final String SHARED_PREFS_ATTRIBUTION_INFO_KEY = "itbl_attribution_info";
    public static final int    SHARED_PREFS_ATTRIBUTION_INFO_EXPIRATION_HOURS = 24;
    public static final String SHARED_PREFS_FCM_MIGRATION_DONE_KEY = "itbl_fcm_migration_done";
    public static final String SHARED_PREFS_SAVED_CONFIGURATION = "itbl_saved_configuration";
    public static final String SHARED_PREFS_OFFLINE_MODE_KEY = "itbl_offline_mode";
    public static final String SHARED_PREFS_EVENT_LIST_KEY = "itbl_event_list";
    public static final String SHARED_PREFS_ANON_SESSIONS = "itbl_anon_sessions";
    public static final String SHARED_PREFS_SESSION_NO = "totalAnonSessionCount";
    public static final String SHARED_PREFS_LAST_SESSION = "lastAnonSession";
    public static final String SHARED_PREFS_FIRST_SESSION = "firstAnonSession";
    public static final String SHARED_PREFS_EVENT_TYPE = "eventType";
    public static final String SHARED_PREFS_CRITERIA = "criteria";
    public static final String SHARED_PREFS_CRITERIA_ID = "matchedCriteriaId";
    public static final String SHARED_PREFS_PUSH_OPT_IN = "mobilePushOptIn";

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
    public static final String DEVICE_SYSTEM_NAME       = "systemName";
    public static final String DEVICE_SYSTEM_VERSION    = "systemVersion";
    public static final String DEVICE_MODEL             = "model";
    public static final String DEVICE_SDK_VERSION       = "sdkVersion";
    public static final String DEVICE_ID                   = "deviceId";
    public static final String DEVICE_APP_PACKAGE_NAME     = "appPackageName";
    public static final String DEVICE_APP_VERSION          = "appVersion";
    public static final String DEVICE_APP_BUILD            = "appBuild";
    public static final String DEVICE_NOTIFICATIONS_ENABLED = "notificationsEnabled";
    public static final String DEVICE_ITERABLE_SDK_VERSION = "iterableSdkVersion";

    public static final String INSTANCE_ID_CLASS        = "com.google.android.gms.iid.InstanceID";
    public static final String ICON_FOLDER_IDENTIFIER   = "drawable";
    public static final String NOTIFICATION_ICON_NAME   = "iterable_notification_icon";
    public static final String NOTIFICAION_BADGING      = "iterable_notification_badging";
    public static final String NOTIFICATION_COLOR       = "iterable_notification_color";
    public static final String NOTIFICATION_CHANNEL_NAME = "iterable_notification_channel_name";
    public static final String DEFAULT_SOUND            = "default";
    public static final String SOUND_FOLDER_IDENTIFIER  = "raw";
    public static final String ANDROID_RESOURCE_PATH    = "android.resource://";
    public static final String ANDROID_STRING           = "string";
    public static final String MAIN_CLASS               = "mainClass";
    public static final String REQUEST_CODE             = "requestCode";
    public static final String ACTION_IDENTIFIER        = "actionIdentifier";
    public static final String USER_INPUT               = "userInput";
    public static final String DATA_REPLACE             = "dataReplace";

    //Firebase
    public static final String FIREBASE_SENDER_ID       = "gcm_defaultSenderId";
    public static final String FIREBASE_MESSAGING_CLASS = "com.google.firebase.messaging.FirebaseMessaging";
    public static final String FIREBASE_COMPATIBLE      = "firebaseCompatible";
    public static final String FIREBASE_TOKEN_TYPE      = "tokenRegistrationType";
    public static final String FIREBASE_INITIAL_UPGRADE = "initialFirebaseUpgrade";

    public static final String ITBL_DEEPLINK_IDENTIFIER = "/a/[A-Za-z0-9]+";
    public static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String PICASSO_CLASS = "com.squareup.picasso.Picasso";
    public static final String LOCATION_HEADER_FIELD = "Location";

    //Embedded Message Constants
    public static final String ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS  = "placements";
    public static final String ITERABLE_EMBEDDED_MESSAGE  = "embeddedMessages";
    public static final String ITERABLE_EMBEDDED_MESSAGE_METADATA = "metadata";
    public static final String ITERABLE_EMBEDDED_MESSAGE_ELEMENTS = "elements";
    public static final String ITERABLE_EMBEDDED_MESSAGE_PAYLOAD = "payload";
    public static final String ITERABLE_EMBEDDED_MESSAGE_ID = "messageId";
    public static final String ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID = "placementId";
    public static final String ITERABLE_EMBEDDED_MESSAGE_CAMPAIGN_ID = "campaignId";
    public static final String ITERABLE_EMBEDDED_MESSAGE_IS_PROOF = "isProof";
    public static final String ITERABLE_EMBEDDED_MESSAGE_TITLE = "title";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BODY = "body";
    public static final String ITERABLE_EMBEDDED_MESSAGE_MEDIA_URL = "mediaUrl";
    public static final String ITERABLE_EMBEDDED_MESSAGE_MEDIA_URL_CAPTION = "mediaUrlCaption";
    public static final String ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION = "defaultAction";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BUTTONS = "buttons";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BUTTON_IDENTIFIER = "buttonIdentifier";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BUTTON_TARGET_URL = "targetUrl";
    public static final String ITERABLE_EMBEDDED_MESSAGE_TEXT = "text";
    public static final String ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_TYPE = "type";
    public static final String ITERABLE_EMBEDDED_MESSAGE_DEFAULT_ACTION_DATA = "data";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BUTTON_ID = "id";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BUTTON_TITLE = "title";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION = "action";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION_TYPE = "type";
    public static final String ITERABLE_EMBEDDED_MESSAGE_BUTTON_ACTION_DATA = "data";
    public static final String ITERABLE_EMBEDDED_MESSAGE_TEXT_ID = "id";
    public static final String ITERABLE_EMBEDDED_MESSAGE_TEXT_TEXT = "text";
    public static final String ITERABLE_EMBEDDED_MESSAGE_TEXT_LABEL = "label";

    public static final String ITERABLE_EMBEDDED_SESSION = "session";
    public static final String ITERABLE_EMBEDDED_SESSION_START = "start";
    public static final String ITERABLE_EMBEDDED_SESSION_END = "end";
    public static final String ITERABLE_EMBEDDED_IMPRESSIONS = "impressions";
    public static final String ITERABLE_EMBEDDED_IMP_DISPLAY_COUNT = "displayCount";
    public static final String ITERABLE_EMBEDDED_IMP_DISPLAY_DURATION = "displayDuration";

    //In-App Constants
    public static final String ITERABLE_IN_APP_BGCOLOR_ALPHA    = "alpha";
    public static final String ITERABLE_IN_APP_BGCOLOR_HEX      = "hex";
    public static final String ITERABLE_IN_APP_BGCOLOR          = "bgColor";
    public static final String ITERABLE_IN_APP_BACKGROUND_COLOR = "backgroundColor";
    public static final String ITERABLE_IN_APP_BACKGROUND_ALPHA = "backgroundAlpha";
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
    public static final String ITERABLE_IN_APP_CLICKED_URL      = "clickedUrl";
    public static final String ITERABLE_IN_APP_HTML             = "html";
    public static final String ITERABLE_IN_APP_CREATED_AT       = "createdAt";
    public static final String ITERABLE_IN_APP_EXPIRES_AT       = "expiresAt";
    public static final String ITERABLE_IN_APP_LEGACY_PAYLOAD   = "payload";
    public static final String ITERABLE_IN_APP_CUSTOM_PAYLOAD   = "customPayload";
    public static final String ITERABLE_IN_APP_TRIGGER          = "trigger";
    public static final String ITERABLE_IN_APP_TRIGGER_TYPE     = "type";
    public static final String ITERABLE_IN_APP_PRIORITY_LEVEL   = "priorityLevel";
    public static final String ITERABLE_IN_APP_SAVE_TO_INBOX    = "saveToInbox";
    public static final String ITERABLE_IN_APP_SILENT_INBOX     = "silentInbox";
    public static final String ITERABLE_IN_APP_INBOX_METADATA   = "inboxMetadata";
    public static final String ITERABLE_IN_APP_DISPLAY_SETTINGS = "inAppDisplaySettings";
    public static final String ITERABLE_IN_APP_PROCESSED        = "processed";
    public static final String ITERABLE_IN_APP_CONSUMED         = "consumed";
    public static final String ITERABLE_IN_APP_READ             = "read";
    public static final String ITERABLE_IN_APP_LOCATION         = "location";
    public static final String ITERABLE_IN_APP_CLOSE_ACTION     = "closeAction";
    public static final String ITERABLE_IN_APP_DELETE_ACTION    = "deleteAction";
    public static final String ITERABLE_INBOX_SESSION_START              = "inboxSessionStart";
    public static final String ITERABLE_INBOX_SESSION_END                = "inboxSessionEnd";
    public static final String ITERABLE_INBOX_START_TOTAL_MESSAGE_COUNT  = "startTotalMessageCount";
    public static final String ITERABLE_INBOX_START_UNREAD_MESSAGE_COUNT = "startUnreadMessageCount";
    public static final String ITERABLE_INBOX_END_TOTAL_MESSAGE_COUNT    = "endTotalMessageCount";
    public static final String ITERABLE_INBOX_END_UNREAD_MESSAGE_COUNT   = "endUnreadMessageCount";
    public static final String ITERABLE_INBOX_START_ACTION               = "startAction";
    public static final String ITERABLE_INBOX_END_ACTION                 = "endAction";
    public static final String ITERABLE_INBOX_IMPRESSIONS                = "impressions";
    public static final String ITERABLE_INBOX_IMP_DISPLAY_COUNT          = "displayCount";
    public static final String ITERABLE_INBOX_IMP_DISPLAY_DURATION       = "displayDuration";
    public static final String ITERABLE_IN_APP_SHOULD_ANIMATE            = "shouldAnimate";
    public static final int ITERABLE_IN_APP_ANIMATION_DURATION           = 500;
    public static final int ITERABLE_IN_APP_BACKGROUND_ANIMATION_DURATION = 300;

    public static final int EXPONENTIAL_FACTOR                              = 2;

    public static final double ITERABLE_IN_APP_PRIORITY_LEVEL_LOW           = 400.0;
    public static final double ITERABLE_IN_APP_PRIORITY_LEVEL_MEDIUM        = 300.0;
    public static final double ITERABLE_IN_APP_PRIORITY_LEVEL_HIGH          = 200.0;
    public static final double ITERABLE_IN_APP_PRIORITY_LEVEL_CRITICAL      = 100.0;
    public static final double ITERABLE_IN_APP_PRIORITY_LEVEL_UNASSIGNED    = 300.5;

    public static final String ITERABLE_IN_APP_TYPE_BOTTOM  = "BOTTOM";
    public static final String ITERABLE_IN_APP_TYPE_CENTER  = "MIDDLE";
    public static final String ITERABLE_IN_APP_TYPE_FULL    = "FULL";
    public static final String ITERABLE_IN_APP_TYPE_TOP     = "TOP";

    public static final String ITERABLE_IN_APP_INBOX_TITLE      = "title";
    public static final String ITERABLE_IN_APP_INBOX_SUBTITLE   = "subtitle";
    public static final String ITERABLE_IN_APP_INBOX_ICON       = "icon";

    // Custom actions handled by the SDK
    public static final String ITERABLE_IN_APP_ACTION_DELETE    = "delete";

    //Offline operation
    public static final long OFFLINE_TASKS_LIMIT                = 1000;

    // URL schemes
    public static final String URL_SCHEME_ITBL = "itbl://";
    public static final String URL_SCHEME_ITERABLE = "iterable://";
    public static final String URL_SCHEME_ACTION = "action://";

    public static final String ITBL_KEY_SDK_VERSION = "SDKVersion";
    public static final String ITBL_PLATFORM_ANDROID = "Android";
    public static final String ITBL_PLATFORM_OTT = "OTT";
    public static final String ITBL_KEY_SDK_VERSION_NUMBER = BuildConfig.ITERABLE_SDK_VERSION;
    public static final String ITBL_SYSTEM_VERSION = "systemVersion";

    public static final String NO_MESSAGES_TITLE = "noMessagesTitle";
    public static final String NO_MESSAGES_BODY = "noMessagesBody";

    // Criteria constants
    public static final String CRITERIA_SETS = "criteriaSets";
    public static final String SEARCH_QUERIES = "searchQueries";
    public static final String SEARCH_QUERY = "searchQuery";
    public static final String CRITERIA_ID = "criteriaId";
    public static final String COMBINATOR = "combinator";
    public static final String SEARCH_COMBO = "searchCombo";
    public static final String FIELD = "field";
    public static final String VALUE = "value";
    public static final String VALUES = "values";
    public static final String DATA_TYPE = "dataType";
    public static final String COMPARATOR_TYPE = "comparatorType";
    public static final String UPDATECART_ITEM_PREFIX = "updateCart.updatedShoppingCartItems.";
    public static final String PURCHASE_ITEM = "shoppingCartItems";
    public static final String PURCHASE_ITEM_PREFIX = PURCHASE_ITEM + ".";
    public static final String MIN_MATCH = "minMatch";


    //Tracking types
    public static final String TRACK_EVENT = "customEvent";
    public static final String TRACK_PURCHASE = "purchase";
    public static final String TRACK_UPDATE_CART = "cartUpdate";
    public static final String UPDATE_CART = "updateCart";
    public static final String TRACK_TOKEN_REGISTRATION = "tokenRegistration";
    public static final String UPDATE_USER = "user";
    public static final String SOURCE_EMAIL = "sourceEmail";
    public static final String SOURCE_USER_ID = "sourceUserId";
    public static final String DESTINATION_EMAIL = "destinationEmail";
    public static final String DESTINATION_USER_ID = "destinationUserId";

    // Merge user constants
    public static final String MERGE_SUCCESSFUL = "merge_successful";
    public static final String MERGE_NOTREQUIRED = "merge_notrequired";
}