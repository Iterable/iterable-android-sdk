package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.iterable.iterableapi.util.DeviceInfoUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableApi {
//region SDK (private/internal)
//---------------------------------------------------------------------------------------
    static volatile IterableApi sharedInstance = new IterableApi();

    private static final String TAG = "IterableApi";
    private Context _applicationContext;
    IterableConfig config;
    private String _apiKey;
    private String _email;
    private String _userId;
    private String _userIdAnon;
    private String _authToken;
    private boolean _debugMode;
    private Bundle _payloadData;
    private IterableNotificationData _notificationData;
    private String _deviceId;
    private boolean _firstForegroundHandled;
    private IterableHelper.SuccessHandler _setUserSuccessCallbackHandler;
    private IterableHelper.FailureHandler _setUserFailureCallbackHandler;

    IterableApiClient apiClient = new IterableApiClient(new IterableApiAuthProvider());
    private static final AnonymousUserManager anonymousUserManager = new AnonymousUserManager();
    private static final AnonymousUserMerge anonymousUserMerge = new AnonymousUserMerge();
    private IterableIdentityResolution identityResolution;
    private @Nullable IterableInAppManager inAppManager;
    private @Nullable IterableEmbeddedManager embeddedManager;
    private String inboxSessionId;
    private IterableAuthManager authManager;
    private HashMap<String, String> deviceAttributes = new HashMap<>();
    private IterableKeychain keychain;

    void fetchRemoteConfiguration() {
        apiClient.getRemoteConfiguration(new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(@Nullable String data) {
                if (data == null) {
                    IterableLogger.e(TAG, "Remote configuration returned null");
                    return;
                }
                try {
                    JSONObject jsonData = new JSONObject(data);
                    boolean offlineConfiguration = jsonData.getBoolean(IterableConstants.KEY_OFFLINE_MODE);
                    sharedInstance.apiClient.setOfflineProcessingEnabled(offlineConfiguration);
                    SharedPreferences sharedPref = sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_SAVED_CONFIGURATION, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(IterableConstants.SHARED_PREFS_OFFLINE_MODE_KEY, offlineConfiguration);
                    editor.apply();
                } catch (JSONException e) {
                    IterableLogger.e(TAG, "Failed to read remote configuration");
                }
            }
        });
    }

    public String getEmail() {
        return _email;
    }

    public String getUserId() {
        return _userId;
    }

    public String getAuthToken() {
        return _authToken;
    }

    private void checkAndUpdateAuthToken(@Nullable String authToken) {
        // If authHandler exists and if authToken is new, it will be considered as a call to update the authToken.
        if (config.authHandler != null && authToken != null && authToken != _authToken) {
            setAuthToken(authToken);
        }
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

    HashMap getDeviceAttributes() {
        return deviceAttributes;
    }

    /**
     * Returns the current context for the application.
     * @return
     */
    Context getMainActivityContext() {
        return _applicationContext;
    }

    /**
     * Returns an {@link IterableAuthManager} that can be used to manage mobile auth.
     * Make sure the Iterable API is initialized before calling this method.
     * @return {@link IterableAuthManager} instance
     */
    @NonNull
    IterableAuthManager getAuthManager() {
        if (authManager == null) {
            authManager = new IterableAuthManager(this, config.authHandler, config.retryPolicy, config.expiringAuthTokenRefreshPeriod);
        }
        return authManager;
    }

    @Nullable
    IterableKeychain getKeychain() {
        if (_applicationContext == null) {
            return null;
        }
        if (keychain == null) {
            try {
                keychain = new IterableKeychain(getMainActivityContext(), config.encryptionEnforced);
            } catch (Exception e) {
                IterableLogger.e(TAG, "Failed to create IterableKeychain", e);
            }
        }

        return keychain;
    }

    static void loadLastSavedConfiguration(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(IterableConstants.SHARED_PREFS_SAVED_CONFIGURATION, Context.MODE_PRIVATE);
        boolean offlineMode = sharedPref.getBoolean(IterableConstants.SHARED_PREFS_OFFLINE_MODE_KEY, false);
        sharedInstance.apiClient.setOfflineProcessingEnabled(offlineMode);
    }

    /**
     * Set the notification icon with the given iconName.
     * @param context
     * @param iconName
     */
    static void setNotificationIcon(Context context, String iconName) {
        SharedPreferences sharedPref = context.getSharedPreferences(IterableConstants.NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.NOTIFICATION_ICON_NAME, iconName);
        editor.apply();
    }

    /**
     * Returns the stored notification icon.
     * @param context
     * @return
     */
    static String getNotificationIcon(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(IterableConstants.NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE);
        return sharedPref.getString(IterableConstants.NOTIFICATION_ICON_NAME, "");
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

    /**
     * Gets a list of InAppNotifications from Iterable; passes the result to the callback.
     * Now package-private. If you were previously using this method, use
     * {@link IterableInAppManager#getMessages()} instead
     *
     * @param count      the number of messages to fetch
     * @param onCallback
     */
    void getInAppMessages(int count, @NonNull IterableHelper.IterableActionHandler onCallback) {
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.getInAppMessages(count, onCallback);
    }

    /**
     * Gets a list of placements for the list of placement ids passed in from Iterable and
     * passes the result to the callback;
     * To get list of messages as a list of Embedded Messages in memory, use
     * {@link IterableEmbeddedManager#getMessages(long)} instead.
     * If no placement ids are passed in, all available messages with corresponding placement id will be returned
     *
     * @param placementIds array of placement ids - optional
     * @param onCallback
     */

    public void getEmbeddedMessages(@Nullable Long[] placementIds, @NonNull IterableHelper.IterableActionHandler onCallback) {
        if (!checkSDKInitialization()) {
            return;
        }
        apiClient.getEmbeddedMessages(placementIds, onCallback);
    }

    /**
     * Gets a list of placements for the list of placement ids passed in from Iterable and
     * passes the result to the success or failure callback;
     * To get list of messages as a list of Embedded Messages in memory, use
     * {@link IterableEmbeddedManager#getMessages(long)} instead.
     * If no placement ids are passed in, all available messages with corresponding placement id will be returned
     *
     * @param placementIds array of placement ids - optional
     * @param onSuccess
     * @param onFailure
     */

    public void getEmbeddedMessages(@Nullable Long[] placementIds, @NonNull IterableHelper.SuccessHandler onSuccess, @NonNull IterableHelper.FailureHandler onFailure) {
        if (!checkSDKInitialization()) {
            return;
        }
        apiClient.getEmbeddedMessages(placementIds, onSuccess, onFailure);
    }

    /**
     * A package-private method to get a list of Embedded Messages from Iterable;
     * Passes the result to the success or failure callback.
     * Used by the IterableEmbeddedManager.
     *
     * To get list of messages as a list of EmbeddedMessages in memory, use
     * {@link IterableEmbeddedManager#getMessages(long)} instead
     *
     * @param onSuccess
     * @param onFailure
     */
    void getEmbeddedMessages(@NonNull IterableHelper.SuccessHandler onSuccess, @NonNull IterableHelper.FailureHandler onFailure) {
        if (!checkSDKInitialization()) {
            return;
        }
        apiClient.getEmbeddedMessages(null, onSuccess, onFailure);
    }

    /**
     * Tracks in-app delivery events (per in-app)
     * @param message the in-app message to be tracked as delivered */
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
     * Tracks embedded message received events (per embedded message)
     * @param message the embedded message to be tracked as received */
    void trackEmbeddedMessageReceived(@NonNull IterableEmbeddedMessage message) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackEmbeddedMessageReceived: message is null");
            return;
        }

        apiClient.trackEmbeddedMessageReceived(message);
    }

    private String getPushIntegrationName() {
        if (config.pushIntegrationName != null) {
            return config.pushIntegrationName;
        } else {
            return _applicationContext.getPackageName();
        }
    }

    private void logoutPreviousUser() {
        if (config.autoPushRegistration && isInitialized()) {
            disablePush();
        }

        getInAppManager().reset();
        getEmbeddedManager().reset();
        getAuthManager().reset();

        apiClient.onLogout();
    }

    private void onLogin(@Nullable String authToken) {
        if (!isInitialized()) {
            setAuthToken(null);
            return;
        }

        getAuthManager().pauseAuthRetries(false);
        if (authToken != null) {
            setAuthToken(authToken);
        } else {
            getAuthManager().requestNewAuthToken(false);
        }
    }

    private void completeUserLogin() {
        if (!isInitialized()) {
            return;
        }

        if (config.autoPushRegistration) {
            registerForPush();
        } else if (_setUserSuccessCallbackHandler != null) {
            _setUserSuccessCallbackHandler.onSuccess(new JSONObject()); // passing blank json object here as onSuccess is @Nonnull
        }

        getInAppManager().syncInApp();
        getEmbeddedManager().syncMessages();
    }

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
            fetchRemoteConfiguration();
        }
    }

    private boolean isInitialized() {
        return _apiKey != null && (_email != null || _userId != null);
    }

    private boolean checkSDKInitialization() {
        if (!isInitialized()) {
            IterableLogger.w(TAG, "Iterable SDK must be initialized with an API key and user email/userId before calling SDK methods");
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
        if (_applicationContext == null) {
            return;
        }
        IterableKeychain iterableKeychain = getKeychain();
        if (iterableKeychain != null) {
            iterableKeychain.saveEmail(_email);
            iterableKeychain.saveUserId(_userId);
            iterableKeychain.saveUserIdAnon(_userIdAnon);
            iterableKeychain.saveAuthToken(_authToken);
        } else {
            IterableLogger.e(TAG, "Shared preference creation failed. ");
        }
    }

    private void retrieveEmailAndUserId() {
        if (_applicationContext == null) {
            return;
        }
        IterableKeychain iterableKeychain = getKeychain();
        if (iterableKeychain != null) {
            _email = iterableKeychain.getEmail();
            _userId = iterableKeychain.getUserId();
            _userIdAnon = iterableKeychain.getUserIdAnon();
            _authToken = iterableKeychain.getAuthToken();
        } else {
            IterableLogger.e(TAG, "retrieveEmailAndUserId: Shared preference creation failed. Could not retrieve email/userId");
        }

        if (config.authHandler != null && checkSDKInitialization()) {
            if (_authToken != null) {
                getAuthManager().queueExpirationRefresh(_authToken);
            } else {
                IterableLogger.d(TAG, "Auth token found as null. Rescheduling auth token refresh");
                getAuthManager().scheduleAuthTokenRefresh(authManager.getNextRetryInterval(), true, null);
            }
        }
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
        public String getUserIdAnon() {
            return _userIdAnon;
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

        @Override
        public void resetAuth() {
            IterableLogger.d(TAG, "Resetting authToken");
            _authToken = null;
        }
    }
//endregion

//region API functions (private/internal)
//---------------------------------------------------------------------------------------
    void setAuthToken(String authToken, boolean bypassAuth) {
        if (isInitialized()) {
            if ((authToken != null && !authToken.equalsIgnoreCase(_authToken)) || (_authToken != null && !_authToken.equalsIgnoreCase(authToken))) {
                _authToken = authToken;
                storeAuthData();
                completeUserLogin();
            } else if (bypassAuth) {
                completeUserLogin();
            }
        }
    }

    protected void registerDeviceToken(final @Nullable String email, final @Nullable String userId, final @Nullable String authToken, final @NonNull String applicationName, final @NonNull String deviceToken, final HashMap<String, String> deviceAttributes) {
        if (deviceToken != null) {
            if (!checkSDKInitialization() && _userIdAnon == null) {
                if (sharedInstance.config.enableAnonTracking) {
                    anonymousUserManager.trackAnonTokenRegistration(deviceToken);
                }
                return;
            }
            final Thread registrationThread = new Thread(new Runnable() {
                public void run() {
                    registerDeviceToken(email, userId, authToken, applicationName, deviceToken, null, deviceAttributes);
                }
            });
            registrationThread.start();
        }
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
        if (deviceToken == null) {
            IterableLogger.d(TAG, "device token not available");
            return;
        }
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

        apiClient.registerDeviceToken(email, userId, authToken, applicationName, deviceToken, dataFields, deviceAttributes, _setUserSuccessCallbackHandler, _setUserFailureCallbackHandler);
    }
//endregion

//region SDK initialization
//---------------------------------------------------------------------------------------
    @NonNull
    public static IterableApi getInstance() {
        return sharedInstance;
    }

    public static void initialize(@NonNull Context context, @NonNull String apiKey) {
        initialize(context, apiKey, null);
    }

    public static void initialize(@NonNull Context context, @NonNull String apiKey, @Nullable IterableConfig config) {
        sharedInstance._applicationContext = context.getApplicationContext();
        sharedInstance._apiKey = apiKey;
        sharedInstance.config = config;

        if (sharedInstance.config == null) {
            sharedInstance.config = new IterableConfig.Builder().build();
        }

        sharedInstance.retrieveEmailAndUserId();

        IterableActivityMonitor.getInstance().registerLifecycleCallbacks(context);
        IterableActivityMonitor.getInstance().addCallback(sharedInstance.activityMonitorListener);

        if (sharedInstance.inAppManager == null) {
            sharedInstance.inAppManager = new IterableInAppManager(
                    sharedInstance,
                    sharedInstance.config.inAppHandler,
                    sharedInstance.config.inAppDisplayInterval,
                    sharedInstance.config.useInMemoryStorageForInApps);
        }

        if (sharedInstance.embeddedManager == null) {
            sharedInstance.embeddedManager = new IterableEmbeddedManager(
                    sharedInstance
            );
        }

        loadLastSavedConfiguration(context);
        IterablePushNotificationUtil.processPendingAction(context);

        if (!sharedInstance.checkSDKInitialization() && sharedInstance._userIdAnon == null && sharedInstance.config.enableAnonTracking) {
            anonymousUserManager.updateAnonSession();
            anonymousUserManager.getCriteria();
        }

        if (DeviceInfoUtils.isFireTV(context.getPackageManager())) {
            try {
                JSONObject dataFields = new JSONObject();
                JSONObject deviceDetails = new JSONObject();
                DeviceInfoUtils.populateDeviceDetails(deviceDetails, context, sharedInstance.getDeviceId());
                dataFields.put(IterableConstants.KEY_FIRETV, deviceDetails);
                sharedInstance.updateUser(dataFields, false);
            } catch (JSONException e) {
                IterableLogger.e(TAG, "initialize: exception", e);
            }
        }
    }

    public static void setContext(Context context) {
        IterableActivityMonitor.getInstance().registerLifecycleCallbacks(context);
    }

    IterableApi() {
        config = new IterableConfig.Builder().build();
    }

    @VisibleForTesting
    IterableApi(IterableInAppManager inAppManager) {
        config = new IterableConfig.Builder().build();
        this.inAppManager = inAppManager;
    }

    @VisibleForTesting
    IterableApi(IterableInAppManager inAppManager, IterableEmbeddedManager embeddedManager) {
        config = new IterableConfig.Builder().build();
        this.inAppManager = inAppManager;
        this.embeddedManager = embeddedManager;
    }

    @VisibleForTesting
    IterableApi(IterableApiClient apiClient, IterableInAppManager inAppManager) {
        config = new IterableConfig.Builder().build();
        this.apiClient = apiClient;
        this.inAppManager = inAppManager;
    }

//endregion

//region SDK public functions
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

    @NonNull
    public IterableEmbeddedManager getEmbeddedManager() {
        if (embeddedManager == null) {
            throw new RuntimeException("IterableApi must be initialized before calling getEmbeddedManager(). " +
                    "Make sure you call IterableApi#initialize() in Application#onCreate");
        }
        return embeddedManager;
    }

    /**
     * Returns the attribution information ({@link IterableAttributionInfo}) for last push open
     * or app link click from an email.
     * @return {@link IterableAttributionInfo} Object containing
     */
    @Nullable
    public IterableAttributionInfo getAttributionInfo() {
        if (_applicationContext == null) {
            return null;
        }
        return IterableAttributionInfo.fromJSONObject(
                IterableUtil.retrieveExpirableJsonObject(getPreferences(), IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_KEY)
        );
    }

    /**
     * // This method gets called from developer end only.
     * @param pauseRetry to pause/unpause auth retries
     */
    public void pauseAuthRetries(boolean pauseRetry) {
        getAuthManager().pauseAuthRetries(pauseRetry);
        if (!pauseRetry) { // request new auth token as soon as unpause
            getAuthManager().requestNewAuthToken(false);
        }
    }

    public void setEmail(@Nullable String email) {
        setEmail(email, null, null, null, null);
    }

    public void setEmail(@Nullable String email, IterableIdentityResolution identityResolution) {
        setEmail(email, null, identityResolution, null, null);
    }

    public void setEmail(@Nullable String email, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        setEmail(email, null, null, successHandler, failureHandler);
    }

    public void setEmail(@Nullable String email, IterableIdentityResolution identityResolution, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        setEmail(email, null, identityResolution, successHandler, failureHandler);
    }

    public void setEmail(@Nullable String email, @Nullable String authToken) {
        setEmail(email, authToken, null, null, null);
    }

    public void setEmail(@Nullable String email, @Nullable String authToken, IterableIdentityResolution identityResolution) {
        setEmail(email, authToken, identityResolution, null, null);
    }

    public void setEmail(@Nullable String email, @Nullable String authToken, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        setEmail(email, authToken, null, successHandler, failureHandler);
    }

    void setEmail(@Nullable String email, @Nullable String authToken, @Nullable IterableIdentityResolution iterableIdentityResolution, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        boolean replay = isReplay(iterableIdentityResolution);
        boolean merge = isMerge(iterableIdentityResolution);

        if (_email != null && _email.equals(email)) {
            checkAndUpdateAuthToken(authToken);
            return;
        }

        if (_email == null && _userId == null && email == null) {
            return;
        }

        logoutPreviousUser();

        _email = email;
        _userId = null;

        if (config.enableAnonTracking) {
            if (email != null) {
                attemptAndProcessMerge(email, true, merge, failureHandler, _userIdAnon);
            }

            if (replay && _userIdAnon == null && _email != null) {
                anonymousUserManager.syncEvents();
            }

            _userIdAnon = null;
        }

        _setUserSuccessCallbackHandler = successHandler;
        _setUserFailureCallbackHandler = failureHandler;
        storeAuthData();

        onLogin(authToken);
    }

    public void setAnonUser(@Nullable String userId) {
        _userIdAnon = userId;
        setUserId(userId, null, null, null, null, true);
        storeAuthData();
    }

    public void setUserId(@Nullable String userId) {
        setUserId(userId, null, null, null, null, false);
    }

    public void setUserId(@Nullable String userId, IterableIdentityResolution identityResolution) {
        setUserId(userId, null, identityResolution, null, null, false);
    }

    public void setUserId(@Nullable String userId, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        setUserId(userId, null, null, successHandler, failureHandler, false);
    }

    public void setUserId(@Nullable String userId, IterableIdentityResolution identityResolution, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        setUserId(userId, null, identityResolution, successHandler, failureHandler, false);
    }

    public void setUserId(@Nullable String userId, @Nullable String authToken) {
        setUserId(userId, authToken, null, null, null, false);
    }

    public void setUserId(@Nullable String userId, @Nullable String authToken, IterableIdentityResolution identityResolution) {
        setUserId(userId, authToken, identityResolution, null, null, false);

    }

    public void setUserId(@Nullable String userId, @Nullable String authToken, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
       setUserId(userId, authToken, null, successHandler, failureHandler, false);
    }

    private void setUserId(@Nullable String userId, @Nullable String authToken, @Nullable IterableIdentityResolution iterableIdentityResolution, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler, boolean isAnon) {
        boolean replay = isReplay(iterableIdentityResolution);
        boolean merge = isMerge(iterableIdentityResolution);

        if (_userId != null && _userId.equals(userId)) {
            checkAndUpdateAuthToken(authToken);
            return;
        }

        if (_email == null && _userId == null && userId == null) {
            return;
        }

        logoutPreviousUser();

        _email = null;
        _userId = userId;

        if (config.enableAnonTracking) {
            if (userId != null && !userId.equals(_userIdAnon)) {
                attemptAndProcessMerge(userId, false, merge, failureHandler, _userIdAnon);
            }

            if (replay && _userIdAnon == null && _userId != null) {
                anonymousUserManager.syncEvents();
            }

            if (!isAnon) {
                _userIdAnon = null;
            }
        }

        _setUserSuccessCallbackHandler = successHandler;
        _setUserFailureCallbackHandler = failureHandler;
        storeAuthData();

        onLogin(authToken);
    }

    private boolean isMerge(@Nullable IterableIdentityResolution iterableIdentityResolution) {
        return (iterableIdentityResolution != null) ? iterableIdentityResolution.getMergeOnAnonymousToKnown() : config.identityResolution.getMergeOnAnonymousToKnown();
    }

    private boolean isReplay(@Nullable IterableIdentityResolution iterableIdentityResolution) {
        return (iterableIdentityResolution != null) ? iterableIdentityResolution.getReplayOnVisitorToKnown() : config.identityResolution.getReplayOnVisitorToKnown();
    }

    private void attemptAndProcessMerge(@NonNull String destinationUser, boolean isEmail, boolean merge, IterableHelper.FailureHandler failureHandler, String anonymousUserId) {
        anonymousUserMerge.tryMergeUser(apiClient, anonymousUserId, destinationUser, isEmail, merge, (mergeResult, error) -> {
            if (!(Objects.equals(mergeResult, IterableConstants.MERGE_SUCCESSFUL) || Objects.equals(mergeResult, IterableConstants.MERGE_NOTREQUIRED))) {
                if (failureHandler != null) {
                    failureHandler.onFailure(error, null);
                }
            }
        });
    }

    public void setAuthToken(String authToken) {
        setAuthToken(authToken, false);
    }

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

    public void setDeviceAttribute(String key, String value) {
        deviceAttributes.put(key, value);
    }

    public void removeDeviceAttribute(String key) {
        deviceAttributes.remove(key);
    }
//endregion

//region API public functions
//---------------------------------------------------------------------------------------
    /**
     * Registers a device token with Iterable.
     * Make sure {@link IterableConfig#pushIntegrationName} is set before calling this.
     * @param deviceToken Push token obtained from GCM or FCM
     */
    public void registerDeviceToken(@NonNull String deviceToken) {
        registerDeviceToken(_email, _userId, _authToken, getPushIntegrationName(), deviceToken, deviceAttributes);
    }

    public void trackPushOpen(int campaignId, int templateId, @NonNull String messageId) {
        trackPushOpen(campaignId, templateId, messageId, null);
    }

    /**
     * Tracks when a push notification is opened on device.
     * @param campaignId
     * @param templateId
     */
    public void trackPushOpen(int campaignId, int templateId, @NonNull String messageId, @Nullable JSONObject dataFields) {
        if (messageId == null) {
            IterableLogger.e(TAG, "messageId is null");
            return;
        }

        apiClient.trackPushOpen(campaignId, templateId, messageId, dataFields);
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
        inAppConsume(message, null, null, null, null);
        IterableLogger.printInfo();
    }

    /**
     * Consumes an InApp message.
     * @param messageId
     * @param successHandler The callback which returns `success`.
     * @param failureHandler The callback which returns `failure`.
     */
    public void inAppConsume(@NonNull String messageId, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        IterableInAppMessage message = getInAppManager().getMessageById(messageId);
        if (checkIfMessageIsNull(message, failureHandler)) {
            return;
        }
        inAppConsume(message, null, null, successHandler, failureHandler);
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
        if (checkIfMessageIsNull(message, null)) {
            return;
        }
        apiClient.inAppConsume(message, source, clickLocation, inboxSessionId, null, null);
    }

    /**
     * Tracks InApp delete.
     * This method from informs Iterable about inApp messages deleted with additional paramters.
     * Call this method from places where inApp deletion are invoked by user. The messages can be swiped to delete or can be deleted using the link to delete button.
     *
     * @param message message object
     * @param source An enum describing how the in App delete was triggered
     * @param clickLocation The module in which the action happened
     * @param successHandler The callback which returns `success`.
     * @param failureHandler The callback which returns `failure`.
     */
    public void inAppConsume(@NonNull IterableInAppMessage message, @Nullable IterableInAppDeleteActionType source, @Nullable IterableInAppLocation clickLocation, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        if (!checkSDKInitialization()) {
            return;
        }
        if (checkIfMessageIsNull(message, failureHandler)) {
            return;
        }
        apiClient.inAppConsume(message, source, clickLocation, inboxSessionId, successHandler, failureHandler);
    }

    /**
     * Handles the case when the provided message is null.
     * If the message is null and a failure handler is provided, it calls the onFailure method of the failure handler.
     *
     * @param message         The in-app message to be checked.
     * @param failureHandler  The failure handler to be called if the message is null.
     * @return                True if the message is null, false otherwise.
     */
    private boolean checkIfMessageIsNull(@Nullable IterableInAppMessage message, @Nullable IterableHelper.FailureHandler failureHandler) {
        if (message == null) {
            IterableLogger.e(TAG, "inAppConsume: message is null");
            if (failureHandler != null) {
                failureHandler.onFailure("inAppConsume: message is null", null);
            }
            return true;
        }
        return false;
    }

    /**
     * Tracks a click on the uri if it is an iterable link.
     * @param uri the
     * @param onCallback Calls the callback handler with the destination location
     *                   or the original url if it is not an Iterable link.
     */
    public void getAndTrackDeepLink(@NonNull String uri, @NonNull IterableHelper.IterableActionHandler onCallback) {
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
    public boolean handleAppLink(@NonNull String uri) {
        if (_applicationContext == null) {
            return false;
        }
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
        IterableRequestTask.overrideUrl = url;
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
        if (!checkSDKInitialization() && _userIdAnon == null) {
            if (sharedInstance.config.enableAnonTracking) {
                anonymousUserManager.trackAnonEvent(eventName, dataFields);
            }
            return;
        }

        apiClient.track(eventName, campaignId, templateId, dataFields);
    }

    /**
     * Updates the status of the cart
     * @param items
     */
    public void updateCart(@NonNull List<CommerceItem> items) {
        if (!checkSDKInitialization() && _userIdAnon == null) {
            if (sharedInstance.config.enableAnonTracking) {
                anonymousUserManager.trackAnonUpdateCart(items);
            }
            return;
        }

        apiClient.updateCart(items);
    }

    /**
     * Tracks a purchase.
     * @param total total purchase amount
     * @param items list of purchased items
     */
    public void trackPurchase(double total, @NonNull List<CommerceItem> items) {
        trackPurchase(total, items, null, null);
    }

    /**
     * Tracks a purchase.
     * @param total total purchase amount
     * @param items list of purchased items
     * @param dataFields a `JSONObject` containing any additional information to save along with the event
     */
    public void trackPurchase(double total, @NonNull List<CommerceItem> items, @Nullable JSONObject dataFields) {
        trackPurchase(total, items, dataFields, null);
    }


    /**
     * Tracks a purchase.
     * @param total total purchase amount
     * @param items list of purchased items
     * @param dataFields a `JSONObject` containing any additional information to save along with the event
     * @param attributionInfo a `JSONObject` containing information about what the purchase was attributed to
     */
    public void trackPurchase(double total, @NonNull List<CommerceItem> items, @Nullable JSONObject dataFields, @Nullable IterableAttributionInfo attributionInfo) {
        if (!checkSDKInitialization() && _userIdAnon == null) {
            if (sharedInstance.config.enableAnonTracking) {
                anonymousUserManager.trackAnonPurchaseEvent(total, items, dataFields);
            }
            return;
        }

        apiClient.trackPurchase(total, items, dataFields, attributionInfo);
    }

    /**
     * Updates the current user's email.
     * Also updates the current email in this IterableAPI instance if the API call was successful.
     * @param newEmail New email
     */
    public void updateEmail(final @NonNull String newEmail) {
        updateEmail(newEmail, null, null, null);
    }

    public void updateEmail(final @NonNull String newEmail, final @NonNull String authToken) {
        updateEmail(newEmail, authToken, null, null);
    }

    public void updateEmail(final @NonNull String newEmail, final @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        updateEmail(newEmail, null, successHandler, failureHandler);
    }

    /**
     * Updates the current user's email.
     * Also updates the current email and authToken in this IterableAPI instance if the API call was successful.
     * @param newEmail New email
     * @param successHandler Success handler. Called when the server returns a success code.
     * @param failureHandler Failure handler. Called when the server call failed.
     */
    public void updateEmail(final @NonNull String newEmail, final @Nullable String authToken, final @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
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
                    _authToken = authToken;
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
        if (!checkSDKInitialization() && _userIdAnon == null) {
            if (sharedInstance.config.enableAnonTracking) {
                anonymousUserManager.trackAnonUpdateUser(dataFields);
            }
            return;
        }

        apiClient.updateUser(dataFields, mergeNestedObjects);
    }

    /**
     * Registers for push notifications.
     * Make sure the API is initialized with {@link IterableConfig#pushIntegrationName} defined, and
     * user email or user ID is set before calling this method.
     */
    public void registerForPush() {
        if (checkSDKInitialization()) {
            IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, _authToken, getPushIntegrationName(), IterablePushRegistrationData.PushRegistrationAction.ENABLE);
            IterablePushRegistration.executePushRegistrationTask(data);
        }
    }

    /**
     * Disables the device from push notifications
     */
    public void disablePush() {
        if (checkSDKInitialization()) {
            IterablePushRegistrationData data = new IterablePushRegistrationData(_email, _userId, _authToken, getPushIntegrationName(), IterablePushRegistrationData.PushRegistrationAction.DISABLE);
            IterablePushRegistration.executePushRegistrationTask(data);
        }
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

    /**
     * Tracks when a link inside an in-app is clicked
     * @param message the in-app message to be tracked
     * @param clickedUrl the URL of the clicked link
     * @param clickLocation the location of the in-app for this event
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

    /**
     * Tracks when an in-app has been closed
     * @param message the in-app message to be tracked
     * @param clickedURL the URL of the clicked link
     * @param closeAction the method of how the in-app was closed
     * @param clickLocation the location of the in-app for this event
     */
    public void trackInAppClose(@NonNull IterableInAppMessage message, @Nullable String clickedURL, @NonNull IterableInAppCloseAction closeAction, @NonNull IterableInAppLocation clickLocation) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppClose: message is null");
            return;
        }

        apiClient.trackInAppClose(message, clickedURL, closeAction, clickLocation, inboxSessionId);
    }

    /**
     * Tracks when a link inside an embedded message is clicked
     * @param message the embedded message to be tracked
     * @param buttonIdentifier identifier that determines which button or if embedded message itself was clicked
     * @param clickedUrl the URL of the clicked button or assigned to the embedded message itself
     */
    public void trackEmbeddedClick(@NonNull IterableEmbeddedMessage message, @Nullable String buttonIdentifier, @Nullable String clickedUrl) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackEmbeddedClick: message is null");
            return;
        }

        apiClient.trackEmbeddedClick(message, buttonIdentifier, clickedUrl);
    }

