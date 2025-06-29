package com.iterable.iterableapi

import androidx.annotation.NonNull

class IterableDefaultInAppHandler : IterableInAppHandler {
    @NonNull
    override fun onNewInApp(@NonNull message: IterableInAppMessage): IterableInAppHandler.InAppResponse {
        return IterableInAppHandler.InAppResponse.SHOW
    }
}
