package com.iterable.iterableapi

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

public class IterableEmbeddedManager {

    // region variables
    var messages: List<IterableEmbeddedMessage> = ArrayList()

    var autoFetchDuration: Int = 0
    var timer: Timer? = null

    var actionHandler: EmbeddedMessageActionHandler? = null
    var updateHandler: EmbeddedMessageUpdateHandler? = null

    var actionHandleListeners = mutableListOf<EmbeddedMessageActionHandler>()
    var updateHandleListeners = mutableListOf<EmbeddedMessageUpdateHandler>()

    // endregion

    // region constructors
    //Constructor of this class
    public constructor() {
        //gets the messages

//        messages = getMessagesFromJson()
        postConstruction()
    }

    fun postConstruction() {
        messages = getEmbeddedMessages()
    }




    //Constructor of this class with actionHandler and updateHandler
    public constructor(actionHandler: EmbeddedMessageActionHandler, updateHandler: EmbeddedMessageUpdateHandler) {
        this.actionHandler = actionHandler
        this.updateHandler = updateHandler
        messages = getMessagesFromJson()
    }

    //Contructor with timer
    public constructor(autoFetchTimer: Int) {
        this.autoFetchDuration = autoFetchTimer
        messages = getMessagesFromJson()
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

    fun getEmbeddedMessages(): List<IterableEmbeddedMessage> {


        IterableApi.sharedInstance.apiClient.getEmbeddedMessages { payload ->
            if (payload != null && !payload.isEmpty()) {
                try {
                    val messages: MutableList<IterableEmbeddedMessage> = ArrayList()
                    val jsonArray = JSONArray(payload)
                    if (jsonArray != null) {
                        for (i in 0 until jsonArray.length()) {
                            val messageJson = jsonArray.optJSONObject(i)
                            val message = IterableEmbeddedMessage.fromJSONObject(messageJson)
                            if (message != null) {
                                messages.add(message)
                            } else {
                                IterableLogger.e(IterableInAppManager.TAG, "message turned out to be null")
                            }
                        }
                    } else {
                        IterableLogger.e(IterableInAppManager.TAG, "Array not found in embedded message response. Probably a parsing failure")
                    }

                    this.messages = messages
                } catch (e: JSONException) {
                    IterableLogger.e(IterableInAppManager.TAG, e.toString())
                }
            } else {
                IterableLogger.e(IterableInAppManager.TAG, "No payload found in embedded message response")
            }
        }

        return messages ?: listOf()
    }

    // endregion

    // region auto fetch functionality

    //Function to resume the timer
    fun startTimer() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                messages = getMessagesFromJson()
                updateHandleListeners.forEach {
                    it.onMessageUpdate()
                }
            }
        }, 0, autoFetchDuration.toLong())
    }

    //Function to pause the timer
    public fun pauseTimer() {
        timer?.cancel()
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
}

// region interfaces

public interface EmbeddedMessageActionHandler {
    fun onTapAction(action: IterableAction)
}

public interface EmbeddedMessageUpdateHandler {
    fun onMessageUpdate()
}

// endregion

