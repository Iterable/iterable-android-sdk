package com.iterable.iterableapi

import android.graphics.Rect
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.core.util.ObjectsCompat
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class IterableInAppMessage internal constructor(
    @NonNull internal val messageId: String,
    @NonNull internal val content: Content,
    @NonNull private val customPayload: JSONObject,
    @NonNull private val createdAt: Date,
    @NonNull private val expiresAt: Date,
    @NonNull private val trigger: Trigger,
    @NonNull private val priorityLevel: Double,
    @Nullable private val saveToInbox: Boolean?,
    @Nullable private val inboxMetadata: InboxMetadata?,
    @Nullable private val campaignId: Long?,
    internal val jsonOnly: Boolean
) {

    companion object {
        private const val TAG = "IterableInAppMessage"

        @JvmStatic
        internal fun fromJSONObject(@NonNull messageJson: JSONObject?, @Nullable storageInterface: IterableInAppStorage?): IterableInAppMessage? {
            if (messageJson == null) {
                return null
            }

            val messageId = messageJson.optString(IterableConstants.KEY_MESSAGE_ID)
            val campaignId = IterableUtil.retrieveValidCampaignIdOrNull(messageJson, IterableConstants.KEY_CAMPAIGN_ID)
            val jsonOnly = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_JSON_ONLY, false)

            var customPayload = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CUSTOM_PAYLOAD)
            if (customPayload == null && jsonOnly) {
                customPayload = JSONObject()
            }

            val content: Content
            if (jsonOnly) {
                content = Content("", Rect(), 0.0, false, InAppDisplaySettings(false, InAppBgColor(null, 0.0)))
            } else {
                val contentJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT)
                    ?: return null

                if (customPayload == null) {
                    customPayload = contentJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_LEGACY_PAYLOAD)
                }

                val html = contentJson.optString(IterableConstants.ITERABLE_IN_APP_HTML, null)
                val inAppDisplaySettingsJson = contentJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_DISPLAY_SETTINGS)
                val padding = getPaddingFromPayload(inAppDisplaySettingsJson)
                val backgroundAlpha = contentJson.optDouble(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ALPHA, 0.0)
                val shouldAnimate = inAppDisplaySettingsJson.optBoolean(IterableConstants.ITERABLE_IN_APP_SHOULD_ANIMATE, false)
                val bgColorJson = inAppDisplaySettingsJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_BGCOLOR)

                var bgColorInHex: String? = null
                var bgAlpha = 0.0
                if (bgColorJson != null) {
                    bgColorInHex = bgColorJson.optString(IterableConstants.ITERABLE_IN_APP_BGCOLOR_HEX)
                    bgAlpha = bgColorJson.optDouble(IterableConstants.ITERABLE_IN_APP_BGCOLOR_ALPHA)
                }

                val inAppDisplaySettings = InAppDisplaySettings(shouldAnimate, InAppBgColor(bgColorInHex, bgAlpha))
                content = Content(html, padding, backgroundAlpha, shouldAnimate, inAppDisplaySettings)
            }

            val createdAtLong = messageJson.optLong(IterableConstants.ITERABLE_IN_APP_CREATED_AT)
            val createdAt = if (createdAtLong != 0L) Date(createdAtLong) else null
            val expiresAtLong = messageJson.optLong(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT)
            val expiresAt = if (expiresAtLong != 0L) Date(expiresAtLong) else null

            val triggerJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_TRIGGER)
            val trigger = Trigger.fromJSONObject(triggerJson)

            val priorityLevel = messageJson.optDouble(
                IterableConstants.ITERABLE_IN_APP_PRIORITY_LEVEL,
                IterableConstants.ITERABLE_IN_APP_PRIORITY_LEVEL_UNASSIGNED
            )

            val saveToInbox: Boolean? = if (messageJson.has(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX)) {
                messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX)
            } else {
                null
            }

            val inboxPayloadJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_INBOX_METADATA)
            val inboxMetadata = InboxMetadata.fromJSONObject(inboxPayloadJson)

            val message = IterableInAppMessage(
                messageId,
                content,
                customPayload!!,
                createdAt!!,
                expiresAt!!,
                trigger,
                priorityLevel,
                saveToInbox,
                inboxMetadata,
                campaignId,
                jsonOnly
            )

            message.inAppStorageInterface = storageInterface
            if (!jsonOnly && content.html != null && content.html!!.isNotEmpty()) {
                message.setLoadedHtmlFromJson(true)
            }
            message.processed = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_PROCESSED, false)
            message.consumed = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_CONSUMED, false)
            message.read = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_READ, false)
            return message
        }

        /**
         * Returns a Rect containing the paddingOptions
         * @param paddingOptions
         * @return
         */
        @JvmStatic
        internal fun getPaddingFromPayload(paddingOptions: JSONObject?): Rect {
            if (paddingOptions == null) {
                return Rect(0, 0, 0, 0)
            }
            val rect = Rect()
            rect.top = decodePadding(paddingOptions.optJSONObject("top"))
            rect.left = decodePadding(paddingOptions.optJSONObject("left"))
            rect.bottom = decodePadding(paddingOptions.optJSONObject("bottom"))
            rect.right = decodePadding(paddingOptions.optJSONObject("right"))

            return rect
        }

        /**
         * Returns a JSONObject containing the encoded padding options
         * @param rect Rect representing the padding options
         * @return JSON object with encoded padding values
         * @throws JSONException
         */
        @JvmStatic
        @Throws(JSONException::class)
        internal fun encodePaddingRectToJson(rect: Rect): JSONObject {
            val paddingJson = JSONObject()
            paddingJson.putOpt("top", encodePadding(rect.top))
            paddingJson.putOpt("left", encodePadding(rect.left))
            paddingJson.putOpt("bottom", encodePadding(rect.bottom))
            paddingJson.putOpt("right", encodePadding(rect.right))
            return paddingJson
        }

        /**
         * Retrieves the padding percentage
         * @discussion -1 is returned when the padding percentage should be auto-sized
         * @param jsonObject
         * @return
         */
        @JvmStatic
        internal fun decodePadding(jsonObject: JSONObject?): Int {
            var returnPadding = 0
            if (jsonObject != null) {
                returnPadding = if ("AutoExpand".equals(jsonObject.optString("displayOption"), ignoreCase = true)) {
                    -1
                } else {
                    jsonObject.optInt("percentage", 0)
                }
            }
            return returnPadding
        }

        /**
         * Encodes the padding percentage to JSON
         * @param padding integer representation of the padding value
         * @return JSON object containing encoded padding data
         * @throws JSONException
         */
        @JvmStatic
        @Throws(JSONException::class)
        internal fun encodePadding(padding: Int): JSONObject {
            val paddingJson = JSONObject()
            if (padding == -1) {
                paddingJson.putOpt("displayOption", "AutoExpand")
            } else {
                paddingJson.putOpt("percentage", padding)
            }
            return paddingJson
        }
    }

    internal class Trigger {
        enum class TriggerType { IMMEDIATE, EVENT, NEVER }

        @Nullable
        val triggerJson: JSONObject?
        @NonNull
        val type: TriggerType

        private constructor(triggerJson: JSONObject) {
            this.triggerJson = triggerJson
            val typeString = triggerJson.optString(IterableConstants.ITERABLE_IN_APP_TRIGGER_TYPE)

            type = when (typeString) {
                "immediate" -> TriggerType.IMMEDIATE
                "never" -> TriggerType.NEVER
                else -> TriggerType.NEVER
            }
        }

        constructor(@NonNull triggerType: TriggerType) {
            triggerJson = null
            this.type = triggerType
        }

        companion object {
            @NonNull
            @JvmStatic
            fun fromJSONObject(triggerJson: JSONObject?): Trigger {
                return if (triggerJson == null) {
                    // Default to 'immediate' if there is no trigger in the payload
                    Trigger(TriggerType.IMMEDIATE)
                } else {
                    Trigger(triggerJson)
                }
            }
        }

        @Nullable
        fun toJSONObject(): JSONObject? {
            return triggerJson
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }
            if (other !is Trigger) {
                return false
            }
            return ObjectsCompat.equals(triggerJson, other.triggerJson)
        }

        override fun hashCode(): Int {
            return ObjectsCompat.hash(triggerJson)
        }
    }

    class Content(
        var html: String?,
        val padding: Rect,
        val backgroundAlpha: Double,
        shouldAnimate: Boolean,
        val inAppDisplaySettings: InAppDisplaySettings
    ) {

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }
            if (other !is Content) {
                return false
            }
            return ObjectsCompat.equals(html, other.html) &&
                    ObjectsCompat.equals(padding, other.padding) &&
                    backgroundAlpha == other.backgroundAlpha
        }

        override fun hashCode(): Int {
            return ObjectsCompat.hash(html, padding, backgroundAlpha)
        }
    }

    class InAppDisplaySettings(
        val shouldAnimate: Boolean,
        val inAppBgColor: InAppBgColor?
    )

    class InAppBgColor(
        val bgHexColor: String?,
        val bgAlpha: Double
    )

    class InboxMetadata(
        @Nullable val title: String?,
        @Nullable val subtitle: String?,
        @Nullable val icon: String?
    ) {

        companion object {
            @Nullable
            @JvmStatic
            fun fromJSONObject(@Nullable inboxMetadataJson: JSONObject?): InboxMetadata? {
                if (inboxMetadataJson == null) {
                    return null
                }

                val title = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_TITLE)
                val subtitle = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_SUBTITLE)
                val icon = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_ICON)
                return InboxMetadata(title, subtitle, icon)
            }
        }

        @NonNull
        fun toJSONObject(): JSONObject {
            val inboxMetadataJson = JSONObject()
            try {
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_TITLE, title)
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_SUBTITLE, subtitle)
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_ICON, icon)
            } catch (e: JSONException) {
                IterableLogger.e(TAG, "Error while serializing inbox metadata", e)
            }
            return inboxMetadataJson
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }
            if (other !is InboxMetadata) {
                return false
            }
            return ObjectsCompat.equals(title, other.title) &&
                    ObjectsCompat.equals(subtitle, other.subtitle) &&
                    ObjectsCompat.equals(icon, other.icon)
        }

        override fun hashCode(): Int {
            return ObjectsCompat.hash(title, subtitle, icon)
        }
    }

    internal interface OnChangeListener {
        fun onInAppMessageChanged(message: IterableInAppMessage)
    }

    private var processed = false
    private var consumed = false
    private var read = false
    private var loadedHtmlFromJson = false
    private var markedForDeletion = false
    @Nullable
    private var inAppStorageInterface: IterableInAppStorage? = null
    private var onChangeListener: OnChangeListener? = null

    // Computed property for saveToInbox validation
    private val processedSaveToInbox: Boolean? = if (saveToInbox != null) (saveToInbox && !jsonOnly) else null

    @NonNull
    fun getMessageId(): String {
        return messageId
    }

    @Nullable
    fun getCampaignId(): Long? {
        return campaignId
    }

    @NonNull
    fun getCreatedAt(): Date {
        return createdAt
    }

    @NonNull
    fun getExpiresAt(): Date {
        return expiresAt
    }

    @NonNull
    fun getContent(): Content {
        if (content.html == null && !jsonOnly) {
            content.html = inAppStorageInterface!!.getHTML(messageId)
        }
        return content
    }

    @NonNull
    fun getCustomPayload(): JSONObject {
        return customPayload
    }

    internal fun isProcessed(): Boolean {
        return processed
    }

    internal fun setProcessed(processed: Boolean) {
        this.processed = processed
        onChanged()
    }

    fun isConsumed(): Boolean {
        return consumed
    }

    internal fun setConsumed(consumed: Boolean) {
        this.consumed = consumed
        onChanged()
    }

    internal fun getTriggerType(): Trigger.TriggerType {
        return trigger.type
    }

    fun getPriorityLevel(): Double {
        return priorityLevel
    }

    fun isInboxMessage(): Boolean {
        return processedSaveToInbox ?: false
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isSilentInboxMessage(): Boolean {
        return isInboxMessage() && getTriggerType() == Trigger.TriggerType.NEVER
    }

    @Nullable
    fun getInboxMetadata(): InboxMetadata? {
        return inboxMetadata
    }

    fun isRead(): Boolean {
        return read
    }

    internal fun setRead(read: Boolean) {
        this.read = read
        onChanged()
    }

    internal fun hasLoadedHtmlFromJson(): Boolean {
        return loadedHtmlFromJson
    }

    internal fun setLoadedHtmlFromJson(loadedHtmlFromJson: Boolean) {
        this.loadedHtmlFromJson = loadedHtmlFromJson
    }

    fun isMarkedForDeletion(): Boolean {
        return markedForDeletion
    }

    fun isExpired(): Boolean {
        return expiresAt.before(Date())
    }

    fun markForDeletion(delete: Boolean) {
        this.markedForDeletion = delete
    }

    fun isJsonOnly(): Boolean {
        return jsonOnly
    }

    @NonNull
    internal fun toJSONObject(): JSONObject {
        val messageJson = JSONObject()
        val contentJson = JSONObject()
        try {
            messageJson.putOpt(IterableConstants.KEY_MESSAGE_ID, messageId)
            if (campaignId != null && IterableUtil.isValidCampaignId(campaignId)) {
                messageJson.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId)
            }
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CREATED_AT, createdAt.time)
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, expiresAt.time)
            if (jsonOnly) {
                messageJson.put(IterableConstants.ITERABLE_IN_APP_JSON_ONLY, 1)
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_TRIGGER, trigger.toJSONObject())

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_PRIORITY_LEVEL, priorityLevel)

            val inAppDisplaySettingsJson = encodePaddingRectToJson(content.padding)

            inAppDisplaySettingsJson.put(IterableConstants.ITERABLE_IN_APP_SHOULD_ANIMATE, content.inAppDisplaySettings.shouldAnimate)
            if (content.inAppDisplaySettings.inAppBgColor != null && content.inAppDisplaySettings.inAppBgColor!!.bgHexColor != null) {
                val bgColorJson = JSONObject()
                bgColorJson.put(IterableConstants.ITERABLE_IN_APP_BGCOLOR_ALPHA, content.inAppDisplaySettings.inAppBgColor!!.bgAlpha)
                bgColorJson.putOpt(IterableConstants.ITERABLE_IN_APP_BGCOLOR_HEX, content.inAppDisplaySettings.inAppBgColor!!.bgHexColor)
                inAppDisplaySettingsJson.put(IterableConstants.ITERABLE_IN_APP_BGCOLOR, bgColorJson)
            }

            contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_DISPLAY_SETTINGS, inAppDisplaySettingsJson)

            if (content.backgroundAlpha != 0.0) {
                contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ALPHA, content.backgroundAlpha)
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONTENT, contentJson)
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CUSTOM_PAYLOAD, customPayload)

            if (saveToInbox != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX, saveToInbox && !jsonOnly)
            }
            if (inboxMetadata != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_METADATA, inboxMetadata.toJSONObject())
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_PROCESSED, processed)
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONSUMED, consumed)
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_READ, read)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing an in-app message", e)
        }
        return messageJson
    }

    internal fun setOnChangeListener(listener: OnChangeListener?) {
        onChangeListener = listener
    }

    private fun onChanged() {
        if (onChangeListener != null) {
            onChangeListener!!.onInAppMessageChanged(this)
        }
    }
}