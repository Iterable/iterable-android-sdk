package com.iterable.iterableapi;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.iterable.iterableapi.IterableInAppHandler.InAppResponse;
import com.iterable.iterableapi.IterableInAppMessage.Trigger.TriggerType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by David Truong dt@iterable.com.
 *
 * The IterableInAppManager handles creating and rendering different types of InApp Notifications received from the IterableApi
 */
public class IterableInAppManager implements IterableActivityMonitor.AppStateCallback {
    static final String TAG = "IterableInAppManager";
    static final long MOVE_TO_FOREGROUND_SYNC_INTERVAL_MS = 60 * 1000;
    static final int MESSAGES_TO_FETCH = 100;

    public interface Listener {
        void onInboxUpdated();
    }

    private final IterableApi api;
    private final Context context;
    private final IterableInAppStorage storage;
    private final IterableInAppHandler handler;
    private final IterableInAppDisplayer displayer;
    private final IterableActivityMonitor activityMonitor;
    private final double inAppDisplayInterval;
    private final List<Listener> listeners = new ArrayList<>();
    private long lastSyncTime = 0;
    private long lastInAppShown = 0;
    private boolean autoDisplayPaused = false;

    IterableInAppManager(IterableApi iterableApi, IterableInAppHandler handler, double inAppDisplayInterval, boolean useInMemoryStorageForInApps) {
        this(iterableApi,
                handler,
                inAppDisplayInterval,
                IterableInAppManager.getInAppStorageModel(iterableApi, useInMemoryStorageForInApps),
                IterableActivityMonitor.getInstance(),
                new IterableInAppDisplayer(IterableActivityMonitor.getInstance()));
    }

    @VisibleForTesting
    IterableInAppManager(IterableApi iterableApi,
                         IterableInAppHandler handler,
                         double inAppDisplayInterval,
                         IterableInAppStorage storage,
                         IterableActivityMonitor activityMonitor,
                         IterableInAppDisplayer displayer) {
        this.api = iterableApi;
        this.context = iterableApi.getMainActivityContext();
        this.handler = handler;
        this.inAppDisplayInterval = inAppDisplayInterval;
        this.storage = storage;
        this.displayer = displayer;
        this.activityMonitor = activityMonitor;
        this.activityMonitor.addCallback(this);

        syncInApp();
    }

    /**
     * Get the list of available in-app messages
     * This list is synchronized with the server by the SDK
     * @return A {@link List} of {@link IterableInAppMessage} objects
     */
    @NonNull
    public synchronized List<IterableInAppMessage> getMessages() {
        List<IterableInAppMessage> filteredList = new ArrayList<>();
        for (IterableInAppMessage message : storage.getMessages()) {
            if (!message.isConsumed() && !isMessageExpired(message)) {
                filteredList.add(message);
            }
        }
        return filteredList;
    }

    synchronized IterableInAppMessage getMessageById(String messageId) {
        return storage.getMessage(messageId);
    }

    /**
     * Get the list of inbox messages
     * @return A {@link List} of {@link IterableInAppMessage} objects stored in inbox
     */
    @NonNull
    public synchronized List<IterableInAppMessage> getInboxMessages() {
        List<IterableInAppMessage> filteredList = new ArrayList<>();
        for (IterableInAppMessage message : storage.getMessages()) {
            if (!message.isConsumed() && !isMessageExpired(message) && message.isInboxMessage()) {
                filteredList.add(message);
            }
        }
        return filteredList;
    }

    /**
     * Get the count of unread inbox messages
     * @return Unread inbox messages count
     */
    public synchronized int getUnreadInboxMessagesCount() {
        int unreadInboxMessageCount = 0;
        for (IterableInAppMessage message : getInboxMessages()) {
            if (!message.isRead()) {
                unreadInboxMessageCount++;
            }
        }
        return unreadInboxMessageCount;
    }

    public synchronized void setRead(@NonNull IterableInAppMessage message, boolean read) {
        setRead(message, read, null, null);
    }
    /**
     * Set the read flag on an inbox message
     * @param message Inbox message object retrieved from {@link IterableInAppManager#getInboxMessages()}
     * @param read Read state flag. true = read, false = unread
     * @param successHandler The callback which returns `success`.
     */
    public synchronized void setRead(@NonNull IterableInAppMessage message, boolean read, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        message.setRead(read);
        if (successHandler != null) {
            successHandler.onSuccess(new JSONObject()); // passing blank json object here as onSuccess is @Nonnull
        }
        notifyOnChange();
    }

