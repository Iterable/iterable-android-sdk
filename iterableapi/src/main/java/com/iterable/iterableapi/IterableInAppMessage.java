package com.iterable.iterableapi;

import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IterableInAppMessage {
    private final IterableInAppStorage storage;
    private final String messageId;
    private final Content content;

    // todo: remove
    private boolean processed = false;
    private boolean consumed = false;

    IterableInAppMessage(IterableInAppStorage storage, String messageId, Content content) {
        this.storage = storage;
        this.messageId = messageId;
        this.content = content;
    }

    public static class Content {
         public final String html;
         public final JSONObject payload;
         public final Rect padding;

        Content(String html, JSONObject payload, Rect padding) {
            this.html = html;
            this.payload = payload;
            this.padding = padding;
        }
    }

    public String getMessageId() {
        return messageId;
    }

    public Content getContent() {
         return content;
    }

    boolean isProcessed() {
        return processed;
    }

    void setProcessed(boolean processed) {
        this.processed = processed;
    }

    boolean isConsumed() {
        return consumed;
    }

    void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public static IterableInAppMessage fromJSON(IterableInAppStorage storage, JSONObject messageJson) {
        if (messageJson != null) {
            JSONObject contentJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
            if (contentJson != null) {
                String messageId = messageJson.optString(IterableConstants.KEY_MESSAGE_ID);
                String html = contentJson.optString("html");
                JSONObject paddingOptions = contentJson.optJSONObject("inAppDisplaySettings");
                Rect padding = getPaddingFromPayload(paddingOptions);
                double backgroundAlpha = contentJson.optDouble("backgroundAlpha", 0);

                return new IterableInAppMessage(storage, messageId, new Content(html, null, padding));
            }
        }
        return null;
    }

    /**
     * Returns a Rect containing the paddingOptions
     * @param paddingOptions
     * @return
     */
    public static Rect getPaddingFromPayload(JSONObject paddingOptions) {
        Rect rect = new Rect();
        rect.top = decodePadding(paddingOptions.optJSONObject("top"));
        rect.left = decodePadding(paddingOptions.optJSONObject("left"));
        rect.bottom = decodePadding(paddingOptions.optJSONObject("bottom"));
        rect.right = decodePadding(paddingOptions.optJSONObject("right"));

        return rect;
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
