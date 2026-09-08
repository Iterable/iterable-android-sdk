package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements {@link IterableApi#switchProject(Context, String, IterableConfig, IterableInitializationCallback)}:
 * moving a running app from one Iterable project to another in place.
 *
 * The eight steps are: guard and validate, raise the switch gate, run the existing logout path,
 * purge the persisted offline queue, clear the previous project's identity and project-scoped
 * storage, release its managers, re-initialize against the new key and config, then lower the gate,
 * drain the calls queued during the window and fire the callbacks.
 *
 * Kept out of {@link IterableApi} so the switch reads as one sequence, mirroring how the iOS SDK
 * organises it in {@code IterableAPI+SwitchProject.swift}.
 */
class IterableProjectSwitcher {
    private static final String TAG = "IterableProjectSwitcher";

    /**
     * How long step 3 waits for the device disable to be handed to the request layer before it gives
     * up and swaps anyway. The disable has to fetch an FCM token first, which is network-bound and
     * untimed, so without a bound a switch could hang for as long as FCM does.
     *
     * Deliberately well under the 5 second grace period the background executor's shutdown allows
     * before it calls {@code shutdownNow()}. A switch started from inside a switch callback runs its
     * teardown, including this wait, on that same single-thread executor while the previous drain's
     * shutdown is counting down, so equal windows would let {@code shutdownNow()} interrupt this wait.
     */
    private static final long DISABLE_DISPATCH_TIMEOUT_MS = 2000;

    /**
     * Overridable so a test can drive the timeout path without waiting for it in real time. Raising
     * it past the shutdown grace period reintroduces the interrupt above.
     */
    @VisibleForTesting
    static long disableDispatchTimeoutMs = DISABLE_DISPATCH_TIMEOUT_MS;

    private IterableProjectSwitcher() { }

    static void switchProject(@NonNull Context context,
                              @NonNull String apiKey,
                              @Nullable IterableConfig config,
                              @Nullable IterableProjectSwitchCallback callback) {
        switchProject(context, apiKey, config, callback, false);
    }

    /**
     * @param resumingGate true when this call is a request that was queued during an earlier switch
     *                     and has inherited its still-raised gate, so it must not raise one of its
     *                     own and owns releasing it if it bails out before the teardown starts. Its
     *                     callbacks already sit with the gate, so {@code callback} is null.
     */
    static void switchProject(@NonNull Context context,
                              @NonNull String apiKey,
                              @Nullable IterableConfig config,
                              @Nullable IterableProjectSwitchCallback callback,
                              boolean resumingGate) {
        // Both parameters are @NonNull, so a null is a programmer error. Reporting it through the
        // callback would mean the same false that documents "we switched, but a cleanup step was
        // noisy" also has to mean "nothing happened at all".
        if (context == null) {
            throw new IllegalArgumentException("switchProject: context must not be null");
        }
        if (apiKey == null) {
            throw new IllegalArgumentException("switchProject: apiKey must not be null");
        }

        // An empty key is a runtime condition rather than a programmer error: a region lookup or a
        // remote config can legitimately return one. Tearing down for it would delete the previous
        // project's identity and offline queue and leave the SDK initialized against nothing, so
        // refuse it and stay put. iOS does the same.
        if (apiKey.trim().isEmpty()) {
            IterableLogger.e(TAG, "switchProject called with an empty API key. The SDK is left on "
                    + "the project it is already on.");
            deliverSwitchCallback(callback, false);
            releaseInheritedGate(resumingGate, false);
            return;
        }

        final IterableApi api = IterableApi.sharedInstance;

        // Step 1: guard and validate.
        if (api._apiKey == null || api._applicationContext == null) {
            IterableLogger.w(TAG, "switchProject called before the SDK was initialized; initializing instead");
            // Reported as clean, matching iOS. There is no previous project, so there is no
            // teardown step that could have been noisy and no device to disable: warnings would
            // point at a warning that does not exist, and an app that uses switchProject as its
            // entry point would see one on every cold start.
            // initializeInBackground notifies on the main thread already, so this does not need
            // deliverSwitchCallback.
            IterableApi.initializeInBackground(context, apiKey, config,
                    callback == null ? null
                            : () -> callback.onProjectSwitched(IterableProjectSwitchResult.SWITCHED_CLEANLY));
            releaseInheritedGate(resumingGate, true);
            return;
        }

        if (apiKey.equals(api._apiKey)) {
            IterableLogger.d(TAG, "switchProject called with the API key already in use; nothing to tear down");
            deliverSwitchCallback(callback, true);
            releaseInheritedGate(resumingGate, true);
            return;
        }

        // initializeInBackground publishes _apiKey synchronously but finishes its init task later,
        // and that task marks initialization complete, which would lower this switch's gate while
        // the teardown was still running. Wait for it instead of tearing down underneath it.
        if (!IterableBackgroundInitializer.isSwitchingProject() && IterableApi.isSDKInitializing()) {
            if (IterableBackgroundInitializer.runWhenInitialized(() -> switchProject(context, apiKey, config, callback))) {
                IterableLogger.w(TAG, "switchProject called while initialization was still in flight; "
                        + "deferring the switch until initialization completes");
                return;
            }
        }

        // Step 2: raise the switch gate synchronously, so calls made after this method returns are
        // queued rather than executed against a half torn-down SDK. Skipped when the gate was
        // inherited from the switch that queued this request: it was never lowered, precisely so
        // nothing could take it in between.
        if (!resumingGate && !IterableBackgroundInitializer.beginProjectSwitch(apiKey, config, callback)) {
            IterableLogger.d(TAG, "switchProject: a switch is already in progress; this request "
                    + "either joins it or runs right after it");
            return;
        }

        // Steps 3-8 run off the main thread on the existing background executor.
        IterableBackgroundInitializer.executeOnBackgroundExecutor(() -> runSwitch(api, context, apiKey, config));
    }

    /**
     * A bail-out on an inherited gate has to release it, or it stays raised for the life of the
     * process and every later SDK call is queued and never drained. The callbacks for this request
     * are already registered with the gate, so completing the switch delivers them.
     */
    private static void releaseInheritedGate(boolean resumingGate, boolean cleanTeardown) {
        if (resumingGate) {
            IterableBackgroundInitializer.completeProjectSwitch(cleanTeardown);
        }
    }

    private static void runSwitch(IterableApi api, Context context, String apiKey, @Nullable IterableConfig config) {
        // Counted down once the device disable has been built and handed off, or straight away when
        // there was no disable to send. disableConfirmed stays false unless a request actually went
        // out, so an app with push disabled, a missing FCM token or a failing disable all report a
        // noisy teardown rather than a clean one.
        final CountDownLatch disableDispatched = new CountDownLatch(1);
        final AtomicBoolean disableConfirmed = new AtomicBoolean(false);
        IterablePushRegistrationData.DispatchListener disableListener = dispatched -> {
            disableConfirmed.set(dispatched);
            disableDispatched.countDown();
        };
        IterableHelper.FailureHandler onDisableFailure = (reason, data) -> {
            disableConfirmed.set(false);
            IterableLogger.w(TAG, "switchProject: the previous project's device disable failed: " + reason);
        };

        boolean cleanTeardown = tearDown(api, disableListener, onDisableFailure);

        // Waited for so the disable is normally on its way before the swap. It carries both the API
        // key and the region endpoint captured when it was initiated, so a timeout here costs
        // promptness rather than correctness: a disable that dispatches afterwards still reaches the
        // project it was created for.
        try {
            if (!disableDispatched.await(disableDispatchTimeoutMs, TimeUnit.MILLISECONDS)) {
                cleanTeardown = false;
                IterableLogger.w(TAG, "switchProject: the previous project's device disable did not dispatch "
                        + "within " + disableDispatchTimeoutMs + "ms; switching anyway");
            }
        } catch (InterruptedException e) {
            cleanTeardown = false;
            IterableLogger.w(TAG, "switchProject: interrupted while waiting for the device disable to dispatch");
            Thread.currentThread().interrupt();
        }

        if (!disableConfirmed.get()) {
            // Expected, not an error, for an app that does not use push or has no token yet. The
            // switch itself is unaffected; the callback reports it so an app that does use push can
            // tell that the previous project may still have this device registered.
            cleanTeardown = false;
            IterableLogger.d(TAG, "switchProject: no device disable was confirmed for the previous project");
        }

        // Step 7: re-initialize with the new API key and config.
        try {
            IterableApi.initialize(context, apiKey, config);
        } catch (Exception e) {
            cleanTeardown = false;
            IterableLogger.e(TAG, "switchProject: re-initialization failed", e);
        } finally {
            // The new project owns the attribution store from here, so attribution writes go back
            // to being filtered by API key alone. Lowered even when initialize threw, because
            // leaving it raised would refuse every attribution write for the life of the process.
            //
            // Lowered after initialize rather than as soon as it publishes the new API key, on
            // purpose: initialize runs processPendingAction, and a push that was tapped before the
            // switch carries no key of its own, so it would write the project it came from into the
            // new project's attribution through the live key. The flag is what refuses it.
            synchronized (api.projectStateLock) {
                api.projectScopedStorageCleared = false;
            }
        }

        // initialize() rebuilds the in-app, embedded and unknown-user managers but never the auth
        // manager, so it is replaced here: in one place, after config has been swapped, and holding
        // the lock getAuthManager() takes, so no other thread can win the race and build one bound
        // to the previous project's IterableAuthHandler.
        try {
            synchronized (api.projectStateLock) {
                api.authManager = null;
                api.getAuthManager();
            }
        } catch (Exception e) {
            cleanTeardown = false;
            IterableLogger.e(TAG, "switchProject: rebuilding the auth manager was not clean", e);
        }

        // The request processor is reused across the switch, so re-bind it to the new auth manager.
        try {
            api.apiClient.rebindAuthTokenListener();
        } catch (Exception e) {
            cleanTeardown = false;
            IterableLogger.e(TAG, "switchProject: failed to re-bind the auth token listener", e);
        }

        // Step 8: lower the gate, drain queued calls FIFO, then fire the callbacks.
        IterableBackgroundInitializer.completeProjectSwitch(cleanTeardown);
    }

    /**
     * Steps 3-6. Every step is independently guarded: a noisy step is logged and reported through
     * the callback, but never stops the swap.
     *
     * @param disableListener notified once the device disable has reached the request layer, or with
     *                        false when there was no disable to send
     * @param onDisableFailure notified if the disable request comes back as a failure
     * @return true when every teardown step completed cleanly
     */
    private static boolean tearDown(IterableApi api,
                                    IterablePushRegistrationData.DispatchListener disableListener,
                                    IterableHelper.FailureHandler onDisableFailure) {
        boolean cleanTeardown = true;

        // Step 3: reuse the existing logout path, so future additions to logout apply here for free.
        // This disables the push token on the previous project and resets its managers.
        boolean disableStarted = false;
        try {
            disableStarted = api.logoutPreviousUser(disableListener, onDisableFailure);
        } catch (Exception e) {
            cleanTeardown = false;
            IterableLogger.e(TAG, "switchProject: logout step was not clean", e);
        }
        if (!disableStarted) {
            disableListener.onDispatched(false);
        }

        // Step 4: logout above already purges the queue, so this is a repeat. It is kept because a
        // logout that threw before reaching apiClient.onLogout() would otherwise carry the previous
        // project's queued work into the new project. Queued device disables survive both purges.
        try {
            api.apiClient.purgeOfflineQueue();
        } catch (Exception e) {
            cleanTeardown = false;
            IterableLogger.e(TAG, "switchProject: offline queue purge was not clean", e);
        }

        // Step 5: clear identity and the rest of the previous project's storage, so
        // retrieveEmailAndUserId() during the re-init cannot repopulate its identity and setEmail()
        // on the new project cannot replay its events. _deviceId and visitor consent are
        // project-agnostic and deliberately left alone.
        cleanTeardown &= clearIdentity(api);
        cleanTeardown &= clearProjectScopedStorage(api);

        // Step 6: release per-project state so initialize() rebuilds it against the new config. The
        // managers are also unregistered from the activity monitor so the discarded instances stop
        // reacting to foreground events. authManager is deliberately left in place until config has
        // been swapped; see the rebuild in runSwitch.
        try {
            IterableActivityMonitor activityMonitor = IterableActivityMonitor.getInstance();
            IterableInAppManager inAppManager = api.getInAppManagerOrNull();
            if (inAppManager != null) {
                activityMonitor.removeCallback(inAppManager);
            }
            IterableEmbeddedManager embeddedManager = api.getEmbeddedManagerOrNull();
            if (embeddedManager != null) {
                activityMonitor.removeCallback(embeddedManager);
            }
            if (api.unknownUserManager != null) {
                activityMonitor.removeCallback(api.unknownUserManager);
            }
            if (api.authManager != null) {
                activityMonitor.removeCallback(api.authManager);
            }
            api.clearMessagingManagers();
            api.clearProjectScopedInstanceState();
            api.unknownUserManager = null;
            // Dropped so getKeychain() rebuilds it against the new config's keychainEncryption and
            // decryptionFailureHandler. Done after clearIdentity, which needs the old one to clear
            // values the old encryption settings wrote.
            api.keychain = null;
            api._firstForegroundHandled = false;
        } catch (Exception e) {
            cleanTeardown = false;
            IterableLogger.e(TAG, "switchProject: releasing per-project state was not clean", e);
        }

        return cleanTeardown;
    }

    private static boolean clearIdentity(IterableApi api) {
        try {
            api._email = null;
            api._userId = null;
            api._userIdUnknown = null;
            api._authToken = null;
            IterableKeychain keychain = api.getKeychain();
            if (keychain == null) {
                IterableLogger.e(TAG, "switchProject: could not clear stored identity, keychain unavailable");
                return false;
            }
            keychain.saveEmail(null);
            keychain.saveUserId(null);
            keychain.saveUserIdUnknown(null);
            keychain.saveAuthToken(null);
            return true;
        } catch (Exception e) {
            IterableLogger.e(TAG, "switchProject: clearing identity was not clean", e);
            return false;
        }
    }

    /**
     * Drops the storage that is scoped to the project being left. The unknown-user event list is the
     * damaging one: {@code setEmail} on the new project runs the event replay, which would post
     * events collected under the previous project to the new one. Activation criteria and push
     * attribution are namespaced per project too, so a stale campaignId would be attached to the
     * first track after the switch.
     */
    private static boolean clearProjectScopedStorage(IterableApi api) {
        boolean clean = true;

        try {
            UnknownUserManager unknownUserManager = api.unknownUserManager;
            if (unknownUserManager != null) {
                unknownUserManager.clearVisitorEventsAndUserData();
            } else {
                // Same keys, for the case where the manager was never built.
                SharedPreferences.Editor editor = api.getPreferences().edit();
                editor.putString(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS, "");
                editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
                editor.putString(IterableConstants.SHARED_PREFS_USER_UPDATE_OBJECT_KEY, "");
                editor.apply();
            }
        } catch (Exception e) {
            clean = false;
            IterableLogger.e(TAG, "switchProject: clearing unknown-user state was not clean", e);
        }

        try {
            // Raised before the attribution keys go, and under the lock setAttributionInfo takes,
            // so a deep link redirect or a push open resolving from here until the new project is
            // up stands down instead of writing the previous project's campaign into the
            // preferences the new project reads.
            synchronized (api.projectStateLock) {
                api.projectScopedStorageCleared = true;
            }
            SharedPreferences.Editor editor = api.getPreferences().edit();
            editor.remove(IterableConstants.SHARED_PREFS_CRITERIA);
            // matchedCriteriaId is nested inside the unknown-session payload cleared above rather
            // than being a key of its own, but remove it too so a build that starts writing it
            // separately cannot carry a previous project's criteria id across a switch.
            editor.remove(IterableConstants.SHARED_PREFS_CRITERIA_ID);
            editor.remove(IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_KEY + IterableConstants.SHARED_PREFS_OBJECT_SUFFIX);
            editor.remove(IterableConstants.SHARED_PREFS_ATTRIBUTION_INFO_KEY + IterableConstants.SHARED_PREFS_EXPIRATION_SUFFIX);
            editor.apply();
        } catch (Exception e) {
            clean = false;
            IterableLogger.e(TAG, "switchProject: clearing criteria and attribution was not clean", e);
        }

        return clean;
    }

    private static void deliverSwitchCallback(@Nullable IterableProjectSwitchCallback callback, boolean cleanTeardown) {
        if (callback == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                callback.onProjectSwitched(IterableProjectSwitchResult.from(cleanTeardown));
            } catch (Exception e) {
                IterableLogger.e(TAG, "Exception in switchProject callback", e);
            }
        });
    }
}
