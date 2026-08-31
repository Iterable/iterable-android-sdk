package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.android.util.concurrent.InlineExecutorService;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPausedAsyncTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Covers the one case the switch cannot wait out: the previous project's device disable is still
 * fetching an FCM token when the dispatch timeout expires, so it is handed to the request layer only
 * after the new project's key and region are already live.
 *
 * {@link IterableSwitchProjectTest} covers the switch itself; this is deliberately a separate class
 * because it has to run push registration on a real background thread rather than inline.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectDisableRegionTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";
    private static final String EMAIL_A = "user-a@example.com";
    private static final String TEST_TOKEN = "testToken";
    private static final String EU_ENDPOINT = IterableDataRegion.EU.getEndpoint();
    private static final String US_ENDPOINT = IterableDataRegion.US.getEndpoint();

    private Context context;
    private MockWebServer server;
    private IterablePushRegistrationTask.Util.UtilImpl originalPushRegistrationUtil;
    private long originalDisableDispatchTimeoutMs;
    private ExecutorService pushRegistrationExecutor;

    private final CountDownLatch releaseDeviceToken = new CountDownLatch(1);
    private final AtomicBoolean holdDeviceToken = new AtomicBoolean(false);

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("{}");
            }
        });
        IterableApi.overrideURLEndpointPath(server.url("").toString());

        originalDisableDispatchTimeoutMs = IterableProjectSwitcher.disableDispatchTimeoutMs;
        // Expire the wait immediately instead of spending the real timeout on every run. The point
        // of the test is what happens after it expires, not how long it is.
        IterableProjectSwitcher.disableDispatchTimeoutMs = 0;

        originalPushRegistrationUtil = IterablePushRegistrationTask.Util.instance;
        IterablePushRegistrationTask.Util.UtilImpl pushRegistrationUtilMock =
                mock(IterablePushRegistrationTask.Util.UtilImpl.class);
        when(pushRegistrationUtilMock.getSenderId(any(Context.class))).thenReturn("12345");
        when(pushRegistrationUtilMock.getFirebaseToken()).thenAnswer(invocation -> {
            if (holdDeviceToken.get()) {
                releaseDeviceToken.await(5, TimeUnit.SECONDS);
            }
            return TEST_TOKEN;
        });
        IterablePushRegistrationTask.Util.instance = pushRegistrationUtilMock;

        // BaseTest runs AsyncTasks inline, which would run the disable's token lookup on the switch's
        // own thread and deadlock it against the latch above.
        pushRegistrationExecutor = Executors.newCachedThreadPool();
        ShadowPausedAsyncTask.overrideExecutor(pushRegistrationExecutor);

        IterableTestUtils.resetIterableApi();
    }

    @After
    public void tearDown() throws Exception {
        releaseDeviceToken.countDown();
        IterablePushRegistrationTask.Util.instance = originalPushRegistrationUtil;
        IterableProjectSwitcher.disableDispatchTimeoutMs = originalDisableDispatchTimeoutMs;
        // The switch gate and the background executor are process-wide statics, so let an in-flight
        // switch finish before resetting.
        for (int i = 0; i < 100 && IterableBackgroundInitializer.isSwitchingProject(); i++) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
            Thread.sleep(20);
        }
        pushRegistrationExecutor.shutdownNow();
        pushRegistrationExecutor.awaitTermination(5, TimeUnit.SECONDS);
        ShadowPausedAsyncTask.overrideExecutor(new InlineExecutorService());
        IterableTestUtils.resetIterableApi();
        IterableRequestTask.overrideUrl = null;
        server.shutdown();
        server = null;
    }

    @Test
    public void testDisableDispatchedAfterTheTimeoutStillCarriesThePreviousProjectsKeyAndEndpoint() throws Exception {
        IterableApi.initialize(context, API_KEY_A, new IterableConfig.Builder()
                .setKeychainEncryption(false)
                .setDataRegion(IterableDataRegion.EU)
                .build());
        IterableApi.getInstance().setEmail(EMAIL_A);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        IterableApiClient apiClientSpy = spy(IterableApi.getInstance().apiClient);
        IterableApi.getInstance().apiClient = apiClientSpy;

        // From here the FCM token lookup blocks, so the disable cannot dispatch before the switch
        // gives up waiting for it.
        holdDeviceToken.set(true);

        CountDownLatch switched = new CountDownLatch(1);
        AtomicBoolean cleanTeardown = new AtomicBoolean(true);
        IterableApi.switchProject(context, new IterableProject(API_KEY_B, new IterableConfig.Builder()
                .setKeychainEncryption(false)
                .setDataRegion(IterableDataRegion.US)
                .build()), clean -> {
            cleanTeardown.set(clean);
            switched.countDown();
        });

        assertTrue("The switch must not wait for the disable indefinitely", awaitSwitch(switched));
        assertFalse("A disable that did not dispatch in time is a noisy teardown", cleanTeardown.get());
        assertEquals("The new project is live before the disable has gone anywhere",
                API_KEY_B, IterableApi.getInstance()._apiKey);
        assertEquals(US_ENDPOINT, IterableApi.getInstance().config.dataRegion.getEndpoint());

        releaseDeviceToken.countDown();

        // The disable is built now, against a live config that already belongs to project B. It has
        // to carry project A's key and project A's endpoint, because either one taken from the live
        // config would send a US key to the EU endpoint or leave project A pushing to this device.
        verify(apiClientSpy, timeout(5000)).sendPostRequest(
                eq(IterableConstants.ENDPOINT_DISABLE_DEVICE),
                any(JSONObject.class),
                nullable(String.class),
                eq(API_KEY_A),
                eq(EU_ENDPOINT),
                isNull(),
                nullable(IterableHelper.FailureHandler.class));

        assertNotEquals(API_KEY_A, API_KEY_B);
        assertNotEquals(EU_ENDPOINT, US_ENDPOINT);
    }

    /** Waits for a switch callback, pumping the main looper so the posted callback can run. */
    private boolean awaitSwitch(CountDownLatch latch) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
            if (latch.await(50, TimeUnit.MILLISECONDS)) {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
                return true;
            }
        }
        return false;
    }
}
