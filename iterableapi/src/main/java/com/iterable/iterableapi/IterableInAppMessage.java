package com.iterable.iterableapi;

import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class IterableInAppMessage {
    private static final String TAG = "IterableInAppMessage";

    private final String messageId;
    private final Content content;
    private final JSONObject customPayload;
    private final Date expiresAt;
    private final Trigger trigger;

    private boolean processed = false;
    private boolean consumed = false;

    IterableInAppMessage(String messageId, Content content, JSONObject customPayload, Date expiresAt, Trigger trigger) {
        this.messageId = messageId;
        this.content = content;
        this.customPayload = customPayload;
        this.expiresAt = expiresAt;
        this.trigger = trigger;
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
    }

    public static class Content {
         public final String html;
         public final Rect padding;
         public final double backgroundAlpha;

        Content(String html, Rect padding, double backgroundAlpha) {
            this.html = html;
            this.padding = padding;
            this.backgroundAlpha = backgroundAlpha;
        }
    }

    public String getMessageId() {
        return messageId;
    }

    Date getExpiresAt() {
        return expiresAt;
    }

    public Content getContent() {
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

    static IterableInAppMessage fromJSONObject(JSONObject messageJson) {
        if (messageJson == null) {
            return null;
        }

        JSONObject contentJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
        if (contentJson == null) {
            return null;
        }

        String messageId = messageJson.optString(IterableConstants.KEY_MESSAGE_ID);
        long expiresAtLong = messageJson.optLong(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT);
        Date expiresAt = expiresAtLong != 0 ? new Date(expiresAtLong) : null;

        String html = contentJson.optString(IterableConstants.ITERABLE_IN_APP_HTML);
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

        IterableInAppMessage message = new IterableInAppMessage(messageId,
                new Content(html, padding, backgroundAlpha), customPayload, expiresAt, trigger);
        message.processed = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_PROCESSED, false);
        message.consumed = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_CONSUMED, false);
        return message;
    }

    JSONObject toJSONObject() {
        JSONObject messageJson = new JSONObject();
        JSONObject contentJson = new JSONObject();
        try {
            messageJson.putOpt(IterableConstants.KEY_MESSAGE_ID, messageId);
            if (expiresAt != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, expiresAt.getTime());
            }
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_TRIGGER, trigger.toJSONObject());

            contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_HTML, content.html);
            contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_DISPLAY_SETTINGS, encodePaddingRectToJson(content.padding));
            if (content.backgroundAlpha != 0) {
                contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ALPHA, content.backgroundAlpha);
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONTENT, contentJson);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CUSTOM_PAYLOAD, customPayload);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_PROCESSED, processed);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONSUMED, consumed);
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
}
