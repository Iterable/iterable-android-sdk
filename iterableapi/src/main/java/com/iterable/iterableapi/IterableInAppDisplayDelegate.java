package com.iterable.iterableapi;

import androidx.annotation.NonNull;

/**
 * Delegate that gives the app real-time, per-message control over whether the SDK is allowed to
 * automatically display a given in-app message at the moment it is about to be shown.
 *
 * When set via {@link IterableConfig.Builder#setInAppDisplayDelegate(IterableInAppDisplayDelegate)},
 * this takes precedence over the global {@link IterableInAppManager#setAutoDisplayPaused(boolean)} flag.
 */
public interface IterableInAppDisplayDelegate {
    /**
     * Called right before the SDK attempts to automatically display an in-app message.
     * @param message The in-app message about to be displayed.
     * @return {@code true} to pause/skip displaying this message for now (it will be reconsidered
     *         on a later display pass); {@code false} to allow it to be shown.
     */
    boolean isAutoDisplayPaused(@NonNull IterableInAppMessage message);
}
