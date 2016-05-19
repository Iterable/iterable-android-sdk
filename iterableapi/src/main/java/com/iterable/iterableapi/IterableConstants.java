package com.iterable.iterableapi;

/**
 * Created by davidtruong on 4/6/16.
 *
 * IterableConstants contains a list of constants used with the Iterable mobile SDK.
 */
public final class IterableConstants {
    public static final String ACTION_NOTIF_OPENED = "com.iterable.push.ACTION_NOTIF_OPENED";
    public static final String ACTION_PUSH_REGISTRATION = "com.iterable.push.ACTION_PUSH_REGISTRATION";

    //API Endpoint Key Constants
    public static final String KEY_API_KEY             = "api_key";
    public static final String KEY_CAMPAIGNID          = "campaignId";
    public static final String KEY_CURRENT_EMAIL       = "currentEmail";
    public static final String KEY_DATAFIELDS          = "dataFields";
    public static final String KEY_EMAIL               = "email";
    public static final String KEY_EVENTNAME           = "eventName";
    public static final String KEY_NEW_EMAIL           = "newEmail";
    public static final String KEY_RECIPIENT_EMAIL     = "recipientEmail";
    public static final String KEY_SEND_AT             = "sendAt";
    public static final String KEY_TEMPLATE_ID         = "templateId";
    public static final String KEY_TOKEN               = "token";
    public static final String KEY_PLATFORM            = "platform";
    public static final String KEY_APPLICATIONNAME     = "applicationName";
    public static final String KEY_DEVICE              = "device";
    public static final String KEY_USER                = "user";
    public static final String KEY_ITEMS               = "items";
    public static final String KEY_TOTAL               = "total";

    public static final String ENDPOINT_PUSHTARGET          = "push/target";
    public static final String ENDPOINT_REGISTERDEVICETOKEN = "users/registerDeviceToken";
    public static final String ENDPOINT_TRACK               = "events/track";
    public static final String ENDPOINT_TRACKCONVERSION     = "events/trackConversion";
    public static final String ENDPOINT_TRACKPURCHASE       = "commerce/trackPurchase";
    public static final String ENDPOINT_TRACKPUSHOPEN       = "events/trackPushOpen";
    public static final String ENDPOINT_UPDATEEMAIL         = "users/updateEmail";

    public static final String PUSH_APPID = "IterableAppId";
    public static final String PUSH_PROJECTID = "GCMProjectNumber";

    public static final String MESSAGING_PLATFORM_GOOGLE = "GCM";
    public static final String MESSAGING_PLATFORM_AMAZON = "ADM";
}
