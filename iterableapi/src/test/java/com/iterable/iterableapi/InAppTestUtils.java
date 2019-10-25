package com.iterable.iterableapi;

import org.json.JSONArray;
import org.json.JSONObject;

public class InAppTestUtils {
    public static IterableInAppMessage getTestInAppMessage() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_single.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        return IterableInAppMessage.fromJSONObject(jsonArray.getJSONObject(0), null);
    }
}
