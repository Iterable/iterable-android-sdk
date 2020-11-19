package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.iterable.iterableapi.ddl.DeviceInfo;
import com.iterable.iterableapi.ddl.MatchFpResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableApi {

//region Variables
//---------------------------------------------------------------------------------------
private static final String TAG = "IterableApi";

    /**
     * {@link IterableApi} singleton instance
     */
    static volatile IterableApi sharedInstance = new IterableApi();
    private static IterableTaskManager dataManager;

    private Context _applicationContext;
    IterableConfig config;
    private String _apiKey;
    private String _email;
    private String _userId;
    private String _authToken;
    private boolean _debugMode;
    private Bundle _payloadData;
    private IterableNotificationData _notificationData;
    private String _deviceId;
    private boolean _firstForegroundHandled;

    IterableApiClient apiClient = new IterableApiClient(new IterableApiAuthProvider());
    private @Nullable IterableInAppManager inAppManager;
    private String inboxSessionId;
    private IterableAuthManager authManager;
    private HashMap<String, String> deviceAttributes = new HashMap<>();

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

    @VisibleForTesting
    IterableApi(IterableApiClient apiClient, IterableInAppManager inAppManager) {
        config = new IterableConfig.Builder().build();
        this.apiClient = apiClient;
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
    public void setNotificationIcon(@Nullable String iconName) {
        setNotificationIcon(_applicationContext, iconName);
    }

    /**
     * Retrieves the payload string for a given key.
     * Used for deeplinking and retrieving extra data passed down along with a campaign.
     * @param key
     * @return Returns the requested payload data from the current push campaign if it exists.
     */
    @Nullable
    public String getPayloadData(@NonNull String key) {
        return (_payloadData != null) ? _payloadData.getString(key, null) : null;
    }

    /**
     * Retrieves all of the payload as a single Bundle Object
     * @return Bundle
     */

    @Nullable
    public Bundle getPayloadData() {
        return _payloadData;
    }

    /**
     * Returns an {@link IterableInAppManager} that can be used to manage in-app messages.
     * Make sure the Iterable API is initialized before calling this method.
     * @return {@link IterableInAppManager} instance
     */
    @NonNull
    public IterableInAppManager getInAppManager() {
        if (inAppManager == null) {
            throw new RuntimeException("IterableApi must be initialized before calling getInAppManager(). " +
                    "Make sure you call IterableApi#initialize() in Application#onCreate");
        }
        return inAppManager;
    }

    /**
     * Returns an {@link IterableAuthManager} that can be used to manage mobile auth.
     * Make sure the Iterable API is initialized before calling this method.
     * @return {@link IterableAuthManager} instance
     */
    @NonNull
    IterableAuthManager getAuthManager() {
        if (authManager == null) {
            authManager = new IterableAuthManager(this, config.authHandler, config.expiringAuthTokenRefreshPeriod);
        }
        return authManager;
    }

    /**
     * Returns the attribution information ({@link IterableAttributionInfo}) for last push open
     * or app link click from an email.
     * @return {@link IterableAttributionInfo} Object containing
     */
    @Nullable
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
        if (extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY) && !IterableNotificationHelper.isGhostPush(extras)) {
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

    void setAuthToken(String authToken) {
        setAuthToken(authToken, false);
    }

    void setAuthToken(String authToken, boolean bypassAuth) {
        if (isInitialized()) {
            if ((authToken != null && !authToken.equalsIgnoreCase(_authToken)) || (_authToken != null && !_authToken.equalsIgnoreCase(authToken))) {
                _authToken = authToken;
                storeAuthData();
                onLogIn();
            } else if (bypassAuth) {
                onLogIn();
            }
        }
    }

    HashMap getDeviceAttributes() {
        return deviceAttributes;
    }

    public void setDeviceAttribute(String key, String value) {
        deviceAttributes.put(key, value);
    }

    public void removeDeviceAttribute(String key) {
        deviceAttributes.remove(key);
    }
//---------------------------------------------------------------------------------------
//endregion



//region Public Functions
//---------------------------------------------------------------------------------------

    /**
     * Get {@link IterableApi} singleton instance
     * @return {@link IterableApi} singleton instance
     */
    @NonNull
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
    public static void initialize(@NonNull Context context, @NonNull String apiKey) {
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
    public static void initialize(@NonNull Context context, @NonNull String apiKey, @Nullable IterableConfig config) {
        sharedInstance._applicationContext = context.getApplicationContext();
        sharedInstance._apiKey = apiKey;
        sharedInstance.config = config;

        if (sharedInstance.config == null) {
            sharedInstance.config = new IterableConfig.Builder().build();
        }
        sharedInstance.retrieveEmailAndUserId();
        sharedInstance.checkForDeferredDeeplink();
        IterableActivityMonitor.getInstance().registerLifecycleCallbacks(context);
        IterableActivityMonitor.getInstance().addCallback(sharedInstance.activityMonitorListener);
        if (sharedInstance.inAppManager == null) {
            sharedInstance.inAppManager = new IterableInAppManager(sharedInstance, sharedInstance.config.inAppHandler,
                    sharedInstance.config.inAppDisplayInterval);
        }
        IterablePushActionReceiver.processPendingAction(context);

        //TODO: Temporary code to test. Datamanger would eventually either go to IterableRequest or apiInternal as architecture concretes.
        //TODO: Have test methods to test all TaskManager methods.
        if (dataManager == null) {
            dataManager = new IterableTaskManager();
        }
        dataManager.createTask("testTask");
        ArrayList<String> taskIDsInDB = dataManager.getAllTaskIds();
        IterableLogger.v(TAG, "These are the tasks in DB now..");
        for (String taskId: taskIDsInDB
             ) {
            IterableLogger.v("Offline", taskId);
            IterableTask task = dataManager.getTask(taskId);
            IterableLogger.v(TAG+"Thats Task",task.createdAt.toString());
            dataManager.updateModifiedAt(taskId, new Date());
        }


    }

    /**
     * Set user email used for API calls
     * Calling this or {@link #setUserId(String)} is required before making any API calls.
     *
     * Note: This clears userId and persists the user email so you only need to call this once when the user logs in.
     * @param email User email
     */
    public void setEmail(@Nullable String email) {
        if (_email != null && _email.equals(email)) {
            return;
        }

        if (_email == null && _userId == null && email == null) {
            return;
        }

        onLogOut();
        _email = email;
        _userId = null;
        storeAuthData();

        if (email != null) {
            getAuthManager().requestNewAuthToken(false);
        } else {
            setAuthToken(null);
        }
    }

    /**
     * Set user ID used for API calls
     * Calling this or {@link #setEmail(String)} is required before making any API calls.
     *
     * Note: This clears user email and persists the user ID so you only need to call this once when the user logs in.
     * @param userId User ID
     */
    public void setUserId(@Nullable String userId) {
        if (_userId != null && _userId.equals(userId)) {
            return;
        }

        if (_email == null && _userId == null && userId == null) {
            return;
        }

        onLogOut();
        _email = null;
        _userId = userId;
        storeAuthData();

        if (userId != null) {
            getAuthManager().requestNewAuthToken(false);
        } else {
            setAuthToken(null);
        }
    }

    /**
     * Tracks a click on the uri if it is an iterable link.
     * @param uri the
     * @param onCallback Calls the callback handler with the destination location
     *                   or the original url if it is not an Iterable link.
     */
    public static void getAndTrackDeeplink(@NonNull String uri, @NonNull IterableHelper.IterableActionHandler onCallback) {
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
     * @return whether or not the app link was handled
     */
    public static boolean handleAppLink(@NonNull String uri) {
        IterableLogger.printInfo();
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
    public static void overrideURLEndpointPath(@NonNull String url) {
        IterableRequest.overrideUrl = url;
    }

    /**
     * Returns whether or not the intent was sent from Iterable.
     */
    public boolean isIterableIntent(@Nullable Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            return (extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY));
        }
        return false;
    }

    /**
     * Registers a device token with Iterable.
     * Make sure {@link IterableConfig#pushIntegrationName} is set before calling this.
     * @param deviceToken Push token obtained from GCM or FCM
     */
    public void registerDeviceToken(@NonNull String deviceToken) {
        registerDeviceToken(_email, _userId, _authToken, getPushIntegrationName(), deviceToken, deviceAttributes);
    }

    protected void registerDeviceToken(final @Nullable String email, final @Nullable String userId, final @Nullable String authToken, final @NonNull String applicationName, final @NonNull String deviceToken, final HashMap<String, String> deviceAttributes) {
        if (deviceToken != null) {
            final Thread registrationThread = new Thread(new Runnable() {
                public void run() {
                    registerDeviceToken(email, userId, authToken, applicationName, deviceToken, null, deviceAttributes);
                }
            });
            registrationThread.start();
        }
    }

    /**
     * Track an event.
     * @param eventName
     */
    public void track(@NonNull String eventName) {
        track(eventName, 0, 0, null);
    }

    /**
     * Track an event.
     * @param eventName
     * @param dataFields
     */
    public void track(@NonNull String eventName, @Nullable JSONObject dataFields) {
        track(eventName, 0, 0, dataFields);
    }

    /**
     * Track an event.
     * @param eventName
     * @param campaignId
     * @param templateId
     */
    public void track(@NonNull String eventName, int campaignId, int templateId) {
        track(eventName, campaignId, templateId, null);
    }

    /**
     * Track an event.
     * @param eventName
     * @param campaignId
     * @param templateId
     * @param dataFields
     */
    public void track(@NonNull String eventName, int campaignId, int templateId, @Nullable JSONObject dataFields) {
        IterableLogger.printInfo();
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.track(eventName, campaignId, templateId, dataFields);
    }

    /**
     * Tracks a purchase.
     * @param total total purchase amount
     * @param items list of purchased items
     */
    public void trackPurchase(double total, @NonNull List<CommerceItem> items) {
        trackPurchase(total, items, null);
    }

    /**
     * Tracks a purchase.
     * @param total total purchase amount
     * @param items list of purchased items
     * @param dataFields a `JSONObject` containing any additional information to save along with the event
     */
    public void trackPurchase(double total, @NonNull List<CommerceItem> items, @Nullable JSONObject dataFields) {
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.trackPurchase(total, items, dataFields);
    }

    /**
     * Updates the current user's email.
     * Also updates the current email in this IterableAPI instance if the API call was successful.
     * @param newEmail New email
     */
    public void updateEmail(final @NonNull String newEmail) {
        updateEmail(newEmail, null, null);
    }

    /**
     * Updates the current user's email.
     * Also updates the current email and authToken in this IterableAPI instance if the API call was successful.
     * @param newEmail New email
     * @param successHandler Success handler. Called when the server returns a success code.
     * @param failureHandler Failure handler. Called when the server call failed.
     */
    public void updateEmail(final @NonNull String newEmail, final @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
       if (!checkSDKInitialization()) {
            IterableLogger.e(TAG, "The Iterable SDK must be initialized with email or userId before " +
                    "calling updateEmail");
            if (failureHandler != null) {
                failureHandler.onFailure("The Iterable SDK must be initialized with email or " +
                        "userId before calling updateEmail", null);
            }
            return;
        }

        apiClient.updateEmail(newEmail, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                if (_email != null) {
                    _email = newEmail;
                }

                storeAuthData();
                getAuthManager().requestNewAuthToken(false);
                if (successHandler != null) {
                    successHandler.onSuccess(data);

                }
            }
        }, failureHandler);
    }

    /**
     * Updates the current user.
     * @param dataFields
     */
    public void updateUser(@NonNull JSONObject dataFields) {
        updateUser(dataFields, false);
    }

    /**
     * Updates the current user.
     * @param dataFields
     * @param mergeNestedObjects
     */
    public void updateUser(@NonNull JSONObject dataFields, Boolean mergeNestedObjects) {
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.updateUser(dataFields, mergeNestedObjects);
    }

    private String getPushIntegrationName() {
        if (config.pushIntegrationName != null) {
            return config.pushIntegrationName;
        } else {
            return _applicationContext.getPackageName();
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

        IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, _authToken, getPushIntegrationName(), IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        IterablePushRegistration.executePushRegistrationTask(data);
    }

    /**
     * Disables the device from push notifications
     */
    public void disablePush() {
        IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, _authToken, getPushIntegrationName(), IterablePushRegistrationData.PushRegistrationAction.DISABLE);
        IterablePushRegistration.executePushRegistrationTask(data);
    }

    /**
     * Updates the user subscription preferences. Passing in an empty array will clear the list, passing in null will not modify the list
     * @param emailListIds
     * @param unsubscribedChannelIds
     * @param unsubscribedMessageTypeIds
     */
    public void updateSubscriptions(@Nullable Integer[] emailListIds, @Nullable Integer[] unsubscribedChannelIds, @Nullable Integer[] unsubscribedMessageTypeIds) {
        updateSubscriptions(emailListIds, unsubscribedChannelIds, unsubscribedMessageTypeIds, null, null, null);
    }

    public void updateSubscriptions(@Nullable Integer[] emailListIds, @Nullable Integer[] unsubscribedChannelIds, @Nullable Integer[] unsubscribedMessageTypeIds, @Nullable Integer[] subscribedMessageTypeIDs, Integer campaignId, Integer templateId) {
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.updateSubscriptions(emailListIds, unsubscribedChannelIds, unsubscribedMessageTypeIds, subscribedMessageTypeIDs, campaignId, templateId);
    }

    /**
     * In-app messages are now shown automatically, and you can customize it via {@link IterableInAppHandler}
     * If you need to show messages manually, see {@link IterableInAppManager#getMessages()} and
     * {@link IterableInAppManager#showMessage(IterableInAppMessage)}
     *
     * @deprecated Please check our migration guide here:
     * https://github.com/iterable/iterable-android-sdk/#migrating-in-app-messages-from-the-previous-version-of-the-sdk
     */
    @Deprecated
    void spawnInAppNotification(final Context context, final IterableHelper.IterableActionHandler clickCallback) {
    }

    /**
     * Gets a list of InAppNotifications from Iterable; passes the result to the callback.
     * @deprecated Use {@link IterableInAppManager#getMessages()} instead
     * @param count the number of messages to fetch
     * @param onCallback
     */
    @Deprecated
    public void getInAppMessages(int count, @NonNull IterableHelper.IterableActionHandler onCallback) {
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.getInAppMessages(count, onCallback);
    }

    /**
     * Tracks an in-app open.
     * @param messageId
     */
    public void trackInAppOpen(@NonNull String messageId) {
        IterableLogger.printInfo();
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.trackInAppOpen(messageId);
    }

    void trackInAppOpen(@NonNull String messageId, @NonNull IterableInAppLocation location) {
        IterableLogger.printInfo();
        IterableInAppMessage message = getInAppManager().getMessageById(messageId);
        if (message != null) {
            trackInAppOpen(message, location);
        } else {
            IterableLogger.w(TAG, "trackInAppOpen: could not find an in-app message with ID: " + messageId);
        }
    }

    /**
     * Tracks an in-app open.
     * @param message in-app message
     */
    public void trackInAppOpen(@NonNull IterableInAppMessage message, @NonNull IterableInAppLocation location) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppOpen: message is null");
            return;
        }

        apiClient.trackInAppOpen(message, location, inboxSessionId);
    }

    void trackInAppClick(@NonNull String messageId, @NonNull String clickedUrl, @NonNull IterableInAppLocation location) {
        IterableLogger.printInfo();
        IterableInAppMessage message = getInAppManager().getMessageById(messageId);
        if (message != null) {
            trackInAppClick(message, clickedUrl, location);
        } else {
            trackInAppClick(messageId, clickedUrl);
        }
    }

    /**
     * Tracks an InApp click.
     * @param messageId
     * @param clickedUrl
     */
    public void trackInAppClick(@NonNull String messageId, @NonNull String clickedUrl) {
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.trackInAppClick(messageId, clickedUrl);
    }

    /**
     * Tracks an InApp click.
     * @param message in-app message
     * @param clickedUrl
     */
    public void trackInAppClick(@NonNull IterableInAppMessage message, @NonNull String clickedUrl, @NonNull IterableInAppLocation clickLocation) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppClick: message is null");
            return;
        }

        apiClient.trackInAppClick(message, clickedUrl, clickLocation, inboxSessionId);
    }


    void trackInAppClose(@NonNull String messageId, @NonNull String clickedURL, @NonNull IterableInAppCloseAction closeAction, @NonNull IterableInAppLocation clickLocation) {
        IterableInAppMessage message = getInAppManager().getMessageById(messageId);
        if (message != null) {
            trackInAppClose(message, clickedURL, closeAction, clickLocation);
            IterableLogger.printInfo();
        } else {
            IterableLogger.w(TAG, "trackInAppClose: could not find an in-app message with ID: " + messageId);
        }
    }

    /**
     *Tracks InApp Close events.
     * @param message in-app message
     * @param clickedURL clicked Url if available
     * @param clickLocation location of the click
     */
    void trackInAppClose(@NonNull IterableInAppMessage message, @NonNull String clickedURL, @NonNull IterableInAppCloseAction closeAction, @NonNull IterableInAppLocation clickLocation) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppClose: message is null");
            return;
        }

        apiClient.trackInAppClose(message, clickedURL, closeAction, clickLocation, inboxSessionId);
    }

    void trackInAppDelivery(@NonNull IterableInAppMessage message) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppDelivery: message is null");
            return;
        }

        apiClient.trackInAppDelivery(message);
    }

    /**
     * Consumes an InApp message.
     * @param messageId
     */
    public void inAppConsume(@NonNull String messageId) {
        IterableInAppMessage message = getInAppManager().getMessageById(messageId);
        if (message == null) {
            IterableLogger.e(TAG, "inAppConsume: message is null");
            return;
        }
        inAppConsume(message, null, null);
        IterableLogger.printInfo();
    }

    /**
     * Tracks InApp delete.
     * This method from informs Iterable about inApp messages deleted with additional paramters.
     * Call this method from places where inApp deletion are invoked by user. The messages can be swiped to delete or can be deleted using the link to delete button.
     *
     * @param message message object
     * @param source An enum describing how the in App delete was triggered
     * @param clickLocation The module in which the action happened
     */
    public void inAppConsume(@NonNull IterableInAppMessage message, @Nullable IterableInAppDeleteActionType source, @Nullable IterableInAppLocation clickLocation) {
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.inAppConsume(message, source, clickLocation, inboxSessionId);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void trackInboxSession(@NonNull IterableInboxSession session) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (session == null) {
            IterableLogger.e(TAG, "trackInboxSession: session is null");
            return;
        }

        if (session.sessionStartTime == null || session.sessionEndTime == null) {
            IterableLogger.e(TAG, "trackInboxSession: sessionStartTime and sessionEndTime must be set");
            return;
        }

        apiClient.trackInboxSession(session, inboxSessionId);
    }

