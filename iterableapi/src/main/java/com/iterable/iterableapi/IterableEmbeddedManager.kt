package com.iterable.iterableapi

import android.content.Context
import com.iterable.iterableapi.IterableHelper.SuccessHandler
import org.json.JSONException
import org.json.JSONObject

public class IterableEmbeddedManager : IterableActivityMonitor.AppStateCallback {

    // region constants
    val TAG = "IterableEmbeddedManager"
    // endregion

    // region variables
    private var localPlacementMessagesMap = mutableMapOf<Long, List<IterableEmbeddedMessage>>()
    private var placementIds = mutableListOf<Long>()

    private var localMessages: List<IterableEmbeddedMessage> = ArrayList()
    private var updateHandleListeners = mutableListOf<IterableEmbeddedUpdateHandler>()
    private var iterableApi: IterableApi
    private var context: Context

    private var embeddedSessionManager = EmbeddedSessionManager()

    private var activityMonitor: IterableActivityMonitor? = null

    // endregion

    // region constructor

    //Constructor of this class with actionHandler and updateHandler
    public constructor(
        iterableApi: IterableApi
    ) {
        this.iterableApi = iterableApi
        this.context = iterableApi.mainActivityContext
        activityMonitor = IterableActivityMonitor.getInstance()
        activityMonitor?.addCallback(this)
    }

    // endregion

    // region getters and setters

    //Add updateHandler to the list
    public fun addUpdateListener(updateHandler: IterableEmbeddedUpdateHandler) {
        updateHandleListeners.add(updateHandler)
    }

    //Remove updateHandler from the list
    public fun removeUpdateListener(updateHandler: IterableEmbeddedUpdateHandler) {
        updateHandleListeners.remove(updateHandler)
        embeddedSessionManager.endSession()
    }

    //Get the list of updateHandlers
    public fun getUpdateHandlers(): List<IterableEmbeddedUpdateHandler> {
        return updateHandleListeners
    }

    public fun getEmbeddedSessionManager(): EmbeddedSessionManager {
        return embeddedSessionManager
    }

    // endregion

    // region public methods

    //Gets the list of embedded messages in memory without syncing
    fun getMessages(placementId: Long?): List<IterableEmbeddedMessage>? {
        return localPlacementMessagesMap[placementId]
    }

    fun reset() {
        val emptyMessages = listOf<IterableEmbeddedMessage>()
        val placementIds = getPlacementIds()
        for (i in placementIds.indices) {
            val placementId = placementIds[i]
            localPlacementMessagesMap[placementId] = emptyMessages
        }
    }

    private fun getPlacementIds(): List<Long> {
        return placementIds
    }

    //Network call to get the embedded messages
    fun syncMessages() {
        IterableLogger.v(TAG, "Syncing messages...")

        var testPlacements: Array<Long> = arrayOf(83)

        IterableApi.sharedInstance.getEmbeddedMessages(testPlacements,SuccessHandler { data ->
            IterableLogger.v(TAG, "Got response from network call to get embedded messages")
            try {
                val placementsArray = data.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE_PLACEMENTS)
                if (placementsArray != null) {
                    for (i in 0 until placementsArray.length()) {
                        val placementJson = placementsArray.optJSONObject(i)
                        val placement = IterableEmbeddedPlacement.fromJSONObject(placementJson)
                        val placementId = placement.placementId
                        val messages = placement.messages
                        IterableLogger.d(TAG, "placement id: $placementId")

                        placementIds.add(placementId)
                        updateLocalMessages(placementId, messages)
                    }
                }

            } catch (e: JSONException) {
                IterableLogger.e(TAG, e.toString())
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
                } else {
                    //TODO: Instead of generic condition, have the retry only in certain situation
                    IterableLogger.e(TAG, "Error while fetching embedded messages: $reason")
                }
            }
        })
    }

    fun handleEmbeddedClick(message: IterableEmbeddedMessage, buttonIdentifier: String?, clickedUrl: String?) {
        IterableActionRunner.executeAction(context, IterableAction.actionOpenUrl(clickedUrl), IterableActionSource.EMBEDDED)
    }

    private fun broadcastSubscriptionInactive() {
        updateHandleListeners.forEach {
            IterableLogger.d(TAG, "Broadcasting subscription inactive to the views")
            it.onEmbeddedMessagingDisabled()
        }
    }

    private fun updateLocalMessages(
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

        // Check for new messages and add them to the local list
        remoteMessageList.forEach {
            if (!localMessageMap.containsKey(it.metadata.messageId)) {
                localMessagesChanged = true
                IterableApi.getInstance().trackEmbeddedMessageReceived(it)
            }
        }

        // Check for messages in the local list that are not in the remote list and remove them
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
}

// endregion

