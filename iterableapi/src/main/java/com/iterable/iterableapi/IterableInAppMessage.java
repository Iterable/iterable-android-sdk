package com.iterable.iterableapi;

import android.graphics.Rect;

import org.json.JSONObject;

public class IterableInAppMessage {
    private final String messageId;
    private final Content content;

    IterableInAppMessage(String messageId, Content content) {
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

    public static IterableInAppMessage fromJSON(JSONObject messageJson) {
        if (messageJson != null) {
            JSONObject contentJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
            if (contentJson != null) {
                String messageId = messageJson.optString(IterableConstants.KEY_MESSAGE_ID);
                String html = contentJson.optString("html");
                JSONObject paddingOptions = contentJson.optJSONObject("inAppDisplaySettings");
                Rect padding = IterableInAppManager.getPaddingFromPayload(paddingOptions);
                double backgroundAlpha = contentJson.optDouble("backgroundAlpha", 0);

                return new IterableInAppMessage(messageId, new Content(html, null, padding));
            }
        }
        return null;
    }
}
