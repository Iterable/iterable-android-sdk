package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableApi {

//region Variables
//---------------------------------------------------------------------------------------
    static final String TAG = "IterableApi";

    static volatile IterableApi sharedInstance = new IterableApi();

    private Context _applicationContext;
    private String _apiKey;
    private String _email;
    private String _userId;
    private boolean _debugMode;
    private Bundle _payloadData;
    private IterableNotificationData _notificationData;

    private static Pattern deeplinkPattern = Pattern.compile(IterableConstants.ITBL_DEEPLINK_IDENTIFIER);

//---------------------------------------------------------------------------------------
//endregion

//region Constructor
//---------------------------------------------------------------------------------------
    IterableApi(){
    }

//---------------------------------------------------------------------------------------
//endregion


//region Getters/Setters
//---------------------------------------------------------------------------------------
    /**
     * Sets the icon to be displayed in notifications.
     * The icon name should match the resource name stored in the /res/drawable directory.
     * @param iconName
     */
    public void setNotificationIcon(String iconName) {
        setNotificationIcon(_applicationContext, iconName);
    }

    /**
     * Retrieves the payload string for a given key.
     * Used for deeplinking and retrieving extra data passed down along with a campaign.
     * @param key
     * @return Returns the requested payload data from the current push campaign if it exists.
     */
    public String getPayloadData(String key) {
        return (_payloadData != null) ? _payloadData.getString(key, null): null;
    }

    /**
     * Returns the current context for the application.
     * @return
     */
    Context getMainActivityContext() {
        return _applicationContext;
    }

    /**
     * Sets debug mode.
     * @param debugMode
     */
    void setDebugMode(boolean debugMode) {
        _debugMode = debugMode;
    }

    /**
     * Gets the current state of the debug mode.
     * @return
     */
    boolean getDebugMode() {
        return _debugMode;
    }

    /**
     * Set the payload for a given intent if it is from Iterable.
     * @param intent
     */
    void setPayloadData(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY) && !IterableNotification.isGhostPush(extras)) {
            setPayloadData(extras);
        }
    }

    /**
     * Sets the payload bundle.
     * @param bundle
     */
    void setPayloadData(Bundle bundle) {
        _payloadData = bundle;
    }

    /**
     * Sets the IterableNotification data
     * @param data
     */
    void setNotificationData(IterableNotificationData data) {
        _notificationData = data;
    }
//---------------------------------------------------------------------------------------
//endregion



