package com.iterable.iterableapi

/**
 * No-op [IterableEmbeddedManager] returned by [IterableApi.getEmbeddedManager] when the SDK has not
 * been initialized. Every method is a benign no-op so callers never crash for a pre-initialization
 * usage error. Use [IterableApi.getEmbeddedManagerOrNull] to detect this state explicitly.
 */
internal class EmptyEmbeddedManager : IterableEmbeddedManager() {

    private val emptySessionManager = EmbeddedSessionManager()

    private fun logNotInitialized(method: String) {
        IterableLogger.e(TAG, "$method called before IterableApi was initialized; no-op. " +
                "Call IterableApi.initialize() in Application#onCreate.")
    }

    override fun addUpdateListener(updateHandler: IterableEmbeddedUpdateHandler) {
        logNotInitialized("addUpdateListener()")
    }

    override fun removeUpdateListener(updateHandler: IterableEmbeddedUpdateHandler) {
        logNotInitialized("removeUpdateListener()")
    }

    override fun getUpdateHandlers(): List<IterableEmbeddedUpdateHandler> {
        logNotInitialized("getUpdateHandlers()")
        return emptyList()
    }

    override fun getEmbeddedSessionManager(): EmbeddedSessionManager {
        logNotInitialized("getEmbeddedSessionManager()")
        return emptySessionManager
    }

    override fun getMessages(placementId: Long): List<IterableEmbeddedMessage>? {
        logNotInitialized("getMessages()")
        return null
    }

    override fun reset() {
        logNotInitialized("reset()")
    }

    override fun getPlacementIds(): List<Long> {
        logNotInitialized("getPlacementIds()")
        return emptyList()
    }

    override fun syncMessages(placementIds: Array<Long>) {
        logNotInitialized("syncMessages()")
    }

    override fun handleEmbeddedClick(message: IterableEmbeddedMessage, buttonIdentifier: String?, clickedUrl: String?) {
        logNotInitialized("handleEmbeddedClick()")
    }

    override fun onSwitchToForeground() {
    }

    override fun onSwitchToBackground() {
    }
}
