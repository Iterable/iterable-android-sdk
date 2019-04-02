package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;

import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iterable.iterableapi.IterableInAppHandler.InAppResponse;
import com.iterable.iterableapi.IterableInAppMessage.Trigger.TriggerType;

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

    private final IterableApi api;
    private final Context context;
    private final IterableInAppStorage storage;
    private final IterableInAppHandler handler;
    private final double inAppDisplayInterval;

    private long lastSyncTime = 0;
    private long lastInAppShown = 0;

    IterableInAppManager(IterableApi iterableApi, IterableInAppHandler handler, double inAppDisplayInterval) {
        this(iterableApi,
                handler,
                inAppDisplayInterval,
                new IterableInAppFileStorage(iterableApi.getMainActivityContext()),
                IterableActivityMonitor.getInstance());
    }

    @VisibleForTesting
    IterableInAppManager(IterableApi iterableApi, IterableInAppHandler handler, double inAppDisplayInterval, IterableInAppStorage storage, IterableActivityMonitor activityMonitor) {
        this.api = iterableApi;
        this.context = iterableApi.getMainActivityContext();
        this.handler = handler;
        this.inAppDisplayInterval = inAppDisplayInterval;
        this.storage = storage;
        activityMonitor.addCallback(this);
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

    /**
     * Trigger a manual sync. This method is called automatically by the SDK, so there should be no
     * need to call this method from your app.
     */
    public void syncInApp() {
        this.api.getInAppMessages(10, new IterableHelper.IterableActionHandler() {
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
                        }
                        syncWithRemoteQueue(messages);
                        lastSyncTime = IterableUtil.currentTimeMillis();
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
    public void showMessage(IterableInAppMessage message, boolean consume, final IterableHelper.IterableActionHandler clickCallback) {
        Activity currentActivity = IterableActivityMonitor.getInstance().getCurrentActivity();
        // Prevent double display
        if (currentActivity != null) {
            if (IterableInAppManager.showIterableNotificationHTML(currentActivity, message.getContent().html, message.getMessageId(), new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String data) {
                    if (clickCallback != null) {
                        clickCallback.execute(data);
                    }
                    if (data != null && !data.isEmpty()) {
                        if (data.startsWith("action://")) {
                            // This is an action:// URL, pass that to the custom action handler
                            IterableActionRunner.executeAction(context, IterableAction.actionCustomAction(data), IterableActionSource.IN_APP);
                        } else if (data.startsWith("itbl://")) {
                            // Handle itbl:// URLs
                            handleInternalAction(data);
                        } else {
                            IterableActionRunner.executeAction(context, IterableAction.actionOpenUrl(data), IterableActionSource.IN_APP);
                        }
                    }
                    lastInAppShown = IterableUtil.currentTimeMillis();
                    scheduleProcessing();
                }
            }, message.getContent().backgroundAlpha, message.getContent().padding, true)) {
                if (consume) {
                    removeMessage(message);
                }
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
    }

    private boolean isMessageExpired(IterableInAppMessage message) {
        if (message.getExpiresAt() != null) {
            return IterableUtil.currentTimeMillis() > message.getExpiresAt().getTime();
        } else {
            return false;
        }
    }

    private void syncWithRemoteQueue(List<IterableInAppMessage> remoteQueue) {
        Map<String, IterableInAppMessage> remoteQueueMap = new HashMap<>();
        for (IterableInAppMessage message : remoteQueue) {
            remoteQueueMap.put(message.getMessageId(), message);
            if (storage.getMessage(message.getMessageId()) == null) {
                storage.addMessage(message);
            }
        }
        for (IterableInAppMessage localMessage : storage.getMessages()) {
            if (!remoteQueueMap.containsKey(localMessage.getMessageId())) {
                storage.removeMessage(localMessage);
            }
        }
        scheduleProcessing();
    }

    private void processMessages() {
        if (!IterableActivityMonitor.getInstance().isInForeground() || isShowingInApp() || !canShowInAppAfterPrevious()) {
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
                    showMessage(message);
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
        return IterableInAppHTMLNotification.getInstance() != null;
    }

    private double getSecondsSinceLastInApp() {
        return (IterableUtil.currentTimeMillis() - lastInAppShown) / 1000.0;
    }

    private boolean canShowInAppAfterPrevious() {
        return getSecondsSinceLastInApp() >= inAppDisplayInterval;
    }

    private void handleInternalAction(String url) {

    }

    /**
     * Displays an html rendered InApp Notification
     * @param context
     * @param htmlString
     * @param messageId
     * @param clickCallback
     * @param backgroundAlpha
     * @param padding
     */
    public static boolean showIterableNotificationHTML(Context context, String htmlString, String messageId, IterableHelper.IterableActionHandler clickCallback, double backgroundAlpha, Rect padding) {
        return showIterableNotificationHTML(context, htmlString, messageId, clickCallback, backgroundAlpha, padding, false);
    }

    public static boolean showIterableNotificationHTML(Context context, String htmlString, String messageId, final IterableHelper.IterableActionHandler clickCallback, double backgroundAlpha, Rect padding, boolean callbackOnCancel) {
        if (context instanceof Activity) {
            Activity currentActivity = (Activity) context;
            if (htmlString != null) {
                if (IterableInAppHTMLNotification.getInstance() != null) {
                    IterableLogger.w(TAG, "Skipping the in-app notification: another notification is already being displayed");
                    return false;
                }

                IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.createInstance(context, htmlString);
                notification.setTrackParams(messageId);
                notification.setCallback(clickCallback);
                notification.setBackgroundAlpha(backgroundAlpha);
                notification.setPadding(padding);
                notification.setOwnerActivity(currentActivity);

                if (callbackOnCancel) {
                    notification.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            clickCallback.execute(null);
                        }
                    });
                }

                notification.show();
                return true;
            }
        } else {
            IterableLogger.w(TAG, "To display in-app notifications, the context must be of an instance of: Activity");
        }
        return false;
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
}