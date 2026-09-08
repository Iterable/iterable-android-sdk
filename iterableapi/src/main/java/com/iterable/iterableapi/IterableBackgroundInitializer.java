package com.iterable.iterableapi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles background initialization of the Iterable SDK to prevent ANRs.
 * This class manages operation queuing, thread safety, and initialization state.
 */
class IterableBackgroundInitializer {
    private static final String TAG = "IterableBackgroundInit";

    // Timeout for initialization to prevent indefinite hangs (5 seconds)
    private static final int INITIALIZATION_TIMEOUT_SECONDS = 5;

    // Callback manager for initialization completion
    private static final IterableInitializationCallbackManager callbackManager = new IterableInitializationCallbackManager();

    /**
     * Outcome of an attempt to drain the operation queue.
     */
    enum DrainResult {
        /** A drain task is running; the drain completion will run on the executor thread. */
        STARTED,
        /** A drain was already in flight, so this call did nothing. */
        ALREADY_DRAINING,
        /** No executor would accept the drain. The queue is still full and nobody will run it. */
        REJECTED
    }

    /**
     * Represents a queued operation that should be executed after initialization
     */
    interface QueuedOperation {
        /**
         * Execute the operation
         */
        void execute();

        /**
         * Get description for debugging
         */
        String getDescription();
    }

    /**
     * Queue for operations called before initialization completes
     */
    private static class OperationQueue {
        private final ConcurrentLinkedQueue<QueuedOperation> operations = new ConcurrentLinkedQueue<>();
        private volatile boolean isProcessing = false;

        void enqueue(QueuedOperation operation) {
            operations.offer(operation);
            IterableLogger.d(TAG, "Queued operation: " + operation.getDescription());
        }

        void processAll(ExecutorService executor) {
            processAll(executor, null, null);
        }

        /**
         * @param onDrained run on the executor thread once every queued operation has executed. Not
         *                  run unless the result is {@link DrainResult#STARTED}.
         * @param lowerGateWhenDrained run under {@code initLock} the first time the queue is
         *                             observed empty. Until it runs, callers still see the gate
         *                             raised and keep enqueueing, so a call made during the drain
         *                             cannot overtake the calls already queued behind the gate.
         */
        DrainResult processAll(ExecutorService executor,
                               @Nullable Runnable onDrained,
                               @Nullable Runnable lowerGateWhenDrained) {
            if (isProcessing) return DrainResult.ALREADY_DRAINING;
            isProcessing = true;

            if (submitDrain(executor, onDrained, lowerGateWhenDrained)) {
                return DrainResult.STARTED;
            }

            // The executor was shut down between being handed to us and execute(). That happens for
            // real: the drain task shuts its own executor down as its last act, so a switch started
            // from inside a switch callback can land in exactly this window.
            IterableLogger.w(TAG, "Background executor rejected the queue drain, retrying on a fresh executor");
            if (submitDrain(replaceIfCurrent(executor), onDrained, lowerGateWhenDrained)) {
                return DrainResult.STARTED;
            }

            // Nothing will run the drain, so isProcessing has to be released. Left set, it would jam
            // the queue for the life of the process and every later drain would be refused.
            isProcessing = false;
            IterableLogger.e(TAG, "Could not drain the operation queue: both executors rejected it");
            return DrainResult.REJECTED;
        }

        /** @return false if {@code executor} rejected the drain */
        private boolean submitDrain(ExecutorService executor,
                                    @Nullable Runnable onDrained,
                                    @Nullable Runnable lowerGateWhenDrained) {
            try {
                executor.execute(() -> {
                    while (true) {
                        QueuedOperation operation;
                        while ((operation = operations.poll()) != null) {
                            try {
                                IterableLogger.d(TAG, "Executing queued operation: " + operation.getDescription());
                                operation.execute();
                            } catch (Exception e) {
                                IterableLogger.e(TAG, "Failed to execute queued operation", e);
                            }
                        }
                        if (lowerGateWhenDrained == null) {
                            break;
                        }
                        // Emptiness is decided under the lock enqueue takes, so a call landing
                        // right now either goes on the queue and is picked up by another pass, or
                        // arrives after the gate is down and runs itself. It can never slip in
                        // between and jump ahead of what is already queued.
                        boolean drained;
                        synchronized (initLock) {
                            drained = operations.isEmpty();
                            if (drained) {
                                lowerGateWhenDrained.run();
                            }
                        }
                        if (drained) {
                            break;
                        }
                    }
                    isProcessing = false;

                    if (onDrained != null) {
                        try {
                            onDrained.run();
                        } catch (Exception e) {
                            IterableLogger.e(TAG, "Failed to run queue drain completion", e);
                        }
                    }

                    IterableLogger.d(TAG, "All queued operations processed, shutting down background executor");
                    shutdownBackgroundExecutorAsync(executor);
                });
                return true;
            } catch (RejectedExecutionException e) {
                return false;
            }
        }

