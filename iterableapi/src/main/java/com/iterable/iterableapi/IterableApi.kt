package com.iterable.iterableapi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationManagerCompat
import com.iterable.iterableapi.util.DeviceInfoUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Created by David Truong dt@iterable.com
 */
class IterableApi {
    companion object {
        @Volatile
        @JvmStatic
        var sharedInstance = IterableApi()
            private set

        private const val TAG = "IterableApi"

        /**
         * Initialize the API
         * @param context  Application context
         * @param apiKey   Iterable Mobile API key
         */
        @JvmStatic
        fun initialize(@NonNull context: Context, @NonNull apiKey: String) {
            initialize(context, apiKey, null)
        }

        /**
         * Initialize the API
         * @param context  Application context
         * @param apiKey   Iterable Mobile API key
         * @param config   Configuration settings
         */
        @JvmStatic
        fun initialize(@NonNull context: Context, @NonNull apiKey: String, @Nullable config: IterableConfig?) {
            val localConfig = config ?: IterableConfig.Builder().build()
            
            sharedInstance.internalInitialize(context, apiKey, localConfig)
        }

        /**
         * Get the current instance
         */
        @JvmStatic
        @NonNull
        fun getInstance(): IterableApi {
            return sharedInstance
        }

        /**
         * Set the notification icon with the given iconName.
         */
        @JvmStatic
        fun setNotificationIcon(context: Context, iconName: String) {
            val sharedPref = context.getSharedPreferences(IterableConstants.NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString(IterableConstants.NOTIFICATION_ICON_NAME, iconName)
            editor.apply()
        }

        /**
         * Returns the stored notification icon.
         */
        @JvmStatic
        fun getNotificationIcon(context: Context): String {
            val sharedPref = context.getSharedPreferences(IterableConstants.NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE)
            return sharedPref.getString(IterableConstants.NOTIFICATION_ICON_NAME, "") ?: ""
        }

        /**
         * Debugging function to send API calls to different url endpoints.
         */
        @JvmStatic
        fun overrideURLEndpointPath(@NonNull url: String) {
            IterableRequestTask.overrideUrl = url
        }

        @JvmStatic
        fun setContext(context: Context) {
            sharedInstance._applicationContext = context.applicationContext
        }

        @JvmStatic
        internal fun loadLastSavedConfiguration(context: Context) {
            val sharedPref = context.getSharedPreferences(IterableConstants.SHARED_PREFS_SAVED_CONFIGURATION, Context.MODE_PRIVATE)
            val offlineMode = sharedPref.getBoolean(IterableConstants.SHARED_PREFS_OFFLINE_MODE_KEY, false)
            sharedInstance.apiClient.setOfflineProcessingEnabled(offlineMode)
        }
    }

    // Private fields
    private var _applicationContext: Context? = null
    internal lateinit var config: IterableConfig
    private var _apiKey: String? = null
    private var _email: String? = null
    private var _userId: String? = null
    private var _authToken: String? = null
    private var _debugMode = false
    private var _payloadData: Bundle? = null
    private var _notificationData: IterableNotificationData? = null
    private var _deviceId: String? = null
    private var _firstForegroundHandled = false
    private var _setUserSuccessCallbackHandler: IterableHelper.SuccessHandler? = null
    private var _setUserFailureCallbackHandler: IterableHelper.FailureHandler? = null

    internal lateinit var apiClient: IterableApiClient
    @Nullable
    internal var inAppManager: IterableInAppManager? = null
    @Nullable 
    internal var embeddedManager: IterableEmbeddedManager? = null
    internal var inboxSessionId: String? = null
    internal var authManager: IterableAuthManager? = null
    internal val deviceAttributes = HashMap<String, String>()
    private var keychain: IterableKeychain? = null

    // Constructors
    constructor()

    @VisibleForTesting
    constructor(inAppManager: IterableInAppManager) {
        this.inAppManager = inAppManager
    }

    @VisibleForTesting
    constructor(inAppManager: IterableInAppManager, embeddedManager: IterableEmbeddedManager) {
        this.inAppManager = inAppManager
        this.embeddedManager = embeddedManager
    }

    @VisibleForTesting
    internal constructor(apiClient: IterableApiClient, inAppManager: IterableInAppManager) {
        this.apiClient = apiClient
        this.inAppManager = inAppManager
    }

    private fun internalInitialize(context: Context, apiKey: String, config: IterableConfig) {
        if (!this::config.isInitialized) {
            this.config = config
        }
        
        if (!this::apiClient.isInitialized) {
            this.apiClient = IterableApiClient(IterableApiAuthProvider())
        }

        _apiKey = apiKey
        _applicationContext = context.applicationContext

        // Register activity monitor callback
        IterableActivityMonitor.getInstance().addCallback(activityMonitorListener)

        // Load stored email/userId if available
        retrieveEmailAndUserId()

        loadLastSavedConfiguration(context)
    }

    // Public API methods
    fun getEmail(): String? = _email
    fun getUserId(): String? = _userId
    fun getAuthToken(): String? = _authToken

    /**
     * Set the user email
     */
    fun setEmail(@Nullable email: String?) {
        setEmail(email, null, null, null)
    }

    fun setEmail(@Nullable email: String?, @Nullable successHandler: IterableHelper.SuccessHandler?, @Nullable failureHandler: IterableHelper.FailureHandler?) {
        setEmail(email, null, successHandler, failureHandler)
    }

    fun setEmail(@Nullable email: String?, @Nullable authToken: String?) {
        setEmail(email, authToken, null, null)
    }

    fun setEmail(@Nullable email: String?, @Nullable authToken: String?, @Nullable successHandler: IterableHelper.SuccessHandler?, @Nullable failureHandler: IterableHelper.FailureHandler?) {
        if (email != null && _email != null && _email != email) {
            logoutPreviousUser()
        }

        _setUserSuccessCallbackHandler = successHandler
        _setUserFailureCallbackHandler = failureHandler

        checkAndUpdateAuthToken(authToken)
        _email = email
        _userId = null

        storeAuthData()
        onLogin(authToken)
        completeUserLogin()
    }

    /**
     * Set the user ID
     */
    fun setUserId(@Nullable userId: String?) {
        setUserId(userId, null, null, null)
    }

    fun setUserId(@Nullable userId: String?, @Nullable successHandler: IterableHelper.SuccessHandler?, @Nullable failureHandler: IterableHelper.FailureHandler?) {
        setUserId(userId, null, successHandler, failureHandler)
    }

    fun setUserId(@Nullable userId: String?, @Nullable authToken: String?) {
        setUserId(userId, authToken, null, null)
    }

    fun setUserId(@Nullable userId: String?, @Nullable authToken: String?, @Nullable successHandler: IterableHelper.SuccessHandler?, @Nullable failureHandler: IterableHelper.FailureHandler?) {
        if (userId != null && _userId != null && _userId != userId) {
            logoutPreviousUser()
        }

        _setUserSuccessCallbackHandler = successHandler
        _setUserFailureCallbackHandler = failureHandler

        checkAndUpdateAuthToken(authToken)
        _email = null
        _userId = userId

        storeAuthData()
        onLogin(authToken)
        completeUserLogin()
    }

    /**
     * Set auth token
     */
    fun setAuthToken(authToken: String) {
        setAuthToken(authToken, false)
    }

    internal fun setAuthToken(authToken: String, bypassAuth: Boolean) {
        if (!bypassAuth && config.authHandler == null) {
            IterableLogger.w(TAG, "Auth handler is not configured, auth token will not be saved")
            return
        }

        _authToken = authToken
        storeAuthData()

        if (bypassAuth) {
            return
        }

        getAuthManager().resetFailedAuth()
        completeUserLogin()
    }

    /**
     * Set the notification icon
     */
    fun setNotificationIcon(@Nullable iconName: String?) {
        if (_applicationContext == null) {
            IterableLogger.e(TAG, "setNotificationIcon: Iterable SDK is not initialized")
            return
        }
        setNotificationIcon(_applicationContext!!, iconName ?: "")
    }

    /**
     * Get payload data for a key
     */
    @Nullable
    fun getPayloadData(@NonNull key: String): String? {
        return if (_payloadData != null) _payloadData!!.getString(key) else null
    }

    /**
     * Get all payload data
     */
    @Nullable
    fun getPayloadData(): Bundle? = _payloadData

    /**
     * Set device attribute
     */
    fun setDeviceAttribute(key: String, value: String) {
        deviceAttributes[key] = value
    }

    /**
     * Remove device attribute
     */
    fun removeDeviceAttribute(key: String) {
        deviceAttributes.remove(key)
    }

    /**
     * Register device token for push notifications
     */
    fun registerDeviceToken(@NonNull deviceToken: String) {
        registerDeviceToken(null, null, null, getPushIntegrationName(), deviceToken, null, deviceAttributes)
    }

    /**
     * Track push open
     */
    fun trackPushOpen(campaignId: Int, templateId: Int, @NonNull messageId: String) {
        trackPushOpen(campaignId, templateId, messageId, null)
    }

    fun trackPushOpen(campaignId: Int, templateId: Int, @NonNull messageId: String, @Nullable dataFields: JSONObject?) {
        IterableLogger.printInfo()
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.trackPushOpen(campaignId, templateId, messageId, dataFields)
    }

    /**
     * In-app consume methods
     */
    fun inAppConsume(@NonNull messageId: String) {
        IterableLogger.printInfo()
        val message = getInAppManager().getMessageById(messageId)
        if (message != null) {
            inAppConsume(message, null, null, null, null)
        } else {
            IterableLogger.w(TAG, "inAppConsume: could not find an in-app message with ID: $messageId")
        }
    }

    fun inAppConsume(@NonNull messageId: String, @Nullable successHandler: IterableHelper.SuccessHandler?, @Nullable failureHandler: IterableHelper.FailureHandler?) {
        IterableLogger.printInfo()
        val message = getInAppManager().getMessageById(messageId)
        if (message != null) {
            inAppConsume(message, null, null, successHandler, failureHandler)
        } else {
            IterableLogger.w(TAG, "inAppConsume: could not find an in-app message with ID: $messageId")
            failureHandler?.onFailure("inAppConsume: could not find an in-app message with ID: $messageId", null)
        }
    }

    fun inAppConsume(@NonNull message: IterableInAppMessage, @Nullable source: IterableInAppDeleteActionType?, @Nullable clickLocation: IterableInAppLocation?) {
        inAppConsume(message, source, clickLocation, null, null)
    }

    fun inAppConsume(@NonNull message: IterableInAppMessage, @Nullable source: IterableInAppDeleteActionType?, @Nullable clickLocation: IterableInAppLocation?, @Nullable successHandler: IterableHelper.SuccessHandler?, @Nullable failureHandler: IterableHelper.FailureHandler?) {
        if (!checkSDKInitialization()) {
            return
        }

        if (checkIfMessageIsNull(message, failureHandler)) {
            return
        }

        apiClient.inAppConsume(message, source, clickLocation, inboxSessionId, successHandler, failureHandler)
    }

    private fun checkIfMessageIsNull(@Nullable message: IterableInAppMessage?, @Nullable failureHandler: IterableHelper.FailureHandler?): Boolean {
        if (message == null) {
            IterableLogger.e(TAG, "inAppConsume: message is null")
            failureHandler?.onFailure("inAppConsume: message is null", null)
            return true
        }
        return false
    }

    /**
     * Deep link tracking
     */
    fun getAndTrackDeepLink(@NonNull uri: String, @NonNull onCallback: IterableHelper.IterableActionHandler) {
        IterableDeeplinkManager.getAndTrackDeeplink(uri, onCallback)
    }

    /**
     * Handle app link
     */
    fun handleAppLink(@NonNull uri: String): Boolean {
        if (_applicationContext == null) {
            return false
        }
        IterableLogger.printInfo()

        return if (IterableDeeplinkManager.isIterableDeeplink(uri)) {
            IterableDeeplinkManager.getAndTrackDeeplink(uri, object : IterableHelper.IterableActionHandler {
                override fun execute(originalUrl: String?) {
                    val action = IterableAction.actionOpenUrl(originalUrl)
                    IterableActionRunner.executeAction(getInstance().getMainActivityContext()!!, action, IterableActionSource.APP_LINK)
                }
            })
            true
        } else {
            val action = IterableAction.actionOpenUrl(uri)
            IterableActionRunner.executeAction(getInstance().getMainActivityContext()!!, action, IterableActionSource.APP_LINK)
        }
    }

    /**
     * Check if intent is from Iterable
     */
    fun isIterableIntent(@Nullable intent: Intent?): Boolean {
        if (intent != null) {
            val extras = intent.extras
            return extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY)
        }
        return false
    }

