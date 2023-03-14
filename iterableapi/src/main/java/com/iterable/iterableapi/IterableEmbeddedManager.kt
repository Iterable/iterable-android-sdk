package com.iterable.iterableapi

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

public class IterableEmbeddedManager: IterableActivityMonitor.AppStateCallback{

    // region constants
    val TAG = "IterableEmbeddedManager"
    // endregion

    // region variables
    private var messages: List<IterableEmbeddedMessage> = ArrayList()

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
        context: Context,
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

        return messages
    }

    //Gets the list of embedded messages in memory without syncing
    fun getEmbeddedMessages(): List<IterableEmbeddedMessage> {
        return messages
    }

    //Network call to get the embedded messages
    private fun syncMessages() {
        IterableLogger.v(TAG, "Syncing messages...")

        //TODO: Remove if condition once backend is ready
        if(false) {
            IterableApi.sharedInstance.apiClient.getEmbeddedMessages { payload ->
                IterableLogger.v(TAG, "Got response from network call to get embedded messages")
                if (payload != null && !payload.isEmpty()) {
                    try {
                        val remoteMessageList: MutableList<IterableEmbeddedMessage> = ArrayList()
                        val jsonArray = JSONArray(payload)
                        if (jsonArray != null) {
                            for (i in 0 until jsonArray.length()) {
                                val messageJson = jsonArray.optJSONObject(i)
                                val message = IterableEmbeddedMessage.fromJSONObject(messageJson)
                                if (message != null) {
                                    remoteMessageList.add(message)
                                } else {
                                    IterableLogger.e(
                                        IterableInAppManager.TAG,
                                        "message turned out to be null"
                                    )
                                }
                            }
                        } else {
                            IterableLogger.e(
                                IterableInAppManager.TAG,
                                "Array not found in embedded message response. Probably a parsing failure"
                            )
                        }
//                    //Directly saving the messages to the list
//                    //TODO: Check and make note of the changes and call the updateHandler accordinly
//                    //TODO: Check for new messages and call delivery on the new ones

                        updateLocalMessages(remoteMessageList)
//                    //Saving the time of last sync
                        IterableLogger.v(TAG, "Resetting last sync time")
                        lastSync = IterableUtil.currentTimeMillis()
                    } catch (e: JSONException) {
                        IterableLogger.e(IterableInAppManager.TAG, e.toString())
                    }
                } else {
                    IterableLogger.e(
                        IterableInAppManager.TAG,
                        "No payload found in embedded message response"
                    )
                }
            }
        } else {
            //TODO: Remove this else block once backend is ready
            //Saving the time of last sync
            IterableLogger.v(TAG, "Resetting last sync time")
            lastSync = IterableUtil.currentTimeMillis()
        }

        scheduleSync()
    }

    fun updateLocalMessages(remoteMessageList: List<IterableEmbeddedMessage>) {
        IterableLogger.printInfo()

        // Get local messages in a mutable list
        val localMessageList = getEmbeddedMessages().toMutableList()
        val localMessageMap = mutableMapOf<String, IterableEmbeddedMessage>()
        localMessageList.forEach {
            localMessageMap[it.metadata.id] = it
        }

        // Check for new messages and add them to the local list
        remoteMessageList.forEach {
            if (!localMessageMap.containsKey(it.metadata.id)) {
                localMessageList.add(it)
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
            }
        }
        localMessageList.removeAll(messagesToRemove)

        this.messages = localMessageList

    }

    // endregion

    // region auto fetch functionality

    fun scheduleSync() {
        IterableLogger.printInfo()
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
    }

    private fun getSecondsSinceLastFetch(): Double {
        return (IterableUtil.currentTimeMillis() - lastSync) / 1000.0
    }

    private fun canSyncEmbeddedMessages(): Boolean {
        return getSecondsSinceLastFetch() >= autoFetchDuration
    }

    // endregion

    // region basic test methods

    //For testing purpose only
    fun getMessagesFromJson(): List<IterableEmbeddedMessage> {
        val file = File("data.json")
        val bufferedReader = file.bufferedReader()
        val jsonString = bufferedReader.use { it.readText() }
        val messageJson = JSONObject(jsonString)

        val embeddedMessages = listOf(
            IterableEmbeddedMessage.fromJSONObject(messageJson),
            IterableEmbeddedMessage.fromJSONObject(messageJson),
            IterableEmbeddedMessage.fromJSONObject(messageJson)
        )

        return embeddedMessages
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

