package com.iterable.iterableapi;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.shadows.ShadowLog;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockWebServer;

import static android.os.Looper.getMainLooper;
import static com.iterable.iterableapi.IterableTestUtils.stubAnyRequestReturningStatusCode;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class IterablePushRegistrationTaskTest extends BaseTest {

    private static final String TEST_TOKEN = "testToken";
    public static final String INTEGRATION_NAME = "integrationName";
    public static final String DEVICE_ATTRIBUTES_KEY = "SDK";
    public static final String DEVICE_ATTRIBUTES_VALUE = "ReactNative 2.3.4";

    private MockWebServer server;
    private IterableApi originalApi;
    private IterableApi apiMock;
    private IterablePushRegistrationTask.Util.UtilImpl originalPushRegistrationUtil;
    private IterablePushRegistrationTask.Util.UtilImpl pushRegistrationUtilMock;
    private HashMap<String, String> deviceAttributes = new HashMap<String, String>();

    @Before
    public void setUp() throws Exception {
        IterableTestUtils.createIterableApi();
        server = new MockWebServer();
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        originalApi = IterableApi.sharedInstance;
        apiMock = spy(IterableApi.sharedInstance);
        IterableApi.sharedInstance = apiMock;

        originalPushRegistrationUtil = IterablePushRegistrationTask.Util.instance;
        pushRegistrationUtilMock = mock(IterablePushRegistrationTask.Util.UtilImpl.class);
        IterablePushRegistrationTask.Util.instance = pushRegistrationUtilMock;

        when(pushRegistrationUtilMock.getSenderId(any(Context.class))).thenReturn("12345");
        ShadowLog.clear();
    }

    @After
    public void tearDown() throws Exception {
        IterablePushRegistrationTask.Util.instance = originalPushRegistrationUtil;
        IterableApi.sharedInstance = originalApi;

        server.shutdown();
        server = null;
    }

    @Test
    public void testEnableDevice() throws Exception {
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn(TEST_TOKEN);
        AtomicReference<Thread> registrationSubmissionThread = new AtomicReference<>();
        doAnswer(invocation -> {
            registrationSubmissionThread.set(Thread.currentThread());
            return null;
        }).when(apiMock).registerDeviceToken(
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(String.class),
                any(String.class),
                anyMap(),
                any(Executor.class)
        );
        Thread taskExecutionThread = Thread.currentThread();

        IterablePushRegistrationData data = new IterablePushRegistrationData(IterableTestUtils.userEmail, null, null, INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        IterableApi.getInstance().setDeviceAttribute(DEVICE_ATTRIBUTES_KEY, DEVICE_ATTRIBUTES_VALUE);
        new IterablePushRegistrationTask(data).run();
        deviceAttributes.put(DEVICE_ATTRIBUTES_KEY, DEVICE_ATTRIBUTES_VALUE);

        assertSame(taskExecutionThread, registrationSubmissionThread.get());
        verify(apiMock).registerDeviceToken(
                eq(IterableTestUtils.userEmail),
                nullable(String.class),
                isNull(),
                eq(INTEGRATION_NAME),
                eq(TEST_TOKEN),
                eq(deviceAttributes),
                any(Executor.class)
        );

        verify(apiMock, never()).disableToken(eq(IterableTestUtils.userEmail), nullable(String.class), nullable(String.class), any(String.class), nullable(IterableHelper.SuccessHandler.class), nullable(IterableHelper.FailureHandler.class));
    }

    @Test
    public void testDisableDevice() throws Exception {
        stubAnyRequestReturningStatusCode(server, 200, "{}");
        when(pushRegistrationUtilMock.getFirebaseToken()).thenReturn("testToken");

        IterablePushRegistrationData data = new IterablePushRegistrationData(IterableTestUtils.userEmail, null, null, INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.DISABLE);
        new IterablePushRegistrationTask(data).run();
        shadowOf(getMainLooper()).idle();

        verify(apiMock).disableToken(eq(IterableTestUtils.userEmail), isNull(), isNull(), eq(TEST_TOKEN), nullable(IterableHelper.SuccessHandler.class), nullable(IterableHelper.FailureHandler.class));
    }

    @Test
    public void testDispatcherUsesConfiguredExecutor() {
        Executor executor = mock(Executor.class);
        IterablePushRegistration pushRegistration = new IterablePushRegistration(executor);
        IterablePushRegistrationData data = new IterablePushRegistrationData(IterableTestUtils.userEmail, null, null, INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.ENABLE);

        pushRegistration.executePushRegistrationTask(data);

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(taskCaptor.capture());
        assertTrue(taskCaptor.getValue() instanceof IterablePushRegistrationTask);
    }

    @Test
    public void testFirebaseTokenFailureIsLoggedAndContained() {
        RuntimeException tokenException = new RuntimeException("Unable to fetch token");
        when(pushRegistrationUtilMock.getFirebaseToken()).thenThrow(tokenException);

        IterablePushRegistrationData data = new IterablePushRegistrationData(IterableTestUtils.userEmail, null, null, INTEGRATION_NAME, IterablePushRegistrationData.PushRegistrationAction.ENABLE);
        new IterablePushRegistrationTask(data).run();

        verify(apiMock, never()).registerDeviceToken(
                any(String.class),
                nullable(String.class),
                nullable(String.class),
                any(String.class),
                any(String.class),
                anyMap(),
                any(Executor.class)
        );
        assertTrue(ShadowLog.getLogsForTag(IterablePushRegistrationTask.TAG).stream()
                .anyMatch(item -> item.msg.contains("Exception while retrieving the device token")
                        && item.throwable == tokenException));
    }
}
