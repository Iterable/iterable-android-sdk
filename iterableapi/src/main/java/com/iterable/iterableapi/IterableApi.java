package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
     * Registers an existing device token with Iterable.
     * Recommended to use registerForPush if you do not already have a deviceToken
     * @param applicationName
     * @param token
     */
    public void registerDeviceToken(String applicationName, String token) {
        registerDeviceToken(applicationName, token, null);
    }

    /**
     * Registers an existing device token with Iterable.
     * Recommended to use registerForPush if you do not already have a deviceToken
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
     * The disablePush call
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
     * The disablePush call
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
                    IterableNotificationData trackParams = new IterableNotificationData(campaignId, templateId, messageId);
                    IterableInAppManager.showNotification(context, message, trackParams, clickCallback);

                }
            }
        });
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
     * @param campaignId
     * @param templateId
     * @param messageId
     * @param buttonIndex
     */
    public void trackInAppClick(int campaignId, int templateId, String messageId, int buttonIndex) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_BUTTON_INDEX, buttonIndex);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, requestJSON);
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
     * Registers the GCM registration ID with Iterable.
     * @param applicationName
     * @param token
     * @param pushServicePlatform
     * @param dataFields
     */
    public void registerDeviceToken(String applicationName, String token, String pushServicePlatform, JSONObject dataFields) {
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