//region Public Functions
//---------------------------------------------------------------------------------------
    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * @param currentActivity The current activity
     * @param userId The current userId
     * @return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKeyWithUserId(Activity currentActivity, String apiKey,
                                                                 String userId)
    {
        return sharedInstanceWithApiKeyWithUserId(currentActivity, apiKey, userId, false);
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * Allows the IterableApi to be intialized with debugging enabled
     * @param currentActivity The current activity
     * @param userId
     * The current userId@return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKeyWithUserId(Activity currentActivity, String apiKey,
                                                                 String userId, boolean debugMode)
    {
        return sharedInstanceWithApiKeyWithUserId((Context) currentActivity, apiKey, userId, debugMode);
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * @param currentContext The current context
     * @param userId The current userId
     * @return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKeyWithUserId(Context currentContext, String apiKey,
                                                                 String userId)
    {
        return sharedInstanceWithApiKey(currentContext, apiKey, null, userId, false);
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * Allows the IterableApi to be intialized with debugging enabled
     * @param currentContext The current context
     * @return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKeyWithUserId(Context currentContext, String apiKey,
                                                                 String userId, boolean debugMode)
    {
        return sharedInstanceWithApiKey(currentContext, apiKey, null, userId, debugMode);
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * @param currentActivity The current activity
     * @param email The current email
     * @return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKey(Activity currentActivity, String apiKey,
                                                       String email)
    {
        return sharedInstanceWithApiKey(currentActivity, apiKey, email, false);
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * Allows the IterableApi to be intialized with debugging enabled
     * @param currentActivity The current activity
     * @param email The current email
     * @return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKey(Activity currentActivity, String apiKey,
                                                       String email, boolean debugMode)
    {
        return sharedInstanceWithApiKey((Context) currentActivity, apiKey, email, debugMode);
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * @param currentContext The current context
     * @param email The current email
     * @return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKey(Context currentContext, String apiKey,
                                                       String email)
    {
        return sharedInstanceWithApiKey(currentContext, apiKey, email, false);
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * Allows the IterableApi to be intialized with debugging enabled
     * @param currentContext The current context
     * @param email The current email
     * @return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKey(Context currentContext, String apiKey,
                                                       String email, boolean debugMode)
    {
        return sharedInstanceWithApiKey(currentContext, apiKey, email, null, debugMode);
    }

    private static IterableApi sharedInstanceWithApiKey(Context currentContext, String apiKey,
                                                       String email, String userId, boolean debugMode)
    {
        sharedInstance.updateData(currentContext.getApplicationContext(), apiKey, email, userId);

        if (currentContext instanceof Activity) {
            Activity currentActivity = (Activity) currentContext;
            sharedInstance.onNewIntent(currentActivity.getIntent());
        } else {
            IterableLogger.w(TAG, "Notification Opens will not be tracked: "+
                    "sharedInstanceWithApiKey called with a Context that is not an instance of Activity. " +
                    "Pass in an Activity to IterableApi.sharedInstanceWithApiKey to enable open tracking" +
                    "or call onNewIntent when a new Intent is received.");
        }

        sharedInstance.setDebugMode(debugMode);

        return sharedInstance;
    }

    /**
     * Tracks a click on the uri if it is an iterable link.
     * @param uri the
     * @param onCallback Calls the callback handler with the destination location
     *                   or the original url if it is not a interable link.
     */
    public static void getAndTrackDeeplink(String uri, IterableHelper.IterableActionHandler onCallback) {
        if (uri != null) {
            Matcher m = deeplinkPattern.matcher(uri);
            if (m.find( )) {
                IterableApiRequest request = new IterableApiRequest(null, uri, null, IterableApiRequest.REDIRECT, onCallback);
                new IterableRequest().execute(request);
            } else {
                onCallback.execute(uri);
            }
        } else {
            onCallback.execute(null);
        }
    }

    /**
     * Debugging function to send API calls to different url endpoints.
     * @param url
     */
    public static void overrideURLEndpointPath(String url) {
        IterableRequest.overrideUrl = url;
    }

    /**
     * Call onNewIntent to set the payload data and track pushOpens directly if
     * sharedInstanceWithApiKey was called with a Context rather than an Activity.
     */
    public void onNewIntent(Intent intent) {
        if (isIterableIntent(intent)) {
            setPayloadData(intent);
            tryTrackNotifOpen(intent);
        } else {
            IterableLogger.d(TAG, "onNewIntent not triggered by an Iterable notification");
        }
    }

    /**
     * Returns whether or not the intent was sent from Iterable.
     */
    public boolean isIterableIntent(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            return (extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY));
        }
        return false;
    }

    /**
     * Registers a device token with Iterable.
     * @param applicationName
     * @param token
     */
    public void registerDeviceToken(String applicationName, String token) {
        registerDeviceToken(applicationName, token, null);
    }

    /**
     * Registers a device token with Iterable.
     * @param applicationName
     * @param token
     * @param pushServicePlatform
     */
    public void registerDeviceToken(final String applicationName, final String token, final String pushServicePlatform) {
        if (token != null) {
            new Thread(new Runnable() {
                public void run() {
                    registerDeviceToken(applicationName, token, pushServicePlatform, null);
                }
            }).start();
        }
    }

    /**
     * Track an event.
     * @param eventName
     */
    public void track(String eventName) {
        track(eventName, 0, 0, null);
    }

    /**
     * Track an event.
     * @param eventName
     * @param dataFields
     */
    public void track(String eventName, JSONObject dataFields) {
        track(eventName, 0, 0, dataFields);
    }

    /**
     * Track an event.
     * @param eventName
     * @param campaignId
     * @param templateId
     */
    public void track(String eventName, int campaignId, int templateId) {
        track(eventName, campaignId, templateId, null);
    }

    /**
     * Track an event.
     * @param eventName
     * @param campaignId
     * @param templateId
     * @param dataFields
     */
    public void track(String eventName, int campaignId, int templateId, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();
        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_EVENT_NAME, eventName);

            requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_TRACK, requestJSON);
    }

    public void sendPush(String email, int campaignId) {
        sendPush(email, campaignId, null, null);
    }

    /**
     * Sends a push campaign to an email address at the given time.
     * @param sendAt Schedule the message for up to 365 days in the future.
     *               If set in the past, message is sent immediately.
     *               Format is YYYY-MM-DD HH:MM:SS in UTC
     */
    public void sendPush(String email, int campaignId, Date sendAt) {
        sendPush(email, campaignId, sendAt, null);
    }

    /**
     * Sends a push campaign to an email address.
     * @param email
     * @param campaignId
     * @param dataFields
     */
    public void sendPush(String email, int campaignId, JSONObject dataFields) {
        sendPush(email, campaignId, null, dataFields);
    }

    /**
     * Sends a push campaign to an email address at the given time.
     * @param sendAt Schedule the message for up to 365 days in the future.
     *               If set in the past, message is sent immediately.
     *               Format is YYYY-MM-DD HH:MM:SS in UTC
     */
    public void sendPush(String email, int campaignId, Date sendAt, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put(IterableConstants.KEY_RECIPIENT_EMAIL, email);
            requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            if (sendAt != null){
                SimpleDateFormat sdf = new SimpleDateFormat(IterableConstants.DATEFORMAT);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String dateString = sdf.format(sendAt);
                requestJSON.put(IterableConstants.KEY_SEND_AT, dateString);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_PUSH_TARGET, requestJSON);
    }

    /**
     * Updates the current user's email.
     * @param newEmail
     */
    public void updateEmail(String newEmail) {
        if (_email != null) {
            JSONObject requestJSON = new JSONObject();

            try {
                requestJSON.put(IterableConstants.KEY_CURRENT_EMAIL, _email);
                requestJSON.put(IterableConstants.KEY_NEW_EMAIL, newEmail);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_EMAIL, requestJSON);
            _email = newEmail;
        } else {
            IterableLogger.w(TAG, "updateEmail should not be called with a userId. " +
                "Init SDK with sharedInstanceWithApiKey instead of sharedInstanceWithApiKeyWithUserId");
        }
    }

    /**
     * Updates the current user.
     * @param dataFields
     */
    public void updateUser(JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_UPDATE_USER, requestJSON);
    }

    /**
     * Registers for push notifications.
     * @param iterableAppId
     * @param gcmProjectNumber
     */
    public void registerForPush(String iterableAppId, String gcmProjectNumber) {
        registerForPush(iterableAppId, gcmProjectNumber, IterableConstants.MESSAGING_PLATFORM_GOOGLE);
    }

    /**
     * Registers for push notifications.
     * @param iterableAppId
     * @param projectNumber
     * @param pushServicePlatform
     */
    public void registerForPush(String iterableAppId, String projectNumber, String pushServicePlatform) {
        IterablePushRegistrationData data = new IterablePushRegistrationData(iterableAppId, projectNumber, pushServicePlatform, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistration().execute(data);
    }

    /**
     * Disables the device from push notifications
     *
     * @param iterableAppId
     * @param gcmProjectNumber
     */
    public void disablePush(String iterableAppId, String gcmProjectNumber) {
        disablePush(iterableAppId, gcmProjectNumber, IterableConstants.MESSAGING_PLATFORM_GOOGLE);
    }

    /**
     * Disables the device from push notifications
     *
     * @param iterableAppId
     * @param projectNumber
     * @param pushServicePlatform
     */
    public void disablePush(String iterableAppId, String projectNumber, String pushServicePlatform) {
        IterablePushRegistrationData data = new IterablePushRegistrationData(iterableAppId, projectNumber, pushServicePlatform, IterablePushRegistrationData.PushRegistrationAction.DISABLE);
        new IterablePushRegistration().execute(data);
    }

    /**
     * Gets a notification from Iterable and displays it on device.
     * @param context
     * @param clickCallback
     */
    public void spawnInAppNotification(final Context context, final IterableHelper.IterableActionHandler clickCallback) {
        String htmlString;

        htmlString = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional //EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\"><head><!--[if gte mso 9]><xml><o:OfficeDocumentSettings><o:AllowPNG/><o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml><![endif]--><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><meta name=\"viewport\" content=\"width=device-width\"/><!--[if !mso]><!--><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/><!--<![endif]--><title>Simple</title><style type=\"text/css\" id=\"media-query\">\n" +
                "      body {\n" +
                "  margin: 0;\n" +
                "  padding: 0;}\n" +
                "\n" +
                "table, tr, td {\n" +
                "  vertical-align: top;\n" +
                "  border-collapse: collapse; }\n" +
                "\n" +
                ".ie-browser table, .mso-container table {\n" +
                "  table-layout: fixed; }\n" +
                "\n" +
                "* {\n" +
                "  line-height: inherit; }\n" +
                "\n" +
                "a[x-apple-data-detectors=true] {\n" +
                "  color: inherit !important;\n" +
                "  text-decoration: none !important; }\n" +
                "\n" +
                "[owa] .img-container div, [owa] .img-container button {\n" +
                "  display: block !important; }\n" +
                "\n" +
                "[owa] .fullwidth button {\n" +
                "  width: 100% !important; }\n" +
                "\n" +
                "[owa] .block-grid .col {\n" +
                "  display: table-cell;\n" +
                "  float: none !important;\n" +
                "  vertical-align: top; }\n" +
                "\n" +
                ".ie-browser .num12, .ie-browser .block-grid, [owa] .num12, [owa] .block-grid {\n" +
                "  width: 480px !important; }\n" +
                "\n" +
                ".ExternalClass, .ExternalClass p, .ExternalClass span, .ExternalClass font, .ExternalClass td, .ExternalClass div {\n" +
                "  line-height: 100%; }\n" +
                "\n" +
                ".ie-browser .mixed-two-up .num4, [owa] .mixed-two-up .num4 {\n" +
                "  width: 160px !important; }\n" +
                "\n" +
                ".ie-browser .mixed-two-up .num8, [owa] .mixed-two-up .num8 {\n" +
                "  width: 320px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.two-up .col, [owa] .block-grid.two-up .col {\n" +
                "  width: 240px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.three-up .col, [owa] .block-grid.three-up .col {\n" +
                "  width: 160px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.four-up .col, [owa] .block-grid.four-up .col {\n" +
                "  width: 120px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.five-up .col, [owa] .block-grid.five-up .col {\n" +
                "  width: 96px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.six-up .col, [owa] .block-grid.six-up .col {\n" +
                "  width: 80px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.seven-up .col, [owa] .block-grid.seven-up .col {\n" +
                "  width: 68px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.eight-up .col, [owa] .block-grid.eight-up .col {\n" +
                "  width: 60px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.nine-up .col, [owa] .block-grid.nine-up .col {\n" +
                "  width: 53px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.ten-up .col, [owa] .block-grid.ten-up .col {\n" +
                "  width: 48px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.eleven-up .col, [owa] .block-grid.eleven-up .col {\n" +
                "  width: 43px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.twelve-up .col, [owa] .block-grid.twelve-up .col {\n" +
                "  width: 40px !important; }\n" +
                "\n" +
                "@media only screen and (min-width: 500px) {\n" +
                "  .block-grid {\n" +
                "    width: 480px !important; }\n" +
                "  .block-grid .col {\n" +
                "    display: table-cell;\n" +
                "    Float: none !important;\n" +
                "    vertical-align: top; }\n" +
                "    .block-grid .col.num12 {\n" +
                "      width: 480px !important; }\n" +
                "  .block-grid.mixed-two-up .col.num4 {\n" +
                "    width: 160px !important; }\n" +
                "  .block-grid.mixed-two-up .col.num8 {\n" +
                "    width: 320px !important; }\n" +
                "  .block-grid.two-up .col {\n" +
                "    width: 240px !important; }\n" +
                "  .block-grid.three-up .col {\n" +
                "    width: 160px !important; }\n" +
                "  .block-grid.four-up .col {\n" +
                "    width: 120px !important; }\n" +
                "  .block-grid.five-up .col {\n" +
                "    width: 96px !important; }\n" +
                "  .block-grid.six-up .col {\n" +
                "    width: 80px !important; }\n" +
                "  .block-grid.seven-up .col {\n" +
                "    width: 68px !important; }\n" +
                "  .block-grid.eight-up .col {\n" +
                "    width: 60px !important; }\n" +
                "  .block-grid.nine-up .col {\n" +
                "    width: 53px !important; }\n" +
                "  .block-grid.ten-up .col {\n" +
                "    width: 48px !important; }\n" +
                "  .block-grid.eleven-up .col {\n" +
                "    width: 43px !important; }\n" +
                "  .block-grid.twelve-up .col {\n" +
                "    width: 40px !important; } }\n" +
                "\n" +
                "@media (max-width: 500px) {\n" +
                "  .block-grid, .col {\n" +
                "    min-width: 320px !important;\n" +
                "    max-width: 100% !important; }\n" +
                "  .block-grid {\n" +
                "    width: calc(100% - 40px) !important; }\n" +
                "  .col {\n" +
                "    width: 100% !important; }\n" +
                "    .col > div {\n" +
                "      margin: 0 auto; }\n" +
                "  img.fullwidth {\n" +
                "    max-width: 100% !important; } }\n" +
                "\n" +
                "    </style></head><body class=\"clean-body\" style=\"margin: 0;padding: 0;-webkit-text-size-adjust: 100%;background-color: transparent\"><!--[if IE]><div class=\"ie-browser\"><![endif]--><!--[if mso]><div class=\"mso-container\"><![endif]--><div class=\"nl-container\" style=\"overflow:hidden;border-radius:25px;min-width: 320px;Margin: 0 auto;background-color: transparent\"><!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td align=\"center\" style=\"background-color: #FFFFFF;\"><![endif]--><div style=\"background-color:#323341;\"><div style=\"Margin: 0 auto;min-width: 320px;max-width: 480px;width: 480px;width: calc(17000% - 84520px);overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: transparent;\" class=\"block-grid \"><div style=\"border-collapse: collapse;display: table;width: 100%;\"><!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"background-color:#323341;\" align=\"center\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width: 480px;\"><tr class=\"layout-full-width\" style=\"background-color:transparent;\"><![endif]--><!--[if (mso)|(IE)]><td align=\"center\" width=\"480\" style=\" width:480px; padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\" valign=\"top\"><![endif]--><div class=\"col num12\" style=\"min-width: 320px;max-width: 480px;width: 480px;width: calc(16000% - 76320px);background-color: transparent;\"><div style=\"background-color: transparent; width: 100% !important;\"><!--[if (!mso)&(!IE)]><!--><div style=\"border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\"><!--<![endif]--><!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"padding-right: 0px; padding-left: 0px; padding-top: 5px; padding-bottom: 20px;\"><![endif]--><div style=\"color:#ffffff;line-height:120%;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif; padding-right: 0px; padding-left: 0px; padding-top: 5px; padding-bottom: 20px;\"><div style=\"font-size:13px;line-height:16px;color:#ffffff;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif;text-align:left;\"><p style=\"margin: 0;font-size: 14px;line-height: 17px;text-align: center\"><strong><span style=\"font-size: 28px; line-height: 33px;\">New Release:</span></strong></p><p style=\"margin: 0;font-size: 14px;line-height: 17px;text-align: center\"><strong><span style=\"font-size: 28px; line-height: 33px;\">HTML In-App Notifications</span></strong></p></div></div><!--[if mso]></td></tr></table><![endif]--><div align=\"center\" class=\"img-container center\" style=\"padding-right: 0px; padding-left: 0px;\"><!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"padding-right: 0px; padding-left: 0px;\" align=\"center\"><![endif]--><a href=\"https://iterable.com\" target=\"_blank\"><img class=\"center\" align=\"center\" border=\"0\" src=\"https://app.iterable.com/assets/templates/builder/img/bee_rocket.png\" alt=\"Image\" title=\"Image\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: none;height: auto;float: none;width: 100%;max-width: 402px\" width=\"402\"/></a><!--[if mso]></td></tr></table><![endif]--></div><!--[if (!mso)&(!IE)]><!--></div><!--<![endif]--></div></div><!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]--></div></div></div><div style=\"background-color:#61626F;\"><div style=\"Margin: 0 auto;min-width: 320px;max-width: 480px;width: 480px;width: calc(17000% - 84520px);overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: transparent;\" class=\"block-grid \"><div style=\"border-collapse: collapse;display: table;width: 100%;\"><!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"background-color:#61626F;\" align=\"center\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width: 480px;\"><tr class=\"layout-full-width\" style=\"background-color:transparent;\"><![endif]--><!--[if (mso)|(IE)]><td align=\"center\" width=\"480\" style=\" width:480px; padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\" valign=\"top\"><![endif]--><div class=\"col num12\" style=\"min-width: 320px;max-width: 480px;width: 480px;width: calc(16000% - 76320px);background-color: transparent;\"><div style=\"background-color: transparent; width: 100% !important;\"><!--[if (!mso)&(!IE)]><!--><div style=\"border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\"><!--<![endif]--><!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 5px;\"><![endif]--><div style=\"color:#ffffff;line-height:120%;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif; padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 5px;\"><div style=\"font-size:13px;line-height:16px;color:#ffffff;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif;text-align:left;\"><p style=\"margin: 0;font-size: 18px;line-height: 22px;text-align: center\"><span style=\"font-size: 24px; line-height: 28px;\"><strong>Create your own fully customizable HTML In-App Notifications</strong></span></p></div></div><!--[if mso]></td></tr></table><![endif]--><!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"padding-right: 10px; padding-left: 10px; padding-top: 0px; padding-bottom: 0px;\"><![endif]--><div style=\"color:#B8B8C0;line-height:150%;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif; padding-right: 10px; padding-left: 10px; padding-top: 0px; padding-bottom: 0px;\"><div style=\"font-size:13px;line-height:20px;color:#B8B8C0;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif;text-align:left;\"><p style=\"margin: 0;font-size: 14px;line-height: 21px;text-align: center\"><span style=\"font-size: 14px; line-height: 21px;\">Design and launch your own mobile in-app notifications with our built in HTML editor.</span></p></div></div><!--[if mso]></td></tr></table><![endif]--><div align=\"center\" class=\"button-container center\" style=\"padding-right: 10px; padding-left: 10px; padding-top:15px; padding-bottom:10px;\"><!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;\"><tr><td style=\"padding-right: 10px; padding-left: 10px; padding-top:15px; padding-bottom:10px;\" align=\"center\"><v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\"itbl://close\" style=\"height:36px; v-text-anchor:middle; width:187px;\" arcsize=\"70%\" strokecolor=\"#C7702E\" fillcolor=\"#C7702E\"><w:anchorlock/><center style=\"color:#ffffff; font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif; font-size:16px;\"><![endif]--><a href=\"itbl://close\" target=\"_blank\" style=\"display: inline-block;text-decoration: none;-webkit-text-size-adjust: none;text-align: center;color: #ffffff; background-color: #C7702E; border-radius: 25px; -webkit-border-radius: 25px; -moz-border-radius: 25px; max-width: 167px; width: 127px; width: 35%; border-top: 0px solid transparent; border-right: 0px solid transparent; border-bottom: 0px solid transparent; border-left: 0px solid transparent; padding-top: 0px; padding-right: 20px; padding-bottom: 5px; padding-left: 20px; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif;mso-border-alt: none\"><span style=\"font-size:16px;line-height:32px;\"><span style=\"font-size: 14px; line-height: 28px;\" data-mce-style=\"font-size: 14px;\">Launch Now</span></span></a><!--[if mso]></center></v:roundrect></td></tr></table><![endif]--></div><div style=\"padding-right: 10px; padding-left: 10px; padding-top: 10px; padding-bottom: 10px;\"><!--[if (mso)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"padding-right: 10px;padding-left: 10px; padding-top: 10px; padding-bottom: 10px;\"><table width=\"100%\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td><![endif]--><div align=\"center\"><div style=\"border-top: 0px solid transparent; width:100%; line-height:0px; height:0px; font-size:0px;\">&#160;</div></div><!--[if (mso)]></td></tr></table></td></tr></table><![endif]--></div><!--[if (!mso)&(!IE)]><!--></div><!--<![endif]--></div></div><!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]--></div></div></div><div style=\"background-color:#ffffff;\"><div style=\"Margin: 0 auto;min-width: 320px;max-width: 480px;width: 480px;width: calc(17000% - 84520px);overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: transparent;\" class=\"block-grid \"><div style=\"border-collapse: collapse;display: table;width: 100%;\"><!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"background-color:#ffffff;\" align=\"center\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width: 480px;\"><tr class=\"layout-full-width\" style=\"background-color:transparent;\"><![endif]--><!--[if (mso)|(IE)]><td align=\"center\" width=\"480\" style=\" width:480px; padding-right: 0px; padding-left: 0px; padding-top:30px; padding-bottom:30px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\" valign=\"top\"><![endif]--><div class=\"col num12\" style=\"min-width: 320px;max-width: 480px;width: 480px;width: calc(16000% - 76320px);background-color: transparent;\"><div style=\"background-color: transparent; width: 100% !important;\"><!--[if (!mso)&(!IE)]><!--><div style=\"border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent; padding-top:30px; padding-bottom:30px; padding-right: 0px; padding-left: 0px;\"><!--<![endif]--><div align=\"center\" style=\"padding-right: 10px; padding-left: 10px; padding-bottom: 10px;\"><div style=\"line-height:10px;font-size:1px\">&#160;</div><div style=\"display: table; max-width:151;\"><!--[if (mso)|(IE)]><table width=\"131\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"border-collapse:collapse; padding-right: 10px; padding-left: 10px; padding-bottom: 10px;\" align=\"center\"><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse; mso-table-lspace: 0pt;mso-table-rspace: 0pt; width:131px;\"><tr><td width=\"32\" style=\"width:32px; padding-right: 5px;\" valign=\"top\"><![endif]--><table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"32\" height=\"32\" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;Margin-right: 5px\"><tbody><tr style=\"vertical-align: top\"><td align=\"left\" valign=\"middle\" style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top\"><a href=\"https://www.facebook.com/\" title=\"Facebook\" target=\"_blank\"><img src=\"https://d2fi4ri5dhpqd1.cloudfront.net/public/resources/social-networks-icon-sets/circle-color/facebook.png\" alt=\"Facebook\" title=\"Facebook\" width=\"32\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: none;height: auto;float: none;max-width: 32px !important\"/></a></td></tr></tbody></table><!--[if (mso)|(IE)]></td><td width=\"32\" style=\"width:32px; padding-right: 5px;\" valign=\"top\"><![endif]--><table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"32\" height=\"32\" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;Margin-right: 5px\"><tbody><tr style=\"vertical-align: top\"><td align=\"left\" valign=\"middle\" style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top\"><a href=\"http://twitter.com/\" title=\"Twitter\" target=\"_blank\"><img src=\"https://d2fi4ri5dhpqd1.cloudfront.net/public/resources/social-networks-icon-sets/circle-color/twitter.png\" alt=\"Twitter\" title=\"Twitter\" width=\"32\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: none;height: auto;float: none;max-width: 32px !important\"/></a></td></tr></tbody></table><!--[if (mso)|(IE)]></td><td width=\"32\" style=\"width:32px; padding-right: 0;\" valign=\"top\"><![endif]--><table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"32\" height=\"32\" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;Margin-right: 0\"><tbody><tr style=\"vertical-align: top\"><td align=\"left\" valign=\"middle\" style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top\"><a href=\"http://plus.google.com/\" title=\"Google+\" target=\"_blank\"><img src=\"https://d2fi4ri5dhpqd1.cloudfront.net/public/resources/social-networks-icon-sets/circle-color/googleplus.png\" alt=\"Google+\" title=\"Google+\" width=\"32\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: none;height: auto;float: none;max-width: 32px !important\"/></a></td></tr></tbody></table><!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]--></div></div><!--[if (!mso)&(!IE)]><!--></div><!--<![endif]--></div></div><!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]--></div></div></div><!--[if (mso)|(IE)]></td></tr></table><![endif]--></div><!--[if (mso)|(IE)]></div><![endif]--></body></html>";

        htmlString = "<head><meta name=\"viewport\"\n" +
                "        content=\"width=device-width, initial-scale=1.0, minimum-scale=1.0\" /><a href=\"http://www.iterabe.com\" target=\"http://www.iterable.com\">test</a></head>";

        final double backgroundAlpha = .5f;

        getInAppMessages(1, new IterableHelper.IterableActionHandler(){
            @Override
            public void execute(String payload) {

                JSONObject dialogOptions = IterableInAppManager.getNextMessageFromPayload(payload);
                if (dialogOptions != null) {
                    JSONObject message = dialogOptions.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
                    int templateId = message.optInt(IterableConstants.KEY_TEMPLATE_ID);

                    int campaignId = dialogOptions.optInt(IterableConstants.KEY_CAMPAIGN_ID);
                    String messageId = dialogOptions.optString(IterableConstants.KEY_MESSAGE_ID);

                    IterableApi.sharedInstance.trackInAppOpen(campaignId, templateId, messageId);
                    IterableApi.sharedInstance.inAppConsume(messageId);
//                    IterableInAppManager.showNotification(context, message, messageId, clickCallback);

                    //TODO: parse and pass into the notification for layout
                    Rect rect = new Rect(1,2,3,4);

                    IterableInAppManager.showIterableNotificationHTML(context, "", "", clickCallback, backgroundAlpha);
                }
            }
        });
        IterableInAppManager.showIterableNotificationHTML(context, htmlString, "", clickCallback, backgroundAlpha);
    }

    /**
     * Gets a list of InAppNotifications from Iterable; passes the result to the callback.
     * @param count the number of messages to fetch
     * @param onCallback
     */
    public void getInAppMessages(int count, IterableHelper.IterableActionHandler onCallback) {
        JSONObject requestJSON = new JSONObject();
        addEmailOrUserIdToJson(requestJSON);
        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_COUNT, count);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        sendGetRequest(IterableConstants.ENDPOINT_GET_INAPP_MESSAGES, requestJSON, onCallback);
    }

    /**
     * Tracks an InApp open.
     * @param campaignId
     * @param templateId
     * @param messageId
     */
    public void trackInAppOpen(int campaignId, int templateId, String messageId) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, requestJSON);
    }

    /**
     * Tracks an InApp click.
     * @param messageId
     * @param buttonIndex
     */
    public void trackInAppClick(String messageId, int buttonIndex) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_BUTTON_INDEX, buttonIndex);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, requestJSON);
    }

    /**
     * Consumes an InApp message.
     * @param messageId
     */
    public void inAppConsume(String messageId) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_INAPP_CONSUME, requestJSON);
    }

