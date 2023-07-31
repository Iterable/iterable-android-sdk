package com.iterable.iterableapi;

import org.json.JSONArray;
import org.json.JSONObject;

public class EmbeddedTestUtils {
    public static IterableEmbeddedMessage getTestEmbeddedMessage() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("embedded_payload_optional_elements_and_custom_payload.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS);

        JSONObject placementJson = jsonArray.optJSONObject(0);
        IterableEmbeddedPlacement placement = IterableEmbeddedPlacement.Companion.fromJSONObject(placementJson);
        IterableEmbeddedMessage message = placement.getMessages().get(0);

        return message;
    }
}
