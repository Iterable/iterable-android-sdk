package com.iterable.iterableapi

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

/**
 * Manages embedded messages for the Iterable SDK.
 *
 * This class is responsible for syncing embedded messages from the Iterable backend,
 * maintaining a local in-memory cache of messages per placement, and notifying registered
 * [IterableEmbeddedUpdateHandler] listeners when messages change.
 *
 * It also tracks embedded session lifecycle (foreground/background transitions) via
 * [IterableActivityMonitor] and exposes [handleEmbeddedClick] for processing URL clicks
 * originating from embedded message UI components.
 *
 * Obtain an instance via [IterableApi.getEmbeddedManager].
 */
public class IterableEmbeddedManager : IterableActivityMonitor.AppStateCallback {

    // region constants
    val TAG = "IterableEmbeddedManager"
    // endregion

    // region variables
    private var localPlacementMessagesMap = mutableMapOf<Long, List<IterableEmbeddedMessage>>()
    private var localPlacementIds = mutableListOf<Long>()

    private var updateHandleListeners = mutableListOf<IterableEmbeddedUpdateHandler>()
    private var iterableApi: IterableApi
    private var context: Context

    private var embeddedSessionManager = EmbeddedSessionManager()

    private var activityMonitor: IterableActivityMonitor? = null

    // endregion

    // region constructor

    /**
     * Creates a new [IterableEmbeddedManager].
     *
     * If [IterableConfig.enableEmbeddedMessaging] is `true` the manager will automatically
     * start/stop embedded sessions and sync messages on app foreground/background transitions.
     *
     * @param iterableApi The [IterableApi] instance used for network calls and configuration.
     */
    public constructor(
        iterableApi: IterableApi
    ) {
        this.iterableApi = iterableApi
        this.context = iterableApi.mainActivityContext
        if(iterableApi.config.enableEmbeddedMessaging) {
            activityMonitor = IterableActivityMonitor.getInstance()
            activityMonitor?.addCallback(this)
        }
    }

    // endregion

    // region getters and setters

    /**
     * Registers a listener that will be notified whenever the embedded message cache changes
     * or sync events occur.
     *
     * @param updateHandler The [IterableEmbeddedUpdateHandler] to add.
     */
    public fun addUpdateListener(updateHandler: IterableEmbeddedUpdateHandler) {
        updateHandleListeners.add(updateHandler)
    }

    /**
     * Unregisters a previously added listener and ends the current embedded session.
     *
     * @param updateHandler The [IterableEmbeddedUpdateHandler] to remove.
     */
    public fun removeUpdateListener(updateHandler: IterableEmbeddedUpdateHandler) {
        updateHandleListeners.remove(updateHandler)
        embeddedSessionManager.endSession()
    }

    /**
     * Returns the current list of registered update listeners.
     *
     * @return An immutable snapshot of all registered [IterableEmbeddedUpdateHandler] instances.
     */
    public fun getUpdateHandlers(): List<IterableEmbeddedUpdateHandler> {
        return updateHandleListeners
    }

    /**
     * Returns the [EmbeddedSessionManager] used to track embedded session start/end events.
     *
     * @return The active [EmbeddedSessionManager].
     */
    public fun getEmbeddedSessionManager(): EmbeddedSessionManager {
        return embeddedSessionManager
    }

    // endregion

    // region public methods

    /**
     * Returns the cached list of embedded messages for the given placement, or `null` if no
     * messages are currently stored for that placement.
     *
     * This does **not** trigger a network sync; call [syncMessages] to refresh data.
     *
     * @param placementId The placement ID whose messages should be returned.
     * @return The list of [IterableEmbeddedMessage] objects, or `null` if none are cached.
     */
    fun getMessages(placementId: Long): List<IterableEmbeddedMessage>? {
        return localPlacementMessagesMap[placementId]
    }

    /**
     * Clears the in-memory message cache for all placements.
     */
    fun reset() {
        localPlacementMessagesMap = mutableMapOf()
    }

    /**
     * Returns the list of placement IDs currently held in the local message cache.
     *
     * @return A list of placement IDs.
     */
    fun getPlacementIds(): List<Long> {
        return localPlacementIds
    }

