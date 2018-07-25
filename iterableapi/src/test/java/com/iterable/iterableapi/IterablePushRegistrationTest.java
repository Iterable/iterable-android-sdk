package com.iterable.iterableapi;

import com.google.firebase.iid.FirebaseInstanceId;
import com.iterable.iterableapi.unit.BaseTest;
import com.iterable.iterableapi.unit.IterableTestUtils;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.MockRepository;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import okhttp3.mockwebserver.MockWebServer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest({IterablePushRegistration.Util.class, FirebaseInstanceId.class})
public class IterablePushRegistrationTest extends BaseTest {

    private static final String TEST_TOKEN = "testToken";
    private static final String NEW_TOKEN = "newToken";
    private static final String OLD_TOKEN = "oldToken";
    private static final String GCM_SENDER_ID = "1234567890";
    public static final String INTEGRATION_NAME = "integrationName";

    private MockWebServer server;
    private IterableApi originalApi;
    private IterableApi apiMock;
    private FirebaseInstanceId mockInstanceId;

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.createIterableApi();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        originalApi = IterableApi.sharedInstance;
        apiMock = spy(IterableApi.sharedInstance);
        IterableApi.sharedInstance = apiMock;

        mockInstanceId = mock(FirebaseInstanceId.class);
        PowerMockito.stub(PowerMockito.method(FirebaseInstanceId.class, "getInstance")).toReturn(mockInstanceId);
        PowerMockito.stub(PowerMockito.method(IterablePushRegistration.Util.class, "getFirebaseResouceId")).toReturn(1);
    }

    @After
    public void tearDown() throws Exception {
        IterableApi.sharedInstance = originalApi;
        MockRepository.remove(FirebaseInstanceId.class);
        MockRepository.remove(IterablePushRegistration.Util.class);

        server.shutdown();
        server = null;
    }

    @Test
    public void testEnableDevice() throws Exception {
        when(mockInstanceId.getToken()).thenReturn(TEST_TOKEN);

        IterablePushRegistrationData data = new IterablePushRegistrationData(INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistration().execute(data);

        verify(apiMock, timeout(100)).registerDeviceToken(eq(INTEGRATION_NAME), eq(TEST_TOKEN));
        verify(apiMock, never()).disableToken(any(String.class), nullable(IterableHelper.SuccessHandler.class), nullable(IterableHelper.FailureHandler.class));
    }

    @Test
    public void testDisableDevice() throws Exception {
        when(mockInstanceId.getToken()).thenReturn("testToken");

        IterablePushRegistrationData data = new IterablePushRegistrationData(INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.DISABLE);
        new IterablePushRegistration().execute(data);

        verify(apiMock, timeout(100)).disableToken(eq(TEST_TOKEN));
    }

    @Test
    public void testDisableOldGcmToken() throws Exception {
        IterableApi.initialize(RuntimeEnvironment.application, "apiKey", new IterableConfig.Builder().setLegacyGCMSenderId(GCM_SENDER_ID).build());

        when(mockInstanceId.getToken()).thenReturn(NEW_TOKEN);
        when(mockInstanceId.getToken(eq(GCM_SENDER_ID), eq(IterableConstants.MESSAGING_PLATFORM_GOOGLE))).thenReturn(OLD_TOKEN);

        IterablePushRegistrationData data = new IterablePushRegistrationData(INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistration().execute(data);
        ShadowApplication.runBackgroundTasks();

        ArgumentCaptor<IterableHelper.SuccessHandler> successHandlerCaptor = ArgumentCaptor.forClass(IterableHelper.SuccessHandler.class);
        verify(apiMock).registerDeviceToken(eq(INTEGRATION_NAME), eq(NEW_TOKEN));
        verify(apiMock, times(1)).disableToken(eq(OLD_TOKEN), successHandlerCaptor.capture(), nullable(IterableHelper.FailureHandler.class));
        successHandlerCaptor.getValue().onSuccess(new JSONObject());

        reset(apiMock);

        new IterablePushRegistration().execute(data);
        verify(apiMock).registerDeviceToken(eq(INTEGRATION_NAME), eq(NEW_TOKEN));
        verify(apiMock, never()).disableToken(any(String.class), nullable(IterableHelper.SuccessHandler.class), nullable(IterableHelper.FailureHandler.class));
    }

}
