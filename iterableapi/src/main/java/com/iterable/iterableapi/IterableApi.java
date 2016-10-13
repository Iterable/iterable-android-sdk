package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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

    Context getMainActivityContext() {
        return _applicationContext;
    }

    void setDebugMode(boolean debugMode) {
        _debugMode = debugMode;
    }

    boolean getDebugMode() {
        return _debugMode;
    }

    void setPayloadData(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)) {
            setPayloadData(extras);
        }
    }

    void setPayloadData(Bundle bundle) {
        _payloadData = bundle;
    }

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
     * Registers an existing GCM device token with Iterable.
     * Recommended to use registerForPush if you do not already have a deviceToken
     * @param applicationName
     * @param token
     */
    public void registerDeviceToken(String applicationName, String token) {
        registerDeviceToken(applicationName, token, null);
    }

    public void track(String eventName) {
        track(eventName, null, null, null);
    }

    public void track(String eventName, JSONObject dataFields) {
        track(eventName, null, null, dataFields);
    }

    public void track(String eventName, String campaignId, String templateId) {
        track(eventName, campaignId, templateId, null);
    }

    public void track(String eventName, String campaignId, String templateId, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();
        try {
            if (_email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            } else {
                requestJSON.put(IterableConstants.KEY_USER_ID, _userId);
            }
            requestJSON.put(IterableConstants.KEY_EVENTNAME, eventName);

            requestJSON.put(IterableConstants.KEY_CAMPAIGNID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_DATAFIELDS, dataFields);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_TRACK, requestJSON);
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
            requestJSON.put(IterableConstants.KEY_CAMPAIGNID, campaignId);
            if (sendAt != null){
                String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String dateString = sdf.format(sendAt);
                requestJSON.put(IterableConstants.KEY_SEND_AT, dateString);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_PUSHTARGET, requestJSON);
    }

    public void updateEmail(String newEmail) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put(IterableConstants.KEY_CURRENT_EMAIL, _email);
            requestJSON.put(IterableConstants.KEY_NEW_EMAIL, newEmail);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_UPDATEEMAIL, requestJSON);

        if (_email != null) {
            _email = newEmail;
        } else {
            IterableLogger.w(TAG, "updateEmail should not be called with a userId. " +
                "Init SDK with sharedInstanceWithApiKey instead of sharedInstanceWithApiKeyWithUserId");
        }
    }

    public void updateUser(JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            if (_email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            } else {
                requestJSON.put(IterableConstants.KEY_USER_ID, _userId);
            }

            requestJSON.put(IterableConstants.KEY_DATAFIELDS, dataFields);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_UPDATEUSER, requestJSON);
    }

    public void registerForPush(String iterableAppId, String gcmProjectNumber) {
        registerForPush(iterableAppId, gcmProjectNumber, false);
    }

    public void disablePush(String iterableAppId, String gcmProjectNumber) {
        registerForPush(iterableAppId, gcmProjectNumber, true);
    }

//---------------------------------------------------------------------------------------
//endregion

//region Protected Fuctions
//---------------------------------------------------------------------------------------
    static void setNotificationIcon(Context context, String iconName) {
        SharedPreferences sharedPref = context.getSharedPreferences(IterableConstants.NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.NOTIFICATION_ICON_NAME, iconName);
        editor.commit();
    }

    static String getNotificationIcon(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(IterableConstants.NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE);
        String iconName = sharedPref.getString(IterableConstants.NOTIFICATION_ICON_NAME, "");
        return iconName;
    }

    protected void registerForPush(String iterableAppId, String gcmProjectNumber, boolean disableAfterRegistration) {
        Intent pushRegistrationIntent = new Intent(_applicationContext, IterablePushReceiver.class);
        pushRegistrationIntent.setAction(IterableConstants.ACTION_PUSH_REGISTRATION);
        pushRegistrationIntent.putExtra(IterableConstants.PUSH_APP_ID, iterableAppId);
        pushRegistrationIntent.putExtra(IterableConstants.PUSH_GCM_PROJECT_NUMBER, gcmProjectNumber);
        pushRegistrationIntent.putExtra(IterableConstants.PUSH_DISABLE_AFTER_REGISTRATION, disableAfterRegistration);
        _applicationContext.sendBroadcast(pushRegistrationIntent);
    }

    /**
     * Track when a push notification is opened on device.
     * @param campaignId
     * @param templateId
     */
    protected void trackPushOpen(int campaignId, int templateId, String messageId) {
        JSONObject requestJSON = new JSONObject();

        try {
            if (_email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            } else {
                requestJSON.put(IterableConstants.KEY_USER_ID, _userId);
            }
            requestJSON.put(IterableConstants.KEY_CAMPAIGNID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_TRACKPUSHOPEN, requestJSON);
    }

    /**
     * Internal api call made from IterablePushRegistrationGCM after a registration is completed.
     * @param token
     */
    protected void disablePush(String token) {
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(IterableConstants.KEY_TOKEN, token);
            if (_email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            } else {
                requestJSON.put(IterableConstants.KEY_USER_ID, _userId);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        sendRequest(IterableConstants.ENDPOINT_DISABLEDEVICE, requestJSON);
    }

//---------------------------------------------------------------------------------------
//endregion

//region Private Fuctions
//---------------------------------------------------------------------------------------
    private void updateData(Context context, String apiKey, String email, String userId) {
        this._applicationContext = context;
        this._apiKey = apiKey;
        this._email = email;
        this._userId = userId;
    }

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
     * Registers the GCM registration ID with Iterable.
     * @param applicationName
     * @param token
     * @param dataFields
     */
    private void registerDeviceToken(String applicationName, String token, JSONObject dataFields) {
        String platform = IterableConstants.MESSAGING_PLATFORM_GOOGLE;

        JSONObject requestJSON = new JSONObject();
        try {
            if (_email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            } else {
                requestJSON.put(IterableConstants.KEY_USER_ID, _userId);
            }

            if (dataFields == null) {
                dataFields = new JSONObject();
            }

            JSONObject device = new JSONObject();
            device.put(IterableConstants.KEY_TOKEN, token);
            device.put(IterableConstants.KEY_PLATFORM, platform);
            device.put(IterableConstants.KEY_APPLICATIONNAME, applicationName);
            device.put(IterableConstants.KEY_DATAFIELDS, dataFields);
            requestJSON.put(IterableConstants.KEY_DEVICE, device);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_REGISTERDEVICETOKEN, requestJSON);
    }
    /**
     * Sends the request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    private void sendRequest(String resourcePath, JSONObject json) {
        IterableApiRequest request = new IterableApiRequest(_apiKey, resourcePath, json.toString());
        new IterableRequest().execute(request);
    }

//---------------------------------------------------------------------------------------
//endregion

}
