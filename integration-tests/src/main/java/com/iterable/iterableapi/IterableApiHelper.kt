package com.iterable.iterableapi

class IterableApiHelper {

    public fun syncInAppMessages() {
        IterableApi.getInstance().inAppManager.syncInApp()
    }
}