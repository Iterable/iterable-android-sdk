package com.iterable.iterableapi

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

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

    // region public methods

    public fun addUpdateListener(updateHandler: IterableEmbeddedUpdateHandler) {
        updateHandleListeners.add(updateHandler)
    }

    public fun removeUpdateListener(updateHandler: IterableEmbeddedUpdateHandler) {
        updateHandleListeners.remove(updateHandler)
        embeddedSessionManager.endSession()
    }

    public fun getUpdateHandlers(): List<IterableEmbeddedUpdateHandler> {
        return updateHandleListeners
    }

    public fun getEmbeddedSessionManager(): EmbeddedSessionManager {
        return embeddedSessionManager
    }

    fun getMessages(placementId: Long): List<IterableEmbeddedMessage>? {
        return localPlacementMessagesMap[placementId]
    }

    fun reset() {
        localPlacementMessagesMap = mutableMapOf()
    }

    fun getPlacementIds(): List<Long> {
        return localPlacementIds
    }


    @JvmOverloads
    fun syncMessages(placementIds: Array<Long> = emptyArray()) {
        if (iterableApi.config.enableEmbeddedMessaging) {
            IterableLogger.v(TAG, "Syncing messages...")

            IterableApi.sharedInstance.getEmbeddedMessages(
                placementIds,
                { data ->
                    try {
                        processEmbeddedMessagesResponse(data)
                    } catch (e: JSONException) {
                        IterableLogger.e(TAG, e.toString())
                    }
                    notifySyncSucceeded()
                },
                { reason, data ->
                    handleSyncFailure(reason, data)
                    notifySyncFailed(reason)
                }
            )
        }
    }

    fun handleEmbeddedClick(message: IterableEmbeddedMessage, buttonIdentifier: String?, clickedUrl: String?) {
        if ((clickedUrl != null) && clickedUrl.toString().isNotEmpty()) {
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
    }

    // endregion

    // region private methods

    private fun processEmbeddedMessagesResponse(data: JSONObject) {
        IterableLogger.v(TAG, "Got response from network call to get embedded messages")
        val previousPlacementIds = getPlacementIds()
        val currentPlacementIds: MutableList<Long> = mutableListOf()

        val placementsArray =
            data.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)
        if (placementsArray != null) {
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

        val removedPlacementIds =
            previousPlacementIds.subtract(currentPlacementIds.toSet())

        if (removedPlacementIds.isNotEmpty()) {
            removedPlacementIds.forEach {
                localPlacementMessagesMap.remove(it)
            }

            updateHandleListeners.forEach {
                IterableLogger.d(TAG, "Calling updateHandler")
                it.onMessagesUpdated()
            }
        }

        localPlacementIds = currentPlacementIds
    }

    private fun handleSyncFailure(reason: String, data: JSONObject?) {
        if (reason.equals(
                "SUBSCRIPTION_INACTIVE",
                ignoreCase = true
            ) || reason.equals("Invalid API Key", ignoreCase = true)
        ) {
            IterableLogger.e(TAG, "Subscription is inactive. Stopping sync")
            broadcastSubscriptionInactive()
        } else {
            IterableLogger.e(TAG, "Error while fetching embedded messages: $reason")
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

        val localMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        getMessages(placementId)?.toMutableList()?.forEach {
            localMessageMap[it.metadata.messageId] = it
        }

        remoteMessageList.forEach {
            if (!localMessageMap.containsKey(it.metadata.messageId)) {
                localMessagesChanged = true
                IterableApi.getInstance().trackEmbeddedMessageReceived(it)
            }
        }

        val remoteMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        remoteMessageList.forEach {
            remoteMessageMap[it.metadata.messageId] = it
        }

        localPlacementMessagesMap[placementId]?.forEach {
            if (!remoteMessageMap.containsKey(it.metadata.messageId)) {
                localMessagesChanged = true
            }
        }

        localPlacementMessagesMap[placementId] = remoteMessageList

        if (localMessagesChanged) {
            updateHandleListeners.forEach {
                IterableLogger.d(TAG, "Calling updateHandler")
                it.onMessagesUpdated()
            }
        }
    }

    // endregion

    // region AppStateCallback overrides

    override fun onSwitchToForeground() {
        IterableLogger.printInfo()
        embeddedSessionManager.startSession()
        IterableLogger.d(TAG, "Calling start session")
        syncMessages()
    }

    override fun onSwitchToBackground() {
        embeddedSessionManager.endSession()
    }

    // endregion
}

// region interfaces

public interface IterableEmbeddedUpdateHandler {
    fun onMessagesUpdated()
    fun onEmbeddedMessagingDisabled()
    fun onEmbeddedMessagingSyncSucceeded() {}
    fun onEmbeddedMessagingSyncFailed(reason: String?) {}
}

// endregion
