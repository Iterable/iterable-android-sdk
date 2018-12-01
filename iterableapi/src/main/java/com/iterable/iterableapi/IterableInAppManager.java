package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;

import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iterable.iterableapi.IterableInAppHandler.InAppResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by David Truong dt@iterable.com.
 *
 * The IterableInAppManager handles creating and rendering different types of InApp Notifications received from the IterableApi
 */
public class IterableInAppManager {
    static final String TAG = "IterableInAppManager";
    static final int IN_APP_DELAY_SECONDS = 30;

    private final IterableInAppStorage storage = new IterableInAppMemoryStorage();
    private final IterableInAppHandler handler;

    private long lastInAppShown = 0;

    IterableInAppManager(IterableInAppHandler handler) {
        this.handler = handler;
    }

    /**
     *
     * @return
     */
    public synchronized List<IterableInAppMessage> getMessages() {
        List<IterableInAppMessage> filteredList = new ArrayList<>();
        for (IterableInAppMessage message : storage.getMessages()) {
            if (!message.isConsumed()) {
                filteredList.add(message);
            }
        }
        return filteredList;
    }

    public void syncInApp() {
        IterableApi.getInstance().getInAppMessages(10, new IterableHelper.IterableActionHandler() {
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
                                IterableInAppMessage message = IterableInAppMessage.fromJSON(storage, messageJson);
                                if (message != null) {
                                    messages.add(message);
                                }
                            }
                        }
                        syncWithRemoteQueue(messages);
                    } catch (JSONException e) {
                        IterableLogger.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    public void showMessage(IterableInAppMessage message) {
        showMessage(message, true, null);
    }

    public void showMessage(IterableInAppMessage message, boolean consume, final IterableHelper.IterableActionHandler clickCallback) {
        Activity currentActivity = IterableActivityMonitor.getCurrentActivity();
        // Prevent double display
        if (currentActivity != null) {
            if (IterableInAppManager.showIterableNotificationHTML(currentActivity, message.getContent().html, message.getMessageId(), new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String data) {
                    if (clickCallback != null) {
                        clickCallback.execute(data);
                    }
                    if (data != null && data.startsWith("itbl://")) {
                        IterableActionRunner.executeAction(IterableApi.getInstance().getMainActivityContext(), IterableAction.actionCustomAction(data), IterableActionSource.IN_APP);
                    } else {
                        IterableActionRunner.executeAction(IterableApi.getInstance().getMainActivityContext(), IterableAction.actionOpenUrl(data), IterableActionSource.IN_APP);
                    }
                    lastInAppShown = System.currentTimeMillis();
                    scheduleProcessing();
                }
            }, 0.0, message.getContent().padding, true)) {
                if (consume) {
                    removeMessage(message);
                }
            }
        }
    }

    public synchronized void removeMessage(IterableInAppMessage message) {
        message.setConsumed(true);
        IterableApi.getInstance().inAppConsume(message.getMessageId());
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
        if (!IterableActivityMonitor.isInForeground() || isShowingInApp() || !canShowInAppAfterPrevious()) {
            return;
        }

        IterableLogger.d(TAG, "processMessages");

        List<IterableInAppMessage> messages = getMessages();
        for (IterableInAppMessage message : messages) {
            if (!message.isProcessed() && !message.isConsumed()) {
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
            }, (getSecondsSinceLastInApp() + 2) * 1000);
        }
    }

    private boolean isShowingInApp() {
        return IterableInAppHTMLNotification.getInstance() != null;
    }

    private int getSecondsSinceLastInApp() {
        return (int) ((System.currentTimeMillis() - lastInAppShown) / 1000);
    }

    private boolean canShowInAppAfterPrevious() {
        return getSecondsSinceLastInApp() >= IN_APP_DELAY_SECONDS;
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

}