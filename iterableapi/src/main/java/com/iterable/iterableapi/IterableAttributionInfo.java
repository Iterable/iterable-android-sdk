package com.iterable.iterableapi;

import org.json.JSONException;
import org.json.JSONObject;

public class IterableAttributionInfo {

    public final int campaignId;
    public final int templateId;
    public final String messageId;

    public IterableAttributionInfo(int campaignId, int templateId, String messageId) {
        this.campaignId = campaignId;
        this.templateId = templateId;
        this.messageId = messageId;
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("campaignId", campaignId);
            jsonObject.put("templateId", templateId);
            jsonObject.put("messageId", messageId);
        } catch (JSONException ignored) {}
        return jsonObject;
    }

    public static IterableAttributionInfo fromJSONObject(JSONObject jsonObject) {
        if (jsonObject != null) {
            return new IterableAttributionInfo(
                    jsonObject.optInt("campaignId"),
                    jsonObject.optInt("templateId"),
                    jsonObject.optString("messageId")
            );
        } else {
            return null;
        }
    }
}