    boolean isAutoDisplayPaused() {
        return autoDisplayPaused;
    }

    /**
     * Set a pause to prevent showing in-app messages automatically. By default the value is set to false.
     * @param paused Whether to pause showing in-app messages.
     */
    public void setAutoDisplayPaused(boolean paused) {
        this.autoDisplayPaused = paused;
        if (!paused) {
            scheduleProcessing();
        }
    }

    /**
     * Trigger a manual sync. This method is called automatically by the SDK, so there should be no
     * need to call this method from your app.
     */
    void syncInApp() {
        IterableLogger.printInfo();
        this.api.getInAppMessages(MESSAGES_TO_FETCH, new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(String payload) {
                if (payload != null && !payload.isEmpty()) {
                    try {
                        List<IterableInAppMessage> messages = new ArrayList<>();
                        JSONObject mainObject = new JSONObject(payload);
                        JSONArray jsonArray = mainObject.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject messageJson = jsonArray.optJSONObject(i);
                                IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson, null);
                                if (message != null) {
                                    messages.add(message);
                                }
                            }

                            syncWithRemoteQueue(messages);
                            lastSyncTime = IterableUtil.currentTimeMillis();
                        }
                    } catch (JSONException e) {
                        IterableLogger.e(TAG, e.toString());
                    }
                } else {
                    scheduleProcessing();
                }
            }
        });
    }

    /**
     * Clear all in-app messages.
     * Should be called on user logout.
     */
    void reset() {
        IterableLogger.printInfo();

        for (IterableInAppMessage message : storage.getMessages()) {
            storage.removeMessage(message);
        }

        notifyOnChange();
    }

    /**
     * Display the in-app message on the screen
     * @param message In-App message object retrieved from {@link IterableInAppManager#getMessages()}
     */
    public void showMessage(@NonNull IterableInAppMessage message) {
        showMessage(message, true, null);
    }

    public void showMessage(@NonNull IterableInAppMessage message, @NonNull IterableInAppLocation location) {
        showMessage(message, location == IterableInAppLocation.IN_APP, null, location);
    }

    /**
     * Display the in-app message on the screen. This method, by default, assumes the current location of activity as InApp. To pass
     * different inAppLocation as paramter, use showMessage method which takes in IterableAppLocation as a parameter.
     * @param message In-App message object retrieved from {@link IterableInAppManager#getMessages()}
     * @param consume A boolean indicating whether to remove the message from the list after showing
     * @param clickCallback A callback that is called when the user clicks on a link in the in-app message
     */
    public void showMessage(final @NonNull IterableInAppMessage message, boolean consume, final @Nullable IterableHelper.IterableUrlCallback clickCallback) {
        showMessage(message, consume, clickCallback, IterableInAppLocation.IN_APP);
    }

    public void showMessage(final @NonNull IterableInAppMessage message, boolean consume, final @Nullable IterableHelper.IterableUrlCallback clickCallback, @NonNull IterableInAppLocation inAppLocation) {
        if (displayer.showMessage(message, inAppLocation, new IterableHelper.IterableUrlCallback() {
            @Override
            public void execute(Uri url) {
                if (clickCallback != null) {
                    clickCallback.execute(url);
                }

                handleInAppClick(message, url);
                lastInAppShown = IterableUtil.currentTimeMillis();
                scheduleProcessing();
            }
        })) {
            setRead(message, true, null, null);
            if (consume) {
                message.markForDeletion(true);
            }
        }
    }

    /**
     * Remove message from the list
     * @param message The message to be removed
     */
    public synchronized void removeMessage(@NonNull IterableInAppMessage message) {
        removeMessage(message, null, null, null, null);
    }

    /**
     * Remove message from the list
     * @param message The message to be removed
     * @param source Source from where the message removal occured. Use IterableInAppDeleteActionType for available sources
     * @param clickLocation Where was the message clicked. Use IterableInAppLocation for available Click Locations
     */
    public synchronized void removeMessage(@NonNull IterableInAppMessage message, @NonNull IterableInAppDeleteActionType source, @NonNull IterableInAppLocation clickLocation) {
        removeMessage(message, source, clickLocation, null, null);
    }

    /**
     * Remove message from the list
     * @param message The message to be removed
     * @param source Source from where the message removal occured. Use IterableInAppDeleteActionType for available sources
     * @param clickLocation Where was the message clicked. Use IterableInAppLocation for available Click Locations
     * @param successHandler The callback which returns `success`.
     * @param failureHandler The callback which returns `failure`.
     */
    public synchronized void removeMessage(@NonNull IterableInAppMessage message, @Nullable IterableInAppDeleteActionType source, @Nullable IterableInAppLocation clickLocation, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        IterableLogger.printInfo();
        if (message != null) {
            message.setConsumed(true);
            api.inAppConsume(message, source, clickLocation, successHandler, failureHandler);
        }
        notifyOnChange();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void handleInAppClick(@NonNull IterableInAppMessage message, @Nullable Uri url) {
        IterableLogger.printInfo();

        if (url != null && !url.toString().isEmpty()) {
            String urlString = url.toString();
            if (urlString.startsWith(IterableConstants.URL_SCHEME_ACTION)) {
                // This is an action:// URL, pass that to the custom action handler
                String actionName = urlString.replace(IterableConstants.URL_SCHEME_ACTION, "");
                IterableActionRunner.executeAction(context, IterableAction.actionCustomAction(actionName), IterableActionSource.IN_APP);
            } else if (urlString.startsWith(IterableConstants.URL_SCHEME_ITBL)) {
                // Handle itbl:// URLs, pass that to the custom action handler for compatibility
                String actionName = urlString.replace(IterableConstants.URL_SCHEME_ITBL, "");
                IterableActionRunner.executeAction(context, IterableAction.actionCustomAction(actionName), IterableActionSource.IN_APP);
            } else if (urlString.startsWith(IterableConstants.URL_SCHEME_ITERABLE)) {
                // Handle iterable:// URLs - reserved for actions defined by the SDK only
                String actionName = urlString.replace(IterableConstants.URL_SCHEME_ITERABLE, "");
                handleIterableCustomAction(actionName, message);
            } else {
                IterableActionRunner.executeAction(context, IterableAction.actionOpenUrl(urlString), IterableActionSource.IN_APP);
            }
        }
    }

    /**
     * Remove message from the queue
     * This will actually remove it from the local queue
     * This should only be called when a silent push is received
     * @param messageId messageId of the message to be removed
     */
    synchronized void removeMessage(String messageId) {
        IterableInAppMessage message = storage.getMessage(messageId);
        if (message != null) {
            storage.removeMessage(message);
        }
        notifyOnChange();
    }

    private boolean isMessageExpired(IterableInAppMessage message) {
        if (message.getExpiresAt() != null) {
            return IterableUtil.currentTimeMillis() > message.getExpiresAt().getTime();
        } else {
            return false;
        }
    }

    private void syncWithRemoteQueue(List<IterableInAppMessage> remoteQueue) {
        boolean changed = false;
        Map<String, IterableInAppMessage> remoteQueueMap = new HashMap<>();

        for (IterableInAppMessage message : remoteQueue) {
            remoteQueueMap.put(message.getMessageId(), message);

            boolean isInAppStored = storage.getMessage(message.getMessageId()) != null;

            if (!isInAppStored) {
                storage.addMessage(message);
                onMessageAdded(message);

                changed = true;
            }

            if (isInAppStored) {
                IterableInAppMessage localMessage = storage.getMessage(message.getMessageId());

                boolean shouldOverwriteInApp = !localMessage.isRead() && message.isRead();

                if (shouldOverwriteInApp) {
                    localMessage.setRead(message.isRead());

                    changed = true;
                }
            }
        }

        for (IterableInAppMessage localMessage : storage.getMessages()) {
            if (!remoteQueueMap.containsKey(localMessage.getMessageId())) {
                // Mark message as consumed before removing it to prevent it from being displayed
                localMessage.setConsumed(true);
                api.inAppConsume(localMessage, null, null, null, null);
                
                storage.removeMessage(localMessage);

                changed = true;
            }
        }

        scheduleProcessing();

        if (changed) {
            notifyOnChange();
        }
    }

    private List<IterableInAppMessage> getMessagesSortedByPriorityLevel(List<IterableInAppMessage> messages) {
        List<IterableInAppMessage> messagesByPriorityLevel = messages;

        Collections.sort(messagesByPriorityLevel, new Comparator<IterableInAppMessage>() {
            @Override
            public int compare(IterableInAppMessage message1, IterableInAppMessage message2) {
                if (message1.getPriorityLevel() < message2.getPriorityLevel()) {
                    return -1;
                } else if (message1.getPriorityLevel() == message2.getPriorityLevel()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        return messagesByPriorityLevel;
    }

    private void processMessages() {
        if (!activityMonitor.isInForeground() || isShowingInApp() || !canShowInAppAfterPrevious() || isAutoDisplayPaused()) {
            return;
        }

        IterableLogger.printInfo();

        List<IterableInAppMessage> messages = getMessages();
        List<IterableInAppMessage> messagesByPriorityLevel = getMessagesSortedByPriorityLevel(messages);

        for (IterableInAppMessage message : messagesByPriorityLevel) {
            if (!message.isProcessed() && !message.isConsumed() && message.getTriggerType() == TriggerType.IMMEDIATE && !message.isRead()) {
                IterableLogger.d(TAG, "Calling onNewInApp on " + message.getMessageId());
                InAppResponse response = handler.onNewInApp(message);
                IterableLogger.d(TAG, "Response: " + response);
                message.setProcessed(true);

                if (message.isJsonOnly()) {
                    setRead(message, true, null, null);
                    message.setConsumed(true);
                    api.inAppConsume(message, null, null, null, null);
                    return;
                }

                if (response == InAppResponse.SHOW) {
                    boolean consume = !message.isInboxMessage();
                    showMessage(message, consume, null);
                    return;
                }
            }
        }
    }

    void scheduleProcessing() {
        IterableLogger.printInfo();
        if (canShowInAppAfterPrevious()) {
            processMessages();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    processMessages();
                }
            }, (long) ((inAppDisplayInterval - getSecondsSinceLastInApp() + 2.0) * 1000));
        }
    }

    private void onMessageAdded(IterableInAppMessage message) {
        if (!message.isRead()) {
            api.trackInAppDelivery(message);
        }
    }

    private boolean isShowingInApp() {
        return displayer.isShowingInApp();
    }

    private double getSecondsSinceLastInApp() {
        return (IterableUtil.currentTimeMillis() - lastInAppShown) / 1000.0;
    }

    private boolean canShowInAppAfterPrevious() {
        return getSecondsSinceLastInApp() >= inAppDisplayInterval;
    }

    private void handleIterableCustomAction(String actionName, IterableInAppMessage message) {
        if (IterableConstants.ITERABLE_IN_APP_ACTION_DELETE.equals(actionName)) {
            removeMessage(message, IterableInAppDeleteActionType.DELETE_BUTTON, IterableInAppLocation.IN_APP, null, null);
        }
    }

    private static IterableInAppStorage getInAppStorageModel(IterableApi iterableApi, boolean useInMemoryForInAppStorage) {
        if (useInMemoryForInAppStorage) {
            checkAndDeleteUnusedInAppFileStorage(iterableApi.getMainActivityContext());

            return new IterableInAppMemoryStorage();
        } else {
            return new IterableInAppFileStorage(iterableApi.getMainActivityContext());
        }
    }

    private static void checkAndDeleteUnusedInAppFileStorage(Context context) {
        File sdkFilesDirectory = IterableUtil.getSDKFilesDirectory(context);
        File inAppContentFolder = IterableUtil.getDirectory(sdkFilesDirectory, "IterableInAppFileStorage");
        File inAppBlob = new File(inAppContentFolder, "itbl_inapp.json");

        if (inAppBlob.exists()) {
            inAppBlob.delete();
        }
    }

    @Override
    public void onSwitchToForeground() {
        if (IterableUtil.currentTimeMillis() - lastSyncTime > MOVE_TO_FOREGROUND_SYNC_INTERVAL_MS) {
            syncInApp();
        } else {
            scheduleProcessing();
        }
    }

    @Override
    public void onSwitchToBackground() {

    }

    public void addListener(@NonNull Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(@NonNull Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void notifyOnChange() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (Listener listener : listeners) {
                        listener.onInboxUpdated();
                    }
                }
            }
        });
    }
}