package com.iterable.iterableapi

import android.content.Context
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.app.NotificationManagerCompat
import com.iterable.iterableapi.util.DeviceInfoUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

internal class IterableApiClient(@NonNull private val authProvider: AuthProvider) {

    companion object {
        private const val TAG = "IterableApiClient"

        @NonNull
        @JvmStatic
        private fun getEmbeddedMessagesPath(placementIds: Array<Long>): String {
            val pathBuilder = StringBuilder(IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES + "?")

            var isFirst = true
            for (placementId in placementIds) {
                if (isFirst) {
                    pathBuilder.append("placementIds=").append(placementId)
                    isFirst = false
                } else {
                    pathBuilder.append("&placementIds=").append(placementId)
                }
            }

            return pathBuilder.toString()
        }
    }

    internal interface AuthProvider {
        @Nullable
        fun getEmail(): String?

        @Nullable
        fun getUserId(): String?

        @Nullable
        fun getAuthToken(): String?

        @Nullable
        fun getApiKey(): String?

        @Nullable
        fun getDeviceId(): String?

        @Nullable
        fun getContext(): Context?

        fun resetAuth()
    }

    private var requestProcessor: RequestProcessor? = null

    private fun getRequestProcessor(): RequestProcessor {
        if (requestProcessor == null) {
            requestProcessor = OnlineRequestProcessor()
        }
        return requestProcessor!!
    }

