package com.iterable.iterableapi;

import android.content.Context;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import okhttp3.mockwebserver.MockWebServer;

import static com.iterable.iterableapi.IterableTestUtils.stubAnyRequestReturningStatusCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IterablePushRegistrationTest extends BaseTest {

    private static final String TEST_TOKEN = "testToken";
    private static final String NEW_TOKEN = "newToken";
    private static final String OLD_TOKEN = "oldToken";
    private static final String GCM_SENDER_ID = "1234567890";
    public static final String INTEGRATION_NAME = "integrationName";

    private MockWebServer server;
    private IterableApi originalApi;
    private IterableApi apiMock;
    private IterablePushRegistration.Util.UtilImpl originalPushRegistrationUtil;
    private IterablePushRegistration.Util.UtilImpl pushRegistrationUtilMock;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.createIterableApi();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        originalApi = IterableApi.sharedInstance;
        apiMock = spy(IterableApi.sharedInstance);
        IterableApi.sharedInstance = apiMock;

        originalPushRegistrationUtil = IterablePushRegistration.Util.instance;
        pushRegistrationUtilMock = mock(IterablePushRegistration.Util.UtilImpl.class);
        IterablePushRegistration.Util.instance = pushRegistrationUtilMock;

        when(pushRegistrationUtilMock.getFirebaseResouceId(any(Context.class))).thenReturn(1);
    }

    @After
    public void tearDown() throws Exception {
        IterablePushRegistration.Util.instance = originalPushRegistrationUtil;
        IterableApi.sharedInstance = originalApi;

        server.shutdown();
        server = null;
    }

    @Test
    public void testEnableDevice() throws Exception {
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn(TEST_TOKEN);

        IterablePushRegistrationData data = new IterablePushRegistrationData(IterableTestUtils.userEmail, null, INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistration().execute(data);

        verify(apiMock, timeout(100)).registerDeviceToken(eq(IterableTestUtils.userEmail), nullable(String.class), eq(INTEGRATION_NAME), eq(TEST_TOKEN));
        verify(apiMock, never()).disableToken(eq(IterableTestUtils.userEmail), nullable(String.class), any(String.class), nullable(IterableHelper.SuccessHandler.class), nullable(IterableHelper.FailureHandler.class));
    }

    @Test
    public void testDisableDevice() throws Exception {
        stubAnyRequestReturningStatusCode(server, 200, "{}");
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn("testToken");

        IterablePushRegistrationData data = new IterablePushRegistrationData(IterableTestUtils.userEmail, null, INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.DISABLE);
        new IterablePushRegistration().execute(data);
        ShadowApplication.runBackgroundTasks();

        verify(apiMock, timeout(100)).disableToken(eq(IterableTestUtils.userEmail), isNull(String.class), eq(TEST_TOKEN));
    }

    @Test
    public void testDisableOldGcmToken() throws Exception {
        stubAnyRequestReturningStatusCode(server, 200, "{}");
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setLegacyGCMSenderId(GCM_SENDER_ID).build());

        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn(NEW_TOKEN);
        when(pushRegistrationUtilMock.getFirebaseToken(eq(GCM_SENDER_ID), eq(IterableConstants.MESSAGING_PLATFORM_GOOGLE))).thenReturn(OLD_TOKEN);

        IterablePushRegistrationData data = new IterablePushRegistrationData(IterableTestUtils.userEmail, null, INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistration().execute(data);

        ArgumentCaptor<IterableHelper.SuccessHandler> successHandlerCaptor = ArgumentCaptor.forClass(IterableHelper.SuccessHandler.class);
        verify(apiMock).registerDeviceToken(eq(IterableTestUtils.userEmail), isNull(String.class), eq(INTEGRATION_NAME), eq(NEW_TOKEN));
        verify(apiMock, times(1)).disableToken(eq(IterableTestUtils.userEmail), isNull(String.class), eq(OLD_TOKEN), successHandlerCaptor.capture(), nullable(IterableHelper.FailureHandler.class));
        successHandlerCaptor.getValue().onSuccess(new JSONObject());

        reset(apiMock);

        new IterablePushRegistration().execute(data);

        verify(apiMock).registerDeviceToken(eq(IterableTestUtils.userEmail), isNull(String.class), eq(INTEGRATION_NAME), eq(NEW_TOKEN));
        verify(apiMock, never()).disableToken(eq(IterableTestUtils.userEmail), isNull(String.class), any(String.class), nullable(IterableHelper.SuccessHandler.class), nullable(IterableHelper.FailureHandler.class));
    }

}
