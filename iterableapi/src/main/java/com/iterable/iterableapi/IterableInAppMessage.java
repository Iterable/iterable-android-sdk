package com.iterable.iterableapi;

import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IterableInAppMessage {
    private final String messageId;
    private final Content content;

    // todo: remove
    private boolean processed = false;
    private boolean consumed = false;

    IterableInAppMessage(String messageId, Content content) {
        this.messageId = messageId;
        this.content = content;
    }

    public static class Content {
         public final String html;
         public final JSONObject payload;
         public final Rect padding;
         public final double backgroundAlpha;

        Content(String html, JSONObject payload, Rect padding, double backgroundAlpha) {
            this.html = html;
            this.payload = payload;
            this.padding = padding;
            this.backgroundAlpha = backgroundAlpha;
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

    static IterableInAppMessage fromJSONObject(JSONObject messageJson) {
        if (messageJson != null) {
            JSONObject contentJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
            if (contentJson != null) {
                String messageId = messageJson.optString(IterableConstants.KEY_MESSAGE_ID);
                String html = contentJson.optString("html");
                JSONObject payload = contentJson.optJSONObject("payload");
                JSONObject paddingOptions = contentJson.optJSONObject("inAppDisplaySettings");
                Rect padding = getPaddingFromPayload(paddingOptions);
                double backgroundAlpha = contentJson.optDouble("backgroundAlpha", 0);

                return new IterableInAppMessage(messageId, new Content(html, payload, padding, backgroundAlpha));
            }
        }
        return null;
    }

    JSONObject toJSONObject() {
        JSONObject messageJson = new JSONObject();
        JSONObject contentJson = new JSONObject();
        try {
            messageJson.putOpt(IterableConstants.KEY_MESSAGE_ID, messageId);

            contentJson.putOpt("html", content.html);
            contentJson.putOpt("payload", content.payload);
            contentJson.putOpt("inAppDisplaySettings", encodePaddingRectToJson(content.padding));
            if (content.backgroundAlpha != 0) {
                contentJson.putOpt("backgroundAlpha", content.backgroundAlpha);
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONTENT, contentJson);
        } catch (JSONException ignored) {}
        return messageJson;
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
