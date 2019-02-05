package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.iterable.iterableapi.ddl.DeviceInfo;
import com.iterable.iterableapi.ddl.MatchFpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableApi {

//region Variables
//---------------------------------------------------------------------------------------
    static final String TAG = "IterableApi";

    /**
     * {@link IterableApi} singleton instance
     */
    static volatile IterableApi sharedInstance = new IterableApi();

    private Context _applicationContext;
    IterableConfig config;
    boolean sdkCompatEnabled;
    private String _apiKey;
    private String _email;
    private String _userId;
    private boolean _debugMode;
    private Bundle _payloadData;
    private IterableNotificationData _notificationData;
    private String _deviceId;

    private IterableInAppManager inAppManager;

//---------------------------------------------------------------------------------------
//endregion

//region Constructor
//---------------------------------------------------------------------------------------
    IterableApi() {
        config = new IterableConfig.Builder().build();
    }

    @VisibleForTesting
    IterableApi(IterableInAppManager inAppManager) {
        config = new IterableConfig.Builder().build();
        this.inAppManager = inAppManager;
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
     * Retrieves all of the payload as a single Bundle Object
     * @return Bundle
     */

    public Bundle getPayloadData() {
        return _payloadData;
    }

    public IterableInAppManager getInAppManager() {
        if (inAppManager == null) {
            inAppManager = new IterableInAppManager(this, config.inAppHandler);
        }
        return inAppManager;
    }

    /**
     * Returns the attribution information ({@link IterableAttributionInfo}) for last push open
     * or app link click from an email.
     * @return {@link IterableAttributionInfo} Object containing
     */
    public IterableAttributionInfo getAttributionInfo() {
        return IterableAttributionInfo.fromJSONObject(
                IterableUtil.retrieveExpirableJsonObject(getPreferences(), IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_KEY)
        );
    }

    /**
     * Stores attribution information.
     * @param attributionInfo Attribution information object
     */
    void setAttributionInfo(IterableAttributionInfo attributionInfo) {
        if (_applicationContext == null) {
            IterableLogger.e(TAG, "setAttributionInfo: Iterable SDK is not initialized with a context.");
            return;
        }

        IterableUtil.saveExpirableJsonObject(
                getPreferences(),
                IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_KEY,
                attributionInfo.toJSONObject(),
                3600 * IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_EXPIRATION_HOURS * 1000
                );
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
        if (extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY) && !IterableNotificationBuilder.isGhostPush(extras)) {
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
        if (data != null) {
            setAttributionInfo(new IterableAttributionInfo(data.getCampaignId(), data.getTemplateId(), data.getMessageId()));
        }
    }
//---------------------------------------------------------------------------------------
//endregion



//region Public Functions
//---------------------------------------------------------------------------------------

    /**
     * Get {@link IterableApi} singleton instance
     * @return {@link IterableApi} singleton instance
     */
    public static IterableApi getInstance() {
        return sharedInstance;
    }

    /**
     * Initializes IterableApi
     * This method must be called from {@link Application#onCreate()}
     * Note: Make sure you also call {@link #setEmail(String)} or {@link #setUserId(String)} before calling other methods
     *
     * @param context Application context
     * @param apiKey Iterable Mobile API key
     */
    public static void initialize(Context context, String apiKey) {
        initialize(context, apiKey, null);
    }

    /**
     * Initializes IterableApi
     * This method must be called from {@link Application#onCreate()}
     * Note: Make sure you also call {@link #setEmail(String)} or {@link #setUserId(String)} before calling other methods
     *
     * @param context Application context
     * @param apiKey Iterable Mobile API key
     * @param config {@link IterableConfig} object holding SDK configuration options
     */
    public static void initialize(Context context, String apiKey, IterableConfig config) {
        sharedInstance._applicationContext = context.getApplicationContext();
        sharedInstance._apiKey = apiKey;
        sharedInstance.config = config;

        if (sharedInstance.config == null) {
            sharedInstance.config = new IterableConfig.Builder().build();
        }
        sharedInstance.sdkCompatEnabled = false;
        sharedInstance.retrieveEmailAndUserId();
        sharedInstance.checkForDeferredDeeplink();
        IterableActivityMonitor.getInstance().registerLifecycleCallbacks(context);

        if (sharedInstance.config.autoPushRegistration && sharedInstance.isInitialized()) {
            sharedInstance.registerForPush();
        }
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * @param currentActivity The current activity
     * @param userId The current userId
     * @return stored instance of IterableApi
     *
     * @deprecated Initialize the SDK with {@link #initialize(Context, String, IterableConfig)} instead
     */
    @Deprecated
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
     * @param userId The current userId@return stored instance of IterableApi
     *
     * @deprecated Initialize the SDK with {@link #initialize(Context, String, IterableConfig)} instead
     */
    @Deprecated
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
     *
     * @deprecated Initialize the SDK with {@link #initialize(Context, String, IterableConfig)} instead
     */
    @Deprecated
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
     *
     * @deprecated Initialize the SDK with {@link #initialize(Context, String, IterableConfig)} instead
     */
    @Deprecated
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
     *
     * @deprecated Initialize the SDK with {@link #initialize(Context, String, IterableConfig)} instead
     */
    @Deprecated
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
    @Deprecated
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
     *
     * @deprecated Initialize the SDK with {@link #initialize(Context, String, IterableConfig)} instead
     */
    @Deprecated
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
     *
     * @deprecated Initialize the SDK with {@link #initialize(Context, String, IterableConfig)} instead
     */
    @Deprecated
    public static IterableApi sharedInstanceWithApiKey(Context currentContext, String apiKey,
                                                       String email, boolean debugMode)
    {
        return sharedInstanceWithApiKey(currentContext, apiKey, email, null, debugMode);
    }

    private static IterableApi sharedInstanceWithApiKey(Context currentContext, String apiKey,
                                                       String email, String userId, boolean debugMode)
    {
        sharedInstance.updateData(currentContext.getApplicationContext(), apiKey, email, userId);
        sharedInstance.setDebugMode(debugMode);
        sharedInstance.sdkCompatEnabled = true;

        return sharedInstance;
    }

    /**
     * Set user email used for API calls
     * Calling this or `setUserId:` is required before making any API calls.
     *
     * Note: This clears userId and persists the user email so you only need to call this once when the user logs in.
     * @param email User email
     */
    public void setEmail(String email) {
        if (_email != null && _email.equals(email)) {
            return;
        }

        if (_email == null && _userId == null && email == null) {
            return;
        }

        onLogOut();
        _email = email;
        _userId = null;
        storeEmailAndUserId();
        onLogIn();
    }

    /**
     * Set user ID used for API calls
     * Calling this or `setEmail:` is required before making any API calls.
     *
     * Note: This clears user email and persists the user ID so you only need to call this once when the user logs in.
     * @param userId User ID
     */
    public void setUserId(String userId) {
        if (_userId != null && _userId.equals(userId)) {
            return;
        }

        if (_email == null && _userId == null && userId == null) {
            return;
        }

        onLogOut();
        _email = null;
        _userId = userId;
        storeEmailAndUserId();
        onLogIn();
    }

    /**
     * Tracks a click on the uri if it is an iterable link.
     * @param uri the
     * @param onCallback Calls the callback handler with the destination location
     *                   or the original url if it is not a interable link.
     */
    public static void getAndTrackDeeplink(String uri, IterableHelper.IterableActionHandler onCallback) {
        IterableDeeplinkManager.getAndTrackDeeplink(uri, onCallback);
    }

    /**
     * Handles an App Link
     * For Iterable links, it will track the click and retrieve the original URL, pass it to
     * {@link IterableUrlHandler} for handling
     * If it's not an Iterable link, it just passes the same URL to {@link IterableUrlHandler}
     *
     * Call this from {@link Activity#onCreate(Bundle)} and {@link Activity#onNewIntent(Intent)}
     * in your deep link handler activity
     * @param uri the URL obtained from {@link Intent#getData()} in your deep link
     *            handler activity
     * @return
     */
    public static boolean handleAppLink(String uri) {
        if (IterableDeeplinkManager.isIterableDeeplink(uri)) {
            IterableDeeplinkManager.getAndTrackDeeplink(uri, new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String originalUrl) {
                    IterableAction action = IterableAction.actionOpenUrl(originalUrl);
                    IterableActionRunner.executeAction(getInstance().getMainActivityContext(), action, IterableActionSource.APP_LINK);
                }
            });
            return true;
        } else {
            IterableAction action = IterableAction.actionOpenUrl(uri);
            return IterableActionRunner.executeAction(getInstance().getMainActivityContext(), action, IterableActionSource.APP_LINK);
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
     * @deprecated Push opens are now tracked automatically.
     */
    @Deprecated
    public void onNewIntent(Intent intent) {

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
     * Make sure {@link IterableConfig#pushIntegrationName} is set before calling this.
     * @param token Push token obtained from GCM or FCM
     */
    public void registerDeviceToken(String token) {
        if (config.pushIntegrationName == null) {
            IterableLogger.e(TAG, "registerDeviceToken: pushIntegrationName is not set");
            return;
        }

        registerDeviceToken(config.pushIntegrationName, token);
    }

    /**
     * Registers a device token with Iterable.
     * @param applicationName
     * @param token
     * @deprecated Call {@link #registerDeviceToken(String)} instead and specify the push
     * integration name in {@link IterableConfig#pushIntegrationName}
     */
    @Deprecated
    public void registerDeviceToken(String applicationName, String token) {
        registerDeviceToken(applicationName, token, IterableConstants.MESSAGING_PLATFORM_FIREBASE);
    }

    /**
     * Registers a device token with Iterable.
     * @param applicationName
     * @param token
     * @param pushServicePlatform
     * @deprecated Call {@link #registerDeviceToken(String)} instead and specify the push
     * integration name in {@link IterableConfig#pushIntegrationName}
     */
    @Deprecated
    public void registerDeviceToken(final String applicationName, final String token, final String pushServicePlatform) {
        registerDeviceToken(_email, _userId, applicationName, token, pushServicePlatform);
    }

    protected void registerDeviceToken(final String email, final String userId, final String applicationName, final String token, final String pushServicePlatform) {
        if (!IterableConstants.MESSAGING_PLATFORM_FIREBASE.equals(pushServicePlatform)) {
            IterableLogger.e(TAG, "registerDeviceToken: only MESSAGING_PLATFORM_FIREBASE is supported.");
            return;
        }

        if (token != null) {
            final Thread registrationThread = new Thread(new Runnable() {
                public void run() {
                    registerDeviceToken(email, userId, applicationName, token, IterableConstants.MESSAGING_PLATFORM_FIREBASE, null);
                }
            });
            registrationThread.start();
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
        if (!checkSDKInitialization()) {
            return;
        }

        JSONObject requestJSON = new JSONObject();
        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_EVENT_NAME, eventName);

            if (campaignId != 0) {
                requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            }
            if (templateId != 0) {
                requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            }
            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK, requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tracks a purchase.
     * @param total total purchase amount
     * @param items list of purchased items
     */
    public void trackPurchase(double total, List<CommerceItem> items) {
        trackPurchase(total, items, null);
    }

    /**
     * Tracks a purchase.
     * @param total total purchase amount
     * @param items list of purchased items
     * @param dataFields a `JSONObject` containing any additional information to save along with the event
     */
    public void trackPurchase(double total, List<CommerceItem> items, JSONObject dataFields) {
        if (!checkSDKInitialization()) {
            return;
        }

        JSONObject requestJSON = new JSONObject();
        try {
            JSONArray itemsArray = new JSONArray();
            for (CommerceItem item : items) {
                itemsArray.put(item.toJSONObject());
            }

            JSONObject userObject = new JSONObject();
            addEmailOrUserIdToJson(userObject);
            requestJSON.put(IterableConstants.KEY_USER, userObject);

            requestJSON.put(IterableConstants.KEY_ITEMS, itemsArray);
            requestJSON.put(IterableConstants.KEY_TOTAL, total);
            if (dataFields != null) {
                requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            }

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_PURCHASE, requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
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
        if (!checkSDKInitialization()) {
            return;
        }

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

            sendPostRequest(IterableConstants.ENDPOINT_PUSH_TARGET, requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the current user's email.
     * Also updates the current email in this IterableAPI instance if the API call was successful.
     * @param newEmail New email
     */
    public void updateEmail(final String newEmail) {
        updateEmail(newEmail, null, null);
    }

    /**
     * Updates the current user's email.
     * Also updates the current email in this IterableAPI instance if the API call was successful.
     * @param newEmail New email
     * @param successHandler Success handler. Called when the server returns a success code.
     * @param failureHandler Failure handler. Called when the server call failed.
     */
    public void updateEmail(final String newEmail, final IterableHelper.SuccessHandler successHandler, IterableHelper.FailureHandler failureHandler) {
        if (_email != null) {
            JSONObject requestJSON = new JSONObject();

            try {
                requestJSON.put(IterableConstants.KEY_CURRENT_EMAIL, _email);
                requestJSON.put(IterableConstants.KEY_NEW_EMAIL, newEmail);

                sendPostRequest(IterableConstants.ENDPOINT_UPDATE_EMAIL, requestJSON, new IterableHelper.SuccessHandler() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        _email = newEmail;
                        storeEmailAndUserId();
                        if (successHandler != null) {
                            successHandler.onSuccess(data);
                        }
                    }
                }, failureHandler);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            IterableLogger.w(TAG, "updateEmail should not be called with a userId. " +
                "Make sure you call setEmail(String) before trying to update it");
        }
    }

    /**
     * Updates the current user.
     * @param dataFields
     */
    public void updateUser(JSONObject dataFields) {
        if (!checkSDKInitialization()) {
            return;
        }

        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields);

            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_USER, requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Registers for push notifications.
     * Make sure the API is initialized with {@link IterableConfig#pushIntegrationName} defined, and
     * user email or user ID is set before calling this method.
     */
    public void registerForPush() {
        if (!checkSDKInitialization()) {
            return;
        }

        if (config.pushIntegrationName == null) {
            IterableLogger.e(TAG, "registerForPush: pushIntegrationName is not set");
            return;
        }

        IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, config.pushIntegrationName, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistration().execute(data);
    }

    /**
     * Registers for push notifications.
     * @param pushIntegrationName
     * @param gcmProjectNumber
     * @deprecated Call {@link #registerForPush()} instead and specify the push
     * integration name in {@link IterableConfig#pushIntegrationName}
     */
    @Deprecated
    public void registerForPush(String pushIntegrationName, String gcmProjectNumber) {
        registerForPush(pushIntegrationName, gcmProjectNumber, IterableConstants.MESSAGING_PLATFORM_FIREBASE);
    }

    /**
     * Registers for push notifications.
     * @param pushIntegrationName
     * @param projectNumber
     * @param pushServicePlatform
     * @deprecated Call {@link #registerForPush()} instead and specify the push
     * integration name in {@link IterableConfig#pushIntegrationName}
     */
    @Deprecated
    public void registerForPush(String pushIntegrationName, String projectNumber, String pushServicePlatform) {
        if (!IterableConstants.MESSAGING_PLATFORM_FIREBASE.equals(pushServicePlatform)) {
            IterableLogger.e(TAG, "registerDeviceToken: only MESSAGING_PLATFORM_FIREBASE is supported.");
            return;
        }

        IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, pushIntegrationName, projectNumber, pushServicePlatform, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistration().execute(data);
    }

    /**
     * Registers for push notifications.
     * @param pushIntegrationName
     * @deprecated Call {@link #registerForPush()} instead and specify the push
     * integration name in {@link IterableConfig#pushIntegrationName}
     */
    @Deprecated
    public void registerForPush(String pushIntegrationName) {
        IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, pushIntegrationName, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistration().execute(data);
    }

    /**
     * Disables the device from push notifications
     */
    public void disablePush() {
        if (config.pushIntegrationName == null) {
            IterableLogger.e(TAG, "disablePush: pushIntegrationName is not set");
            return;
        }

        IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, config.pushIntegrationName, IterablePushRegistrationData.PushRegistrationAction.DISABLE);
        new IterablePushRegistration().execute(data);
    }

    /**
     * Disables the device from push notifications
     * @param iterableAppId
     * @param gcmProjectNumber
     * @deprecated Call {@link #disablePush()} instead and specify the push
     * integration name in {@link IterableConfig#pushIntegrationName}
     */
    @Deprecated
    public void disablePush(String iterableAppId, String gcmProjectNumber) {
        disablePush(iterableAppId, gcmProjectNumber, IterableConstants.MESSAGING_PLATFORM_FIREBASE);
    }

    /**
     * Disables the device from push notifications
     * @param iterableAppId
     * @param projectNumber
     * @param pushServicePlatform
     * @deprecated Call {@link #disablePush()} instead and specify the push
     * integration name in {@link IterableConfig#pushIntegrationName}
     */
    @Deprecated
    public void disablePush(String iterableAppId, String projectNumber, String pushServicePlatform) {
        if (!IterableConstants.MESSAGING_PLATFORM_FIREBASE.equals(pushServicePlatform)) {
            IterableLogger.e(TAG, "registerDeviceToken: only MESSAGING_PLATFORM_FIREBASE is supported.");
            return;
        }

        IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, iterableAppId, projectNumber, pushServicePlatform, IterablePushRegistrationData.PushRegistrationAction.DISABLE);
        new IterablePushRegistration().execute(data);
    }

    /**
     * Updates the user subscription preferences. Passing in an empty array will clear the list, passing in null will not modify the list
     * @param emailListIds
     * @param unsubscribedChannelIds
     * @param unsubscribedMessageTypeIds
     */
    public void updateSubscriptions(Integer[] emailListIds, Integer[] unsubscribedChannelIds, Integer[] unsubscribedMessageTypeIds) {
        if (!checkSDKInitialization()) {
            return;
        }

        JSONObject requestJSON = new JSONObject();
        addEmailOrUserIdToJson(requestJSON);

        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_EMAIL_LIST_IDS, emailListIds);
        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_UNSUB_CHANNEL, unsubscribedChannelIds);
        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_UNSUB_MESSAGE, unsubscribedMessageTypeIds);

        sendPostRequest(IterableConstants.ENDPOINT_UPDATE_USER_SUBS, requestJSON);
    }

    /**
     * Attempts to add an array as a JSONArray to a JSONObject
     * @param requestJSON
     * @param key
     * @param value
     */
    void tryAddArrayToJSON(JSONObject requestJSON, String key, Object[] value) {
        if (requestJSON != null && key != null && value != null)
            try {
                JSONArray mJSONArray = new JSONArray(Arrays.asList(value));
                requestJSON.put(key, mJSONArray);
            } catch (JSONException e) {
                IterableLogger.e(TAG, e.toString());
            }
    }

    /**
     * Gets a notification from Iterable and displays it on device.
     * @param context
     * @param clickCallback
     */
    public void spawnInAppNotification(final Context context, final IterableHelper.IterableActionHandler clickCallback) {
        if (!checkSDKInitialization()) {
            return;
        }

        getInAppMessages(1, new IterableHelper.IterableActionHandler(){
            @Override
            public void execute(String payload) {

                JSONObject dialogOptions = IterableInAppMessage.getNextMessageFromPayload(payload);
                if (dialogOptions != null) {
                    JSONObject message = dialogOptions.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
                    if (message != null) {
                        String messageId = dialogOptions.optString(IterableConstants.KEY_MESSAGE_ID);
                        String html = message.optString("html");
                        if (html.toLowerCase().contains("href")) {
                            JSONObject paddingOptions = message.optJSONObject("inAppDisplaySettings");
                            Rect padding = IterableInAppMessage.getPaddingFromPayload(paddingOptions);

                            double backgroundAlpha = message.optDouble("backgroundAlpha", 0);
                            if (IterableInAppManager.showIterableNotificationHTML(context, html, messageId, clickCallback, backgroundAlpha, padding)) {
                                IterableApi.sharedInstance.inAppConsume(messageId);
                            }
                        } else {
                            IterableLogger.w(TAG, "No href tag in found in the in-app html payload: "+ html);
                            IterableApi.sharedInstance.inAppConsume(messageId);
                        }
                    }
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
        if (!checkSDKInitialization()) {
            return;
        }

        JSONObject requestJSON = new JSONObject();
        addEmailOrUserIdToJson(requestJSON);
        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_COUNT, count);
            requestJSON.put(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID);
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);

            sendGetRequest(IterableConstants.ENDPOINT_GET_INAPP_MESSAGES, requestJSON, onCallback);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tracks an InApp open.
     * @param messageId
     */
    public void trackInAppOpen(String messageId) {
        if (!checkSDKInitialization()) {
            return;
        }

        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tracks an InApp click.
     * @param messageId
     * @param urlClick
     */
    public void trackInAppClick(String messageId, String urlClick) {
        if (!checkSDKInitialization()) {
            return;
        }

        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_URL_CLICK, urlClick);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Consumes an InApp message.
     * @param messageId
     */
    public void inAppConsume(String messageId) {
        if (!checkSDKInitialization()) {
            return;
        }

        JSONObject requestJSON = new JSONObject();

        try {
            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);

            sendPostRequest(IterableConstants.ENDPOINT_INAPP_CONSUME, requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

//---------------------------------------------------------------------------------------
//endregion


//region Package-Protected Fuctions
//---------------------------------------------------------------------------------------

    /**
     * Get user email
     * @return user email
     */
    String getEmail() {
        return _email;
    }

    /**
     * Get user ID
     * @return user ID
     */
    String getUserId() {
        return _userId;
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

    protected void trackPushOpen(int campaignId, int templateId, String messageId) {
        trackPushOpen(campaignId, templateId, messageId, null);
    }

    /**
     * Tracks when a push notification is opened on device.
     * @param campaignId
     * @param templateId
     */
    protected void trackPushOpen(int campaignId, int templateId, String messageId, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            if (dataFields == null) {
                dataFields = new JSONObject();
            }

            addEmailOrUserIdToJson(requestJSON);
            requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId);
            requestJSON.putOpt(IterableConstants.KEY_DATA_FIELDS, dataFields);

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_PUSH_OPEN, requestJSON);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void disableToken(String email, String userId, String token) {
        disableToken(email, userId, token, null, null);
    }

    /**
     * Internal api call made from IterablePushRegistration after a registrationToken is obtained.
     * @param token
     */
    protected void disableToken(String email, String userId, String token, IterableHelper.SuccessHandler onSuccess, IterableHelper.FailureHandler onFailure) {
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(IterableConstants.KEY_TOKEN, token);
            if (email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, email);
            } else {
                requestJSON.put(IterableConstants.KEY_USER_ID, userId);
            }

            sendPostRequest(IterableConstants.ENDPOINT_DISABLE_DEVICE, requestJSON, onSuccess, onFailure);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers the GCM registration ID with Iterable.
     * @param applicationName
     * @param token
     * @param pushServicePlatform
     * @param dataFields
     */
    protected void registerDeviceToken(String email, String userId, String applicationName, String token, String pushServicePlatform, JSONObject dataFields) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (token == null) {
            IterableLogger.e(TAG, "registerDeviceToken: token is null");
            return;
        }

        if (applicationName == null) {
            IterableLogger.e(TAG, "registerDeviceToken: applicationName is null, check that pushIntegrationName is set in IterableConfig");
        }

        JSONObject requestJSON = new JSONObject();
        try {
            addEmailOrUserIdToJson(requestJSON);

            if (dataFields == null) {
                dataFields = new JSONObject();
            }
            dataFields.put(IterableConstants.FIREBASE_TOKEN_TYPE, IterableConstants.MESSAGING_PLATFORM_FIREBASE);
            dataFields.put(IterableConstants.FIREBASE_COMPATIBLE, true);
            dataFields.put(IterableConstants.DEVICE_BRAND, Build.BRAND); //brand: google
            dataFields.put(IterableConstants.DEVICE_MANUFACTURER, Build.MANUFACTURER); //manufacturer: samsung
            dataFields.putOpt(IterableConstants.DEVICE_ADID, getAdvertisingId()); //ADID: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
            dataFields.put(IterableConstants.DEVICE_SYSTEM_NAME, Build.DEVICE); //device name: toro
            dataFields.put(IterableConstants.DEVICE_SYSTEM_VERSION, Build.VERSION.RELEASE); //version: 4.0.4
            dataFields.put(IterableConstants.DEVICE_MODEL, Build.MODEL); //device model: Galaxy Nexus
            dataFields.put(IterableConstants.DEVICE_SDK_VERSION, Build.VERSION.SDK_INT); //sdk version/api level: 15

            dataFields.put(IterableConstants.DEVICE_ID, getDeviceId()); // Random UUID
            dataFields.put(IterableConstants.DEVICE_APP_PACKAGE_NAME, _applicationContext.getPackageName());
            dataFields.put(IterableConstants.DEVICE_APP_VERSION, IterableUtil.getAppVersion(_applicationContext));
            dataFields.put(IterableConstants.DEVICE_APP_BUILD, IterableUtil.getAppVersionCode(_applicationContext));
            dataFields.put(IterableConstants.DEVICE_ITERABLE_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);
            dataFields.put(IterableConstants.DEVICE_NOTIFICATIONS_ENABLED, NotificationManagerCompat.from(_applicationContext).areNotificationsEnabled());

            JSONObject device = new JSONObject();
            device.put(IterableConstants.KEY_TOKEN, token);
            device.put(IterableConstants.KEY_PLATFORM, IterableConstants.MESSAGING_PLATFORM_GOOGLE);
            device.put(IterableConstants.KEY_APPLICATION_NAME, applicationName);
            device.putOpt(IterableConstants.KEY_DATA_FIELDS, dataFields);
            requestJSON.put(IterableConstants.KEY_DEVICE, device);

            // Create the user by userId if it doesn't exist
            if (email == null && userId != null) {
                requestJSON.put(IterableConstants.KEY_PREFER_USER_ID, true);
            }

            sendPostRequest(IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, requestJSON);
        } catch (JSONException e) {
            IterableLogger.e(TAG, "registerDeviceToken: exception", e);
        }
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

    private boolean isInitialized() {
        return _apiKey != null && (_email != null || _userId != null);
    }

    private boolean checkSDKInitialization() {
        if (!isInitialized()) {
            IterableLogger.e(TAG, "Iterable SDK must be initialized with an API key and user email/userId before calling SDK methods");
            return false;
        }
        return true;
    }

    private SharedPreferences getPreferences() {
        return _applicationContext.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Sends the POST request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    void sendPostRequest(String resourcePath, JSONObject json) {
        IterableApiRequest request = new IterableApiRequest(_apiKey, resourcePath, json, IterableApiRequest.POST, null, null);
        new IterableRequest().execute(request);
    }

    void sendPostRequest(String resourcePath, JSONObject json, IterableHelper.SuccessHandler onSuccess, IterableHelper.FailureHandler onFailure) {
        IterableApiRequest request = new IterableApiRequest(_apiKey, resourcePath, json, IterableApiRequest.POST, onSuccess, onFailure);
        new IterableRequest().execute(request);
    }

    /**
     * Sends a GET request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    void sendGetRequest(String resourcePath, JSONObject json, IterableHelper.IterableActionHandler onCallback) {
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
        } catch (ClassNotFoundException e) {
            IterableLogger.d(TAG, "ClassNotFoundException: Can't track ADID. " +
                    "Check that play-services-ads is added to the dependencies.", e);
        } catch (Exception e) {
            IterableLogger.w(TAG, e.getMessage());
        }
        return advertisingId;
    }

    private String getDeviceId() {
        if (_deviceId == null) {
            _deviceId = getPreferences().getString(IterableConstants.SHARED_PREFS_DEVICEID_KEY, null);
            if (_deviceId == null) {
                _deviceId = UUID.randomUUID().toString();
                getPreferences().edit().putString(IterableConstants.SHARED_PREFS_DEVICEID_KEY, _deviceId).apply();
            }
        }
        return _deviceId;
    }

    private void storeEmailAndUserId() {
        try {
            SharedPreferences.Editor editor = getPreferences().edit();
            editor.putString(IterableConstants.SHARED_PREFS_EMAIL_KEY, _email);
            editor.putString(IterableConstants.SHARED_PREFS_USERID_KEY, _userId);
            editor.commit();
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while persisting email/userId", e);
        }
    }

    private void retrieveEmailAndUserId() {
        try {
            SharedPreferences prefs = getPreferences();
            _email = prefs.getString(IterableConstants.SHARED_PREFS_EMAIL_KEY, null);
            _userId = prefs.getString(IterableConstants.SHARED_PREFS_USERID_KEY, null);
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while retrieving email/userId", e);
        }
    }

    private void onLogOut() {
        if (config.autoPushRegistration && isInitialized()) {
            disablePush();
        }
    }

    private void onLogIn() {
        if (config.autoPushRegistration && isInitialized()) {
            registerForPush();
        }
        getInAppManager().syncInApp();
    }

    private boolean getDDLChecked() {
        return getPreferences().getBoolean(IterableConstants.SHARED_PREFS_DDL_CHECKED_KEY, false);
    }

    private void setDDLChecked(boolean value) {
        getPreferences().edit().putBoolean(IterableConstants.SHARED_PREFS_DDL_CHECKED_KEY, value).apply();
    }

    private void checkForDeferredDeeplink() {
        if (!config.checkForDeferredDeeplink) {
            return;
        }

        try {
            if (getDDLChecked()) {
                return;
            }

            JSONObject requestJSON = DeviceInfo.createDeviceInfo(_applicationContext).toJSONObject();

            IterableApiRequest request = new IterableApiRequest(_apiKey, IterableConstants.BASE_URL_LINKS,
                    IterableConstants.ENDPOINT_DDL_MATCH, requestJSON, IterableApiRequest.POST, new IterableHelper.SuccessHandler() {
                @Override
                public void onSuccess(JSONObject data) {
                    handleDDL(data);
                }
            }, new IterableHelper.FailureHandler() {
                @Override
                public void onFailure(String reason, JSONObject data) {
                    IterableLogger.e(TAG, "Error while checking deferred deep link: " + reason + ", response: " + data);
                }
            });
            new IterableRequest().execute(request);

        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while checking deferred deep link", e);
        }
    }

    private void handleDDL(JSONObject response) {
        IterableLogger.d(TAG, "handleDDL: " + response);
        try {
            MatchFpResponse matchFpResponse = MatchFpResponse.fromJSONObject(response);

            if (matchFpResponse.isMatch) {
                IterableAction action = IterableAction.actionOpenUrl(matchFpResponse.destinationUrl);
                IterableActionRunner.executeAction(getMainActivityContext(), action, IterableActionSource.APP_LINK);
            }
        } catch (JSONException e) {
            IterableLogger.e(TAG, "Error while handling deferred deep link", e);
        }
        setDDLChecked(true);
    }

//---------------------------------------------------------------------------------------
//endregion

}
