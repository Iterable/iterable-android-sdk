package com.iterable.iterableapi;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * No-op {@link IterableInAppManager} returned by {@link IterableApi#getInAppManager()} when the SDK
 * has not been initialized. Every method is a benign no-op so callers never crash for a
 * pre-initialization usage error. Use {@link IterableApi#getInAppManagerOrNull()} to detect this
 * state explicitly.
 */
class EmptyInAppManager extends IterableInAppManager {
    private static final String TAG = "EmptyInAppManager";

    EmptyInAppManager() {
        super();
    }

    private void logNotInitialized(String method) {
        IterableLogger.e(TAG, method + " called before IterableApi was initialized; no-op. " +
                "Call IterableApi.initialize() in Application#onCreate.");
    }

    @NonNull
    @Override
    public synchronized List<IterableInAppMessage> getMessages() {
        logNotInitialized("getMessages()");
        return Collections.emptyList();
    }

    @Override
    synchronized IterableInAppMessage getMessageById(String messageId) {
        logNotInitialized("getMessageById()");
        return null;
    }

    @NonNull
    @Override
    public synchronized List<IterableInAppMessage> getInboxMessages() {
        logNotInitialized("getInboxMessages()");
        return Collections.emptyList();
    }

    @Override
    public synchronized int getUnreadInboxMessagesCount() {
        logNotInitialized("getUnreadInboxMessagesCount()");
        return 0;
    }

    @Override
    public synchronized void setRead(@NonNull IterableInAppMessage message, boolean read) {
        logNotInitialized("setRead()");
    }

    @Override
    public synchronized void setRead(@NonNull IterableInAppMessage message, boolean read, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        logNotInitialized("setRead()");
        if (failureHandler != null) {
            failureHandler.onFailure("Iterable SDK is not initialized", null);
        }
    }

    @Override
    public void setAutoDisplayPaused(boolean paused) {
        logNotInitialized("setAutoDisplayPaused()");
    }

    @Override
    public void resumeInAppDisplay() {
        logNotInitialized("resumeInAppDisplay()");
    }

    @Override
    void syncInApp() {
        logNotInitialized("syncInApp()");
    }

    @Override
    void reset() {
        logNotInitialized("reset()");
    }

    @Override
    public void showMessage(@NonNull IterableInAppMessage message) {
        logNotInitialized("showMessage()");
    }

    @Override
    public void showMessage(@NonNull IterableInAppMessage message, @NonNull IterableInAppLocation location) {
        logNotInitialized("showMessage()");
    }

    @Override
    public void showMessage(final @NonNull IterableInAppMessage message, boolean consume, final @Nullable IterableHelper.IterableUrlCallback clickCallback) {
        logNotInitialized("showMessage()");
    }

    @Override
    public void showMessage(final @NonNull IterableInAppMessage message, boolean consume, final @Nullable IterableHelper.IterableUrlCallback clickCallback, @NonNull IterableInAppLocation inAppLocation) {
        logNotInitialized("showMessage()");
    }

    @Override
    public synchronized void removeMessage(@NonNull IterableInAppMessage message) {
        logNotInitialized("removeMessage()");
    }

    @Override
    public synchronized void removeMessage(@NonNull IterableInAppMessage message, @NonNull IterableInAppDeleteActionType source, @NonNull IterableInAppLocation clickLocation) {
        logNotInitialized("removeMessage()");
    }

    @Override
    public synchronized void removeMessage(@NonNull IterableInAppMessage message, @Nullable IterableInAppDeleteActionType source, @Nullable IterableInAppLocation clickLocation, @Nullable IterableHelper.SuccessHandler successHandler, @Nullable IterableHelper.FailureHandler failureHandler) {
        logNotInitialized("removeMessage()");
        if (failureHandler != null) {
            failureHandler.onFailure("Iterable SDK is not initialized", null);
        }
    }

    @Override
    synchronized void removeMessage(String messageId) {
        logNotInitialized("removeMessage()");
    }

    @Override
    public void handleInAppClick(@NonNull IterableInAppMessage message, @Nullable Uri url) {
        logNotInitialized("handleInAppClick()");
    }

    @Override
    public void onSwitchToForeground() {
    }

    @Override
    public void onSwitchToBackground() {
    }

    @Override
    public void addListener(@NonNull Listener listener) {
        logNotInitialized("addListener()");
    }

    @Override
    public void removeListener(@NonNull Listener listener) {
        logNotInitialized("removeListener()");
    }

    @Override
    public void notifyOnChange() {
    }
}
