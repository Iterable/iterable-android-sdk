package com.iterable.iterableapi;

import android.net.Uri;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

@PrepareForTest({IterableUtil.class, IterableUtil.IterableUtilImpl.class})
public class IterableApiTest extends BasePowerMockTest {

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
        assertEquals("com.iterable.iterableapi.test", dataFields.getString("appPackageName"));
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

}