    fun setOfflineProcessingEnabled(offlineMode: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (offlineMode) {
                if (this.requestProcessor == null || this.requestProcessor!!.javaClass != OfflineRequestProcessor::class.java) {
                    this.requestProcessor = OfflineRequestProcessor(authProvider.getContext()!!)
                }
            } else {
                if (this.requestProcessor == null || this.requestProcessor!!.javaClass != OnlineRequestProcessor::class.java) {
                    this.requestProcessor = OnlineRequestProcessor()
                }
            }
        }
    }

    fun getRemoteConfiguration(actionHandler: IterableHelper.IterableActionHandler) {
        val requestJSON = JSONObject()
        try {
            requestJSON.putOpt(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID)
            requestJSON.putOpt(IterableConstants.DEVICE_APP_PACKAGE_NAME, authProvider.getContext()!!.packageName)
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER)
            requestJSON.put(IterableConstants.ITBL_SYSTEM_VERSION, Build.VERSION.RELEASE)
            sendGetRequest(IterableConstants.ENDPOINT_GET_REMOTE_CONFIGURATION, requestJSON, actionHandler)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun track(@NonNull eventName: String, campaignId: Int, templateId: Int, @Nullable dataFields: JSONObject?) {
        val requestJSON = JSONObject()
        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_EVENT_NAME, eventName)

            if (campaignId != 0) {
                requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId)
            }
            if (templateId != 0) {
                requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId)
            }
            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields)

            sendPostRequest(IterableConstants.ENDPOINT_TRACK, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun updateCart(@NonNull items: List<CommerceItem>) {
        val requestJSON = JSONObject()

        try {
            val itemsArray = JSONArray()
            for (item in items) {
                itemsArray.put(item.toJSONObject())
            }

            val userObject = JSONObject()
            addEmailOrUserIdToJson(userObject)
            requestJSON.put(IterableConstants.KEY_USER, userObject)

            requestJSON.put(IterableConstants.KEY_ITEMS, itemsArray)

            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_CART, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackPurchase(
        total: Double,
        @NonNull items: List<CommerceItem>,
        @Nullable dataFields: JSONObject?,
        @Nullable attributionInfo: IterableAttributionInfo?
    ) {
        val requestJSON = JSONObject()
        try {
            val itemsArray = JSONArray()
            for (item in items) {
                itemsArray.put(item.toJSONObject())
            }

            val userObject = JSONObject()
            addEmailOrUserIdToJson(userObject)
            requestJSON.put(IterableConstants.KEY_USER, userObject)

            requestJSON.put(IterableConstants.KEY_ITEMS, itemsArray)
            requestJSON.put(IterableConstants.KEY_TOTAL, total)
            if (dataFields != null) {
                requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields)
            }

            if (attributionInfo != null) {
                requestJSON.putOpt(IterableConstants.KEY_CAMPAIGN_ID, attributionInfo.campaignId)
                requestJSON.putOpt(IterableConstants.KEY_TEMPLATE_ID, attributionInfo.templateId)
            }

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_PURCHASE, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun updateEmail(
        @NonNull newEmail: String,
        @Nullable successHandler: IterableHelper.SuccessHandler?,
        @Nullable failureHandler: IterableHelper.FailureHandler?
    ) {
        val requestJSON = JSONObject()

        try {
            if (authProvider.getEmail() != null) {
                requestJSON.put(IterableConstants.KEY_CURRENT_EMAIL, authProvider.getEmail())
            } else {
                requestJSON.put(IterableConstants.KEY_CURRENT_USERID, authProvider.getUserId())
            }
            requestJSON.put(IterableConstants.KEY_NEW_EMAIL, newEmail)

            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_EMAIL, requestJSON, successHandler, failureHandler)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun updateUser(@NonNull dataFields: JSONObject, mergeNestedObjects: Boolean?) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)

            // Create the user by userId if it doesn't exist
            if (authProvider.getEmail() == null && authProvider.getUserId() != null) {
                requestJSON.put(IterableConstants.KEY_PREFER_USER_ID, true)
            }

            requestJSON.put(IterableConstants.KEY_DATA_FIELDS, dataFields)
            requestJSON.put(IterableConstants.KEY_MERGE_NESTED_OBJECTS, mergeNestedObjects)

            sendPostRequest(IterableConstants.ENDPOINT_UPDATE_USER, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun updateSubscriptions(
        @Nullable emailListIds: Array<Int>?,
        @Nullable unsubscribedChannelIds: Array<Int>?,
        @Nullable unsubscribedMessageTypeIds: Array<Int>?,
        @Nullable subscribedMessageTypeIDs: Array<Int>?,
        campaignId: Int?,
        templateId: Int?
    ) {
        val requestJSON = JSONObject()
        addEmailOrUserIdToJson(requestJSON)

        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_EMAIL_LIST_IDS, emailListIds)
        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_UNSUB_CHANNEL, unsubscribedChannelIds)
        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_UNSUB_MESSAGE, unsubscribedMessageTypeIds)
        tryAddArrayToJSON(requestJSON, IterableConstants.KEY_SUB_MESSAGE, subscribedMessageTypeIDs)
        try {
            if (campaignId != null && campaignId != 0) {
                requestJSON.putOpt(IterableConstants.KEY_CAMPAIGN_ID, campaignId)
            }
            if (templateId != null && templateId != 0) {
                requestJSON.putOpt(IterableConstants.KEY_TEMPLATE_ID, templateId)
            }
        } catch (e: JSONException) {
            IterableLogger.e(TAG, e.toString())
        }
        sendPostRequest(IterableConstants.ENDPOINT_UPDATE_USER_SUBS, requestJSON)
    }

    fun getInAppMessages(count: Int, @NonNull onCallback: IterableHelper.IterableActionHandler) {
        val requestJSON = JSONObject()
        addEmailOrUserIdToJson(requestJSON)
        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_COUNT, count)
            requestJSON.put(
                IterableConstants.KEY_PLATFORM,
                if (DeviceInfoUtils.isFireTV(authProvider.getContext()!!.packageManager)) 
                    IterableConstants.ITBL_PLATFORM_OTT 
                else 
                    IterableConstants.ITBL_PLATFORM_ANDROID
            )
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER)
            requestJSON.put(IterableConstants.ITBL_SYSTEM_VERSION, Build.VERSION.RELEASE)
            requestJSON.put(IterableConstants.KEY_PACKAGE_NAME, authProvider.getContext()!!.packageName)

            sendGetRequest(IterableConstants.ENDPOINT_GET_INAPP_MESSAGES, requestJSON, onCallback)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun getEmbeddedMessages(@Nullable placementIds: Array<Long>?, @NonNull onCallback: IterableHelper.IterableActionHandler) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID)
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER)
            requestJSON.put(IterableConstants.ITBL_SYSTEM_VERSION, Build.VERSION.RELEASE)
            requestJSON.put(IterableConstants.KEY_PACKAGE_NAME, authProvider.getContext()!!.packageName)

            if (placementIds != null && placementIds.isNotEmpty()) {
                val path = getEmbeddedMessagesPath(placementIds)
                sendGetRequest(path, requestJSON, onCallback)
            } else {
                sendGetRequest(IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES, requestJSON, onCallback)
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun getEmbeddedMessages(
        @Nullable placementIds: Array<Long>?,
        @NonNull onSuccess: IterableHelper.SuccessHandler,
        @NonNull onFailure: IterableHelper.FailureHandler
    ) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID)
            requestJSON.put(IterableConstants.ITBL_KEY_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER)
            requestJSON.put(IterableConstants.ITBL_SYSTEM_VERSION, Build.VERSION.RELEASE)
            requestJSON.put(IterableConstants.KEY_PACKAGE_NAME, authProvider.getContext()!!.packageName)

            if (placementIds != null && placementIds.isNotEmpty()) {
                val path = getEmbeddedMessagesPath(placementIds)
                sendGetRequest(path, requestJSON, onSuccess, onFailure)
            } else {
                sendGetRequest(IterableConstants.ENDPOINT_GET_EMBEDDED_MESSAGES, requestJSON, onSuccess, onFailure)
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackInAppOpen(@NonNull messageId: String) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId)

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackInAppOpen(
        @NonNull message: IterableInAppMessage,
        @NonNull location: IterableInAppLocation,
        @Nullable inboxSessionId: String?
    ) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId())
            requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, location))
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())
            if (location == IterableInAppLocation.INBOX) {
                addInboxSessionID(requestJSON, inboxSessionId)
            }
            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackInAppClick(@NonNull messageId: String, @NonNull clickedUrl: String) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId)
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_CLICKED_URL, clickedUrl)

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackInAppClick(
        @NonNull message: IterableInAppMessage,
        @NonNull clickedUrl: String,
        @NonNull clickLocation: IterableInAppLocation,
        @Nullable inboxSessionId: String?
    ) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId())
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_CLICKED_URL, clickedUrl)
            requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, clickLocation))
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())
            if (clickLocation == IterableInAppLocation.INBOX) {
                addInboxSessionID(requestJSON, inboxSessionId)
            }
            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackEmbeddedClick(
        @NonNull message: IterableEmbeddedMessage,
        @Nullable buttonIdentifier: String?,
        @Nullable clickedUrl: String?
    ) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMetadata().getMessageId())
            requestJSON.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_IDENTIFIER, buttonIdentifier)
            requestJSON.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_TARGET_URL, clickedUrl)
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_EMBEDDED_CLICK, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackInAppClose(
        @NonNull message: IterableInAppMessage,
        @Nullable clickedURL: String?,
        @NonNull closeAction: IterableInAppCloseAction,
        @NonNull clickLocation: IterableInAppLocation,
        @Nullable inboxSessionId: String?
    ) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId())
            requestJSON.putOpt(IterableConstants.ITERABLE_IN_APP_CLICKED_URL, clickedURL)
            requestJSON.put(IterableConstants.ITERABLE_IN_APP_CLOSE_ACTION, closeAction.toString())
            requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, clickLocation))
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())

            if (clickLocation == IterableInAppLocation.INBOX) {
                addInboxSessionID(requestJSON, inboxSessionId)
            }

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackInAppDelivery(@NonNull message: IterableInAppMessage) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId())
            requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, null))
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackEmbeddedMessageReceived(@NonNull message: IterableEmbeddedMessage) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMetadata().getMessageId())
            requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())
            sendPostRequest(IterableConstants.ENDPOINT_TRACK_EMBEDDED_RECEIVED, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun inAppConsume(
        @NonNull message: IterableInAppMessage,
        @Nullable source: IterableInAppDeleteActionType?,
        @Nullable clickLocation: IterableInAppLocation?,
        @Nullable inboxSessionId: String?,
        @Nullable successHandler: IterableHelper.SuccessHandler?,
        @Nullable failureHandler: IterableHelper.FailureHandler?
    ) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, message.getMessageId())
            if (source != null) {
                requestJSON.put(IterableConstants.ITERABLE_IN_APP_DELETE_ACTION, source.toString())
            }

            if (clickLocation != null) {
                requestJSON.put(IterableConstants.KEY_MESSAGE_CONTEXT, getInAppMessageContext(message, clickLocation))
                requestJSON.put(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())
            }

            if (clickLocation == IterableInAppLocation.INBOX) {
                addInboxSessionID(requestJSON, inboxSessionId)
            }

            sendPostRequest(IterableConstants.ENDPOINT_INAPP_CONSUME, requestJSON, successHandler, failureHandler)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackInboxSession(@NonNull session: IterableInboxSession, @Nullable inboxSessionId: String?) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)

            requestJSON.put(IterableConstants.ITERABLE_INBOX_SESSION_START, session.sessionStartTime?.time ?: 0)
            requestJSON.put(IterableConstants.ITERABLE_INBOX_SESSION_END, session.sessionEndTime?.time ?: 0)
            requestJSON.put(IterableConstants.ITERABLE_INBOX_START_TOTAL_MESSAGE_COUNT, session.startTotalMessageCount)
            requestJSON.put(IterableConstants.ITERABLE_INBOX_START_UNREAD_MESSAGE_COUNT, session.startUnreadMessageCount)
            requestJSON.put(IterableConstants.ITERABLE_INBOX_END_TOTAL_MESSAGE_COUNT, session.endTotalMessageCount)
            requestJSON.put(IterableConstants.ITERABLE_INBOX_END_UNREAD_MESSAGE_COUNT, session.endUnreadMessageCount)

            if (session.impressions != null) {
                val impressionsJsonArray = JSONArray()
                for (impression in session.impressions!!) {
                    val impressionJson = JSONObject()
                    impressionJson.put(IterableConstants.KEY_MESSAGE_ID, impression.messageId)
                    impressionJson.put(IterableConstants.ITERABLE_IN_APP_SILENT_INBOX, impression.silentInbox)
                    impressionJson.put(IterableConstants.ITERABLE_INBOX_IMP_DISPLAY_COUNT, impression.displayCount)
                    impressionJson.put(IterableConstants.ITERABLE_INBOX_IMP_DISPLAY_DURATION, impression.duration)
                    impressionsJsonArray.put(impressionJson)
                }
                requestJSON.put(IterableConstants.ITERABLE_INBOX_IMPRESSIONS, impressionsJsonArray)
            }

            requestJSON.putOpt(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())
            addInboxSessionID(requestJSON, inboxSessionId)

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_INBOX_SESSION, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun trackEmbeddedSession(@NonNull session: IterableEmbeddedSession) {
        val requestJSON = JSONObject()

        try {
            addEmailOrUserIdToJson(requestJSON)

            val sessionJson = JSONObject()
            if (session.getId() != null) {
                sessionJson.put(IterableConstants.KEY_EMBEDDED_SESSION_ID, session.getId())
            }
            sessionJson.put(IterableConstants.ITERABLE_EMBEDDED_SESSION_START, session.getStart()?.time ?: 0)
            sessionJson.put(IterableConstants.ITERABLE_EMBEDDED_SESSION_END, session.getEnd()?.time ?: 0)

            requestJSON.put(IterableConstants.ITERABLE_EMBEDDED_SESSION, sessionJson)

            if (session.getImpressions() != null) {
                val impressionsJsonArray = JSONArray()
                for (impression in session.getImpressions()!!) {
                    val impressionJson = JSONObject()
                    impressionJson.put(IterableConstants.KEY_MESSAGE_ID, impression.getMessageId())
                    impressionJson.put(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID, impression.getPlacementId())
                    impressionJson.put(IterableConstants.ITERABLE_EMBEDDED_IMP_DISPLAY_COUNT, impression.getDisplayCount())
                    impressionJson.put(IterableConstants.ITERABLE_EMBEDDED_IMP_DISPLAY_DURATION, impression.getDuration())
                    impressionsJsonArray.put(impressionJson)
                }
                requestJSON.put(IterableConstants.ITERABLE_EMBEDDED_IMPRESSIONS, impressionsJsonArray)
            }

            requestJSON.putOpt(IterableConstants.KEY_DEVICE_INFO, getDeviceInfoJson())

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_EMBEDDED_SESSION, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    internal fun trackPushOpen(campaignId: Int, templateId: Int, @NonNull messageId: String, @Nullable dataFields: JSONObject?) {
        val requestJSON = JSONObject()

        try {
            var dataFields = dataFields
            if (dataFields == null) {
                dataFields = JSONObject()
            }

            addEmailOrUserIdToJson(requestJSON)
            requestJSON.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId)
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId)
            requestJSON.put(IterableConstants.KEY_MESSAGE_ID, messageId)
            requestJSON.putOpt(IterableConstants.KEY_DATA_FIELDS, dataFields)

            sendPostRequest(IterableConstants.ENDPOINT_TRACK_PUSH_OPEN, requestJSON)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    internal fun disableToken(
        @Nullable email: String?,
        @Nullable userId: String?,
        @Nullable authToken: String?,
        @NonNull deviceToken: String,
        @Nullable onSuccess: IterableHelper.SuccessHandler?,
        @Nullable onFailure: IterableHelper.FailureHandler?
    ) {
        val requestJSON = JSONObject()
        try {
            requestJSON.put(IterableConstants.KEY_TOKEN, deviceToken)
            if (email != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, email)
            } else if (userId != null) {
                requestJSON.put(IterableConstants.KEY_USER_ID, userId)
            }

            sendPostRequest(IterableConstants.ENDPOINT_DISABLE_DEVICE, requestJSON, authToken, onSuccess, onFailure)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    internal fun registerDeviceToken(
        @Nullable email: String?,
        @Nullable userId: String?,
        @Nullable authToken: String?,
        @NonNull applicationName: String,
        @NonNull deviceToken: String,
        @Nullable dataFields: JSONObject?,
        deviceAttributes: HashMap<String, String>,
        @Nullable successHandler: IterableHelper.SuccessHandler?,
        @Nullable failureHandler: IterableHelper.FailureHandler?
    ) {
        val context = authProvider.getContext()
        val requestJSON = JSONObject()
        try {
            addEmailOrUserIdToJson(requestJSON)

            var dataFields = dataFields
            if (dataFields == null) {
                dataFields = JSONObject()
            }

            for ((key, value) in deviceAttributes) {
                dataFields.put(key, value)
            }

            dataFields.put(IterableConstants.FIREBASE_TOKEN_TYPE, IterableConstants.MESSAGING_PLATFORM_FIREBASE)
            dataFields.put(IterableConstants.FIREBASE_COMPATIBLE, true)

            var frameworkInfo = IterableApi.sharedInstance.config.mobileFrameworkInfo
            if (frameworkInfo == null) {
                val detectedFramework = IterableMobileFrameworkDetector.detectFramework(context!!)
                val sdkVersion = if (detectedFramework == IterableAPIMobileFrameworkType.NATIVE) 
                    IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER 
                else 
                    null

                frameworkInfo = IterableAPIMobileFrameworkInfo(
                    detectedFramework,
                    sdkVersion
                )
            }

            DeviceInfoUtils.populateDeviceDetails(dataFields, context!!, authProvider.getDeviceId()!!, frameworkInfo)
            dataFields.put(IterableConstants.DEVICE_NOTIFICATIONS_ENABLED, NotificationManagerCompat.from(context!!).areNotificationsEnabled())

            val device = JSONObject()
            device.put(IterableConstants.KEY_TOKEN, deviceToken)
            device.put(IterableConstants.KEY_PLATFORM, IterableConstants.MESSAGING_PLATFORM_GOOGLE)
            device.put(IterableConstants.KEY_APPLICATION_NAME, applicationName)
            device.putOpt(IterableConstants.KEY_DATA_FIELDS, dataFields)
            requestJSON.put(IterableConstants.KEY_DEVICE, device)

            // Create the user by userId if it doesn't exist
            if (email == null && userId != null) {
                requestJSON.put(IterableConstants.KEY_PREFER_USER_ID, true)
            }

            sendPostRequest(IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, requestJSON, authToken, successHandler, failureHandler)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "registerDeviceToken: exception", e)
        }
    }

    /**
     * Adds the current email or userID to the json request.
     * @param requestJSON
     */
    private fun addEmailOrUserIdToJson(requestJSON: JSONObject) {
        try {
            if (authProvider.getEmail() != null) {
                requestJSON.put(IterableConstants.KEY_EMAIL, authProvider.getEmail())
            } else {
                requestJSON.put(IterableConstants.KEY_USER_ID, authProvider.getUserId())
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @Throws(JSONException::class)
    private fun addInboxSessionID(@NonNull requestJSON: JSONObject, @Nullable inboxSessionId: String?) {
        if (inboxSessionId != null) {
            requestJSON.put(IterableConstants.KEY_INBOX_SESSION_ID, inboxSessionId)
        }
    }

    private fun getInAppMessageContext(@NonNull message: IterableInAppMessage, @Nullable location: IterableInAppLocation?): JSONObject {
        val messageContext = JSONObject()
        try {
            val isSilentInbox = message.isSilentInboxMessage()

            messageContext.putOpt(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX, message.isInboxMessage())
            messageContext.putOpt(IterableConstants.ITERABLE_IN_APP_SILENT_INBOX, isSilentInbox)
            if (location != null) {
                messageContext.putOpt(IterableConstants.ITERABLE_IN_APP_LOCATION, location.toString())
            }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Could not populate messageContext JSON", e)
        }
        return messageContext
    }

    @NonNull
    private fun getDeviceInfoJson(): JSONObject {
        val deviceInfo = JSONObject()
        try {
            deviceInfo.putOpt(IterableConstants.DEVICE_ID, authProvider.getDeviceId())
            deviceInfo.putOpt(IterableConstants.KEY_PLATFORM, IterableConstants.ITBL_PLATFORM_ANDROID)
            deviceInfo.putOpt(IterableConstants.DEVICE_APP_PACKAGE_NAME, authProvider.getContext()!!.packageName)
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Could not populate deviceInfo JSON", e)
        }
        return deviceInfo
    }

    /**
     * Attempts to add an array as a JSONArray to a JSONObject
     * @param requestJSON
     * @param key
     * @param value
     */
    internal fun tryAddArrayToJSON(requestJSON: JSONObject, key: String, value: Array<*>?) {
        if (requestJSON != null && key != null && value != null)
            try {
                val mJSONArray = JSONArray(listOf(*value))
                requestJSON.put(key, mJSONArray)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, e.toString())
            }
    }

    /**
     * Sends the POST request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    internal fun sendPostRequest(@NonNull resourcePath: String, @NonNull json: JSONObject) {
        sendPostRequest(resourcePath, json, authProvider.getAuthToken())
    }

    internal fun sendPostRequest(@NonNull resourcePath: String, @NonNull json: JSONObject, @Nullable authToken: String?) {
        sendPostRequest(resourcePath, json, authToken, null, null)
    }

    internal fun sendPostRequest(
        @NonNull resourcePath: String,
        @NonNull json: JSONObject,
        @Nullable onSuccess: IterableHelper.SuccessHandler?,
        @Nullable onFailure: IterableHelper.FailureHandler?
    ) {
        sendPostRequest(resourcePath, json, authProvider.getAuthToken(), onSuccess, onFailure)
    }

    internal fun sendPostRequest(
        @NonNull resourcePath: String,
        @NonNull json: JSONObject,
        @Nullable authToken: String?,
        @Nullable onSuccess: IterableHelper.SuccessHandler?,
        @Nullable onFailure: IterableHelper.FailureHandler?
    ) {
        getRequestProcessor().processPostRequest(authProvider.getApiKey(), resourcePath, json, authToken, onSuccess, onFailure)
    }

    /**
     * Sends a GET request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    internal fun sendGetRequest(@NonNull resourcePath: String, @NonNull json: JSONObject, @Nullable onCallback: IterableHelper.IterableActionHandler?) {
        getRequestProcessor().processGetRequest(authProvider.getApiKey(), resourcePath, json, authProvider.getAuthToken(), onCallback)
    }

    internal fun sendGetRequest(
        @NonNull resourcePath: String,
        @NonNull json: JSONObject,
        @NonNull onSuccess: IterableHelper.SuccessHandler,
        @NonNull onFailure: IterableHelper.FailureHandler
    ) {
        getRequestProcessor().processGetRequest(authProvider.getApiKey(), resourcePath, json, authProvider.getAuthToken(), onSuccess, onFailure)
    }

    fun onLogout() {
        getRequestProcessor().onLogout(authProvider.getContext())
        authProvider.resetAuth()
    }
}