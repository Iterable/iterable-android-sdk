package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * The unknown user manager is released by the teardown and only rebuilt by the new project's
 * initialize. Calls that are not queued, and callbacks still in flight from the previous project,
 * can reach it in that window, with unknown user activation on and nobody identified.
 */
@RunWith(TestRunner.class)
public class IterableSwitchProjectUnknownUserTest extends BaseTest {

    private static final String API_KEY_A = "project-a-key";
    private static final String API_KEY_B = "project-b-key";
    private static final String EMAIL_A = "user-a@example.com";
    private static final String JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjI5MTYyMzkwMjJ9.mYtgSqdUIxK8_RnYBTUP4cmpKw83aKi7cMiixF3qMB4";

    private Context context;
    private MockWebServer server;
    private IterablePushRegistration.IterablePushRegistrationImpl originalPushRegistration;
    private SwitchTeardownHold hold;
    private CountDownLatch landed;

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
        originalPushRegistration = IterablePushRegistration.instance;
        IterableTestUtils.resetIterableApi();
    }

    @After
    public void tearDown() throws Exception {
        for (int i = 0; i < 200 && IterableBackgroundInitializer.isSwitchingProject(); i++) {
            drainMainThread();
            Thread.sleep(10);
        }
        IterablePushRegistration.instance = originalPushRegistration;
        IterableTestUtils.resetIterableApi();
        IterableRequestTask.overrideUrl = null;
        server.shutdown();
    }

    /**
     * Skipped: the registration belongs to the previous project's visitor, whose unknown user data
     * the teardown has already discarded, and the new project registers the token itself once a
     * user is identified on it.
     */
    @Test
    public void testTokenRegistrationDuringTheTeardownIsSkipped() throws Exception {
        startHeldSwitch(unknownUserConfig(null), true);

        IterableApi.getInstance().registerDeviceToken(null, null, null, "app", "device-token-a", null);

        finishSwitch();
        assertFalse("The previous project's token registration must not be recorded for the new one",
                prefs().getString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "").contains("device-token-a"));
    }

    /**
     * The consent is stored straight away and the session start is left to the new project's
     * initialize, which starts one whenever visitor usage is tracked and nobody is identified.
     */
    @Test
    public void testVisitorConsentDuringTheTeardownStartsTheSessionOnTheNewProject() throws Exception {
        startHeldSwitch(unknownUserConfig(null), false);

        IterableApi.getInstance().setVisitorUsageTracked(true);

        finishSwitch();
        assertTrue("Consent is project-agnostic and must be kept", IterableApi.getInstance().getVisitorUsageTracked());
        assertFalse("The new project must start the unknown user session",
                prefs().getString(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS, "").isEmpty());
    }

    /**
     * The JWT requested by the previous project's login arrives mid-teardown and runs the merge and
     * replay. The visitor data it would clear is the previous project's, which the teardown already
     * cleared, so skipping the clear is right. A throw here is swallowed by the auth manager and
     * reported as an auth failure, so the token registration callback is what proves it ran.
     */
    @Test
    public void testAuthTokenArrivingDuringTheTeardownSkipsTheReleasedManager() throws Exception {
        BlockingAuthHandler authHandler = new BlockingAuthHandler();
        startHeldSwitch(unknownUserConfig(authHandler), true);
        assertNull("Precondition: the teardown has cleared the identity", IterableApi.getInstance().getEmail());

        authHandler.release();

        assertTrue("The auth token callback must complete", authHandler.registered.await(5, TimeUnit.SECONDS));
        finishSwitch();
    }

    /**
     * Same callback, reaching the event replay branch. Nothing public sets an identity while the
     * manager is released, so one is seeded to reach the branch; skipping the replay is right
     * because the events it would send were collected under the previous project.
     */
    @Test
    public void testAuthTokenArrivingWithAnIdentityDuringTheTeardownSkipsTheReplay() throws Exception {
        BlockingAuthHandler authHandler = new BlockingAuthHandler();
        startHeldSwitch(unknownUserConfig(authHandler), true);
        IterableApi.getInstance()._email = EMAIL_A;

        authHandler.release();

        assertTrue("The auth token callback must complete", authHandler.registered.await(5, TimeUnit.SECONDS));
        finishSwitch();
    }

    // region Helpers

    private IterableConfig unknownUserConfig(@Nullable IterableAuthHandler authHandler) {
        IterableConfig.Builder builder = new IterableConfig.Builder()
                .setKeychainEncryption(false)
                .setEnableUnknownUserActivation(true);
        if (authHandler != null) {
            builder.setAuthHandler(authHandler);
        }
        return builder.build();
    }

    /**
     * Initializes A with a user identified, then switches to B and holds the switch once the
     * teardown has released the unknown user manager.
     */
    private void startHeldSwitch(IterableConfig configA, boolean visitorUsageTracked) throws Exception {
        hold = SwitchTeardownHold.install();
        IterableApi.initialize(context, API_KEY_A, configA);
        if (visitorUsageTracked) {
            IterableApi.getInstance().setVisitorUsageTracked(true);
        }
        IterableApi.getInstance().setEmail(EMAIL_A);
        drainMainThread();

        landed = new CountDownLatch(1);
        IterableApi.switchProject(context, new IterableProject(API_KEY_B, unknownUserConfig(null)),
                ignored -> landed.countDown());
        hold.awaitTeardown();
    }

    private void finishSwitch() throws InterruptedException {
        hold.release();
        for (int i = 0; i < 100 && !landed.await(50, TimeUnit.MILLISECONDS); i++) {
            drainMainThread();
        }
        assertEquals("Callback should fire", 0, landed.getCount());
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
    }

    private void drainMainThread() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    /** Holds the token request of the previous project's login until the teardown is underway. */
    private static class BlockingAuthHandler implements IterableAuthHandler {
        private final CountDownLatch requested = new CountDownLatch(1);
        final CountDownLatch registered = new CountDownLatch(1);

        void release() {
            requested.countDown();
        }

        @Override
        public String onAuthTokenRequested() {
            try {
                requested.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return JWT;
        }

        @Override
        public void onTokenRegistrationSuccessful(String authToken) {
            registered.countDown();
        }

        @Override
        public void onAuthFailure(AuthFailure authFailure) { }
    }

    // endregion
}
