package com.iterable.androidsdk.iterableapi

import com.iterable.iterableapi.IterableApi

class IterableInternal {
    companion object {
        fun syncInApp() {
            IterableApi.getInstance().inAppManager.syncInApp()
        }

        fun syncEmbedded() {
            IterableApi.getInstance().embeddedManager.syncMessages()
        }
    }
}