package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.BaseTest;
import com.iterable.iterableapi.unit.IterableTestUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IterableInAppMessageTest extends BaseTest {

    @Before
    public void setUp() {

    }

    @Test
    public void testInAppMessageSerialization() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_single.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject messageJson = jsonArray.optJSONObject(i);
                IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson);
                assertNotNull(message);
                JSONAssert.assertEquals(messageJson, message.toJSONObject(), JSONCompareMode.STRICT);
            }
        }
    }
}
