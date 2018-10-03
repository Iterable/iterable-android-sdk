package com.iterable.iterableapi.ddl;

import org.json.JSONException;
import org.json.JSONObject;

public class MatchFpResponse {
    public final boolean isMatch;
    public final String destinationUrl;
    public final int campaignId;
    public final int templateId;
    public final String messageId;

    public MatchFpResponse(boolean isMatch, String destinationUrl, int campaignId, int templateId, String messageId) {
        this.isMatch = isMatch;
        this.destinationUrl = destinationUrl;
        this.campaignId = campaignId;
        this.templateId = templateId;
        this.messageId = messageId;
    }

    public static MatchFpResponse fromJSONObject(JSONObject json) throws JSONException {
        return new MatchFpResponse(
                json.getBoolean("isMatch"),
                json.getString("destinationUrl"),
                json.getInt("campaignId"),
                json.getInt("templateId"),
                json.getString("messageId")
        );
    }
}
