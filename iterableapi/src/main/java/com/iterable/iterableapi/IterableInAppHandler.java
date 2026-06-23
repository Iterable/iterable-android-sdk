package com.iterable.iterableapi;

import androidx.annotation.NonNull;

public interface IterableInAppHandler {
    enum InAppResponse {
        /** Display the in-app message now. */
        SHOW,
        /** Do not display the in-app message; it is marked processed and will not be reconsidered. */
        SKIP,
        /**
         * Do not display the in-app message right now, but leave it pending so the SDK asks again on
         * a later display pass (e.g. the next foreground, sync, or newly arrived message). Use this
         * for temporary, per-message suppression — for example while a splash screen is showing.
         */
        DEFER
    }

    @NonNull
    InAppResponse onNewInApp(@NonNull IterableInAppMessage message);
}
