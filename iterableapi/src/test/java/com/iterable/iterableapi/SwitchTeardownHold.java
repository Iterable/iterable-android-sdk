package com.iterable.iterableapi;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds a project switch at the end of its teardown, once the previous project's identity and
 * managers are gone and before the new project is initialized, so a test can act in that window
 * without racing the switch.
 *
 * It works by keeping the previous project's device disable from confirming, which the switch
 * waits on for up to {@link IterableProjectSwitcher#disableDispatchTimeoutMs}. That needs auto push
 * registration on and a user identified on the previous project, or there is no disable to hold.
 * Installing it replaces {@link IterablePushRegistration#instance}; the test restores it.
 */
final class SwitchTeardownHold {
    private final AtomicReference<IterablePushRegistrationData> heldDisable = new AtomicReference<>();

    static SwitchTeardownHold install() {
        SwitchTeardownHold hold = new SwitchTeardownHold();
        IterablePushRegistration.instance = new IterablePushRegistration.IterablePushRegistrationImpl() {
            @Override
            void executePushRegistrationTask(IterablePushRegistrationData data) {
                if (data.pushRegistrationAction == IterablePushRegistrationData.PushRegistrationAction.DISABLE) {
                    hold.heldDisable.set(data);
                }
            }
        };
        return hold;
    }

    /** Returns once the switch has released the previous project's managers. */
    void awaitTeardown() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (heldDisable.get() != null && IterableApi.getInstance().unknownUserManager == null) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("The switch never reached the end of its teardown");
    }

    /** Confirms the held disable, which lets the switch re-initialize on the new project. */
    void release() {
        IterablePushRegistrationData data = heldDisable.get();
        if (data != null && data.dispatchListener != null) {
            data.dispatchListener.onDispatched(true);
        }
    }
}
