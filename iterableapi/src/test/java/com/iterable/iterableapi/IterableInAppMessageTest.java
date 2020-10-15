package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class IterableInAppMessageTest {
    @Before
    public void setUp() {

    }

    @Test
    public void testInAppMessageDeserialization() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_multiple.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject messageJson = jsonArray.optJSONObject(i);

                //Stripping out HTML content from the copy
                JSONObject messageJsonHTMLStripped = new JSONObject(messageJson.toString());
                JSONObject content = (JSONObject) messageJsonHTMLStripped.get("content");
                JSONObject inAppDisplaySettings = content.getJSONObject("inAppDisplaySettings");
                assertEquals( (Boolean) inAppDisplaySettings.get("shouldAnimate"), true);
                JSONObject bgColor = (JSONObject) inAppDisplaySettings.get("bgColor");
                assertNotNull(bgColor.get("hex"));
                assertNotNull(bgColor.get("alpha"));
                content.remove("html");
                messageJsonHTMLStripped.put("content", content);

                IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson, null);
                assertNotNull(message);
                assertNotNull(message.getContent().inAppDisplaySettings.inAppBgColor.bgHexColor);
                assertNotNull(message.getContent().inAppDisplaySettings.inAppBgColor.bgAlpha);
                assertNotNull(message.getContent().inAppDisplaySettings.shouldAnimate);
                JSONAssert.assertEquals(messageJsonHTMLStripped, message.toJSONObject(), JSONCompareMode.STRICT_ORDER);
            }
        }
    }

    @Test
    public void testInAppLegacyPayloadDeserialization() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_legacy.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        JSONObject messageJson = jsonArray.optJSONObject(0);
        IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson, null);
        assertNotNull(message);
        assertNotNull(message.getCustomPayload());
        assertEquals(123, message.getCustomPayload().getInt("intValue"));
        assertEquals("test", message.getCustomPayload().getString("stringValue"));
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


    @Test
    public void testStorageNotInvoked() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_multiple.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        String storageInterfaceHTML = "HTML from storage interface";
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject messageJson = jsonArray.optJSONObject(i);

                IterableInAppStorage storageInterface = mock(IterableInAppStorage.class);
                IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson, storageInterface);
                assertNotNull(message.getContent().html);
                assertNotEquals(message.getContent().html, storageInterfaceHTML);

                //Asserting if the storage was accessed even once
                verify(storageInterface, times(0)).getHTML(message.getMessageId());
            }
        }
    }

    @Test
    public void testInAppStorageAccess() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_multiple.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        String storageInterfaceHTML = "HTML from storage interface";
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject messageJson = jsonArray.optJSONObject(i);

                //Stripping out HTML content from the copy
                JSONObject messageJsonHTMLStripped = new JSONObject(messageJson.toString());
                JSONObject content = (JSONObject) messageJsonHTMLStripped.get("content");
                content.remove("html");
                messageJsonHTMLStripped.put("content", content);

                IterableInAppStorage storageInterface = mock(IterableInAppStorage.class);
                IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJsonHTMLStripped, storageInterface);
                when(storageInterface.getHTML(message.getMessageId())).thenReturn(storageInterfaceHTML);
                assertNotNull(message.getContent().html);
                assertEquals(storageInterfaceHTML, message.getContent().html);
                verify(storageInterface, times(1)).getHTML(message.getMessageId());
            }
        }
    }

    @Test
    public void testInAppMessageSerialization() throws Exception {
        JSONObject payload = new JSONObject(IterableTestUtils.getResourceString("inapp_payload_multiple.json"));
        JSONArray jsonArray = payload.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject messageJson = jsonArray.optJSONObject(i);
                IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson, null);

                //Making sure if messageObject actually contains html content
                assertEquals(message.getContent().html == "", false);

                JSONObject messageBackInJSON = message.toJSONObject();
                JSONObject contentJSON = messageBackInJSON.getJSONObject("content");
                String html = contentJSON.optString("html", null);
                assertNull(html);
            }
        }
    }
}
