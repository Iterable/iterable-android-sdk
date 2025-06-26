package com.iterable.iterableapi

import androidx.annotation.VisibleForTesting

internal object IterablePushRegistration {

    @VisibleForTesting
    var instance = IterablePushRegistrationImpl()

    fun executePushRegistrationTask(data: IterablePushRegistrationData) {
        instance.executePushRegistrationTask(data)
    }

    class IterablePushRegistrationImpl {
        fun executePushRegistrationTask(data: IterablePushRegistrationData) {
            IterablePushRegistrationTask().execute(data)
        }
    }
}
