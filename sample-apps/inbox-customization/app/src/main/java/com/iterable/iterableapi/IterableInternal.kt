package com.iterable.iterableapi

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