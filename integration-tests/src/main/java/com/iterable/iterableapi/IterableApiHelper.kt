package com.iterable.iterableapi

class IterableApiHelper {

    public fun syncInAppMessages() {
        IterableApi.getInstance().inAppManager.syncInApp()
    }

    // Replaces the SDK singleton with a fresh instance. Lives here because
    // `sharedInstance` is package-private to com.iterable.iterableapi; tests in
    // other packages can't reach it. Lets a test start from a known-empty
    // in-memory identity/unknown-user state.
    fun resetSharedInstance() {
        IterableApi.sharedInstance = IterableApi()
    }

    // `_apiKey` and `config` are package-private. The project switch screen and test read the live
    // project through these instead of widening SDK visibility.
    fun currentApiKey(): String? = IterableApi.getInstance()._apiKey

    fun currentBaseUrl(): String? = IterableApi.getInstance().config?.dataRegion?.endpoint
}