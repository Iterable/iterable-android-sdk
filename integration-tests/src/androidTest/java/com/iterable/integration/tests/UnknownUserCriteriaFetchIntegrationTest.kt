package com.iterable.integration.tests

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableApiHelper
import com.iterable.iterableapi.IterableConfig
import com.iterable.iterableapi.IterableConstants
import com.iterable.iterableapi.IterableUnknownUserHandler
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * BCIT coverage for SDK-497: the unknown user criteria fetch callback.
 *
 * Scenario the callback is built for: an app needs to act (e.g. track an event /
 * update the user) as soon as criteria are available, without racing the
 * asynchronous criteria fetch. The app tracks the criteria-matching event from
 * inside [IterableUnknownUserHandler.onCriteriaReceived]; the SDK then evaluates it
 * and creates the unknown user, which we observe via
 * [IterableUnknownUserHandler.onUnknownUserCreated].
 *
 * Backend dependency: the BCIT project (behind ITERABLE_API_KEY) must have the
 * criteria set "simple meow criteria" (criteriaId 541): a single customEvent with
 * `eventName == "meow"`. [MATCHING_EVENT_NAME] below must equal that event name.
 */
@RunWith(AndroidJUnit4::class)
class UnknownUserCriteriaFetchIntegrationTest {

    companion object {
        private const val TAG = "UUACriteriaCallbackTest"

        // Must match the server-side criteria set on the BCIT project (criteriaId 541).
        private const val MATCHING_EVENT_NAME = "meow"

        // Generous: criteria fetch round-trips to api.iterable.com, then the
        // unknownuser/events/session POST must also succeed.
        private const val USER_CREATED_TIMEOUT_SECONDS = 30L
    }

    private lateinit var context: Context
    private val criteriaReceived = AtomicBoolean(false)
    private val createdUnknownUserId = AtomicReference<String?>(null)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Stop MainActivity (the launcher) from auto-initializing the SDK with its
        // own config — we install our own UUA-enabled config below.
        System.setProperty("iterable.test.mode", "true")

        // Fresh singleton: clears any in-memory _userIdUnknown / email / userId
        // carried over from a previous test in the same process.
        IterableApiHelper().resetSharedInstance()

        // Wipe persisted unknown-user state so the anonymous path is guaranteed
        // active and the criteria cache starts empty.
        clearUnknownUserState()
    }

    @After
    fun tearDown() {
        IterableApi.getInstance().setUserId(null)
        IterableApi.getInstance().setEmail(null)
        clearUnknownUserState()
        System.clearProperty("iterable.test.mode")
    }

    @Test
    fun trackingMatchingEventInsideCriteriaCallback_createsUnknownUser() {
        val config = IterableConfig.Builder()
            .setEnableUnknownUserActivation(true)
            .setEnableForegroundCriteriaFetch(true)
            .setUnknownUserHandler(object : IterableUnknownUserHandler {
                override fun onUnknownUserCreated(userId: String) {
                    Log.d(TAG, "onUnknownUserCreated: $userId")
                    createdUnknownUserId.set(userId)
                }

                override fun onCriteriaReceived(criteria: JSONObject) {
                    Log.d(TAG, "criteria received, tracking matching event")
                    criteriaReceived.set(true)
                    // The callback's purpose: act once criteria are guaranteed present.
                    IterableApi.getInstance().track(MATCHING_EVENT_NAME)
                }

                override fun onCriteriaFetchFailed(reason: String) {
                    Log.e(TAG, "criteria fetch failed: $reason")
                }
            })
            .build()

        IterableApi.initialize(context, BuildConfig.ITERABLE_API_KEY, config)

        // Stay anonymous so track() routes to the unknown path.
        IterableApi.getInstance().setEmail(null)
        IterableApi.getInstance().setUserId(null)

        // Triggers the criteria fetch; its onSuccess tracks the matching event.
        IterableApi.getInstance().setVisitorUsageTracked(true)

        Assert.assertTrue(
            "Criteria callback (onSuccess) should fire after the fetch completes",
            waitFor { criteriaReceived.get() }
        )
        Assert.assertTrue(
            "Tracking the matching event from inside the criteria callback should create " +
                "an unknown user, but onUnknownUserCreated never fired within " +
                "${USER_CREATED_TIMEOUT_SECONDS}s.",
            waitFor { createdUnknownUserId.get() != null }
        )
    }

    private fun waitFor(condition: () -> Boolean): Boolean {
        return try {
            Awaitility.await()
                .atMost(USER_CREATED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until { condition() }
            true
        } catch (e: ConditionTimeoutException) {
            false
        }
    }

    private fun clearUnknownUserState() {
        val prefs = context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(IterableConstants.SHARED_PREFS_CRITERIA, "")
            .putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "")
            .putString(IterableConstants.SHARED_PREFS_USER_UPDATE_OBJECT_KEY, "")
            .putString(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS, "")
            .putBoolean(IterableConstants.SHARED_PREFS_VISITOR_USAGE_TRACKED, false)
            .apply()
    }
}
