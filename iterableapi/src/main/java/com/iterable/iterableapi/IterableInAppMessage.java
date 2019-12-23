package com.iterable.iterableapi;

import android.graphics.Rect;
import android.support.annotation.RestrictTo;
import android.support.v4.util.ObjectsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class IterableInAppMessage implements Comparable<IterableInAppMessage> {
    private static final String TAG = "IterableInAppMessage";

    private final String messageId;
    private final Content content;
    private final JSONObject customPayload;
    private final Date createdAt;
    private final Date expiresAt;
    private final Trigger trigger;
    private final Boolean saveToInbox;
    private final InboxMetadata inboxMetadata;
    private boolean processed = false;
    private boolean consumed = false;
    private boolean read = false;
    private boolean loadedHtmlFromJson = false;
    private IterableInAppStorage inAppStorageInterface;

    IterableInAppMessage(String messageId, Content content, JSONObject customPayload, Date createdAt, Date expiresAt, Trigger trigger, Boolean saveToInbox, InboxMetadata inboxMetadata) {
        this.messageId = messageId;
        this.content = content;
        this.customPayload = customPayload;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.trigger = trigger;
        this.saveToInbox = saveToInbox;
        this.inboxMetadata = inboxMetadata;
    }

    static class Trigger {
        enum TriggerType { IMMEDIATE, EVENT, NEVER }
        final JSONObject triggerJson;
        final TriggerType type;

        private Trigger(JSONObject triggerJson) {
            this.triggerJson = triggerJson;
            String typeString = triggerJson.optString(IterableConstants.ITERABLE_IN_APP_TRIGGER_TYPE);

            switch(typeString) {
                case "immediate":
                    type = TriggerType.IMMEDIATE;
                    break;
                case "never":
                    type = TriggerType.NEVER;
                    break;
                default:
                    type = TriggerType.NEVER;
            }
        }

        Trigger(TriggerType triggerType) {
            triggerJson = null;
            this.type = triggerType;
        }

        static Trigger fromJSONObject(JSONObject triggerJson) {
            if (triggerJson == null) {
                // Default to 'immediate' if there is no trigger in the payload
                return new Trigger(TriggerType.IMMEDIATE);
            } else {
                return new Trigger(triggerJson);
            }
        }

        JSONObject toJSONObject() {
            return triggerJson;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Trigger)) {
                return false;
            }
            Trigger trigger = (Trigger) obj;
            return ObjectsCompat.equals(triggerJson, trigger.triggerJson);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(triggerJson);
        }
    }

    public static class Content {
        public String html;
        public final Rect padding;
        public final double backgroundAlpha;

        Content(String html, Rect padding, double backgroundAlpha) {
            this.html = html;
            this.padding = padding;
            this.backgroundAlpha = backgroundAlpha;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Content)) {
                return false;
            }
            Content content = (Content) obj;
            return ObjectsCompat.equals(html, content.html) &&
                    ObjectsCompat.equals(padding, content.padding) &&
                    backgroundAlpha == backgroundAlpha;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(html, padding, backgroundAlpha);
        }
    }

    public static class InboxMetadata {
        public final String title;
        public final String subtitle;
        public final String icon;

        public InboxMetadata(String title, String subtitle, String icon) {
            this.title = title;
            this.subtitle = subtitle;
            this.icon = icon;
        }

        static InboxMetadata fromJSONObject(JSONObject inboxMetadataJson) {
            if (inboxMetadataJson == null) {
                return null;
            }

            String title = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_TITLE);
            String subtitle = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_SUBTITLE);
            String icon = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_ICON);
            return new InboxMetadata(title, subtitle, icon);
        }

        JSONObject toJSONObject() {
            JSONObject inboxMetadataJson = new JSONObject();
            try {
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_TITLE, title);
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_SUBTITLE, subtitle);
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_ICON, icon);
            } catch (JSONException e) {
                IterableLogger.e(TAG, "Error while serializing inbox metadata", e);
            }
            return inboxMetadataJson;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof InboxMetadata)) {
                return false;
            }
            InboxMetadata inboxMetadata = (InboxMetadata) obj;
            return ObjectsCompat.equals(title, inboxMetadata.title) &&
                    ObjectsCompat.equals(subtitle, inboxMetadata.subtitle) &&
                    ObjectsCompat.equals(icon, inboxMetadata.icon);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(title, subtitle, icon);
        }
    }

    public String getMessageId() {
        return messageId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    Date getExpiresAt() {
        return expiresAt;
    }

    public Content getContent() {
        if (content.html == null) {
            content.html = inAppStorageInterface.getHTML(messageId);
        }
        return content;
    }

    public JSONObject getCustomPayload() {
        return customPayload;
    }

    boolean isProcessed() {
        return processed;
    }

    void setProcessed(boolean processed) {
        this.processed = processed;
        onChanged();
    }

    boolean isConsumed() {
        return consumed;
    }

    void setConsumed(boolean consumed) {
        this.consumed = consumed;
        onChanged();
    }

    Trigger.TriggerType getTriggerType() {
        return trigger.type;
    }

    public boolean isInboxMessage() {
        return saveToInbox != null ? saveToInbox : false;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isSilentInboxMessage() {
        return isInboxMessage() && getTriggerType() == IterableInAppMessage.Trigger.TriggerType.NEVER;
    }

    public InboxMetadata getInboxMetadata() {
        return inboxMetadata;
    }

    public boolean isRead() {
        return read;
    }

    void setRead(boolean read) {
        this.read = read;
        onChanged();
    }

    boolean hasLoadedHtmlFromJson() {
        return loadedHtmlFromJson;
    }

    void setLoadedHtmlFromJson(boolean loadedHtmlFromJson) {
        this.loadedHtmlFromJson = loadedHtmlFromJson;
    }

    static IterableInAppMessage fromJSONObject(JSONObject messageJson, IterableInAppStorage storageInterface) {

        if (messageJson == null) {
            return null;
        }

        JSONObject contentJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
        if (contentJson == null) {
            return null;
        }

        String messageId = messageJson.optString(IterableConstants.KEY_MESSAGE_ID);
        long createdAtLong = messageJson.optLong(IterableConstants.ITERABLE_IN_APP_CREATED_AT);
        Date createdAt = createdAtLong != 0 ? new Date(createdAtLong) : null;
        long expiresAtLong = messageJson.optLong(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT);
        Date expiresAt = expiresAtLong != 0 ? new Date(expiresAtLong) : null;

        String html = contentJson.optString(IterableConstants.ITERABLE_IN_APP_HTML, null);

        JSONObject paddingOptions = contentJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_DISPLAY_SETTINGS);
        Rect padding = getPaddingFromPayload(paddingOptions);
        double backgroundAlpha = contentJson.optDouble(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ALPHA, 0);

        JSONObject triggerJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_TRIGGER);
        Trigger trigger = Trigger.fromJSONObject(triggerJson);
        JSONObject customPayload = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CUSTOM_PAYLOAD);
        if (customPayload == null) {
            customPayload = contentJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_LEGACY_PAYLOAD);
        }
        if (customPayload == null) {
            customPayload = new JSONObject();
        }

        Boolean saveToInbox = messageJson.has(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX) ? messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX) : null;
        JSONObject inboxPayloadJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_INBOX_METADATA);
        InboxMetadata inboxMetadata = InboxMetadata.fromJSONObject(inboxPayloadJson);

        IterableInAppMessage message = new IterableInAppMessage(messageId,
                new Content(html, padding, backgroundAlpha), customPayload, createdAt, expiresAt, trigger, saveToInbox, inboxMetadata);
        message.inAppStorageInterface = storageInterface;
        if (html != null) {
            message.setLoadedHtmlFromJson(true);
        }
        message.processed = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_PROCESSED, false);
        message.consumed = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_CONSUMED, false);
        message.read = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_READ, false);
        return message;
    }

    JSONObject toJSONObject() {
        JSONObject messageJson = new JSONObject();
        JSONObject contentJson = new JSONObject();
        try {
            messageJson.putOpt(IterableConstants.KEY_MESSAGE_ID, messageId);
            if (createdAt != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CREATED_AT, createdAt.getTime());
            }
            if (expiresAt != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, expiresAt.getTime());
            }
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_TRIGGER, trigger.toJSONObject());
            contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_DISPLAY_SETTINGS, encodePaddingRectToJson(content.padding));
            if (content.backgroundAlpha != 0) {
                contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ALPHA, content.backgroundAlpha);
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONTENT, contentJson);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CUSTOM_PAYLOAD, customPayload);

            if (saveToInbox != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX, saveToInbox);
            }
            if (inboxMetadata != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_METADATA, inboxMetadata.toJSONObject());
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_PROCESSED, processed);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONSUMED, consumed);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_READ, read);
        } catch (JSONException e) {
            IterableLogger.e(TAG, "Error while serializing an in-app message", e);
        }
        return messageJson;
    }

    interface OnChangeListener {
        void onInAppMessageChanged(IterableInAppMessage message);
    }

    private OnChangeListener onChangeListener;

    void setOnChangeListener(OnChangeListener listener) {
        onChangeListener = listener;
    }

    private void onChanged() {
        if (onChangeListener != null) {
            onChangeListener.onInAppMessageChanged(this);
        }
    }

    /**
     * Returns a Rect containing the paddingOptions
     * @param paddingOptions
     * @return
     */
    static Rect getPaddingFromPayload(JSONObject paddingOptions) {
        Rect rect = new Rect();
        rect.top = decodePadding(paddingOptions.optJSONObject("top"));
        rect.left = decodePadding(paddingOptions.optJSONObject("left"));
        rect.bottom = decodePadding(paddingOptions.optJSONObject("bottom"));
        rect.right = decodePadding(paddingOptions.optJSONObject("right"));

        return rect;
    }

    /**
     * Returns a JSONObject containing the encoded padding options
     * @param rect Rect representing the padding options
     * @return JSON object with encoded padding values
     * @throws JSONException
     */
    static JSONObject encodePaddingRectToJson(Rect rect) throws JSONException {
        JSONObject paddingJson = new JSONObject();
        paddingJson.putOpt("top", encodePadding(rect.top));
        paddingJson.putOpt("left", encodePadding(rect.left));
        paddingJson.putOpt("bottom", encodePadding(rect.bottom));
        paddingJson.putOpt("right", encodePadding(rect.right));
        return paddingJson;
    }

    /**
     * Retrieves the padding percentage
     * @discussion -1 is returned when the padding percentage should be auto-sized
     * @param jsonObject
     * @return
     */
    static int decodePadding(JSONObject jsonObject) {
        int returnPadding = 0;
        if (jsonObject != null) {
            if ("AutoExpand".equalsIgnoreCase(jsonObject.optString("displayOption"))) {
                returnPadding = -1;
            } else {
                returnPadding = jsonObject.optInt("percentage", 0);
            }
        }
        return returnPadding;
    }

    /**
     * Encodes the padding percentage to JSON
     * @param padding integer representation of the padding value
     * @return JSON object containing encoded padding data
     * @throws JSONException
     */
    static JSONObject encodePadding(int padding) throws JSONException {
        JSONObject paddingJson = new JSONObject();
        if (padding == -1) {
            paddingJson.putOpt("displayOption", "AutoExpand");
        } else {
            paddingJson.putOpt("percentage", padding);
        }
        return paddingJson;
    }

    /**
     * Gets the next message from the payload
     * @param payload
     * @return
     */
    public static JSONObject getNextMessageFromPayload(String payload) {
        JSONObject returnObject = null;
        if (payload != null && payload != "") {
            try {
                JSONObject mainObject = new JSONObject(payload);
                JSONArray jsonArray = mainObject.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
                if (jsonArray != null) {
                    returnObject = jsonArray.optJSONObject(0);
                }
            } catch (JSONException e) {
                IterableLogger.e(IterableInAppManager.TAG, e.toString());
            }
        }
        return returnObject;
    }

    @Override
    public int compareTo(IterableInAppMessage message) {
        if (message.getCreatedAt() != null) {
            return message.getCreatedAt().compareTo(getCreatedAt());
        }
        return 0;
    }
}