//---------------------------------------------------------------------------------------
//endregion


//region Package-Protected Functions
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

    /**
     * Get the authentication token
     * @return authentication token
     */
    String getAuthToken() {
        return _authToken;
    }

//---------------------------------------------------------------------------------------
//endregion

//region Protected Functions
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

    protected void trackPushOpen(int campaignId, int templateId, @NonNull String messageId) {
        trackPushOpen(campaignId, templateId, messageId, null);
    }

    /**
     * Tracks when a push notification is opened on device.
     * @param campaignId
     * @param templateId
     */
    protected void trackPushOpen(int campaignId, int templateId, @NonNull String messageId, @Nullable JSONObject dataFields) {
        if (messageId == null) {
            IterableLogger.e(TAG, "messageId is null");
            return;
        }

        apiClient.trackPushOpen(campaignId, templateId, messageId, dataFields);
    }

    protected void disableToken(@Nullable String email, @Nullable String userId, @NonNull String token) {
        disableToken(email, userId, null, token, null, null);
    }

    /**
     * Internal api call made from IterablePushRegistration after a registrationToken is obtained.
     * It disables the device for all users with this device by default. If `email` or `userId` is provided, it will disable the device for the specific user.
     * @param email User email for whom to disable the device.
     * @param userId User ID for whom to disable the device.
     * @param authToken
     * @param deviceToken The device token
     */
    protected void disableToken(@Nullable String email, @Nullable String userId, @Nullable String authToken, @NonNull String deviceToken, @Nullable IterableHelper.SuccessHandler onSuccess, @Nullable IterableHelper.FailureHandler onFailure) {
        apiClient.disableToken(email, userId, authToken, deviceToken, onSuccess, onFailure);
    }

    /**
     * Registers the GCM registration ID with Iterable.
     *
     * @param authToken
     * @param applicationName
     * @param deviceToken
     * @param dataFields
     */
    protected void registerDeviceToken(@Nullable String email, @Nullable String userId, @Nullable String authToken, @NonNull String applicationName, @NonNull String deviceToken, @Nullable JSONObject dataFields, HashMap<String, String> deviceAttributes) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (deviceToken == null) {
            IterableLogger.e(TAG, "registerDeviceToken: token is null");
            return;
        }

        if (applicationName == null) {
            IterableLogger.e(TAG, "registerDeviceToken: applicationName is null, check that pushIntegrationName is set in IterableConfig");
        }

        apiClient.registerDeviceToken(email, userId, authToken, applicationName, deviceToken, dataFields, deviceAttributes);
    }

