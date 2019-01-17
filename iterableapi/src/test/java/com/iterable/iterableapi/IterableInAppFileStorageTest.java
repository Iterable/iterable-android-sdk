package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.BaseTest;
import com.iterable.iterableapi.unit.IterableTestUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.Assert.assertEquals;

public class IterableInAppFileStorageTest extends BaseTest {

    private IterableInAppMessage getTestInAppMessage() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_single.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        return IterableInAppMessage.fromJSONObject(jsonArray.getJSONObject(0));
    }

    @Test
    public void testInAppPersistence() throws Exception {
        IterableInAppFileStorage storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        IterableInAppMessage testInAppMessage = getTestInAppMessage();

        assertEquals(0, storage.getMessages().size());

        storage.addMessage(testInAppMessage);
        assertEquals(1, storage.getMessages().size());
        assertEquals(storage.getMessage(testInAppMessage.getMessageId()), testInAppMessage);

        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(1, storage.getMessages().size());
        JSONAssert.assertEquals(testInAppMessage.toJSONObject(), storage.getMessages().get(0).toJSONObject(), JSONCompareMode.STRICT);

        storage.removeMessage(storage.getMessage(testInAppMessage.getMessageId()));
        assertEquals(0, storage.getMessages().size());
        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(0, storage.getMessages().size());
    }
}