    /**
     * Track an event.
     */
    fun track(@NonNull eventName: String) {
        track(eventName, 0, 0, null)
    }

    fun track(@NonNull eventName: String, @Nullable dataFields: JSONObject?) {
        track(eventName, 0, 0, dataFields)
    }

    fun track(@NonNull eventName: String, campaignId: Int, templateId: Int) {
        track(eventName, campaignId, templateId, null)
    }

    fun track(@NonNull eventName: String, campaignId: Int, templateId: Int, @Nullable dataFields: JSONObject?) {
        IterableLogger.printInfo()
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.track(eventName, campaignId, templateId, dataFields)
    }

    /**
     * Update cart
     */
    fun updateCart(@NonNull items: List<CommerceItem>) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.updateCart(items)
    }

    /**
     * Track purchase
     */
    fun trackPurchase(total: Double, @NonNull items: List<CommerceItem>) {
        trackPurchase(total, items, null, null)
    }

    fun trackPurchase(total: Double, @NonNull items: List<CommerceItem>, @Nullable dataFields: JSONObject?) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.trackPurchase(total, items, dataFields, null)
    }

    fun trackPurchase(total: Double, @NonNull items: List<CommerceItem>, @Nullable dataFields: JSONObject?, @Nullable attributionInfo: IterableAttributionInfo?) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.trackPurchase(total, items, dataFields, attributionInfo)
    }

    /**
     * Update email
     */
    fun updateEmail(@NonNull newEmail: String) {
        updateEmail(newEmail, null, null, null)
    }

    fun updateEmail(@NonNull newEmail: String, @NonNull authToken: String) {
        updateEmail(newEmail, authToken, null, null)
    }

    fun updateEmail(@NonNull newEmail: String, @Nullable successHandler: IterableHelper.SuccessHandler?, @Nullable failureHandler: IterableHelper.FailureHandler?) {
        updateEmail(newEmail, null, successHandler, failureHandler)
    }

    fun updateEmail(@NonNull newEmail: String, @Nullable authToken: String?, @Nullable successHandler: IterableHelper.SuccessHandler?, @Nullable failureHandler: IterableHelper.FailureHandler?) {
        if (!checkSDKInitialization()) {
            IterableLogger.e(TAG, "The Iterable SDK must be initialized with email or userId before calling updateEmail")
            failureHandler?.onFailure("The Iterable SDK must be initialized with email or userId before calling updateEmail", null)
            return
        }

        apiClient.updateEmail(newEmail, object : IterableHelper.SuccessHandler {
            override fun onSuccess(@NonNull data: JSONObject) {
                if (_email != null) {
                    _email = newEmail
                    _authToken = authToken
                }

                storeAuthData()
                getAuthManager().requestNewAuthToken(false)

                successHandler?.onSuccess(data)
            }
        }, failureHandler)
    }

    /**
     * Update user
     */
    fun updateUser(@NonNull dataFields: JSONObject) {
        updateUser(dataFields, false)
    }

    fun updateUser(@NonNull dataFields: JSONObject, mergeNestedObjects: Boolean?) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.updateUser(dataFields, mergeNestedObjects)
    }

    /**
     * Register for push notifications
     */
    fun registerForPush() {
        if (checkSDKInitialization()) {
            val data = IterablePushRegistrationData(_email, _userId, _authToken, getPushIntegrationName(), IterablePushRegistrationData.PushRegistrationAction.ENABLE)
            IterablePushRegistration.executePushRegistrationTask(data)
        }
    }

    /**
     * Disable push notifications
     */
    fun disablePush() {
        if (checkSDKInitialization()) {
            val data = IterablePushRegistrationData(_email, _userId, _authToken, getPushIntegrationName(), IterablePushRegistrationData.PushRegistrationAction.DISABLE)
            IterablePushRegistration.executePushRegistrationTask(data)
        }
    }

    /**
     * Update subscriptions
     */
    fun updateSubscriptions(@Nullable emailListIds: Array<Int>?, @Nullable unsubscribedChannelIds: Array<Int>?, @Nullable unsubscribedMessageTypeIds: Array<Int>?) {
        updateSubscriptions(emailListIds, unsubscribedChannelIds, unsubscribedMessageTypeIds, null, null, null)
    }

    fun updateSubscriptions(@Nullable emailListIds: Array<Int>?, @Nullable unsubscribedChannelIds: Array<Int>?, @Nullable unsubscribedMessageTypeIds: Array<Int>?, @Nullable subscribedMessageTypeIDs: Array<Int>?, campaignId: Int?, templateId: Int?) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.updateSubscriptions(emailListIds, unsubscribedChannelIds, unsubscribedMessageTypeIds, subscribedMessageTypeIDs, campaignId, templateId)
    }

    /**
     * In-app tracking methods
     */
    fun trackInAppOpen(@NonNull message: IterableInAppMessage, @NonNull location: IterableInAppLocation) {
        if (!checkSDKInitialization()) {
            return
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppOpen: message is null")
            return
        }

        apiClient.trackInAppOpen(message, location, inboxSessionId)
    }

    fun trackInAppClick(@NonNull message: IterableInAppMessage, @NonNull clickedUrl: String, @NonNull clickLocation: IterableInAppLocation) {
        if (!checkSDKInitialization()) {
            return
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppClick: message is null")
            return
        }

        apiClient.trackInAppClick(message, clickedUrl, clickLocation, inboxSessionId)
    }

    fun trackInAppClose(@NonNull message: IterableInAppMessage, @Nullable clickedURL: String?, @NonNull closeAction: IterableInAppCloseAction, @NonNull clickLocation: IterableInAppLocation) {
        if (!checkSDKInitialization()) {
            return
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppClose: message is null")
            return
        }

        apiClient.trackInAppClose(message, clickedURL, closeAction, clickLocation, inboxSessionId)
    }

    fun trackEmbeddedClick(@NonNull message: IterableEmbeddedMessage, @Nullable buttonIdentifier: String?, @Nullable clickedUrl: String?) {
        if (!checkSDKInitialization()) {
            return
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackEmbeddedClick: message is null")
            return
        }

        apiClient.trackEmbeddedClick(message, buttonIdentifier, clickedUrl)
    }

    // DEPRECATED METHODS
    @Deprecated("Use trackInAppOpen(IterableInAppMessage, IterableInAppLocation) instead")
    fun trackInAppOpen(@NonNull messageId: String) {
        IterableLogger.printInfo()
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.trackInAppOpen(messageId)
    }

    @Deprecated("Use trackInAppOpen(IterableInAppMessage, IterableInAppLocation) instead")
    internal fun trackInAppOpen(@NonNull messageId: String, @NonNull location: IterableInAppLocation) {
        IterableLogger.printInfo()
        val message = getInAppManager().getMessageById(messageId)
        if (message != null) {
            trackInAppOpen(message, location)
        } else {
            IterableLogger.w(TAG, "trackInAppOpen: could not find an in-app message with ID: $messageId")
        }
    }

    @Deprecated("Use trackInAppClick(IterableInAppMessage, String, IterableInAppLocation) instead")
    internal fun trackInAppClick(@NonNull messageId: String, @NonNull clickedUrl: String, @NonNull location: IterableInAppLocation) {
        IterableLogger.printInfo()
        val message = getInAppManager().getMessageById(messageId)
        if (message != null) {
            trackInAppClick(message, clickedUrl, location)
        } else {
            trackInAppClick(messageId, clickedUrl)
        }
    }

    @Deprecated("Use trackInAppClick(IterableInAppMessage, String, IterableInAppLocation) instead")
    fun trackInAppClick(@NonNull messageId: String, @NonNull clickedUrl: String) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.trackInAppClick(messageId, clickedUrl)
    }

    @Deprecated("Use trackInAppClose(IterableInAppMessage, String, IterableInAppCloseAction, IterableInAppLocation) instead")
    internal fun trackInAppClose(@NonNull messageId: String, @NonNull clickedURL: String, @NonNull closeAction: IterableInAppCloseAction, @NonNull clickLocation: IterableInAppLocation) {
        val message = getInAppManager().getMessageById(messageId)
        if (message != null) {
            trackInAppClose(message, clickedURL, closeAction, clickLocation)
            IterableLogger.printInfo()
        } else {
            IterableLogger.w(TAG, "trackInAppClose: could not find an in-app message with ID: $messageId")
        }
    }

    // LIBRARY SCOPED METHODS
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun trackInboxSession(@NonNull session: IterableInboxSession) {
        if (!checkSDKInitialization()) {
            return
        }

        if (session == null) {
            IterableLogger.e(TAG, "trackInboxSession: session is null")
            return
        }

        if (session.sessionStartTime == null || session.sessionEndTime == null) {
            IterableLogger.e(TAG, "trackInboxSession: sessionStartTime and sessionEndTime must be set")
            return
        }

        apiClient.trackInboxSession(session, inboxSessionId)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun setInboxSessionId(@Nullable inboxSessionId: String?) {
        this.inboxSessionId = inboxSessionId
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun clearInboxSessionId() {
        this.inboxSessionId = null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun trackEmbeddedSession(@NonNull session: IterableEmbeddedSession) {
        if (!checkSDKInitialization()) {
            return
        }

        if (session == null) {
            IterableLogger.e(TAG, "trackEmbeddedSession: session is null")
            return
        }

        if (session.getStart() == null || session.getEnd() == null) {
            IterableLogger.e(TAG, "trackEmbeddedSession: sessionStartTime and sessionEndTime must be set")
            return
        }

        apiClient.trackEmbeddedSession(session)
    }

    // Internal methods
    internal fun getDeviceAttributes(): HashMap<String, String> = deviceAttributes

    internal fun setDebugMode(debugMode: Boolean) {
        _debugMode = debugMode
    }

    internal fun getDebugMode(): Boolean = _debugMode

    internal fun setPayloadData(intent: Intent) {
        val extras = intent.extras
        if (extras != null && extras.containsKey(IterableConstants.ITERABLE_DATA_KEY) && !IterableNotificationHelper.isGhostPush(extras)) {
            setPayloadData(extras)
        }
    }

    internal fun setPayloadData(bundle: Bundle) {
        _payloadData = bundle
    }

    internal fun setNotificationData(data: IterableNotificationData?) {
        _notificationData = data
        if (data != null) {
            setAttributionInfo(IterableAttributionInfo(data.getCampaignId(), data.getTemplateId(), data.getMessageId()))
        }
    }

    internal fun setAttributionInfo(attributionInfo: IterableAttributionInfo) {
        if (_applicationContext == null) {
            IterableLogger.e(TAG, "setAttributionInfo: Iterable SDK is not initialized with a context.")
            return
        }

        IterableUtil.saveExpirableJsonObject(
            getPreferences(),
            IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_KEY,
            attributionInfo.toJSONObject(),
            3600 * IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_EXPIRATION_HOURS * 1000
        )
    }

    @Nullable
    fun getAttributionInfo(): IterableAttributionInfo? {
        if (_applicationContext == null) {
            return null
        }

        val expirableJson = IterableUtil.retrieveExpirableJsonObject(getPreferences(), IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_KEY)
        return if (expirableJson != null) {
            IterableAttributionInfo.fromJSONObject(expirableJson)
        } else {
            null
        }
    }

    fun pauseAuthRetries(pauseRetry: Boolean) {
        if (!checkSDKInitialization()) {
            IterableLogger.e(TAG, "Iterable SDK must be initialized before calling pauseAuthRetries")
            return
        }
        getAuthManager().pauseAuthRetries(pauseRetry)
    }

    internal fun getInAppMessages(count: Int, @NonNull onCallback: IterableHelper.IterableActionHandler) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.getInAppMessages(count, onCallback)
    }

    fun getEmbeddedMessages(@Nullable placementIds: Array<Long>?, @NonNull onCallback: IterableHelper.IterableActionHandler) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.getEmbeddedMessages(placementIds, onCallback)
    }

    fun getEmbeddedMessages(@Nullable placementIds: Array<Long>?, @NonNull onSuccess: IterableHelper.SuccessHandler, @NonNull onFailure: IterableHelper.FailureHandler) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.getEmbeddedMessages(placementIds, onSuccess, onFailure)
    }

    internal fun getEmbeddedMessages(@NonNull onSuccess: IterableHelper.SuccessHandler, @NonNull onFailure: IterableHelper.FailureHandler) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.getEmbeddedMessages(null, onSuccess, onFailure)
    }

    internal fun trackInAppDelivery(@NonNull message: IterableInAppMessage) {
        if (!checkSDKInitialization()) {
            return
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackInAppDelivery: message is null")
            return
        }

        apiClient.trackInAppDelivery(message)
    }

    internal fun trackEmbeddedMessageReceived(@NonNull message: IterableEmbeddedMessage) {
        if (!checkSDKInitialization()) {
            return
        }

        if (message == null) {
            IterableLogger.e(TAG, "trackEmbeddedMessageReceived: message is null")
            return
        }

        apiClient.trackEmbeddedMessageReceived(message)
    }

    internal fun registerDeviceToken(@Nullable email: String?, @Nullable userId: String?, @Nullable authToken: String?, @NonNull applicationName: String, @NonNull deviceToken: String, deviceAttributes: HashMap<String, String>) {
        registerDeviceToken(email, userId, authToken, applicationName, deviceToken, null, deviceAttributes)
    }

    internal fun disableToken(@Nullable email: String?, @Nullable userId: String?, @NonNull token: String) {
        disableToken(email, userId, null, token, null, null)
    }

    internal fun disableToken(@Nullable email: String?, @Nullable userId: String?, @Nullable authToken: String?, @NonNull deviceToken: String, @Nullable onSuccess: IterableHelper.SuccessHandler?, @Nullable onFailure: IterableHelper.FailureHandler?) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.disableToken(email, userId, authToken, deviceToken, onSuccess, onFailure)
    }

    internal fun registerDeviceToken(@Nullable email: String?, @Nullable userId: String?, @Nullable authToken: String?, @NonNull applicationName: String, @NonNull deviceToken: String, @Nullable dataFields: JSONObject?, deviceAttributes: HashMap<String, String>) {
        if (!checkSDKInitialization()) {
            return
        }
        apiClient.registerDeviceToken(email, userId, authToken, applicationName, deviceToken, dataFields, deviceAttributes, null, null)
    }

    // Core functionality methods
    private fun checkAndUpdateAuthToken(@Nullable authToken: String?) {
        if (config.authHandler != null && authToken != null && authToken != _authToken) {
            setAuthToken(authToken)
        }
    }

    private fun logoutPreviousUser() {
        if (config.autoPushRegistration && isInitialized()) {
            disablePush()
        }

        getInAppManager().reset()
        getEmbeddedManager().reset()
        getAuthManager().reset()

        apiClient.onLogout()
    }

    private fun onLogin(@Nullable authToken: String?) {
        if (!isInitialized()) {
            setAuthToken("", true)
            return
        }

        getAuthManager().pauseAuthRetries(false)
        if (authToken != null) {
            setAuthToken(authToken)
        } else {
            getAuthManager().requestNewAuthToken(false)
        }
    }

    private fun completeUserLogin() {
        if (!isInitialized()) {
            return
        }

        if (config.autoPushRegistration) {
            registerForPush()
        } else if (_setUserSuccessCallbackHandler != null) {
            _setUserSuccessCallbackHandler!!.onSuccess(JSONObject())
        }

        getInAppManager().syncInApp()
        getEmbeddedManager().syncMessages()
    }

    private fun isInitialized(): Boolean {
        return _apiKey != null && (_email != null || _userId != null)
    }

    private fun checkSDKInitialization(): Boolean {
        if (!isInitialized()) {
            IterableLogger.w(TAG, "Iterable SDK must be initialized with an API key and user email/userId before calling SDK methods")
            return false
        }
        return true
    }

    /**
     * Returns the current context for the application.
     */
    fun getMainActivityContext(): Context? = _applicationContext
    
    /**
     * Property accessor for main activity context
     */
    val mainActivityContext: Context?
        get() = _applicationContext

    @NonNull
    fun getInAppManager(): IterableInAppManager {
        if (inAppManager == null) {
            inAppManager = IterableInAppManager(this, config.inAppHandler, config.inAppDisplayInterval, config.useInMemoryStorageForInApps)
        }
        return inAppManager!!
    }

    @NonNull
    fun getEmbeddedManager(): IterableEmbeddedManager {
        if (embeddedManager == null) {
            embeddedManager = IterableEmbeddedManager(this)
        }
        return embeddedManager!!
    }

    @NonNull
    internal fun getAuthManager(): IterableAuthManager {
        if (authManager == null) {
            authManager = IterableAuthManager(this, config.authHandler, config.retryPolicy, config.expiringAuthTokenRefreshPeriod)
        }
        return authManager!!
    }

    @Nullable
    internal fun getKeychain(): IterableKeychain? {
        if (_applicationContext == null) {
            return null
        }
        if (keychain == null) {
            try {
                keychain = IterableKeychain(getMainActivityContext()!!, config.decryptionFailureHandler, null, config.keychainEncryption)
            } catch (e: Exception) {
                IterableLogger.e(TAG, "Failed to create IterableKeychain", e)
            }
        }
        return keychain
    }

    private fun getPushIntegrationName(): String {
        return config.pushIntegrationName ?: _applicationContext!!.packageName
    }

    // Activity Monitor Callback
    private val activityMonitorListener = object : IterableActivityMonitor.AppStateCallback {
        override fun onSwitchToForeground() {
            onForeground()
        }

        override fun onSwitchToBackground() {}
    }

    private fun onForeground() {
        if (!_firstForegroundHandled) {
            _firstForegroundHandled = true
            if (config.autoPushRegistration && isInitialized()) {
                registerForPush()
            }
            fetchRemoteConfiguration()
        }

        if (_applicationContext == null || getMainActivityContext() == null) {
            IterableLogger.w(TAG, "onForeground: _applicationContext is null")
            return
        }

        val systemNotificationEnabled = NotificationManagerCompat.from(_applicationContext!!).areNotificationsEnabled()
        val sharedPref = getMainActivityContext()!!.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)

        val hasStoredPermission = sharedPref.contains(IterableConstants.SHARED_PREFS_DEVICE_NOTIFICATIONS_ENABLED)
        val isNotificationEnabled = sharedPref.getBoolean(IterableConstants.SHARED_PREFS_DEVICE_NOTIFICATIONS_ENABLED, false)

        if (isInitialized()) {
            if (config.autoPushRegistration && hasStoredPermission && (isNotificationEnabled != systemNotificationEnabled)) {
                registerForPush()
            }

            val editor = sharedPref.edit()
            editor.putBoolean(IterableConstants.SHARED_PREFS_DEVICE_NOTIFICATIONS_ENABLED, systemNotificationEnabled)
            editor.apply()
        }
    }

    // Helper methods
    private fun getPreferences(): SharedPreferences {
        return _applicationContext!!.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
    }

    private fun getDeviceId(): String {
        if (_deviceId == null) {
            _deviceId = getPreferences().getString(IterableConstants.SHARED_PREFS_DEVICEID_KEY, null)
            if (_deviceId == null) {
                _deviceId = UUID.randomUUID().toString()
                getPreferences().edit().putString(IterableConstants.SHARED_PREFS_DEVICEID_KEY, _deviceId).apply()
            }
        }
        return _deviceId!!
    }

    private fun storeAuthData() {
        if (_applicationContext == null) {
            return
        }
        val iterableKeychain = getKeychain()
        if (iterableKeychain != null) {
            iterableKeychain.saveEmail(_email)
            iterableKeychain.saveUserId(_userId)
            iterableKeychain.saveAuthToken(_authToken)
        } else {
            IterableLogger.e(TAG, "Shared preference creation failed.")
        }
    }

    private fun retrieveEmailAndUserId() {
        if (_applicationContext == null) {
            return
        }
        val iterableKeychain = getKeychain()
        if (iterableKeychain != null) {
            _email = iterableKeychain.getEmail()
            _userId = iterableKeychain.getUserId()
            _authToken = iterableKeychain.getAuthToken()
        } else {
            IterableLogger.e(TAG, "retrieveEmailAndUserId: Shared preference creation failed. Could not retrieve email/userId")
        }

        if (this::config.isInitialized && config.authHandler != null && checkSDKInitialization()) {
            val authToken = _authToken
            if (authToken != null) {
                getAuthManager().queueExpirationRefresh(authToken)
            } else {
                IterableLogger.d(TAG, "Auth token found as null. Rescheduling auth token refresh")
                getAuthManager().scheduleAuthTokenRefresh(authManager!!.getNextRetryInterval(), true, null)
            }
        }
    }

    internal fun fetchRemoteConfiguration() {
        apiClient.getRemoteConfiguration(object : IterableHelper.IterableActionHandler {
            override fun execute(@Nullable data: String?) {
                if (data == null) {
                    IterableLogger.e(TAG, "Remote configuration returned null")
                    return
                }
                try {
                    val jsonData = JSONObject(data)
                    val offlineConfiguration = jsonData.getBoolean(IterableConstants.KEY_OFFLINE_MODE)
                    sharedInstance.apiClient.setOfflineProcessingEnabled(offlineConfiguration)
                    val sharedPref = sharedInstance.getMainActivityContext()!!.getSharedPreferences(IterableConstants.SHARED_PREFS_SAVED_CONFIGURATION, Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putBoolean(IterableConstants.SHARED_PREFS_OFFLINE_MODE_KEY, offlineConfiguration)
                    editor.apply()
                } catch (e: JSONException) {
                    IterableLogger.e(TAG, "Failed to read remote configuration")
                }
            }
        })
    }

    // Inner class for Auth Provider
    private inner class IterableApiAuthProvider : IterableApiClient.AuthProvider {
        @Nullable
        override fun getEmail(): String? = _email

        @Nullable
        override fun getUserId(): String? = _userId

        @Nullable
        override fun getAuthToken(): String? = _authToken

        override fun getApiKey(): String? = _apiKey

        override fun getDeviceId(): String? = getDeviceId()

        override fun getContext(): Context? = _applicationContext

        override fun resetAuth() {
            _email = null
            _userId = null
            _authToken = null
            storeAuthData()
        }
    }
}