        int size() {
            return operations.size();
        }

        void clear() {
            operations.clear();
            isProcessing = false;
        }
    }

    // Background initialization infrastructure
    private static volatile ExecutorService backgroundExecutor;
    private static final Object initLock = new Object();

    static {
        backgroundExecutor = createExecutor();
    }

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "IterableBackgroundInit");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
    }

    private static final OperationQueue operationQueue = new OperationQueue();
    private static volatile boolean isInitializing = false;
    private static volatile boolean isBackgroundInitialized = false;
    private static final ConcurrentLinkedQueue<IterableInitializationCallback> pendingCallbacks = new ConcurrentLinkedQueue<>();

    /**
     * Work that must not run until the in-flight background initialization has finished. Currently
     * only {@link IterableApi#switchProject}, which cannot tear the SDK down while an init task is
     * still going to mark initialization complete underneath it.
     */
    private static final ConcurrentLinkedQueue<Runnable> pendingInitActions = new ConcurrentLinkedQueue<>();

    /**
     * Initialize the Iterable SDK in the background to avoid ANRs.
     * This method returns immediately and performs all initialization work on a background thread.
     * Any API calls made before initialization completes will be queued and executed after initialization.
     *
     * @param context Application context
     * @param apiKey Iterable API key
     * @param config Optional configuration (can be null)
     * @param callback Optional callback for initialization completion (can be null)
     */
    static void initializeInBackground(@NonNull Context context,
                                     @NonNull String apiKey,
                                     @Nullable IterableConfig config,
                                     @Nullable IterableInitializationCallback callback) {
        // Handle null context early - still report success but log error
        if (context == null) {
            IterableLogger.e(TAG, "Context cannot be null, but reporting success");
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(callback::onSDKInitialized);
            }
            return;
        }

        synchronized (initLock) {
            if (isInitializing || isBackgroundInitialized) {
                IterableLogger.w(TAG, "initializeInBackground called but initialization already in progress or completed");
                if (callback != null) {
                    if (isBackgroundInitialized) {
                        // Initialization already complete, call callback immediately
                        new Handler(Looper.getMainLooper()).post(callback::onSDKInitialized);
                    } else {
                        // Initialization in progress, queue callback for later
                        pendingCallbacks.offer(callback);
                    }
                }
                return;
            }

            // Set initializing flag and essential properties inside synchronized block
            isInitializing = true;
            IterableApi.sharedInstance._applicationContext = context.getApplicationContext();
            IterableApi.sharedInstance._apiKey = apiKey;
            IterableApi.sharedInstance.config = (config != null) ? config : new IterableConfig.Builder().build();
        }

        IterableLogger.d(TAG, "Starting background initialization");

        // Create a separate executor for the actual initialization to enable timeout
        ExecutorService initExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "IterableInit");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });

        Runnable initTask = () -> {
            long startTime = System.currentTimeMillis();
            boolean initSucceeded = false;

            try {
                IterableLogger.d(TAG, "Starting initialization with " + INITIALIZATION_TIMEOUT_SECONDS + " second timeout");

                // Submit the actual initialization task
                Future<?> initFuture = initExecutor.submit(() -> {
                    IterableLogger.d(TAG, "Executing initialization on background thread");
                    IterableApi.initialize(context, apiKey, config);
                });

                // Wait for initialization with timeout
                initFuture.get(INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                initSucceeded = true;

                long elapsedTime = System.currentTimeMillis() - startTime;
                IterableLogger.d(TAG, "Background initialization completed successfully in " + elapsedTime + "ms");

            } catch (TimeoutException e) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                IterableLogger.w(TAG, "Background initialization timed out after " + elapsedTime + "ms, continuing anyway");
                // Cancel the hanging initialization task
                initExecutor.shutdownNow();

            } catch (Exception e) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                IterableLogger.e(TAG, "Background initialization encountered error after " + elapsedTime + "ms, but continuing", e);
            }

            // Always mark as completed and call callbacks regardless of success/timeout/failure
            markInitializationComplete();

            // Notify completion on main thread (always success)
            final boolean finalInitSucceeded = initSucceeded;
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    long totalTime = System.currentTimeMillis() - startTime;
                    if (finalInitSucceeded) {
                        IterableLogger.d(TAG, "Initialization completed successfully, notifying callbacks after " + totalTime + "ms");
                    } else {
                        IterableLogger.w(TAG, "Initialization timed out or failed, but notifying callbacks anyway after " + totalTime + "ms");
                    }

                    // Call the original callback directly
                    if (callback != null) {
                        try {
                            callback.onSDKInitialized();
                        } catch (Exception e) {
                            IterableLogger.e(TAG, "Exception in initialization callback", e);
                        }
                    }

                    // Call all pending callbacks from concurrent initialization attempts
                    IterableInitializationCallback pendingCallback;
                    while ((pendingCallback = pendingCallbacks.poll()) != null) {
                        try {
                            pendingCallback.onSDKInitialized();
                        } catch (Exception e) {
                            IterableLogger.e(TAG, "Exception in pending initialization callback", e);
                        }
                    }

                } catch (Exception e) {
                    IterableLogger.e(TAG, "Exception in initialization completion notification", e);
                }
            });

             // Clean up the init executor
             try {
                 if (!initExecutor.isShutdown()) {
                     initExecutor.shutdown();
                     if (!initExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                         initExecutor.shutdownNow();
                     }
                 }
             } catch (InterruptedException e) {
                 initExecutor.shutdownNow();
                 Thread.currentThread().interrupt();
             }
        };

        // Via ensureBackgroundExecutor rather than backgroundExecutor directly: the drain task shuts
        // its own executor down when it finishes, so the field can hold a dead executor by the time
        // an app initializes again, and a rejection here would throw out of a public API call.
        ExecutorService executor;
        synchronized (initLock) {
            executor = ensureBackgroundExecutor();
        }
        executor.execute(initTask);
    }

    /**
     * Lowers the initialization gate, drains the calls that were queued behind it, and runs anything
     * that was waiting for initialization to finish.
     *
     * Does nothing to the gate while a project switch owns it: the switch raises the same flags and
     * {@link #completeProjectSwitch(boolean)} is what lowers them. Without that check an init task
     * completing mid-switch would open the gate while the teardown was still running, so calls in
     * that window would execute against a half torn-down SDK.
     */
    private static void markInitializationComplete() {
        boolean switchOwnsGate;
        synchronized (initLock) {
            switchOwnsGate = isSwitchingProject;
            if (!switchOwnsGate) {
                isBackgroundInitialized = true;
                isInitializing = false;
            }
        }

        if (switchOwnsGate) {
            IterableLogger.d(TAG, "Initialization finished during a project switch; the switch owns the gate");
            return;
        }

        operationQueue.processAll(backgroundExecutor);
        runPendingInitActions();
    }

    private static void runPendingInitActions() {
        Runnable action;
        while ((action = pendingInitActions.poll()) != null) {
            try {
                action.run();
            } catch (Exception e) {
                IterableLogger.e(TAG, "Failed to run deferred post-initialization action", e);
            }
        }
    }

    /**
     * Defers {@code action} until the in-flight background initialization completes.
     *
     * @return true if the action was deferred, false if no initialization is in flight and the
     *         caller should run it itself
     */
    static boolean runWhenInitialized(@NonNull Runnable action) {
        synchronized (initLock) {
            if (!isInitializing || isBackgroundInitialized) {
                return false;
            }
            pendingInitActions.offer(action);
            return true;
        }
    }

    /**
     * Check if background initialization is in progress
     * @return true if initialization is currently running in background
     */
    static boolean isInitializingInBackground() {
        return isInitializing;
    }

    /**
     * Check if background initialization has completed
     * @return true if background initialization completed successfully
     */
    static boolean isBackgroundInitializationComplete() {
        return isBackgroundInitialized;
    }

    /**
     * Queue an operation if initialization is in progress, otherwise execute immediately
     * @param operation The operation to queue or execute
     * @return true if operation was queued, false if executed immediately
     */
    static boolean queueOrExecute(QueuedOperation operation) {
        synchronized (initLock) {
            if (isInitializing && !isBackgroundInitialized) {
                operationQueue.enqueue(operation);
                return true;
            }
        }
        // Execute immediately if not initializing
        operation.execute();
        return false;
    }

    /**
     * Convenient method for one-liner operation queuing
     * @param runnable The operation to execute
     * @param description Description for debugging
     */
    static void queueOrExecute(Runnable runnable, String description) {
        queueOrExecute(new QueuedOperation() {
            @Override
            public void execute() {
                runnable.run();
            }

            @Override
            public String getDescription() {
                return description;
            }
        });
    }

    /**
     * Queues behind an initialization but never behind a project switch.
     *
     * A queued operation is replayed once the new project is live, which is right for a call that
     * carries no project-scoped identifiers and wrong for one that does. A push open replayed after
     * a switch reports the previous project's campaignId, templateId and messageId to the new
     * project, where those IDs do not exist. Running it inline instead sends it to whichever project
     * is live at the time, which is the previous project for all of the teardown. iOS does not gate
     * push handling at all, for the same reason.
     *
     * Initialization queueing is deliberately left alone: a call made while a background
     * initialization is still in flight has no previous project to be misattributed to.
     *
     * @return true if the operation was queued
     */
    static boolean queueOrExecuteUnlessSwitching(QueuedOperation operation) {
        boolean switching;
        synchronized (initLock) {
            if (isInitializing && !isBackgroundInitialized && !isSwitchingProject) {
                operationQueue.enqueue(operation);
                return true;
            }
            // Read under the same lock that raises the gate, so the branch cannot be decided on a
            // stale value, then log and execute outside it.
            switching = isSwitchingProject;
        }
        if (switching) {
            IterableLogger.w(TAG, "switchProject is in progress. Running " + operation.getDescription()
                    + " against the project that is live now instead of queueing it, because it "
                    + "carries identifiers that only exist on the project that produced it.");
        }
        operation.execute();
        return false;
    }

    static void queueOrExecuteUnlessSwitching(Runnable runnable, String description) {
        queueOrExecuteUnlessSwitching(new QueuedOperation() {
            @Override
            public void execute() {
                runnable.run();
            }

            @Override
            public String getDescription() {
                return description;
            }
        });
    }


    //region Project switching
    //---------------------------------------------------------------------------------------

    private static volatile boolean isSwitchingProject = false;
    private static final ConcurrentLinkedQueue<IterableProjectSwitchCallback> switchCallbacks = new ConcurrentLinkedQueue<>();

    /**
     * The project the in-flight switch is heading to, so a second request can be told apart by
     * where it is going rather than only by the fact that a switch is running.
     */
    private static volatile String inFlightSwitchApiKey;

    /**
     * Switches asked for while another was in flight, targeting a project that one will not land
     * on. Run in the order they were asked for once the gate drops.
     */
    private static final ConcurrentLinkedQueue<PendingProjectSwitch> pendingSwitches = new ConcurrentLinkedQueue<>();

    /** A switch request parked until the one in flight finishes. */
    static final class PendingProjectSwitch {
        final String apiKey;
        @Nullable final IterableConfig config;
        final List<IterableProjectSwitchCallback> callbacks = new ArrayList<>();

        PendingProjectSwitch(@NonNull String apiKey, @Nullable IterableConfig config) {
            this.apiKey = apiKey;
            this.config = config;
        }
    }

    /**
     * @return true while {@link IterableApi#switchProject} is tearing down and re-initializing
     */
    static boolean isSwitchingProject() {
        return isSwitchingProject;
    }

    /**
     * Raises the switch gate so every SDK call made from now until
     * {@link #completeProjectSwitch(boolean)} is queued instead of running against a half
     * torn-down SDK, and registers {@code callback} with the switch.
     *
     * A request arriving while a switch is running is handled by where it is headed. One asking
     * for the project the in-flight switch is already going to joins it, so a picker tapped twice
     * on the same destination runs one teardown. One asking for a different project is parked and
     * run as soon as that switch finishes: dropping it would leave the SDK on a project the app
     * has already asked to leave, with its callback reporting a completed switch, and nothing in
     * the API for the app to detect that with.
     *
     * @return true if this call owns the switch, false if a switch was already in progress (the
     *         callback is either registered with the in-flight switch or parked with the request
     *         that will follow it)
     */
    static boolean beginProjectSwitch(@NonNull String apiKey,
                                      @Nullable IterableConfig config,
                                      @Nullable IterableProjectSwitchCallback callback) {
        synchronized (initLock) {
            if (isSwitchingProject) {
                if (apiKey.equals(inFlightSwitchApiKey)) {
                    if (callback != null) {
                        switchCallbacks.offer(callback);
                    }
                } else {
                    parkPendingSwitchLocked(apiKey, config, callback);
                }
                return false;
            }
            if (callback != null) {
                switchCallbacks.offer(callback);
            }
            isSwitchingProject = true;
            inFlightSwitchApiKey = apiKey;
            isInitializing = true;
            isBackgroundInitialized = false;
            // The new project runs through initialize() again, and notifyInitializationComplete()
            // only fires once per initialized flag, so the flag has to be cleared for the second
            // initialize() to notify. Subscribers themselves are kept: clearing them would drop a
            // subscriber registered while the first initialization was still in flight.
            callbackManager.clearInitializedFlag();
            return true;
        }
    }

    /** Repeated requests for one destination share a single parked entry, so B, C, C runs B then C. */
    private static void parkPendingSwitchLocked(@NonNull String apiKey,
                                                @Nullable IterableConfig config,
                                                @Nullable IterableProjectSwitchCallback callback) {
        PendingProjectSwitch parked = null;
        for (PendingProjectSwitch candidate : pendingSwitches) {
            if (candidate.apiKey.equals(apiKey)) {
                parked = candidate;
                break;
            }
        }
        if (parked == null) {
            parked = new PendingProjectSwitch(apiKey, config);
            pendingSwitches.offer(parked);
        }
        if (callback != null) {
            parked.callbacks.add(callback);
        }
    }

    /**
     * Lowers the switch gate, drains the calls queued during the switch window FIFO against the new
     * project, then delivers every callback registered with this switch on the main thread.
     *
     * When a switch was requested while this one ran, the gate is handed straight to it rather than
     * lowered, and {@link #startPendingSwitch} runs it on the gate it inherited.
     *
     * @param cleanTeardown false when a teardown step was noisy. The SDK is on the new project
     *                      either way; this never means the switch failed.
     */
    static void completeProjectSwitch(boolean cleanTeardown) {
        final List<IterableProjectSwitchCallback> switchCallbacksToNotify = new ArrayList<>();
        final List<IterableInitializationCallback> initCallbacksToNotify = new ArrayList<>();
        final AtomicReference<PendingProjectSwitch> nextSwitch = new AtomicReference<>();
        final ExecutorService executor;
        synchronized (initLock) {
            executor = ensureBackgroundExecutor();
        }

        // Run under initLock once the drain has emptied the queue, not before it starts. Lowering
        // the gate first lets a call arriving as the switch lands run ahead of the calls already
        // queued behind it, which is the FIFO order this gate exists to provide.
        Runnable lowerGate = () -> {
            IterableProjectSwitchCallback switchCallback;
            while ((switchCallback = switchCallbacks.poll()) != null) {
                switchCallbacksToNotify.add(switchCallback);
            }
            // initializeInBackground parks its callback whenever initialization looks in flight,
            // which a switch makes true for its whole window. Nothing else drains them, so without
            // this they would never fire.
            IterableInitializationCallback initCallback;
            while ((initCallback = pendingCallbacks.poll()) != null) {
                initCallbacksToNotify.add(initCallback);
            }

            PendingProjectSwitch next = pendingSwitches.poll();
            nextSwitch.set(next);
            if (next != null) {
                // Handed to the queued request rather than lowered and raised again. Lowering it in
                // between leaves a window in which a brand new switchProject takes the gate first
                // and is then overtaken by this older queued request, so the SDK settles on the
                // project the app asked for second-to-last while both callbacks report success.
                inFlightSwitchApiKey = next.apiKey;
                switchCallbacks.addAll(next.callbacks);
                return;
            }
            isSwitchingProject = false;
            inFlightSwitchApiKey = null;
            isInitializing = false;
            isBackgroundInitialized = true;
        };

        Runnable notifyCallbacks = () -> {
            notifySwitchCallbacks(switchCallbacksToNotify, cleanTeardown);
            notifyInitializationCallbacks(initCallbacksToNotify);
            startPendingSwitch(nextSwitch.get());
        };

        DrainResult drainResult = operationQueue.processAll(executor, notifyCallbacks, lowerGate);
        if (drainResult == DrainResult.STARTED) {
            return;
        }

        // No drain will run, so nothing else is going to lower the gate.
        synchronized (initLock) {
            lowerGate.run();
        }
        if (drainResult == DrainResult.REJECTED) {
            // New calls run now that the gate is down, but the calls queued during the switch are
            // stranded. Report that as a noisy switch rather than silently dropping the callbacks.
            notifySwitchCallbacks(switchCallbacksToNotify, false);
            notifyInitializationCallbacks(initCallbacksToNotify);
            startPendingSwitch(nextSwitch.get());
            return;
        }
        notifyCallbacks.run();
    }

    /**
     * Runs a switch that was asked for while another was in flight, so the SDK ends up on the
     * project the app last asked for rather than the one it happened to be heading to.
     *
     * The gate it runs on is the one {@code lowerGate} handed over, still raised, so it does not
     * raise one of its own. Its callbacks moved into {@link #switchCallbacks} with the handover and
     * are delivered from there when it lands, together with any later request for the same project
     * that joined it in the meantime.
     */
    private static void startPendingSwitch(@Nullable PendingProjectSwitch pending) {
        if (pending == null) {
            return;
        }
        Context context = IterableApi.sharedInstance._applicationContext;
        if (context == null) {
            // Nothing can run this switch, and the gate it inherited would stay raised for the life
            // of the process. Release it instead, which reports a noisy switch to everyone waiting
            // on this request rather than leaving them with no result at all.
            IterableLogger.e(TAG, "switchProject: cannot run the switch that was requested during "
                    + "the previous one, the SDK has no application context");
            completeProjectSwitch(false);
            return;
        }
        IterableLogger.d(TAG, "switchProject: running the switch that was requested while the "
                + "previous one was in flight");
        // Posted rather than run inline, so it lands behind the callbacks for the switch that just
        // finished, which were posted to the same looper. Whatever the app does in those, in
        // particular re-identifying the user, then runs against the project it was told it was on
        // before this teardown starts.
        new Handler(Looper.getMainLooper()).post(() ->
                IterableProjectSwitcher.switchProject(context, pending.apiKey, pending.config, null, true));
    }

    private static void notifySwitchCallbacks(List<IterableProjectSwitchCallback> callbacks, boolean cleanTeardown) {
        if (callbacks.isEmpty()) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            for (IterableProjectSwitchCallback callback : callbacks) {
                try {
                    callback.onProjectSwitched(IterableProjectSwitchResult.from(cleanTeardown));
                } catch (Exception e) {
                    IterableLogger.e(TAG, "Exception in switchProject callback", e);
                }
            }
        });
    }

    /**
     * Callbacks that asked about initialization, not about the switch, so they get the plain
     * no-argument notification rather than the switch's teardown verdict.
     */
    private static void notifyInitializationCallbacks(List<IterableInitializationCallback> callbacks) {
        if (callbacks.isEmpty()) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            for (IterableInitializationCallback callback : callbacks) {
                try {
                    callback.onSDKInitialized();
                } catch (Exception e) {
                    IterableLogger.e(TAG, "Exception in pending initialization callback", e);
                }
            }
        });
    }

    /**
     * Runs project switch teardown and re-initialization off the main thread on the existing
     * background executor.
     */
    static void executeOnBackgroundExecutor(Runnable task) {
        ExecutorService executor;
        synchronized (initLock) {
            executor = ensureBackgroundExecutor();
        }
        executeOn(executor, task);
    }

    /**
     * Split out from {@link #executeOnBackgroundExecutor(Runnable)} so a test can supply an executor
     * that rejects, which is otherwise only reachable through a race.
     */
    @VisibleForTesting
    static void executeOn(ExecutorService executor, Runnable task) {
        try {
            executor.execute(task);
            return;
        } catch (RejectedExecutionException e) {
            // The executor was shut down after we picked it up but before execute(), which the drain
            // task's own shutdown makes reachable. Retry once on a fresh one.
            IterableLogger.w(TAG, "Background executor rejected the project switch, retrying on a fresh executor");
        }

        try {
            replaceIfCurrent(executor).execute(task);
        } catch (RejectedExecutionException e) {
            // Nothing will run the teardown, so the gate would stay raised forever with no callback.
            // Lower it and report a failed switch instead.
            IterableLogger.e(TAG, "Could not start the project switch: both executors rejected it", e);
            completeProjectSwitch(false);
        }
    }

    /**
     * Returns a live executor after {@code rejected} refused a task. Only swaps the shared executor
     * when {@code rejected} is the shared one, so a caller-supplied executor cannot take a healthy
     * shared executor down with it.
     */
    private static ExecutorService replaceIfCurrent(ExecutorService rejected) {
        synchronized (initLock) {
            if (backgroundExecutor == rejected) {
                swapBackgroundExecutor();
            }
            return ensureBackgroundExecutor();
        }
    }

    /**
     * Returns a usable background executor, replacing it first if the previous one was shut down
     * after draining the initial queue. Caller must hold {@link #initLock}.
     */
    private static ExecutorService ensureBackgroundExecutor() {
        if (backgroundExecutor == null || backgroundExecutor.isShutdown()) {
            swapBackgroundExecutor();
        }
        return backgroundExecutor;
    }

    /**
     * Swaps in a fresh executor first, then shuts down the old one. This ordering ensures
     * shutdownBackgroundExecutorAsync (which may still be pending from the old executor) cannot
     * kill the new one. Caller must hold {@link #initLock}.
     */
    private static void swapBackgroundExecutor() {
        ExecutorService oldExecutor = backgroundExecutor;
        backgroundExecutor = createExecutor();
        if (oldExecutor != null && !oldExecutor.isShutdown()) {
            oldExecutor.shutdownNow();
        }
    }

    //endregion

    /**
     * Shutdown the background executor for proper cleanup
     * Should be called during application shutdown or for testing
     */
    @VisibleForTesting
    static void shutdownBackgroundExecutor() {
        synchronized (initLock) {
            if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
                backgroundExecutor.shutdown();
                try {
                    if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        backgroundExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    backgroundExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Shutdown the given executor asynchronously to avoid blocking the executor thread itself.
     * The caller passes the exact executor instance that should be shut down, so a concurrent
     * reset() that swaps in a new executor cannot cause us to shut down the wrong one.
     */
    private static void shutdownBackgroundExecutorAsync(ExecutorService executorToShutdown) {
        if (executorToShutdown == null || executorToShutdown.isShutdown()) {
            return;
        }
        new Thread(() -> {
            try {
                executorToShutdown.shutdown();
                if (!executorToShutdown.awaitTermination(5, TimeUnit.SECONDS)) {
                    IterableLogger.w(TAG, "Background executor did not terminate gracefully, forcing shutdown");
                    executorToShutdown.shutdownNow();
                }
            } catch (InterruptedException e) {
                IterableLogger.w(TAG, "Interrupted while waiting for executor termination");
                executorToShutdown.shutdownNow();
                Thread.currentThread().interrupt();
            }
            IterableLogger.d(TAG, "Background executor shutdown completed");
        }, "IterableExecutorShutdown").start();
    }

    /**
     * Register a callback to be notified when SDK initialization completes.
     * If the SDK is already initialized, the callback is invoked immediately.
     *
     * @param callback The callback to be notified when initialization completes
     */
    static void onSDKInitialized(@NonNull IterableInitializationCallback callback) {
        callbackManager.addSubscriber(callback);
    }

    /**
     * Notify that initialization has completed - called by IterableApi.initialize()
     */
    static void notifyInitializationComplete() {
        callbackManager.notifyInitializationComplete();
    }

    /**
     * Simulate the "initializing" state for testing, without starting actual background init.
     * This allows tests to deterministically queue operations without race conditions.
     */
    @VisibleForTesting
    static void simulateInitializingState() {
        synchronized (initLock) {
            isInitializing = true;
            isBackgroundInitialized = false;
        }
    }

    /**
     * Simulate initialization completion for testing.
     * Marks initialization as complete and processes any queued operations.
     */
    @VisibleForTesting
    static void simulateInitializationComplete() {
        markInitializationComplete();
    }

    /**
     * Reset background initialization state - for testing only
     */
    @VisibleForTesting
    static void resetBackgroundInitializationState() {
        synchronized (initLock) {
            isInitializing = false;
            isBackgroundInitialized = false;
            isSwitchingProject = false;
            inFlightSwitchApiKey = null;
            operationQueue.clear();
            pendingCallbacks.clear();
            pendingInitActions.clear();
            switchCallbacks.clear();
            pendingSwitches.clear();
            callbackManager.reset();

            swapBackgroundExecutor();
        }
    }

    // ========================================
    // Test Support Methods
    // ========================================

    /**
     * Get the number of queued operations (for testing)
     */
    @VisibleForTesting
    static int getQueuedOperationCount() {
        return operationQueue.size();
    }

    /**
     * Drains the operation queue on a caller-supplied executor, so a test can supply one that
     * rejects. Only reachable through a race otherwise.
     */
    @VisibleForTesting
    static DrainResult processQueuedOperationsOn(ExecutorService executor) {
        return operationQueue.processAll(executor, null, null);
    }

    /**
     * Clear all queued operations (for testing)
     */
    @VisibleForTesting
    static void clearQueuedOperations() {
        operationQueue.clear();
    }

    /**
     * Get descriptions of all queued operations (for testing PII masking)
     */
    @VisibleForTesting
    static List<String> getQueuedOperationDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (QueuedOperation op : operationQueue.operations) {
            descriptions.add(op.getDescription());
        }
        return descriptions;
    }
}

/**
 * Manages initialization callbacks for the Iterable SDK.
 * Supports multiple subscribers and ensures callbacks are called on the main thread.
 */
class IterableInitializationCallbackManager {
    private static final String TAG = "IterableInitCallbackMgr";

    // Thread-safe collections for callback management
    private final CopyOnWriteArraySet<IterableInitializationCallback> subscribers = new CopyOnWriteArraySet<>();
    private final ConcurrentLinkedQueue<IterableInitializationCallback> oneTimeCallbacks = new ConcurrentLinkedQueue<>();

    private volatile boolean isInitialized = false;
    private final Object initLock = new Object();

    /**
     * Add a callback that will be called every time initialization completes.
     * If initialization has already completed, the callback is called immediately.
     *
     * @param callback The callback to add (must not be null)
     */
    void addSubscriber(@NonNull IterableInitializationCallback callback) {
        if (callback == null) {
            IterableLogger.w(TAG, "Cannot add null callback subscriber");
            return;
        }

        subscribers.add(callback);

        // If already initialized, call immediately on main thread
        synchronized (initLock) {
            if (isInitialized) {
                callCallbackOnMainThread(callback, "subscriber (immediate)");
                subscribers.remove(callback); // Auto-remove after calling
            }
        }
    }

    /**
     * Add a one-time callback that will be called once when initialization completes.
     * If initialization has already completed, the callback is called immediately.
     * This is used for the callback parameter in initialize() methods.
     *
     * @param callback The one-time callback to add (can be null)
     */
    void addOneTimeCallback(@Nullable IterableInitializationCallback callback) {
        if (callback == null) {
            return;
        }

        synchronized (initLock) {
            if (isInitialized) {
                // Call immediately if already initialized
                callCallbackOnMainThread(callback, "one-time (immediate)");
            } else {
                // Queue for later
                oneTimeCallbacks.offer(callback);
            }
        }
    }

    /**
     * Notify all callbacks that initialization has completed.
     * This should be called once when initialization finishes.
     */
    void notifyInitializationComplete() {
        synchronized (initLock) {
            if (isInitialized) {
                IterableLogger.d(TAG, "notifyInitializationComplete called but already initialized");
                return;
            }
            isInitialized = true;
        }

        IterableLogger.d(TAG, "Notifying initialization completion to " +
                         subscribers.size() + " subscribers and " +
                         oneTimeCallbacks.size() + " one-time callbacks");

        // Notify all subscribers and clear the list (auto-remove after calling)
        for (IterableInitializationCallback callback : subscribers) {
            callCallbackOnMainThread(callback, "subscriber");
        }
        subscribers.clear(); // Auto-remove all subscribers after calling them

        // Notify and clear one-time callbacks
        IterableInitializationCallback oneTimeCallback;
        while ((oneTimeCallback = oneTimeCallbacks.poll()) != null) {
            callCallbackOnMainThread(oneTimeCallback, "one-time");
        }
    }

    /**
     * Reset the initialization state - for testing only
     */
    void reset() {
        synchronized (initLock) {
            isInitialized = false;
            subscribers.clear();
            oneTimeCallbacks.clear();
        }
    }

    /**
     * Allows the next {@link #notifyInitializationComplete()} to fire again, without discarding
     * anything that is waiting to be notified. Used when the SDK re-runs initialize() for a new
     * project.
     */
    void clearInitializedFlag() {
        synchronized (initLock) {
            isInitialized = false;
        }
    }

    /**
     * Helper method to ensure callbacks are called on the main thread
     */
    private void callCallbackOnMainThread(@NonNull IterableInitializationCallback callback, String type) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread
            try {
                callback.onSDKInitialized();
                IterableLogger.d(TAG, "Called " + type + " callback on main thread");
            } catch (Exception e) {
                IterableLogger.e(TAG, "Exception in " + type + " initialization callback", e);
            }
        } else {
            // Post to main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    callback.onSDKInitialized();
                    IterableLogger.d(TAG, "Called " + type + " callback via main thread handler");
                } catch (Exception e) {
                    IterableLogger.e(TAG, "Exception in " + type + " initialization callback", e);
                }
            });
        }
    }
}
