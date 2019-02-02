package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(TestRunner.class)
public class IterableInAppMessageTest {
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
                JSONAssert.assertEquals(messageJson, message.toJSONObject(), JSONCompareMode.STRICT_ORDER);
            }
        }
    }

    @Test
    public void testInAppMessageOnChangeListener_processed() throws Exception {
        IterableInAppMessage testInAppMessage = InAppTestUtils.getTestInAppMessage();
        IterableInAppMessage.OnChangeListener mockChangeListener = mock(IterableInAppMessage.OnChangeListener.class);
        testInAppMessage.setOnChangeListener(mockChangeListener);

        testInAppMessage.setProcessed(true);
        verify(mockChangeListener).onInAppMessageChanged(testInAppMessage);
    }

    @Test
    public void testInAppMessageOnChangeListener_consumed() throws Exception {
        IterableInAppMessage testInAppMessage = InAppTestUtils.getTestInAppMessage();
        IterableInAppMessage.OnChangeListener mockChangeListener = mock(IterableInAppMessage.OnChangeListener.class);
        testInAppMessage.setOnChangeListener(mockChangeListener);

        testInAppMessage.setConsumed(true);
        verify(mockChangeListener).onInAppMessageChanged(testInAppMessage);
    }
}
