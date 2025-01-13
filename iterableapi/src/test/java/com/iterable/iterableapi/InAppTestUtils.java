package com.iterable.iterableapi;

import android.graphics.Rect;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

public class InAppTestUtils {
    public static IterableInAppMessage getTestInAppMessage() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_single.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        return IterableInAppMessage.fromJSONObject(jsonArray.getJSONObject(0), null);
    }

    public static IterableInAppMessage getTestInboxInAppWithId(String messageId) {
        return new IterableInAppMessage(
                messageId,
                new IterableInAppMessage.Content("",
                        new Rect(0, 0, 0, 0),
                        0.0,
                        true,
                        new IterableInAppMessage.InAppDisplaySettings(true,
                                new IterableInAppMessage.InAppBgColor("000000", 0.0))),
                new JSONObject(),
                new Date(),
                new Date(),
                new IterableInAppMessage.Trigger(IterableInAppMessage.Trigger.TriggerType.IMMEDIATE),
                new Double(300.5),
                true,
                null,
                null,
                false
                );
    }
}
