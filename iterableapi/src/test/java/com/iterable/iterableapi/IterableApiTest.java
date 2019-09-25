package com.iterable.iterableapi;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IterableApiTest extends BaseTest {

    public static final String PACKAGE_NAME = "com.iterable.iterableapi.test";
    private MockWebServer server;
    private IterableUtil.IterableUtilImpl originalIterableUtil;
    private IterableUtil.IterableUtilImpl iterableUtilSpy;

    @Before
    public void setUp() {
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());
        reInitIterableApi();

        originalIterableUtil = IterableUtil.instance;
        iterableUtilSpy = spy(originalIterableUtil);
        IterableUtil.instance = iterableUtilSpy;
    }

    @After
    public void tearDown() throws IOException {
        IterableUtil.instance = originalIterableUtil;
        iterableUtilSpy = null;
        server.shutdown();
        server = null;
    }

    private void reInitIterableApi() {
        IterableApi.sharedInstance = spy(new IterableApi());
        IterableInAppManager inAppManagerMock = mock(IterableInAppManager.class);
        doReturn(inAppManagerMock).when(IterableApi.sharedInstance).getInAppManager();
    }

    @Test
    public void testSdkInitializedWithoutEmailOrUserId() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        IterableApi.getInstance().setEmail(null);

        // Verify that none of the calls to the API result in a request
        IterableApi.getInstance().track("testEvent");
        IterableApi.getInstance().trackInAppOpen("12345");
        IterableApi.getInstance().inAppConsume("12345");
        IterableApi.getInstance().trackInAppClick("12345", "");
        IterableApi.getInstance().registerDeviceToken("12345");
        IterableApi.getInstance().disablePush("12345", "12345");
        IterableApi.getInstance().updateUser(new JSONObject());
        IterableApi.getInstance().updateEmail("");
        IterableApi.getInstance().trackPurchase(10.0, new ArrayList<CommerceItem>());

        RecordedRequest request = server.takeRequest(100, TimeUnit.MILLISECONDS);
        assertNull(request);
    }

    @Test
    public void testEmailUserIdPersistence() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        IterableApi.getInstance().setEmail("test@email.com");

        reInitIterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        assertEquals("test@email.com", IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getUserId());

        IterableApi.getInstance().setUserId("testUserId");
        reInitIterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        assertEquals("testUserId", IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getEmail());
    }

    @Test
    public void testAttributionInfoPersistence() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");

        IterableAttributionInfo attributionInfo = new IterableAttributionInfo(1234, 4321, "message");
        IterableApi.getInstance().setAttributionInfo(attributionInfo);

        // 23 hours, not expired, still present
        doReturn(System.currentTimeMillis() + 3600 * 23 * 1000).when(iterableUtilSpy).currentTimeMillis();
        IterableAttributionInfo storedAttributionInfo = IterableApi.getInstance().getAttributionInfo();
        assertNotNull(storedAttributionInfo);
        assertEquals(attributionInfo.campaignId, storedAttributionInfo.campaignId);
        assertEquals(attributionInfo.templateId, storedAttributionInfo.templateId);
        assertEquals(attributionInfo.messageId, storedAttributionInfo.messageId);

        // 24 hours, expired, attributionInfo should be null
        doReturn(System.currentTimeMillis() + 3600 * 24 * 1000).when(iterableUtilSpy).currentTimeMillis();
        storedAttributionInfo = IterableApi.getInstance().getAttributionInfo();
        assertNull(storedAttributionInfo);
    }

    @Test
    public void testUpdateEmailPersistence() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        IterableApi.getInstance().setEmail("test@email.com");
        assertEquals("test@email.com", IterableApi.getInstance().getEmail());

        IterableApi.getInstance().updateEmail("new@email.com");
        server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("new@email.com", IterableApi.getInstance().getEmail());

        reInitIterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        assertEquals("new@email.com", IterableApi.getInstance().getEmail());
    }

    @Test
    public void testUpdateEmailWithOldEmail() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().updateEmail("new@email.com");

        RecordedRequest updateEmailRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(updateEmailRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_UPDATE_EMAIL, updateEmailRequest.getPath());
        JSONObject requestJson = new JSONObject(updateEmailRequest.getBody().readUtf8());
        assertEquals("test@email.com", requestJson.getString(IterableConstants.KEY_CURRENT_EMAIL));
        assertEquals("new@email.com", requestJson.getString(IterableConstants.KEY_NEW_EMAIL));
    }

    @Test
    public void testUpdateEmailWithUserId() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey");
        IterableApi.getInstance().setUserId("testUserId");
        IterableApi.getInstance().updateEmail("new@email.com");

        RecordedRequest updateEmailRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(updateEmailRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_UPDATE_EMAIL, updateEmailRequest.getPath());
        JSONObject requestJson = new JSONObject(updateEmailRequest.getBody().readUtf8());
        assertEquals("testUserId", requestJson.getString(IterableConstants.KEY_CURRENT_USERID));
        assertEquals("new@email.com", requestJson.getString(IterableConstants.KEY_NEW_EMAIL));
        assertNull(IterableApi.getInstance().getEmail());
        assertEquals("testUserId", IterableApi.getInstance().getUserId());
    }

    @Test
    public void testHandleUniversalLinkRewrite() throws Exception {
        IterableUrlHandler urlHandlerMock = mock(IterableUrlHandler.class);
        when(urlHandlerMock.handleIterableURL(any(Uri.class), any(IterableActionContext.class))).thenReturn(true);
        IterableApi.initialize(RuntimeEnvironment.application, "fake_key", new IterableConfig.Builder().setUrlHandler(urlHandlerMock).build());

        String url = "https://links.iterable.com/api/docs#!/email";
        IterableApi.handleAppLink("http://links.iterable.com/a/60402396fbd5433eb35397b47ab2fb83?_e=joneng%40iterable.com&_m=93125f33ba814b13a882358f8e0852e0");

        ArgumentCaptor<Uri> capturedUri = ArgumentCaptor.forClass(Uri.class);
        ArgumentCaptor<IterableActionContext> capturedActionContext = ArgumentCaptor.forClass(IterableActionContext.class);
        verify(urlHandlerMock, timeout(5000)).handleIterableURL(capturedUri.capture(), capturedActionContext.capture());
        assertEquals(url, capturedUri.getValue().toString());
        assertEquals(IterableActionSource.APP_LINK, capturedActionContext.getValue().source);
        assertTrue(capturedActionContext.getValue().action.isOfType(IterableAction.ACTION_TYPE_OPEN_URL));
        assertEquals(url, capturedActionContext.getValue().action.getData());
    }

    @Test
    public void testSetEmailWithAutomaticPushRegistration() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());

        // Check that setEmail calls registerForPush
        IterableApi.getInstance().setEmail("test@email.com");
        verify(IterableApi.sharedInstance).registerForPush();
        Mockito.reset(IterableApi.sharedInstance);

        // Check that setEmail(null) disables the device
        IterableApi.getInstance().setEmail(null);
        verify(IterableApi.sharedInstance).disablePush();
        Mockito.reset(IterableApi.sharedInstance);
    }

    @Test
    public void testSetEmailWithoutAutomaticPushRegistration() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(false).build());

        // Check that setEmail doesn't call registerForPush or disablePush
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().setEmail(null);
        verify(IterableApi.sharedInstance, never()).registerForPush();
        verify(IterableApi.sharedInstance, never()).disablePush();
        Mockito.reset(IterableApi.sharedInstance);
    }

    @Test
    public void testSetUserIdWithAutomaticPushRegistration() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());

        // Check that setUserId calls registerForPush
        IterableApi.getInstance().setUserId("userId");
        verify(IterableApi.sharedInstance).registerForPush();
        Mockito.reset(IterableApi.sharedInstance);

        // Check that setUserId(null) disables the device
        IterableApi.getInstance().setUserId(null);
        verify(IterableApi.sharedInstance).disablePush();
        Mockito.reset(IterableApi.sharedInstance);
    }

    @Test
    public void testSetUserIdWithoutAutomaticPushRegistration() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(false).build());

        // Check that setEmail calls registerForPush
        IterableApi.getInstance().setUserId("userId");
        IterableApi.getInstance().setUserId(null);
        verify(IterableApi.sharedInstance, never()).registerForPush();
        verify(IterableApi.sharedInstance, never()).disablePush();
        Mockito.reset(IterableApi.sharedInstance);
    }

    @Test
    public void testAutomaticPushRegistrationOnInit() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());
        IterableApi.getInstance().setEmail("test@email.com");

        reInitIterableApi();
        IterableApi.initialize(RuntimeEnvironment.application, "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());
        verify(IterableApi.sharedInstance).registerForPush();
        Mockito.reset(IterableApi.sharedInstance);
    }

    @Test
    public void testPushRegistrationDeviceFields() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().registerDeviceToken("pushIntegration", "token", IterableConstants.MESSAGING_PLATFORM_FIREBASE);
        Thread.sleep(100);  // Since the network request is queued from a background thread, we need to wait
        Robolectric.flushBackgroundThreadScheduler();
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);

        JSONObject requestJson = new JSONObject(request.getBody().readUtf8());
        JSONObject dataFields = requestJson.getJSONObject("device").getJSONObject("dataFields");
        assertNotNull(dataFields.getString("deviceId"));
        assertEquals(UUID.randomUUID().toString().length(), dataFields.getString("deviceId").length());
        assertEquals(PACKAGE_NAME, dataFields.getString("appPackageName"));
        assertEquals("1.2.3", dataFields.getString("appVersion"));
        assertEquals("321", dataFields.getString("appBuild"));
        assertEquals(IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER, dataFields.getString("iterableSdkVersion"));
        assertEquals(true, dataFields.getBoolean("notificationsEnabled"));
    }

    @Test
    public void testPushRegistrationWithUserId() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setUserId("testUserId");
        IterableApi.getInstance().registerDeviceToken("pushIntegration", "token", IterableConstants.MESSAGING_PLATFORM_FIREBASE);
        Thread.sleep(1000);  // Since the network request is queued from a background thread, we need to wait
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest registerDeviceRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(registerDeviceRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, registerDeviceRequest.getPath());
        JSONObject requestJson = new JSONObject(registerDeviceRequest.getBody().readUtf8());
        assertEquals(requestJson.getBoolean(IterableConstants.KEY_PREFER_USER_ID), true);
    }

    @Test
    public void testUpdateUserWithUserId() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setUserId("testUserId");
        IterableApi.getInstance().updateUser(new JSONObject("{\"key\": \"value\"}"));
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest updateUserRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(updateUserRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_UPDATE_USER, updateUserRequest.getPath());
        JSONObject requestJson = new JSONObject(updateUserRequest.getBody().readUtf8());
        assertEquals(true, requestJson.getBoolean(IterableConstants.KEY_PREFER_USER_ID));
        assertEquals("value", requestJson.getJSONObject(IterableConstants.KEY_DATA_FIELDS).getString("key"));
    }

    @Test
    public void testGetInAppMessages() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableHelper.IterableActionHandler handlerMock = mock(IterableHelper.IterableActionHandler.class);

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().getInAppMessages(10, handlerMock);
        Robolectric.flushBackgroundThreadScheduler();

        verify(handlerMock).execute(eq("{}"));

        RecordedRequest getInAppMessagesRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(getInAppMessagesRequest);
        Uri uri = Uri.parse(getInAppMessagesRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_GET_INAPP_MESSAGES, uri.getPath());
        assertEquals("10", uri.getQueryParameter(IterableConstants.ITERABLE_IN_APP_COUNT));
        assertEquals(IterableConstants.ITBL_PLATFORM_ANDROID, uri.getQueryParameter(IterableConstants.KEY_PLATFORM));
        assertEquals(IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER, uri.getQueryParameter(IterableConstants.ITBL_KEY_SDK_VERSION));
        assertEquals(RuntimeEnvironment.application.getPackageName(), uri.getQueryParameter(IterableConstants.KEY_PACKAGE_NAME));
    }

    @Test
    public void testInAppOpen() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppOpen("testMessageId");
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppOpenRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppOpenRequest);
        Uri uri = Uri.parse(trackInAppOpenRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppOpenRequest.getBody().readUtf8());
        assertEquals("testMessageId", requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
    }

    @Test
    public void testInAppOpenExtended() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppOpen(message, IterableInAppLocation.IN_APP);
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppOpenRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppOpenRequest);
        Uri uri = Uri.parse(trackInAppOpenRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppOpenRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        verifyMessageContext(requestJson);
    }

    @Test
    public void testInAppClick() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppClick("testMessageId", "https://www.google.com");
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppClickRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppClickRequest);
        Uri uri = Uri.parse(trackInAppClickRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppClickRequest.getBody().readUtf8());
        assertEquals("testMessageId", requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals("https://www.google.com", requestJson.getString(IterableConstants.ITERABLE_IN_APP_CLICKED_URL));
    }

    @Test
    public void testInAppClose() throws Exception{
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppClose(message, "https://www.google.com", IterableInAppCloseAction.BACK, null);
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppCloseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppCloseRequest);
        Uri uri = Uri.parse(trackInAppCloseRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppCloseRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals("https://www.google.com", requestJson.getString(IterableConstants.ITERABLE_IN_APP_CLICKED_URL));
        assertEquals("back", requestJson.getString(IterableConstants.ITERABLE_IN_APP_CLOSE_ACTION));
    }

    @Test
    public void testInAppClickExtended() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppClick(message, "https://www.google.com", IterableInAppLocation.IN_APP);
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppClickRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppClickRequest);
        Uri uri = Uri.parse(trackInAppClickRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppClickRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals("https://www.google.com", requestJson.getString(IterableConstants.ITERABLE_IN_APP_CLICKED_URL));
        verifyMessageContext(requestJson);
    }

    @Test
    public void testInAppDelivery() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppDelivery(message);
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppClickRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppClickRequest);
        Uri uri = Uri.parse(trackInAppClickRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppClickRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        verifyMessageContext(requestJson);
    }

    @Test
    public void testInboxSession() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");

        // Set up test data
        Date sessionStartTime = new Date();
        List<IterableInboxSession.Impression> impressions = new ArrayList<>();
        impressions.add(new IterableInboxSession.Impression(
                "messageId1",
                true,
                2,
                5.5f
        ));
        impressions.add(new IterableInboxSession.Impression(
                "messageId2",
                false,
                1,
                2.0f
        ));
        IterableInboxSession session = new IterableInboxSession(
                sessionStartTime,
                new Date(sessionStartTime.getTime() + 3600),
                10,
                5,
                8,
                3,
                impressions);
        IterableApi.getInstance().trackInboxSession(session);
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppClickRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppClickRequest);
        Uri uri = Uri.parse(trackInAppClickRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INBOX_SESSION, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppClickRequest.getBody().readUtf8());

        // Check top-level fields
        assertEquals(sessionStartTime.getTime(), requestJson.getLong(IterableConstants.ITERABLE_INBOX_SESSION_START));
        assertEquals(sessionStartTime.getTime() + 3600, requestJson.getLong(IterableConstants.ITERABLE_INBOX_SESSION_END));
        assertEquals(10, requestJson.getInt(IterableConstants.ITERABLE_INBOX_START_TOTAL_MESSAGE_COUNT));
        assertEquals(5, requestJson.getInt(IterableConstants.ITERABLE_INBOX_START_UNREAD_MESSAGE_COUNT));
        assertEquals(8, requestJson.getInt(IterableConstants.ITERABLE_INBOX_END_TOTAL_MESSAGE_COUNT));
        assertEquals(3, requestJson.getInt(IterableConstants.ITERABLE_INBOX_END_UNREAD_MESSAGE_COUNT));
        verifyDeviceInfo(requestJson.getJSONObject(IterableConstants.KEY_DEVICE_INFO));

        // Check impression data
        JSONArray impressionsJsonArray = requestJson.getJSONArray(IterableConstants.ITERABLE_INBOX_IMPRESSIONS);
        assertEquals(2, impressionsJsonArray.length());
        assertEquals("messageId1", impressionsJsonArray.getJSONObject(0).getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals(true, impressionsJsonArray.getJSONObject(0).getBoolean(IterableConstants.ITERABLE_IN_APP_SILENT_INBOX));
        assertEquals(2, impressionsJsonArray.getJSONObject(0).getInt(IterableConstants.ITERABLE_INBOX_IMP_DISPLAY_COUNT));
        assertEquals(5.5, impressionsJsonArray.getJSONObject(0).getDouble(IterableConstants.ITERABLE_INBOX_IMP_DISPLAY_DURATION));
    }

    private void verifyMessageContext(JSONObject requestJson) throws JSONException {
        JSONObject messageContext = requestJson.getJSONObject(IterableConstants.KEY_MESSAGE_CONTEXT);
        assertNotNull(messageContext);
        assertEquals(false, messageContext.getBoolean(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX));
        assertEquals(false, messageContext.getBoolean(IterableConstants.ITERABLE_IN_APP_SILENT_INBOX));
        JSONObject deviceInfo = messageContext.getJSONObject(IterableConstants.KEY_DEVICE_INFO);
        verifyDeviceInfo(deviceInfo);
    }

    private void verifyDeviceInfo(JSONObject deviceInfo) throws JSONException {
        assertNotNull(deviceInfo);
        assertNotNull(deviceInfo.getString(IterableConstants.DEVICE_ID));
        assertEquals(IterableConstants.ITBL_PLATFORM_ANDROID, deviceInfo.getString(IterableConstants.KEY_PLATFORM));
        assertEquals(PACKAGE_NAME, deviceInfo.getString(IterableConstants.DEVICE_APP_PACKAGE_NAME));
    }

    @Test
    public void testTrackInAppDelete() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().inAppConsume(message, IterableInAppDeleteActionType.INBOX_SWIPE, null);
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppCloseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppCloseRequest);
        Uri uri = Uri.parse(trackInAppCloseRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_INAPP_CONSUME, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppCloseRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals("inbox-swipe", requestJson.getString(IterableConstants.ITERABLE_IN_APP_DELETE_ACTION));

    }

    @Test
    public void testTrackInAppDeleteWithNullParameters() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().inAppConsume(message, null, null);
        Robolectric.flushBackgroundThreadScheduler();

        RecordedRequest trackInAppCloseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppCloseRequest);
        Uri uri = Uri.parse(trackInAppCloseRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_INAPP_CONSUME, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppCloseRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        Log.d("msg", requestJson.toString());
    }

}
