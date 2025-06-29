package com.iterable.iterableapi

import androidx.annotation.NonNull

interface IterableInAppHandler {
    enum class InAppResponse {
        SHOW,
        SKIP
    }

    @NonNull
    fun onNewInApp(@NonNull message: IterableInAppMessage): InAppResponse
}
