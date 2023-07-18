package com.iterable.iterableapi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iterable.iterableapi.IterableHelper.SuccessHandler
import org.json.JSONException
import org.json.JSONObject

public class IterableEmbeddedManager{

    // region constants
    val TAG = "IterableEmbeddedManager"
    // endregion

    // region variables
    //TODO: See if coalescing all the messages into one list making one source of truth for local messages can be done.
    private var _messages = MutableLiveData<List<IterableEmbeddedMessage>>()
    val messages: LiveData<List<IterableEmbeddedMessage>>
        get() = _messages

    private var localMessages: List<IterableEmbeddedMessage> = ArrayList()
    private var actionHandler: EmbeddedMessageActionHandler? = null
    private var updateHandler: EmbeddedMessageUpdateHandler? = null
    private var actionHandleListeners = mutableListOf<EmbeddedMessageActionHandler>()
    private var updateHandleListeners = mutableListOf<EmbeddedMessageUpdateHandler>()

    // endregion

    // region constructor

    //Constructor of this class with actionHandler and updateHandler
    public constructor(
        actionHandler: EmbeddedMessageActionHandler?,
        updateHandler: EmbeddedMessageUpdateHandler?
    ) {
        this.actionHandler = actionHandler
        this.updateHandler = updateHandler
    }
    // endregion

    // region getters and setters

    //Add actionHandler to the list
    public fun addActionHandler(actionHandler: EmbeddedMessageActionHandler) {
        actionHandleListeners.add(actionHandler)
    }

    //Add updateHandler to the list
    public fun addUpdateHandler(updateHandler: EmbeddedMessageUpdateHandler) {
        updateHandleListeners.add(updateHandler)
    }

    //Remove actionHandler from the list
    public fun removeActionHandler(actionHandler: EmbeddedMessageActionHandler) {
        actionHandleListeners.remove(actionHandler)
    }

    //Remove updateHandler from the list
    public fun removeUpdateHandler(updateHandler: EmbeddedMessageUpdateHandler) {
        updateHandleListeners.remove(updateHandler)
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
    fun getEmbeddedMessages(): List<IterableEmbeddedMessage> {
        return localMessages
    }

    //Network call to get the embedded messages
    public fun syncMessages() {
        IterableLogger.v(TAG, "Syncing messages...")

        IterableApi.sharedInstance.getEmbeddedMessages(SuccessHandler { data ->
            IterableLogger.v(TAG, "Got response from network call to get embedded messages")
            try {
                val remoteMessageList: MutableList<IterableEmbeddedMessage> = ArrayList()
                val jsonArray =
                    data.optJSONArray(IterableConstants.ITERABLE_EMBEDDED_MESSAGE)

                if (jsonArray != null) {
                    for (i in 0 until jsonArray.length()) {
                        val messageJson = jsonArray.optJSONObject(i)
                        val message = IterableEmbeddedMessage.fromJSONObject(messageJson)
                        remoteMessageList.add(message)
                    }
                } else {
                    IterableLogger.e(
                        TAG,
                        "Array not found in embedded message response. Probably a parsing failure"
                    )
                }
                updateLocalMessages(remoteMessageList)
                IterableLogger.v(TAG, "$localMessages")

            } catch (e: JSONException) {
                IterableLogger.e(TAG, e.toString())
            }
        }, object : IterableHelper.FailureHandler {
            override fun onFailure(reason: String, data: JSONObject?) {
                if(reason.equals("SUBSCRIPTION_INACTIVE", ignoreCase = true) || reason.equals("Invalid API Key", ignoreCase = true)) {
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

    fun broadcastSubscriptionInactive() {
        updateHandleListeners.forEach {
            IterableLogger.d(TAG, "Broadcasting subscription inactive to the views")
            it.onFeatureDisabled()
        }
    }

    fun updateLocalMessages(remoteMessageList: List<IterableEmbeddedMessage>) {
        IterableLogger.printInfo()
        var localMessagesChanged = false

        // Get local messages in a mutable list
        val localMessageList = getEmbeddedMessages().toMutableList()
        val localMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        localMessageList.forEach {
            localMessageMap[it.metadata.messageId] = it
        }

        // Check for new messages and add them to the local list
        remoteMessageList.forEach {
            if (!localMessageMap.containsKey(it.metadata.messageId)) {
                localMessagesChanged = true
                localMessageList.add(it)
                IterableApi.getInstance().trackEmbeddedMessageReceived(it)
            }
            //TODO: Make a call to the updateHandler to notify that the message has been added
        }

        // Check for messages in the local list that are not in the remote list and remove them
        val remoteMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        remoteMessageList.forEach {
            remoteMessageMap[it.metadata.messageId] = it
        }
        val messagesToRemove = mutableListOf<IterableEmbeddedMessage>()
        localMessageList.forEach {
            if(!remoteMessageMap.containsKey(it.metadata.messageId)) {
                messagesToRemove.add(it)

                //TODO: Make a call to the updateHandler to notify that the message has been removed
                //TODO: Make a call to backend if needed
                localMessagesChanged = true
            }
        }
        localMessageList.removeAll(messagesToRemove)

        this.localMessages = localMessageList
        _messages.value = localMessageList

        if(localMessagesChanged) {
            //TODO: Make a call to the updateHandler to notify that the message list has been updated
            updateHandleListeners.forEach {
                IterableLogger.d(TAG, "Calling updateHandler")
                it.onMessageUpdate()
            }
        }
    }
    // endregion
}

// region interfaces

public interface EmbeddedMessageActionHandler {
    fun onTapAction(action: IterableAction)
}

public interface EmbeddedMessageUpdateHandler {
    fun onMessageUpdate()
    fun onFeatureDisabled()
}

// endregion

