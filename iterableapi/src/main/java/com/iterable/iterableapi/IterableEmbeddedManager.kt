package com.iterable.iterableapi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iterable.iterableapi.IterableHelper.SuccessHandler
import org.json.JSONException
import org.json.JSONObject

public class IterableEmbeddedManager : IterableActivityMonitor.AppStateCallback {

    // region constants
    val TAG = "IterableEmbeddedManager"
    // endregion

    // region variables
    private var localMessages = mutableMapOf<String, List<IterableEmbeddedMessage>>()
    private var actionHandler: EmbeddedMessageActionHandler? = null
    private var updateHandler: EmbeddedMessageUpdateHandler? = null
    private var actionHandleListeners = mutableListOf<EmbeddedMessageActionHandler>()
    private var updateHandleListeners = mutableListOf<EmbeddedMessageUpdateHandler>()

    var embeddedSessionManager = EmbeddedSessionManager()

    private var activityMonitor: IterableActivityMonitor? = null

    // endregion

    // region constructor

    //Constructor of this class with actionHandler and updateHandler
    public constructor(
        actionHandler: EmbeddedMessageActionHandler?,
        updateHandler: EmbeddedMessageUpdateHandler?
    ) {
        this.actionHandler = actionHandler
        this.updateHandler = updateHandler
        activityMonitor = IterableActivityMonitor.getInstance()
        activityMonitor?.addCallback(this)
    }
    // endregion

    // region getters and setters

    //Add actionHandler to the list
    public fun addActionHandler(actionHandler: EmbeddedMessageActionHandler) {
        actionHandleListeners.add(actionHandler)
    }

    //Add updateHandler to the list
    public fun addUpdateListener(updateHandler: EmbeddedMessageUpdateHandler) {
        updateHandleListeners.add(updateHandler)
    }

    //Remove actionHandler from the list
    public fun removeActionHandler(actionHandler: EmbeddedMessageActionHandler) {
        actionHandleListeners.remove(actionHandler)
    }

    //Remove updateHandler from the list
    public fun removeUpdateListener(updateHandler: EmbeddedMessageUpdateHandler) {
        updateHandleListeners.remove(updateHandler)
        embeddedSessionManager.endSession()
    }

    //Get the list of actionHandlers
    public fun getActionHandlers(): List<EmbeddedMessageActionHandler> {
        return actionHandleListeners
    }

    //Get the list of updateHandlers
    public fun getUpdateHandlers(): List<EmbeddedMessageUpdateHandler> {
        return updateHandleListeners
    }

    // endregion

    // region public methods

    //Gets the list of embedded messages in memory without syncing
    fun getMessages(placementId: String?): List<IterableEmbeddedMessage>? {
        return localMessages[placementId]
    }

    //for testing purposes
    fun updatePlacementMessages(placementId: String, messages: List<IterableEmbeddedMessage>) {
        updateLocalMessages(placementId, messages)
        IterableLogger.d(TAG, "$localMessages")
    }

    //Network call to get the embedded messages
    fun syncMessages() {
        IterableLogger.v(TAG, "Syncing messages...")

        IterableApi.sharedInstance.getEmbeddedMessages(SuccessHandler { data ->
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

    private fun broadcastSubscriptionInactive() {
        updateHandleListeners.forEach {
            IterableLogger.d(TAG, "Broadcasting subscription inactive to the views")
            it.onEmbeddedMessagingDisabled()
        }
    }

    private fun updateLocalMessages(
        placementId: String,
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

        localMessages[placementId]?.forEach {
            if (!remoteMessageMap.containsKey(it.metadata.messageId)) {
                localMessagesChanged = true
            }
        }

        localMessages[placementId] = remoteMessageList

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
        syncMessages()
    }

    override fun onSwitchToBackground() {
        embeddedSessionManager.endSession()
    }
}

// region interfaces

public interface EmbeddedMessageActionHandler {
    fun onTapAction(action: IterableAction)
}

public interface EmbeddedMessageUpdateHandler {
    fun onMessagesUpdated()
    fun onEmbeddedMessagingDisabled()
}

// endregion