//---------------------------------------------------------------------------------------
//endregion

//region Protected Fuctions
//---------------------------------------------------------------------------------------

    /**
     * Set the notification icon with the given iconName.
     * @param context
     * @param iconName
     */
    static void setNotificationIcon(Context context, String iconName) {
        SharedPreferences sharedPref = context.getSharedPreferences(IterableConstants.NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.NOTIFICATION_ICON_NAME, iconName);
        editor.commit();
    }

    /**
     * Returns the stored notification icon.
     * @param context
     * @return
     */
    static String getNotificationIcon(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(IterableConstants.NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE);
        String iconName = sharedPref.getString(IterableConstants.NOTIFICATION_ICON_NAME, "");
        return iconName;
    }

    /**
     * Tracks when a push notification is opened on device.
     * @param campaignId
     * @param templateId
     */
    protected void trackPushOpen(int campaignId, int templateId, String messageId) {
        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_TRACK_PUSH_OPEN, requestJSON);
    }

    /**
     * Internal api call made from IterablePushRegistration after a registrationToken is obtained.
     * @param token
     */
    protected void disablePush(String token) {
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(IterableConstants.KEY_TOKEN, token);
            addEmailOrUserIdToJson(requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        sendPostRequest(IterableConstants.ENDPOINT_DISABLE_DEVICE, requestJSON);
    }

    /**
     * Registers the GCM registration ID with Iterable.
     * @param applicationName
     * @param token
     * @param pushServicePlatform
     * @param dataFields
     */
    protected void registerDeviceToken(String applicationName, String token, String pushServicePlatform, JSONObject dataFields) {
        String platform = IterableConstants.MESSAGING_PLATFORM_GOOGLE;

        JSONObject requestJSON = new JSONObject();
        try {
            addEmailOrUserIdToJson(requestJSON);

            if (dataFields == null) {
                dataFields = new JSONObject();
            }
            if (pushServicePlatform != null) {
                dataFields.put(IterableConstants.FIREBASE_COMPATIBLE, pushServicePlatform.equalsIgnoreCase(IterableConstants.MESSAGING_PLATFORM_FIREBASE));
            }
            dataFields.put(IterableConstants.DEVICE_BRAND, Build.BRAND); //brand: google
            dataFields.put(IterableConstants.DEVICE_MANUFACTURER, Build.MANUFACTURER); //manufacturer: samsung
            dataFields.putOpt(IterableConstants.DEVICE_ADID, getAdvertisingId()); //ADID: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
            dataFields.put(IterableConstants.DEVICE_SYSTEM_NAME, Build.DEVICE); //device name: toro
            dataFields.put(IterableConstants.DEVICE_SYSTEM_VERSION, Build.VERSION.RELEASE); //version: 4.0.4
            dataFields.put(IterableConstants.DEVICE_MODEL, Build.MODEL); //device model: Galaxy Nexus
            dataFields.put(IterableConstants.DEVICE_SDK_VERSION, Build.VERSION.SDK_INT); //sdk version/api level: 15

            JSONObject device = new JSONObject();
            device.put(IterableConstants.KEY_TOKEN, token);
            device.put(IterableConstants.KEY_PLATFORM, platform);
            device.put(IterableConstants.KEY_APPLICATION_NAME, applicationName);
            device.putOpt(IterableConstants.KEY_DATA_FIELDS, dataFields);
            requestJSON.put(IterableConstants.KEY_DEVICE, device);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, requestJSON);
    }

//---------------------------------------------------------------------------------------
//endregion

//region Private Fuctions
//---------------------------------------------------------------------------------------

    /**
     * Updates the data for the current user.
     * @param context
     * @param apiKey
     * @param email
     * @param userId
     */
    private void updateData(Context context, String apiKey, String email, String userId) {

        this._applicationContext = context;
        this._apiKey = apiKey;
        this._email = email;
        this._userId = userId;
    }

    /**
     * Attempts to track a notifOpened event from the called Intent.
     * @param calledIntent
     */
    private void tryTrackNotifOpen(Intent calledIntent) {
        Bundle extras = calledIntent.getExtras();
        if (extras != null) {
            Intent intent = new Intent();
            intent.setClass(_applicationContext, IterablePushOpenReceiver.class);
            intent.setAction(IterableConstants.ACTION_NOTIF_OPENED);
            intent.putExtras(extras);
            _applicationContext.sendBroadcast(intent);
        }
    }

    /**
     * Sends the POST request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    private void sendPostRequest(String resourcePath, JSONObject json) {
        IterableApiRequest request = new IterableApiRequest(_apiKey, resourcePath, json, IterableApiRequest.POST, null);
        new IterableRequest().execute(request);
    }

    /**
     * Sends a GET request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    private void sendGetRequest(String resourcePath, JSONObject json, IterableHelper.IterableActionHandler onCallback) {
        IterableApiRequest request = new IterableApiRequest(_apiKey, resourcePath, json, IterableApiRequest.GET, onCallback);
        new IterableRequest().execute(request);
    }

    /**
     * Adds the current email or userID to the json request.
     * @param requestJSON
     */
    private void addEmailOrUserIdToJson(JSONObject requestJSON) {
        try {
            if (_email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            } else {
                requestJSON.put(IterableConstants.KEY_USER_ID, _userId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the advertisingId if available
     * @return
     */
    private String getAdvertisingId() {
        String advertisingId = null;
        try {
            Class adClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            if (adClass != null) {
                AdvertisingIdClient.Info advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(_applicationContext);
                if (advertisingIdInfo != null) {
                    advertisingId = advertisingIdInfo.getId();
                }
            }
        } catch (IOException e) {
            IterableLogger.w(TAG, e.getMessage());
        } catch (GooglePlayServicesNotAvailableException e) {
            IterableLogger.w(TAG, e.getMessage());
        } catch (GooglePlayServicesRepairableException e) {
            IterableLogger.w(TAG, e.getMessage());
        } catch (ClassNotFoundException e) {
            IterableLogger.d(TAG, "ClassNotFoundException: Can't track ADID. " +
                    "Check that play-services-ads is added to the dependencies.", e);
        }
        return advertisingId;
    }

//---------------------------------------------------------------------------------------
//endregion

}
