package com.iterable.iterableapi;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import com.iterable.iterableapi.IterableInAppHandler.InAppResponse;
import com.iterable.iterableapi.IterableInAppMessage.Trigger.TriggerType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

    IterableInAppManager(IterableApi iterableApi, IterableInAppHandler handler, double inAppDisplayInterval) {
        this(iterableApi,
                handler,
                inAppDisplayInterval,
                new IterableInAppFileStorage(iterableApi.getMainActivityContext()),
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
    }

    /**
     * Get the list of available in-app messages
     * This list is synchronized with the server by the SDK
     * @return A {@link List} of {@link IterableInAppMessage} objects
     */
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

    /**
     * Set the read flag on an inbox message
     * @param message Inbox message object retrieved from {@link IterableInAppManager#getInboxMessages()}
     * @param read Read state flag. true = read, false = unread
     */
    public synchronized void setRead(IterableInAppMessage message, boolean read) {
        message.setRead(read);
        notifyOnChange();
    }

    /**
     * Trigger a manual sync. This method is called automatically by the SDK, so there should be no
     * need to call this method from your app.
     */
    void syncInApp() {
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
                                IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson);
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
                }
            }
        });
    }

    /**
     * Display the in-app message on the screen
     * @param message In-App message object retrieved from {@link IterableInAppManager#getMessages()}
     */
    public void showMessage(IterableInAppMessage message) {
        showMessage(message, true, null);
    }

    /**
     * Display the in-app message on the screen
     * @param message In-App message object retrieved from {@link IterableInAppManager#getMessages()}
     * @param consume A boolean indicating whether to remove the message from the list after showing
     * @param clickCallback A callback that is called when the user clicks on a link in the in-app message
     */
    public void showMessage(final IterableInAppMessage message, boolean consume, final IterableHelper.IterableUrlCallback clickCallback) {
        if (displayer.showMessage(message, new IterableHelper.IterableUrlCallback() {
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
            setRead(message, true);
            if (consume) {
                removeMessage(message);
            }
        }
    }

    /**
     * Remove message from the list
     * @param message The message to be removed
     */
    public synchronized void removeMessage(IterableInAppMessage message) {
        message.setConsumed(true);
        api.inAppConsume(message.getMessageId());
        notifyOnChange();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void handleInAppClick(IterableInAppMessage message, Uri url) {
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
            if (storage.getMessage(message.getMessageId()) == null) {
                storage.addMessage(message);
                changed = true;
            }
        }
        for (IterableInAppMessage localMessage : storage.getMessages()) {
            if (!remoteQueueMap.containsKey(localMessage.getMessageId())) {
                storage.removeMessage(localMessage);
                changed = true;
            }
        }
        scheduleProcessing();
        if (changed) {
            notifyOnChange();
        }
    }

    private void processMessages() {
        if (!activityMonitor.isInForeground() || isShowingInApp() || !canShowInAppAfterPrevious()) {
            return;
        }

        IterableLogger.d(TAG, "processMessages");

        List<IterableInAppMessage> messages = getMessages();
        for (IterableInAppMessage message : messages) {
            if (!message.isProcessed() && !message.isConsumed() && message.getTriggerType() == TriggerType.IMMEDIATE) {
                IterableLogger.d(TAG, "Calling onNewInApp on " + message.getMessageId());
                InAppResponse response = handler.onNewInApp(message);
                IterableLogger.d(TAG, "Response: " + response);
                message.setProcessed(true);
                if (response == InAppResponse.SHOW) {
                    boolean consume = !message.isInboxMessage();
                    showMessage(message, consume, null);
                    return;
                }
            }
        }
    }

    void scheduleProcessing() {
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
            removeMessage(message);
        }
    }

    @Override
    public void onSwitchToForeground() {
        scheduleProcessing();
        if (IterableUtil.currentTimeMillis() - lastSyncTime > MOVE_TO_FOREGROUND_SYNC_INTERVAL_MS) {
            syncInApp();
        }
    }

    @Override
    public void onSwitchToBackground() {

    }

    public void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void notifyOnChange() {
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.onInboxUpdated();
            }
        }
    }
}