//endregion

//region DEPRECATED - API public functions
//---------------------------------------------------------------------------------------
    /**
     * (DEPRECATED) Tracks an in-app open
     * @param messageId
     */
    @Deprecated
    public void trackInAppOpen(@NonNull String messageId) {
        IterableLogger.printInfo();
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.trackInAppOpen(messageId);
    }

    /**
     * (DEPRECATED) Tracks an in-app open
     * @param messageId the ID of the in-app message
     * @param location where the in-app was opened
     */
    @Deprecated
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
     * (DEPRECATED) Tracks when a link inside an in-app is clicked
     * @param messageId the ID of the in-app message
     * @param clickedUrl the URL of the clicked link
     * @param location where the in-app was opened
     */
    @Deprecated
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
     * (DEPRECATED) Tracks when a link inside an in-app is clicked
     * @param messageId the ID of the in-app message
     * @param clickedUrl the URL of the clicked link
     */
    @Deprecated
    public void trackInAppClick(@NonNull String messageId, @NonNull String clickedUrl) {
        if (!checkSDKInitialization()) {
            return;
        }

        apiClient.trackInAppClick(messageId, clickedUrl);
    }

    /**
     * (DEPRECATED) Tracks when an in-app has been closed
     * @param messageId the ID of the in-app message
     * @param clickedURL the URL of the clicked link
     * @param closeAction the method of how the in-app was closed
     * @param clickLocation where the in-app was closed
     */
    @Deprecated
    void trackInAppClose(@NonNull String messageId, @NonNull String clickedURL, @NonNull IterableInAppCloseAction closeAction, @NonNull IterableInAppLocation clickLocation) {
        IterableInAppMessage message = getInAppManager().getMessageById(messageId);
        if (message != null) {
            trackInAppClose(message, clickedURL, closeAction, clickLocation);
            IterableLogger.printInfo();
        } else {
            IterableLogger.w(TAG, "trackInAppClose: could not find an in-app message with ID: " + messageId);
        }
    }
//endregion

//region library scoped
//---------------------------------------------------------------------------------------
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setInboxSessionId(@Nullable String inboxSessionId) {
        this.inboxSessionId = inboxSessionId;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void clearInboxSessionId() {
        this.inboxSessionId = null;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void trackEmbeddedSession(@NonNull IterableEmbeddedSession session) {
        if (!checkSDKInitialization()) {
            return;
        }

        if (session == null) {
            IterableLogger.e(TAG, "trackEmbeddedSession: session is null");
            return;
        }

        if (session.getStart() == null || session.getEnd() == null) {
            IterableLogger.e(TAG, "trackEmbeddedSession: sessionStartTime and sessionEndTime must be set");
            return;
        }

        apiClient.trackEmbeddedSession(session);
    }
//endregion
}
