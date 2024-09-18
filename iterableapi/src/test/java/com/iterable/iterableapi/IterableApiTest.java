package com.iterable.iterableapi;

import com.iterable.iterableapi.util.DeviceInfoUtils;
import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static android.os.Looper.getMainLooper;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class IterableApiTest extends BaseTest {

    public static final String PACKAGE_NAME = "com.iterable.iterableapi.test";
    private MockWebServer server;
    private IterableApiClient originalApiClient;
    private IterableApiClient mockApiClient;
    private IterablePushRegistration.IterablePushRegistrationImpl originalPushRegistrationImpl;

    @Before
    public void setUp() {
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        reInitIterableApi();

        originalPushRegistrationImpl = IterablePushRegistration.instance;
        IterablePushRegistration.instance = mock(IterablePushRegistration.IterablePushRegistrationImpl.class);
    }

    @After
    public void tearDown() throws IOException {
        IterablePushRegistration.instance = originalPushRegistrationImpl;

        server.shutdown();
        server = null;
    }

    private void reInitIterableApi() {
        IterableInAppManager inAppManagerMock = mock(IterableInAppManager.class);
        IterableEmbeddedManager embeddedManagerMock = mock(IterableEmbeddedManager.class);

        IterableApi.sharedInstance = new IterableApi(inAppManagerMock, embeddedManagerMock);

        originalApiClient = IterableApi.sharedInstance.apiClient;
        mockApiClient = spy(originalApiClient);
        IterableApi.sharedInstance.apiClient = mockApiClient;
    }

    @Test
    public void testSdkInitializedWithoutEmailOrUserId() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");
        clearInvocations(mockApiClient);

        IterableApi.getInstance().setEmail(null);
        // Verify that none of the calls to the API result in a request
        IterableApi.getInstance().track("testEvent");
        IterableApi.getInstance().trackInAppOpen("12345");
        IterableApi.getInstance().inAppConsume("12345");
        IterableApi.getInstance().trackInAppClick("12345", "");
        IterableApi.getInstance().registerDeviceToken("12345");
        IterableApi.getInstance().disablePush();
        IterableApi.getInstance().updateUser(new JSONObject());
        IterableApi.getInstance().updateEmail("");
        IterableApi.getInstance().trackPurchase(10.0, new ArrayList<CommerceItem>());
        verifyNoMoreInteractions(mockApiClient);
    }

    @Test
    public void testEmailUserIdPersistence() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");
        IterableApi.getInstance().setEmail("test@email.com");

        reInitIterableApi();
        IterableApi.initialize(getContext(), "apiKey");
        assertEquals("test@email.com", IterableApi.getInstance().getEmail());
        assertNull(IterableApi.getInstance().getUserId());

        IterableApi.getInstance().setUserId("testUserId");
        reInitIterableApi();
        IterableApi.initialize(getContext(), "apiKey");
        assertEquals("testUserId", IterableApi.getInstance().getUserId());
        assertNull(IterableApi.getInstance().getEmail());
    }

    @Test
    public void testAttributionInfoPersistence() throws Exception {
        IterableApi.initialize(getContext(), "apiKey");

        IterableAttributionInfo attributionInfo = new IterableAttributionInfo(1234, 4321, "message");
        IterableApi.getInstance().setAttributionInfo(attributionInfo);

        // 23 hours, not expired, still present
        doReturn(System.currentTimeMillis() + 3600 * 23 * 1000).when(utilsRule.iterableUtilSpy).currentTimeMillis();
        IterableAttributionInfo storedAttributionInfo = IterableApi.getInstance().getAttributionInfo();
        assertNotNull(storedAttributionInfo);
        assertEquals(attributionInfo.campaignId, storedAttributionInfo.campaignId);
        assertEquals(attributionInfo.templateId, storedAttributionInfo.templateId);
        assertEquals(attributionInfo.messageId, storedAttributionInfo.messageId);

        // 24 hours, expired, attributionInfo should be null
        doReturn(System.currentTimeMillis() + 3600 * 24 * 1000).when(utilsRule.iterableUtilSpy).currentTimeMillis();
        storedAttributionInfo = IterableApi.getInstance().getAttributionInfo();
        assertNull(storedAttributionInfo);
    }

    @Test
    public void testUpdateEmailPersistence() throws Exception {
        String oldEmail = "test@email.com";
        String newEmail = "new@email.com";

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(getContext(), "apiKey");
        IterableApi.getInstance().setEmail(oldEmail);
        assertEquals(oldEmail, IterableApi.getInstance().getEmail());

        IterableApi.getInstance().updateEmail(newEmail);
        shadowOf(getMainLooper()).idle();
        verify(mockApiClient).updateEmail(eq(newEmail), nullable(IterableHelper.SuccessHandler.class), nullable(IterableHelper.FailureHandler.class));
        server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("new@email.com", IterableApi.getInstance().getEmail());

        reInitIterableApi();
        IterableApi.initialize(getContext(), "apiKey");
        assertEquals("new@email.com", IterableApi.getInstance().getEmail());
    }

    @Test
    public void testSetEmailWithCallback() {
        IterableApi.initialize(getContext(), "apiKey");

        String email = "test@example.com";
        IterableApi.getInstance().setEmail(email, null, null, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                assertTrue(true); // callback should be called with success
            }
        }, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertTrue(false); // callback should be called with failure
            }
        });
    }

    @Test
    public void testSetUserIdWithCallback() {
        IterableApi.initialize(getContext(), "apiKey");

        String userId = "test_user_id";
        IterableApi.getInstance().setUserId(userId, new IterableHelper.SuccessHandler() {
            @Override
            public void onSuccess(@NonNull JSONObject data) {
                assertTrue(true); // callback should be called with success
            }
        }, new IterableHelper.FailureHandler() {
            @Override
            public void onFailure(@NonNull String reason, @Nullable JSONObject data) {
                assertTrue(false); // callback should be called with failure
            }
        });
    }

    @Test
    public void testUpdateEmailWithOldEmail() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(getContext(), "apiKey");
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
        IterableApi.initialize(getContext(), "apiKey");
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

    @Ignore
    @Test
    public void testHandleUniversalLinkRewrite() throws Exception {
        IterableUrlHandler urlHandlerMock = mock(IterableUrlHandler.class);
        when(urlHandlerMock.handleIterableURL(any(Uri.class), any(IterableActionContext.class))).thenReturn(true);
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setUrlHandler(urlHandlerMock).build());

        String url = "https://iterable.com";
        IterableApi.getInstance().handleAppLink(
                "https://links.iterable.com/a/60402396fbd5433eb35397b47ab2fb83?_e=joneng%40iterable.com&_m=93125f33ba814b13a882358f8e0852e0");

        ArgumentCaptor<Uri> capturedUri = ArgumentCaptor.forClass(Uri.class);
        ArgumentCaptor<IterableActionContext> capturedActionContext = ArgumentCaptor.forClass(IterableActionContext.class);
        shadowOf(getMainLooper()).idle();
        verify(urlHandlerMock, timeout(5000)).handleIterableURL(capturedUri.capture(), capturedActionContext.capture());
        assertEquals(url, capturedUri.getValue().toString());
        assertEquals(IterableActionSource.APP_LINK, capturedActionContext.getValue().source);
        assertTrue(capturedActionContext.getValue().action.isOfType(IterableAction.ACTION_TYPE_OPEN_URL));
        assertEquals(url, capturedActionContext.getValue().action.getData());
    }

    @Test
    public void testSetEmailWithAutomaticPushRegistration() throws Exception {
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());

        // Check that setEmail calls registerForPush
        IterableApi.getInstance().setEmail("test@email.com");
        ArgumentCaptor<IterablePushRegistrationData> capturedPushRegistrationData = ArgumentCaptor.forClass(IterablePushRegistrationData.class);
        verify(IterablePushRegistration.instance).executePushRegistrationTask(capturedPushRegistrationData.capture());
        assertEquals(IterablePushRegistrationData.PushRegistrationAction.ENABLE, capturedPushRegistrationData.getValue().pushRegistrationAction);
        Mockito.reset(IterablePushRegistration.instance);

        // Check that setEmail(null) disables the device
        IterableApi.getInstance().setEmail(null);
        capturedPushRegistrationData = ArgumentCaptor.forClass(IterablePushRegistrationData.class);
        verify(IterablePushRegistration.instance).executePushRegistrationTask(capturedPushRegistrationData.capture());
        assertEquals(IterablePushRegistrationData.PushRegistrationAction.DISABLE, capturedPushRegistrationData.getValue().pushRegistrationAction);
    }

    @Test
    public void testSetEmailWithoutAutomaticPushRegistration() throws Exception {
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(false).build());

        // Check that setEmail doesn't call registerForPush or disablePush
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().setEmail(null);
        verify(IterablePushRegistration.instance, never()).executePushRegistrationTask(any(IterablePushRegistrationData.class));
    }

    @Test
    public void testSetUserIdWithAutomaticPushRegistration() throws Exception {
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());

        // Check that setUserId calls registerForPush
        IterableApi.getInstance().setUserId("userId");
        ArgumentCaptor<IterablePushRegistrationData> capturedPushRegistrationData = ArgumentCaptor.forClass(IterablePushRegistrationData.class);
        verify(IterablePushRegistration.instance).executePushRegistrationTask(capturedPushRegistrationData.capture());
        assertEquals(IterablePushRegistrationData.PushRegistrationAction.ENABLE, capturedPushRegistrationData.getValue().pushRegistrationAction);
        Mockito.reset(IterablePushRegistration.instance);

        // Check that setUserId(null) disables the device
        IterableApi.getInstance().setUserId(null);
        capturedPushRegistrationData = ArgumentCaptor.forClass(IterablePushRegistrationData.class);
        verify(IterablePushRegistration.instance).executePushRegistrationTask(capturedPushRegistrationData.capture());
        assertEquals(IterablePushRegistrationData.PushRegistrationAction.DISABLE, capturedPushRegistrationData.getValue().pushRegistrationAction);
    }

    @Test
    public void testSetUserIdWithoutAutomaticPushRegistration() throws Exception {
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(false).build());

        // Check that setEmail calls registerForPush
        IterableApi.getInstance().setUserId("userId");
        IterableApi.getInstance().setUserId(null);
        verify(IterablePushRegistration.instance, never()).executePushRegistrationTask(any(IterablePushRegistrationData.class));
    }

    @Test
    public void testNoAutomaticPushRegistrationOnInit() throws Exception {
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());
        IterableApi.getInstance().setEmail("test@email.com");
        Mockito.reset(IterablePushRegistration.instance);

        reInitIterableApi();
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());
        verify(IterablePushRegistration.instance, never()).executePushRegistrationTask(any(IterablePushRegistrationData.class));
    }

    @Test
    public void testAutomaticPushRegistrationOnInitAndForeground() throws Exception {
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());
        IterableApi.getInstance().setEmail("test@email.com");
        Mockito.reset(IterablePushRegistration.instance);

        reInitIterableApi();
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class).create().start().resume();

        ArgumentCaptor<IterablePushRegistrationData> capturedPushRegistrationData = ArgumentCaptor.forClass(IterablePushRegistrationData.class);
        verify(IterablePushRegistration.instance).executePushRegistrationTask(capturedPushRegistrationData.capture());
        assertEquals(IterablePushRegistrationData.PushRegistrationAction.ENABLE, capturedPushRegistrationData.getValue().pushRegistrationAction);

        activityController.pause().stop().destroy();
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
    }

    @Test
    public void testPushRegistrationDeviceFields() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().registerDeviceToken("token");
        Thread.sleep(100);  // Since the network request is queued from a background thread, we need to wait
        shadowOf(getMainLooper()).idle();
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

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setUserId("testUserId");
        IterableApi.getInstance().registerDeviceToken("token");
        Thread.sleep(1000);  // Since the network request is queued from a background thread, we need to wait
        shadowOf(getMainLooper()).idle();

        RecordedRequest registerDeviceRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(registerDeviceRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_REGISTER_DEVICE_TOKEN, registerDeviceRequest.getPath());
        JSONObject requestJson = new JSONObject(registerDeviceRequest.getBody().readUtf8());
        assertEquals(requestJson.getBoolean(IterableConstants.KEY_PREFER_USER_ID), true);
    }

    @Test
    public void testInAppResetOnLogout() throws Exception {
        IterableApi.initialize(getContext(), "fake_key", new IterableConfig.Builder().setPushIntegrationName("pushIntegration").setAutoPushRegistration(true).build());

        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().setEmail(null);
        verify(IterableApi.sharedInstance.getInAppManager(), times(2)).reset();
    }

    @Ignore("Ignoring this test as it fails on CI for some reason")
    @Test
    public void databaseClearOnLogout() throws Exception {
        IterableTaskStorage taskStorage = IterableTaskStorage.sharedInstance(getContext());
        taskStorage.createTask("Test", IterableTaskType.API, "data");
        assertFalse(taskStorage.getAllTaskIds().isEmpty());
        IterableApi.sharedInstance.apiClient.setOfflineProcessingEnabled(true);
        IterableApi.sharedInstance.setEmail("test@email.com");
        assertTrue(taskStorage.getAllTaskIds().isEmpty());
    }

    @Test
    public void testUpdateUserWithUserId() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setUserId("testUserId");
        IterableApi.getInstance().updateUser(new JSONObject("{\"key\": \"value\"}"));
        shadowOf(getMainLooper()).idle();

        RecordedRequest updateUserRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(updateUserRequest);
        assertEquals("/" + IterableConstants.ENDPOINT_UPDATE_USER, updateUserRequest.getPath());
        JSONObject requestJson = new JSONObject(updateUserRequest.getBody().readUtf8());
        assertEquals(true, requestJson.getBoolean(IterableConstants.KEY_PREFER_USER_ID));
        assertEquals("value", requestJson.getJSONObject(IterableConstants.KEY_DATA_FIELDS).getString("key"));
        assertEquals(false, requestJson.getBoolean(IterableConstants.KEY_MERGE_NESTED_OBJECTS));
    }

    @Test
    public void testGetInAppMessages() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableHelper.IterableActionHandler handlerMock = mock(IterableHelper.IterableActionHandler.class);

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().getInAppMessages(10, handlerMock);
        shadowOf(getMainLooper()).idle();

        verify(handlerMock).execute(eq("{}"));

        RecordedRequest getInAppMessagesRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(getInAppMessagesRequest);
        Uri uri = Uri.parse(getInAppMessagesRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_GET_INAPP_MESSAGES, uri.getPath());
        assertEquals("10", uri.getQueryParameter(IterableConstants.ITERABLE_IN_APP_COUNT));
        assertEquals(DeviceInfoUtils.isFireTV(getContext().getPackageManager()) ? IterableConstants.ITBL_PLATFORM_OTT : IterableConstants.ITBL_PLATFORM_ANDROID, uri.getQueryParameter(IterableConstants.KEY_PLATFORM));

        assertEquals(IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER, uri.getQueryParameter(IterableConstants.ITBL_KEY_SDK_VERSION));
        assertNotNull(uri.getQueryParameter(IterableConstants.ITBL_SYSTEM_VERSION));
        assertEquals(getContext().getPackageName(), uri.getQueryParameter(IterableConstants.KEY_PACKAGE_NAME));
    }

    @Test
    public void testInAppOpen() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppOpen("testMessageId");
        shadowOf(getMainLooper()).idle();

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

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().setInboxSessionId("SomeRandomSessionID");
        IterableApi.getInstance().trackInAppOpen(message, IterableInAppLocation.IN_APP);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackInAppOpenRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppOpenRequest);
        Uri uri = Uri.parse(trackInAppOpenRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_OPEN, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppOpenRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertNull(requestJson.optString(IterableConstants.KEY_INBOX_SESSION_ID, null));
        verifyMessageContext(requestJson);
        verifyDeviceInfo(requestJson);
    }

    @Test
    public void testInAppClick() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppClick("testMessageId", "https://www.google.com");
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackInAppClickRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppClickRequest);
        Uri uri = Uri.parse(trackInAppClickRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppClickRequest.getBody().readUtf8());
        assertEquals("testMessageId", requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals("https://www.google.com", requestJson.getString(IterableConstants.ITERABLE_IN_APP_CLICKED_URL));
    }

    @Test
    public void testInAppClose() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(getContext(), "apiKey",
                new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");

        IterableApi.getInstance().setInboxSessionId("SomeRandomSessionID");
        IterableApi.getInstance()
                .trackInAppClose(message, "https://www.google.com", IterableInAppCloseAction.BACK, IterableInAppLocation.IN_APP);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackInAppCloseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppCloseRequest);
        Uri uri = Uri.parse(trackInAppCloseRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_CLOSE, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppCloseRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals("https://www.google.com", requestJson.getString(IterableConstants.ITERABLE_IN_APP_CLICKED_URL));
        assertEquals("back", requestJson.getString(IterableConstants.ITERABLE_IN_APP_CLOSE_ACTION));
        assertNull(requestJson.optString(IterableConstants.KEY_INBOX_SESSION_ID, null));
        assertEquals(IterableInAppLocation.IN_APP.toString(), requestJson.optJSONObject(IterableConstants.KEY_MESSAGE_CONTEXT)
                .optString(IterableConstants.ITERABLE_IN_APP_LOCATION));

        verifyMessageContext(requestJson);
        verifyDeviceInfo(requestJson);

        //Making another request to check if inbox location is tracked
        IterableApi.getInstance()
                .trackInAppClose(message, "https://www.google.com", IterableInAppCloseAction.BACK, IterableInAppLocation.INBOX);
        shadowOf(getMainLooper()).idle();
        trackInAppCloseRequest = server.takeRequest(1, TimeUnit.SECONDS);
        requestJson = new JSONObject(trackInAppCloseRequest.getBody().readUtf8());
        assertEquals(IterableInAppLocation.INBOX.toString(), requestJson.optJSONObject(IterableConstants.KEY_MESSAGE_CONTEXT)
                .optString(IterableConstants.ITERABLE_IN_APP_LOCATION));
    }

    @Test
    public void testInAppClickExtended() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");

        IterableApi.getInstance().setInboxSessionId("SomeRandomSessionID");
        IterableApi.getInstance().trackInAppClick(message, "https://www.google.com", IterableInAppLocation.IN_APP);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackInAppClickRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppClickRequest);
        Uri uri = Uri.parse(trackInAppClickRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_CLICK, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppClickRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals("https://www.google.com", requestJson.getString(IterableConstants.ITERABLE_IN_APP_CLICKED_URL));
        assertNull(requestJson.optString(IterableConstants.KEY_INBOX_SESSION_ID, null));
        verifyMessageContext(requestJson);
        verifyDeviceInfo(requestJson);
    }

    @Test
    public void testEmbeddedClick() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableEmbeddedMessage message = EmbeddedTestUtils.getTestEmbeddedMessage();

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackEmbeddedClick(message, message.getElements().getButtons().get(0).getId(), message.getElements().getButtons().get(0).getAction().getData());
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackEmbeddedClickRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackEmbeddedClickRequest);
        Uri uri = Uri.parse(trackEmbeddedClickRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_EMBEDDED_CLICK, uri.getPath());
        JSONObject requestJson = new JSONObject(trackEmbeddedClickRequest.getBody().readUtf8());
        assertEquals(message.getMetadata().getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals(message.getElements().getButtons().get(0).getId(), requestJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_IDENTIFIER));
        assertEquals(message.getElements().getButtons().get(0).getAction().getData(), requestJson.getString(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_BUTTON_TARGET_URL));
        verifyDeviceInfo(requestJson);
    }

    @Test
    public void testInAppDelivery() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackInAppDelivery(message);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackInAppClickRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppClickRequest);
        Uri uri = Uri.parse(trackInAppClickRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INAPP_DELIVERY, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppClickRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        verifyMessageContext(requestJson);
        verifyDeviceInfo(requestJson);
    }

    @Test
    public void testEmbeddedMessageReceived() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableEmbeddedMessage message = EmbeddedTestUtils.getTestEmbeddedMessage();

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().trackEmbeddedMessageReceived(message);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackEmbeddedDeliveredRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackEmbeddedDeliveredRequest);
        Uri uri = Uri.parse(trackEmbeddedDeliveredRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_EMBEDDED_RECEIVED, uri.getPath());
        JSONObject requestJson = new JSONObject(trackEmbeddedDeliveredRequest.getBody().readUtf8());
        assertEquals(message.getMetadata().getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        verifyDeviceInfo(requestJson);
    }

    @Test
    public void testInboxSession() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
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
        IterableApi.getInstance().setInboxSessionId(session.sessionId);
        IterableApi.getInstance().trackInboxSession(session);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackInboxSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInboxSessionRequest);
        Uri uri = Uri.parse(trackInboxSessionRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_INBOX_SESSION, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInboxSessionRequest.getBody().readUtf8());

        // Check top-level fields
        assertEquals(sessionStartTime.getTime(), requestJson.getLong(IterableConstants.ITERABLE_INBOX_SESSION_START));
        assertEquals(sessionStartTime.getTime() + 3600, requestJson.getLong(IterableConstants.ITERABLE_INBOX_SESSION_END));
        assertEquals(10, requestJson.getInt(IterableConstants.ITERABLE_INBOX_START_TOTAL_MESSAGE_COUNT));
        assertEquals(5, requestJson.getInt(IterableConstants.ITERABLE_INBOX_START_UNREAD_MESSAGE_COUNT));
        assertEquals(8, requestJson.getInt(IterableConstants.ITERABLE_INBOX_END_TOTAL_MESSAGE_COUNT));
        assertEquals(3, requestJson.getInt(IterableConstants.ITERABLE_INBOX_END_UNREAD_MESSAGE_COUNT));
        assertEquals(session.sessionId, requestJson.getString(IterableConstants.KEY_INBOX_SESSION_ID));
        verifyDeviceInfo(requestJson);

        // Check impression data
        JSONArray impressionsJsonArray = requestJson.getJSONArray(IterableConstants.ITERABLE_INBOX_IMPRESSIONS);
        assertEquals(2, impressionsJsonArray.length());
        assertEquals("messageId1", impressionsJsonArray.getJSONObject(0).getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals(true, impressionsJsonArray.getJSONObject(0).getBoolean(IterableConstants.ITERABLE_IN_APP_SILENT_INBOX));
        assertEquals(2, impressionsJsonArray.getJSONObject(0).getInt(IterableConstants.ITERABLE_INBOX_IMP_DISPLAY_COUNT));
        assertEquals(5.5, impressionsJsonArray.getJSONObject(0).getDouble(IterableConstants.ITERABLE_INBOX_IMP_DISPLAY_DURATION));
    }

    @Test
    public void testEmbeddedSession() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");

        // Set up test data
        Date sessionStartTime = new Date();
        List<IterableEmbeddedImpression> impressions = new ArrayList<>();
        impressions.add(new IterableEmbeddedImpression(
                "messageId1",
                0,
                1,
                2.0f
        ));
        impressions.add(new IterableEmbeddedImpression(
                "messageId2",
                0,
                3,
                6.5f
        ));

        IterableEmbeddedSession session = new IterableEmbeddedSession(
                sessionStartTime,
                new Date(sessionStartTime.getTime() + 3600),
                impressions);

        IterableApi.getInstance().trackEmbeddedSession(session);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackEmbeddedSessionRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackEmbeddedSessionRequest);
        Uri uri = Uri.parse(trackEmbeddedSessionRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_TRACK_EMBEDDED_SESSION, uri.getPath());
        JSONObject requestJson = new JSONObject(trackEmbeddedSessionRequest.getBody().readUtf8());

        // Check top-level fields
        verifyDeviceInfo(requestJson);

        // Check session data
        JSONObject sessionJson = requestJson.getJSONObject(IterableConstants.ITERABLE_EMBEDDED_SESSION);
        assertEquals(session.getId(), sessionJson.getString(IterableConstants.KEY_EMBEDDED_SESSION_ID));
        assertEquals(sessionStartTime.getTime(), sessionJson.getLong(IterableConstants.ITERABLE_EMBEDDED_SESSION_START));
        assertEquals(sessionStartTime.getTime() + 3600, sessionJson.getLong(IterableConstants.ITERABLE_EMBEDDED_SESSION_END));

        // Check impression data
        JSONArray impressionsJsonArray = requestJson.getJSONArray(IterableConstants.ITERABLE_EMBEDDED_IMPRESSIONS);
        assertEquals(2, impressionsJsonArray.length());
        assertEquals("messageId1", impressionsJsonArray.getJSONObject(0).getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals(0, impressionsJsonArray.getJSONObject(0).getLong(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENT_ID));
        assertEquals(1, impressionsJsonArray.getJSONObject(0).getInt(IterableConstants.ITERABLE_EMBEDDED_IMP_DISPLAY_COUNT));
        assertEquals(2.0, impressionsJsonArray.getJSONObject(0).getDouble(IterableConstants.ITERABLE_EMBEDDED_IMP_DISPLAY_DURATION));
    }

    private void verifyMessageContext(JSONObject requestJson) throws JSONException {
        JSONObject messageContext = requestJson.getJSONObject(IterableConstants.KEY_MESSAGE_CONTEXT);
        assertNotNull(messageContext);
        assertEquals(false, messageContext.getBoolean(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX));
        assertEquals(false, messageContext.getBoolean(IterableConstants.ITERABLE_IN_APP_SILENT_INBOX));
    }

    private void verifyDeviceInfo(JSONObject requestJson) throws JSONException {
        JSONObject deviceInfo = requestJson.getJSONObject(IterableConstants.KEY_DEVICE_INFO);
        assertNotNull(deviceInfo);
        assertNotNull(deviceInfo.getString(IterableConstants.DEVICE_ID));
        assertEquals(IterableConstants.ITBL_PLATFORM_ANDROID, deviceInfo.getString(IterableConstants.KEY_PLATFORM));
        assertEquals(PACKAGE_NAME, deviceInfo.getString(IterableConstants.DEVICE_APP_PACKAGE_NAME));
    }

    @Test
    public void testTrackInAppDelete() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");

        //Explicitly updating sessionId in IterableAPI as it is done when IterableInboxFragment initializes session manager
        IterableApi.getInstance().setInboxSessionId("SomeRandomSessionID");
        IterableApi.getInstance().inAppConsume(message, IterableInAppDeleteActionType.INBOX_SWIPE, IterableInAppLocation.INBOX);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackInAppConsumeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppConsumeRequest);
        Uri uri = Uri.parse(trackInAppConsumeRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_INAPP_CONSUME, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppConsumeRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertEquals("inbox-swipe", requestJson.getString(IterableConstants.ITERABLE_IN_APP_DELETE_ACTION));
        assertEquals("SomeRandomSessionID", requestJson.getString(IterableConstants.KEY_INBOX_SESSION_ID));
        verifyMessageContext(requestJson);
        verifyDeviceInfo(requestJson);
    }

    @Test
    public void testTrackInAppDeleteWithNullParameters() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        IterableInAppMessage message = InAppTestUtils.getTestInAppMessage();
        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        IterableApi.getInstance().setEmail("test@email.com");
        IterableApi.getInstance().setInboxSessionId("SomeRandomSessionID");
        IterableApi.getInstance().inAppConsume(message, null, null);
        shadowOf(getMainLooper()).idle();

        RecordedRequest trackInAppConsumeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppConsumeRequest);
        Uri uri = Uri.parse(trackInAppConsumeRequest.getRequestUrl().toString());
        assertEquals("/" + IterableConstants.ENDPOINT_INAPP_CONSUME, uri.getPath());
        JSONObject requestJson = new JSONObject(trackInAppConsumeRequest.getBody().readUtf8());
        assertEquals(message.getMessageId(), requestJson.getString(IterableConstants.KEY_MESSAGE_ID));
        assertNull(requestJson.optString(IterableConstants.KEY_INBOX_SESSION_ID, null));
    }


    @Test
    public void testFetchRemoteConfigurationCalledWhenInForeground() throws Exception {

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\n" +
                "            \"offlineMode\": false,\n" +
                "            \"" + IterableConstants.KEY_OFFLINE_MODE + "\": true,\n" +
                "            \"someOtherKey1\": \"someOtherValue1\"\n" +
                "        }"));
        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();

        IterableApi.initialize(getContext(), "apiKey", new IterableConfig.Builder().setAutoPushRegistration(false).build());
        verify(mockApiClient).setOfflineProcessingEnabled(false);
        clearInvocations(mockApiClient);
        Robolectric.buildActivity(Activity.class).create().start().resume();
        shadowOf(getMainLooper()).idle();
        RecordedRequest trackInAppConsumeRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(trackInAppConsumeRequest);
        assertTrue(trackInAppConsumeRequest.getRequestUrl().toString().contains("/getRemoteConfiguration"));
        verify(mockApiClient).setOfflineProcessingEnabled(true);

        IterableActivityMonitor.getInstance().unregisterLifecycleCallbacks(getContext());
        IterableActivityMonitor.instance = new IterableActivityMonitor();
    }

}