//---------------------------------------------------------------------------------------
//endregion

//region Private Functions
//---------------------------------------------------------------------------------------

    private final IterableActivityMonitor.AppStateCallback activityMonitorListener = new IterableActivityMonitor.AppStateCallback() {
        @Override
        public void onSwitchToForeground() {
            onForeground();
        }

        @Override
        public void onSwitchToBackground() {}
    };

    private void onForeground() {
        if (!_firstForegroundHandled) {
            _firstForegroundHandled = true;
            if (sharedInstance.config.autoPushRegistration && sharedInstance.isInitialized()) {
                IterableLogger.d(TAG, "Performing automatic push registration");
                sharedInstance.registerForPush();
            }
        }
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

    private void storeAuthData() {
        try {
            SharedPreferences.Editor editor = getPreferences().edit();
            editor.putString(IterableConstants.SHARED_PREFS_EMAIL_KEY, _email);
            editor.putString(IterableConstants.SHARED_PREFS_USERID_KEY, _userId);
            editor.putString(IterableConstants.SHARED_PREFS_AUTH_TOKEN_KEY, _authToken);
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
            _authToken = prefs.getString(IterableConstants.SHARED_PREFS_AUTH_TOKEN_KEY, null);
            if (_authToken != null) {
                getAuthManager().queueExpirationRefresh(_authToken);
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while retrieving email/userId/authToken", e);
        }
    }

    private void onLogOut() {
        if (config.autoPushRegistration && isInitialized()) {
            disablePush();
        }
        getInAppManager().reset();
        getAuthManager().clearRefreshTimer();
    }

    private void onLogIn() {
        if (!isInitialized()) {
            return;
        }

        if (config.autoPushRegistration) {
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
                    IterableConstants.ENDPOINT_DDL_MATCH, requestJSON, IterableApiRequest.POST, null, new IterableHelper.SuccessHandler() {
                @Override
                public void onSuccess(@NonNull JSONObject data) {
                    handleDDL(data);
                }
            }, new IterableHelper.FailureHandler() {
                @Override
                public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setInboxSessionId(@Nullable String inboxSessionId) {
        this.inboxSessionId = inboxSessionId;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void clearInboxSessionId() {
        this.inboxSessionId = null;
    }


    private class IterableApiAuthProvider implements IterableApiClient.AuthProvider {
        @Nullable
        @Override
        public String getEmail() {
            return _email;
        }

        @Nullable
        @Override
        public String getUserId() {
            return _userId;
        }

        @Nullable
        @Override
        public String getAuthToken() {
            return _authToken;
        }

        @Override
        public String getApiKey() {
            return _apiKey;
        }

        @Override
        public String getDeviceId() {
            return IterableApi.this.getDeviceId();
        }

        @Override
        public Context getContext() {
            return _applicationContext;
        }
    }

//---------------------------------------------------------------------------------------
//endregion

}
