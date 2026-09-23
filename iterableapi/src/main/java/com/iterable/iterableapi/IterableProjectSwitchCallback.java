package com.iterable.iterableapi;

import androidx.annotation.NonNull;

/**
 * Callback for {@link IterableApi#switchProject}.
 *
 * This is a single-method interface so a lambda receives the result.
 * {@link IterableInitializationCallback} cannot serve this purpose: its only abstract method takes
 * no arguments, so a lambda would bind to that one and silently discard the result.
 */
public interface IterableProjectSwitchCallback {
    /**
     * Called on the main thread once the SDK is running on the new project. The SDK is on the new
     * project by the time this runs, whatever the result says.
     *
     * @param result {@link IterableProjectSwitchResult#SWITCHED_CLEANLY} when every teardown step
     *               completed cleanly, {@link IterableProjectSwitchResult#SWITCHED_WITH_WARNINGS}
     *               when a cleanup step was noisy or no device disable was confirmed for the previous
     *               project. Neither is a failure and neither was rolled back, so the response to
     *               both is the same: carry on and re-identify the user with
     *               {@link IterableApi#setEmail(String)} or {@link IterableApi#setUserId(String)}.
     */
    void onProjectSwitched(@NonNull IterableProjectSwitchResult result);
}
