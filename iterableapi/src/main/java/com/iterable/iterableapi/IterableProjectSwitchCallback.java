package com.iterable.iterableapi;

/**
 * Callback for {@link IterableApi#switchProject}.
 *
 * This is a single-method interface so that a lambda receives the teardown result.
 * {@link IterableInitializationCallback} cannot serve this purpose: its only abstract method takes
 * no arguments, so a lambda would bind to that one and silently discard the result.
 */
public interface IterableProjectSwitchCallback {
    /**
     * Called on the main thread once the SDK is running on the new project. The SDK is on the new
     * project by the time this runs, whatever the value of {@code cleanTeardown}.
     *
     * @param cleanTeardown true when every teardown step completed cleanly. False means at least one
     *                      cleanup step was noisy, or that no device disable was confirmed for the
     *                      previous project. False never means the switch failed or was rolled back,
     *                      so the right response is the same either way: carry on and re-identify
     *                      the user with {@link IterableApi#setEmail(String)} or
     *                      {@link IterableApi#setUserId(String)}.
     *                      <p>
     *                      False is expected in normal operation and is not an error. An app that
     *                      does not use push registration, or that has no device token yet, will
     *                      always see false, because the switch could not confirm a device disable
     *                      for the previous project.
     */
    void onProjectSwitched(boolean cleanTeardown);
}