    /**
     * Syncs embedded messages from the Iterable backend for the given placement IDs.
     *
     * When the response is received the local cache is updated and all registered
     * [IterableEmbeddedUpdateHandler] listeners are notified. If [placementIds] is empty, all
     * available placements are returned by the backend.
     *
     * This method is a no-op when [IterableConfig.enableEmbeddedMessaging] is `false`.
     *
     * @param placementIds Array of placement IDs to sync. Defaults to an empty array (all placements).
     */
    @JvmOverloads
    fun syncMessages(placementIds: Array<Long> = emptyArray()) {
        if (iterableApi.config.enableEmbeddedMessaging) {
            IterableLogger.v(TAG, "Syncing messages...")

            IterableApi.sharedInstance.getEmbeddedMessages(placementIds, { data ->
                IterableLogger.v(TAG, "Got response from network call to get embedded messages")
                try {
                    val previousPlacementIds = getPlacementIds()
                    val currentPlacementIds: MutableList<Long> = mutableListOf()

                    val placementsArray =
                        data.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)
                    if (placementsArray != null) {
                        //if there are no placements in the payload
                        //reset the local message storage and trigger a UI update
                        if (placementsArray.length() == 0) {
                            reset()
                            if (previousPlacementIds.isNotEmpty()) {
                                updateHandleListeners.forEach {
                                    IterableLogger.d(TAG, "Calling updateHandler")
                                    it.onMessagesUpdated()
                                }
                            }
                        } else {
                            for (i in 0 until placementsArray.length()) {
                                val placementJson = placementsArray.optJSONObject(i)
                                val placement =
                                    IterableEmbeddedPlacement.fromJSONObject(placementJson)
                                val placementId = placement.placementId
                                val messages = placement.messages

                                currentPlacementIds.add(placementId)
                                updateLocalMessageMap(placementId, messages)
                            }
                        }
                    }

                    // compare previous placements to the current placement payload
                    val removedPlacementIds =
                        previousPlacementIds.subtract(currentPlacementIds.toSet())

                    //if there are placements removed, update the local storage and trigger UI update
                    if (removedPlacementIds.isNotEmpty()) {
                        removedPlacementIds.forEach {
                            localPlacementMessagesMap.remove(it)
                        }

                        updateHandleListeners.forEach {
                            IterableLogger.d(TAG, "Calling updateHandler")
                            it.onMessagesUpdated()
                        }
                    }

                    //store placements from payload for next comparison
                    localPlacementIds = currentPlacementIds

                    notifySyncSucceeded()
                } catch (e: JSONException) {
                    IterableLogger.e(TAG, e.toString())
                    notifySyncFailed(e.message)
                }
            }, object : IterableHelper.FailureHandler {
                override fun onFailure(reason: String, data: JSONObject?) {
                    if (reason.equals(
                            "SUBSCRIPTION_INACTIVE",
                            ignoreCase = true
                        ) || reason.equals("Invalid API Key", ignoreCase = true)
                    ) {
                        IterableLogger.e(TAG, "Subscription is inactive. Stopping sync")
                        broadcastSubscriptionInactive()
                        return
                    }
                    IterableLogger.e(TAG, "Error while fetching embedded messages: $reason")
                    notifySyncFailed(reason)
                }
            })
        }
    }

    /**
     * Handles a click on a URL originating from an embedded message.
     *
     * The URL is dispatched as follows:
     * - URLs with the `action://` scheme are forwarded to the custom action handler.
     * - URLs with the `itbl://` scheme are forwarded to the custom action handler for
     *   backwards compatibility.
     * - All other URLs are opened via [IterableActionRunner] as a standard open-URL action.
     *
     * @param clickedUrl The URL that was clicked. Must not be null or empty.
     */
    fun handleEmbeddedClick(clickedUrl: String) {
        if (clickedUrl.isEmpty()) return

        if (clickedUrl.startsWith(IterableConstants.URL_SCHEME_ACTION)) {
            val actionName: String = clickedUrl.replace(IterableConstants.URL_SCHEME_ACTION, "")
            IterableActionRunner.executeAction(
                context,
                IterableAction.actionCustomAction(actionName),
                IterableActionSource.EMBEDDED
            )
        } else if (clickedUrl.startsWith(IterableConstants.URL_SCHEME_ITBL)) {
            val actionName: String = clickedUrl.replace(IterableConstants.URL_SCHEME_ITBL, "")
            IterableActionRunner.executeAction(
                context,
                IterableAction.actionCustomAction(actionName),
                IterableActionSource.EMBEDDED
            )
        } else {
            IterableActionRunner.executeAction(
                context,
                IterableAction.actionOpenUrl(clickedUrl),
                IterableActionSource.EMBEDDED
            )
        }
    }

    /**
     * Handles a click on an embedded message URL.
     *
     * The [message] and [buttonIdentifier] parameters are not used internally; use the
     * simplified [handleEmbeddedClick] overload that accepts only the URL instead.
     *
     * @param message The embedded message containing the clicked element (unused).
     * @param buttonIdentifier The identifier of the clicked button, if any (unused).
     * @param clickedUrl The URL that was clicked. When `null` or empty this method is a no-op.
     */
    @Deprecated(
        message = "Use handleEmbeddedClick(clickedUrl: String) instead. The message and buttonIdentifier parameters are unused.",
        replaceWith = ReplaceWith("handleEmbeddedClick(clickedUrl ?: return)")
    )
    fun handleEmbeddedClick(message: IterableEmbeddedMessage, buttonIdentifier: String?, clickedUrl: String?) {
        if (clickedUrl != null && clickedUrl.isNotEmpty()) {
            handleEmbeddedClick(clickedUrl)
        }
    }

    private fun notifySyncSucceeded() {
        updateHandleListeners.forEach {
            it.onEmbeddedMessagingSyncSucceeded()
        }
    }

    private fun notifySyncFailed(reason: String?) {
        updateHandleListeners.forEach {
            it.onEmbeddedMessagingSyncFailed(reason)
        }
    }

    private fun broadcastSubscriptionInactive() {
        updateHandleListeners.forEach {
            IterableLogger.d(TAG, "Broadcasting subscription inactive to the views")
            it.onEmbeddedMessagingDisabled()
        }
    }

    private fun updateLocalMessageMap(
        placementId: Long,
        remoteMessageList: List<IterableEmbeddedMessage>
    ) {
        IterableLogger.printInfo()
        var localMessagesChanged = false

        // Get local messages in a mutable list
        val localMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        getMessages(placementId)?.toMutableList()?.forEach {
            localMessageMap[it.metadata.messageId] = it
        }

        // Compare the remote list to local list
        // if there are new messages, trigger a message update in UI and send out received events
        remoteMessageList.forEach {
            if (!localMessageMap.containsKey(it.metadata.messageId)) {
                localMessagesChanged = true
                IterableApi.getInstance().trackEmbeddedMessageReceived(it)
            }
        }

        // Compare the local list to remote list
        // if there are messages to remove, trigger a message update in UI
        val remoteMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        remoteMessageList.forEach {
            remoteMessageMap[it.metadata.messageId] = it
        }

        localPlacementMessagesMap[placementId]?.forEach {
            if (!remoteMessageMap.containsKey(it.metadata.messageId)) {
                localMessagesChanged = true
            }
        }

        // update local message map for placement with remote message list
        localPlacementMessagesMap[placementId] = remoteMessageList

        //if local messages changed, trigger a message update in UI
        if (localMessagesChanged) {
            updateHandleListeners.forEach {
                IterableLogger.d(TAG, "Calling updateHandler")
                it.onMessagesUpdated()
            }
        }
    }
    // endregion

    override fun onSwitchToForeground() {
        IterableLogger.printInfo()
        embeddedSessionManager.startSession()
        IterableLogger.d(TAG, "Calling start session")
        syncMessages()
    }

    override fun onSwitchToBackground() {
        embeddedSessionManager.endSession()
    }
}

// region interfaces

public interface IterableEmbeddedUpdateHandler {
    fun onMessagesUpdated()
    fun onEmbeddedMessagingDisabled()
    fun onEmbeddedMessagingSyncSucceeded() {}
    fun onEmbeddedMessagingSyncFailed(reason: String?) {}
}

// endregion
