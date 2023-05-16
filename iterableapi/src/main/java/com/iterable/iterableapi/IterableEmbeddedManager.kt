package com.iterable.iterableapi

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.iterable.iterableapi.IterableHelper.SuccessHandler
import org.json.JSONException
import org.json.JSONObject

public class IterableEmbeddedManager: IterableActivityMonitor.AppStateCallback{

    // region constants
    val TAG = "IterableEmbeddedManager"
    // endregion

    // region variables
    //TODO: See if coalescing all the messages into one list making one source of truth for local messages can be done.
    private var _messages = MutableLiveData<List<IterableEmbeddedMessage>>()
    val messages: LiveData<List<IterableEmbeddedMessage>>
        get() = _messages

    private var localMessages: List<IterableEmbeddedMessage> = ArrayList()

    private var autoFetchDuration: Double = 0.0
    private var lastSync: Long = 0
    private var actionHandler: EmbeddedMessageActionHandler? = null
    private var updateHandler: EmbeddedMessageUpdateHandler? = null
    private var actionHandleListeners = mutableListOf<EmbeddedMessageActionHandler>()
    private var updateHandleListeners = mutableListOf<EmbeddedMessageUpdateHandler>()
    private var activityMonitor: IterableActivityMonitor? = null
    private var isAppInBackground = false

    // endregion

    // region constructor

    //Constructor of this class with actionHandler and updateHandler
    public constructor(
        autoFetchInterval: Double,
        actionHandler: EmbeddedMessageActionHandler?,
        updateHandler: EmbeddedMessageUpdateHandler?
    ) {
        this.actionHandler = actionHandler
        this.updateHandler = updateHandler
        autoFetchDuration = autoFetchInterval
        activityMonitor = IterableActivityMonitor.getInstance()
        activityMonitor?.addCallback(this)

        postConstruction()
    }

    fun postConstruction() {
        scheduleSync()
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

    //Get the list of embedded messages after syncing
    //TODO: Still not a proper async call. It will still return the message in memory
    // but the sync will happen in background.
    fun getSyncedEmbeddedMessages(): List<IterableEmbeddedMessage> {

        IterableLogger.v(TAG, "Going to sync messages")

        syncMessages()

        IterableLogger.v(TAG, "Returning messages")

        return localMessages
    }

    //Gets the list of embedded messages in memory without syncing
    fun getEmbeddedMessages(): List<IterableEmbeddedMessage> {
        return localMessages
    }

    //Network call to get the embedded messages
    private fun syncMessages() {
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
            lastSync = IterableUtil.currentTimeMillis()
            scheduleSync()
        }, object : IterableHelper.FailureHandler {
            override fun onFailure(reason: String, data: JSONObject?) {
                if(reason.equals("SUBSCRIPTION_INACTIVE", ignoreCase = true)) {
                    IterableLogger.e(TAG, "Subscription is inactive. Stopping sync")
                    autoFetchDuration = 0.0
                    return
                } else {
                    IterableLogger.e(TAG, "Error while fetching embedded messages: $reason")
                    scheduleSync()
                }
            }
        })

    }

    fun updateLocalMessages(remoteMessageList: List<IterableEmbeddedMessage>) {
        IterableLogger.printInfo()
        var localMessagesChanged = false

        // Get local messages in a mutable list
        val localMessageList = getEmbeddedMessages().toMutableList()
        val localMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        localMessageList.forEach {
            localMessageMap[it.metadata.id] = it
        }

        // Check for new messages and add them to the local list
        remoteMessageList.forEach {
            if (!localMessageMap.containsKey(it.metadata.id)) {
                localMessagesChanged = true
                localMessageList.add(it)
                IterableApi.getInstance().trackEmbeddedMessageReceived(it)
            }
            //TODO: Make a call to the updateHandler to notify that the message has been added
        }

        // Check for messages in the local list that are not in the remote list and remove them
        val remoteMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        remoteMessageList.forEach {
            remoteMessageMap[it.metadata.id] = it
        }
        val messagesToRemove = mutableListOf<IterableEmbeddedMessage>()
        localMessageList.forEach {
            if(!remoteMessageMap.containsKey(it.metadata.id)) {
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

    // region auto fetch functionality

    fun scheduleSync() {
        IterableLogger.printInfo()
        if(autoFetchDuration > 0) {
            if (canSyncEmbeddedMessages()) {
                IterableLogger.v(TAG, "Can sync now.. Syncing now")
                IterableLogger.v(TAG, "setting isSyncScheduled to false in first if")
                getSyncedEmbeddedMessages()

            } else {
                if (!isAppInBackground) {
                    IterableLogger.v(
                        TAG,
                        "Scheduling sync after ${autoFetchDuration - getSecondsSinceLastFetch()} seconds"
                    )
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            getSyncedEmbeddedMessages()
                            IterableLogger.v(TAG, "inside looper setting isSyncScheduled to false")
                        },
                        ((autoFetchDuration - getSecondsSinceLastFetch()) * 1000).toLong()
                    )
                    IterableLogger.v(TAG, "setting isSyncScheduled to true")
                } else {
                    IterableLogger.v(TAG, "Not scheduling a sync.. App is in background")
                    lastSync = autoFetchDuration.toLong()
                }
            }
        } else {
            IterableLogger.v(TAG, "embedded messaging automatic fetching not started since autoFetchDuration is <= 0")
        }
    }

    private fun getSecondsSinceLastFetch(): Double {
        return (IterableUtil.currentTimeMillis() - lastSync) / 1000.0
    }

    private fun canSyncEmbeddedMessages(): Boolean {
        return getSecondsSinceLastFetch() >= autoFetchDuration
    }

    // endregion

    // region IterableActivityMonitor.AppStateCallback
    override fun onSwitchToForeground() {
        IterableLogger.printInfo()
        isAppInBackground = false
        //TODO: resume the timer
        if (IterableUtil.currentTimeMillis() - lastSync > autoFetchDuration * 1000) {
            IterableLogger.v(
                TAG,
                "Duration passed is greater than auto fetch duration. Syncing now... " + (IterableUtil.currentTimeMillis() - lastSync) + " > " + autoFetchDuration * 1000
            )
            //Check if looper is already running
            scheduleSync()
        } else {
            IterableLogger.v(
                TAG,
                "Duration passed is lesser than auto fetch duration. Hence not scheduling " + (IterableUtil.currentTimeMillis() - lastSync) + " < " + autoFetchDuration * 1000
            )
        }
    }

    override fun onSwitchToBackground() {
        IterableLogger.printInfo()
        isAppInBackground = true
    }
    // endregion
}

// region interfaces

public interface EmbeddedMessageActionHandler {
    fun onTapAction(action: IterableAction)
}

public interface EmbeddedMessageUpdateHandler {
    fun onMessageUpdate()
}

// endregion

