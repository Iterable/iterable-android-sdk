package com.iterable.iterableapi;

/**
 * The outcome of {@link IterableApi#switchProject}.
 *
 * Both values mean the SDK is on the new project. They differ only in whether leaving the previous
 * project was tidy. Neither is a failure, and neither is a reason to retry the switch or to show the
 * user an error: the response to both is the same, carry on and re-identify the user.
 *
 * The exception is a switch made before the SDK was initialized. There is no previous project, so
 * there {@link #SWITCHED_WITH_WARNINGS} means the initialization itself failed or timed out.
 */
public enum IterableProjectSwitchResult {
    /** Every teardown step completed cleanly at the point the callback fired. */
    SWITCHED_CLEANLY,

    /**
     * The SDK is on the new project, but leaving the previous one was not completely tidy: either a
     * cleanup step was noisy, or no device disable could be confirmed for the previous project.
     *
     * This is expected in normal operation rather than exceptional. An app that does not use push
     * registration, or that has no device token yet, will always see it, because there was no device
     * disable to confirm. Check the logs for the specific step if you need to know which one it was.
     *
     * Called before the SDK was initialized, there is no previous project to leave, and this instead
     * means that initialization failed or timed out. The SDK holds the requested API key and config,
     * but its initialization may not have completed, so later SDK calls may not take effect. Check
     * the logs. Calling {@code switchProject} again with the same project does not retry it: that is
     * a no-op reporting {@link #SWITCHED_CLEANLY}.
     */
    SWITCHED_WITH_WARNINGS;

    /**
     * Internal plumbing carries a boolean for "was the teardown clean", set by the individual
     * teardown steps. This converts at the callback boundary so the public contract is the named
     * result rather than a boolean whose meaning has to be explained.
     */
    static IterableProjectSwitchResult from(boolean cleanTeardown) {
        return cleanTeardown ? SWITCHED_CLEANLY : SWITCHED_WITH_WARNINGS;
    }
}
