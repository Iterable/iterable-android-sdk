package com.iterable.iterableapi

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.iterable.iterableapi.IterableInAppHandler.InAppResponse
import com.iterable.iterableapi.IterableInAppMessage.Trigger.TriggerType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * Created by David Truong dt@iterable.com.
 *
 * The IterableInAppManager handles creating and rendering different types of InApp Notifications received from the IterableApi
 */
class IterableInAppManager @VisibleForTesting constructor(
    private val api: IterableApi,
    private val handler: IterableInAppHandler,
    private val inAppDisplayInterval: Double,
    private val storage: IterableInAppStorage,
    private val activityMonitor: IterableActivityMonitor,
    private val displayer: IterableInAppDisplayer
) : IterableActivityMonitor.AppStateCallback {

    companion object {
        const val TAG = "IterableInAppManager"
        const val MOVE_TO_FOREGROUND_SYNC_INTERVAL_MS = 60 * 1000L
        const val MESSAGES_TO_FETCH = 100

        @JvmStatic
        private fun getInAppStorageModel(iterableApi: IterableApi, useInMemoryForInAppStorage: Boolean): IterableInAppStorage {
            return if (useInMemoryForInAppStorage) {
                checkAndDeleteUnusedInAppFileStorage(iterableApi.getMainActivityContext())
                IterableInAppMemoryStorage()
            } else {
                IterableInAppFileStorage(iterableApi.getMainActivityContext())
            }
        }

        @JvmStatic
        private fun checkAndDeleteUnusedInAppFileStorage(context: Context) {
            val sdkFilesDirectory = IterableUtil.getSDKFilesDirectory(context)
            val inAppContentFolder = IterableUtil.getDirectory(sdkFilesDirectory, "IterableInAppFileStorage")
            val inAppBlob = File(inAppContentFolder, "itbl_inapp.json")

            if (inAppBlob.exists()) {
                inAppBlob.delete()
            }
        }
    }

    interface Listener {
        fun onInboxUpdated()
    }

    private val context: Context = api.getMainActivityContext()
    private val listeners = mutableListOf<Listener>()
    private var lastSyncTime = 0L
    private var lastInAppShown = 0L
    private var autoDisplayPaused = false

    constructor(
        iterableApi: IterableApi,
        handler: IterableInAppHandler,
        inAppDisplayInterval: Double,
        useInMemoryStorageForInApps: Boolean
    ) : this(
        iterableApi,
        handler,
        inAppDisplayInterval,
        getInAppStorageModel(iterableApi, useInMemoryStorageForInApps),
        IterableActivityMonitor.getInstance(),
        IterableInAppDisplayer(IterableActivityMonitor.getInstance())
    )

    init {
        activityMonitor.addCallback(this)
        syncInApp()
    }

    /**
     * Get the list of available in-app messages
     * This list is synchronized with the server by the SDK
     * @return A [List] of [IterableInAppMessage] objects
     */
    @NonNull
    @Synchronized
    fun getMessages(): List<IterableInAppMessage> {
        val filteredList = mutableListOf<IterableInAppMessage>()
        for (message in storage.getMessages()) {
            if (!message.isConsumed() && !isMessageExpired(message)) {
                filteredList.add(message)
            }
        }
        return filteredList
    }

    @Synchronized
    internal fun getMessageById(messageId: String): IterableInAppMessage? {
        return storage.getMessage(messageId)
    }

    /**
     * Get the list of inbox messages
     * @return A [List] of [IterableInAppMessage] objects stored in inbox
     */
    @NonNull
    @Synchronized
    fun getInboxMessages(): List<IterableInAppMessage> {
        val filteredList = mutableListOf<IterableInAppMessage>()
        for (message in storage.getMessages()) {
            if (!message.isConsumed() && !isMessageExpired(message) && message.isInboxMessage()) {
                filteredList.add(message)
            }
        }
        return filteredList
    }

    /**
     * Get the count of unread inbox messages
     * @return Unread inbox messages count
     */
    @Synchronized
    fun getUnreadInboxMessagesCount(): Int {
        var unreadInboxMessageCount = 0
        for (message in getInboxMessages()) {
            if (!message.isRead()) {
                unreadInboxMessageCount++
            }
        }
        return unreadInboxMessageCount
    }

    @Synchronized
    fun setRead(@NonNull message: IterableInAppMessage, read: Boolean) {
        setRead(message, read, null, null)
    }

    /**
     * Set the read flag on an inbox message
     * @param message Inbox message object retrieved from [IterableInAppManager.getInboxMessages]
     * @param read Read state flag. true = read, false = unread
     * @param successHandler The callback which returns `success`.
     */
    @Synchronized
    fun setRead(
        @NonNull message: IterableInAppMessage,
        read: Boolean,
        @Nullable successHandler: IterableHelper.SuccessHandler?,
        @Nullable failureHandler: IterableHelper.FailureHandler?
    ) {
        message.setRead(read)
        if (successHandler != null) {
            successHandler.onSuccess(JSONObject()) // passing blank json object here as onSuccess is @Nonnull
        }
        notifyOnChange()
    }

    internal fun isAutoDisplayPaused(): Boolean {
        return autoDisplayPaused
    }

    /**
     * Set a pause to prevent showing in-app messages automatically. By default the value is set to false.
     * @param paused Whether to pause showing in-app messages.
     */
    fun setAutoDisplayPaused(paused: Boolean) {
        this.autoDisplayPaused = paused
        if (!paused) {
            scheduleProcessing()
        }
    }

    /**
     * Trigger a manual sync. This method is called automatically by the SDK, so there should be no
     * need to call this method from your app.
     */
    internal fun syncInApp() {
        IterableLogger.printInfo()
        this.api.getInAppMessages(MESSAGES_TO_FETCH, object : IterableHelper.IterableActionHandler {
            override fun execute(payload: String?) {
                if (payload != null && payload.isNotEmpty()) {
                    try {
                        val messages = mutableListOf<IterableInAppMessage>()
                        val mainObject = JSONObject(payload)
                        val jsonArray = mainObject.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE)
                        if (jsonArray != null) {
                            for (i in 0 until jsonArray.length()) {
                                val messageJson = jsonArray.optJSONObject(i)
                                val message = IterableInAppMessage.fromJSONObject(messageJson, null)
                                if (message != null) {
                                    messages.add(message)
                                }
                            }

                            syncWithRemoteQueue(messages)
                            lastSyncTime = IterableUtil.currentTimeMillis()
                        }
                    } catch (e: JSONException) {
                        IterableLogger.e(TAG, e.toString())
                    }
                } else {
                    scheduleProcessing()
                }
            }
        })
    }

    /**
     * Clear all in-app messages.
     * Should be called on user logout.
     */
    internal fun reset() {
        IterableLogger.printInfo()

        for (message in storage.getMessages()) {
            storage.removeMessage(message)
        }

        notifyOnChange()
    }

    /**
     * Display the in-app message on the screen
     * @param message In-App message object retrieved from [IterableInAppManager.getMessages]
     */
    fun showMessage(@NonNull message: IterableInAppMessage) {
        showMessage(message, true, null)
    }

    fun showMessage(@NonNull message: IterableInAppMessage, @NonNull location: IterableInAppLocation) {
        showMessage(message, location == IterableInAppLocation.IN_APP, null, location)
    }

    /**
     * Display the in-app message on the screen. This method, by default, assumes the current location of activity as InApp. To pass
     * different inAppLocation as paramter, use showMessage method which takes in IterableAppLocation as a parameter.
     * @param message In-App message object retrieved from [IterableInAppManager.getMessages]
     * @param consume A boolean indicating whether to remove the message from the list after showing
     * @param clickCallback A callback that is called when the user clicks on a link in the in-app message
     */
    fun showMessage(
        @NonNull message: IterableInAppMessage,
        consume: Boolean,
        @Nullable clickCallback: IterableHelper.IterableUrlCallback?
    ) {
        showMessage(message, consume, clickCallback, IterableInAppLocation.IN_APP)
    }

    fun showMessage(
        @NonNull message: IterableInAppMessage,
        consume: Boolean,
        @Nullable clickCallback: IterableHelper.IterableUrlCallback?,
        @NonNull inAppLocation: IterableInAppLocation
    ) {
        if (displayer.showMessage(message, inAppLocation, object : IterableHelper.IterableUrlCallback {
            override fun execute(url: Uri?) {
                if (clickCallback != null) {
                    clickCallback.execute(url)
                }

                handleInAppClick(message, url)
                lastInAppShown = IterableUtil.currentTimeMillis()
                scheduleProcessing()
            }
        })) {
            setRead(message, true, null, null)
            if (consume) {
                message.markForDeletion(true)
            }
        }
    }

    /**
     * Remove message from the list
     * @param message The message to be removed
     */
    @Synchronized
    fun removeMessage(@NonNull message: IterableInAppMessage) {
        removeMessage(message, null, null, null, null)
    }

    /**
     * Remove message from the list
     * @param message The message to be removed
     * @param source Source from where the message removal occured. Use IterableInAppDeleteActionType for available sources
     * @param clickLocation Where was the message clicked. Use IterableInAppLocation for available Click Locations
     */
    @Synchronized
    fun removeMessage(
        @NonNull message: IterableInAppMessage,
        @NonNull source: IterableInAppDeleteActionType,
        @NonNull clickLocation: IterableInAppLocation
    ) {
        removeMessage(message, source, clickLocation, null, null)
    }

    /**
     * Remove message from the list
     * @param message The message to be removed
     * @param source Source from where the message removal occured. Use IterableInAppDeleteActionType for available sources
     * @param clickLocation Where was the message clicked. Use IterableInAppLocation for available Click Locations
     * @param successHandler The callback which returns `success`.
     * @param failureHandler The callback which returns `failure`.
     */
    @Synchronized
    fun removeMessage(
        @NonNull message: IterableInAppMessage,
        @Nullable source: IterableInAppDeleteActionType?,
        @Nullable clickLocation: IterableInAppLocation?,
        @Nullable successHandler: IterableHelper.SuccessHandler?,
        @Nullable failureHandler: IterableHelper.FailureHandler?
    ) {
        IterableLogger.printInfo()
        if (message != null) {
            message.setConsumed(true)
            api.inAppConsume(message, source, clickLocation, successHandler, failureHandler)
        }
        notifyOnChange()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun handleInAppClick(@NonNull message: IterableInAppMessage, @Nullable url: Uri?) {
        IterableLogger.printInfo()

        if (url != null && url.toString().isNotEmpty()) {
            val urlString = url.toString()
            when {
                urlString.startsWith(IterableConstants.URL_SCHEME_ACTION) -> {
                    // This is an action:// URL, pass that to the custom action handler
                    val actionName = urlString.replace(IterableConstants.URL_SCHEME_ACTION, "")
                    IterableActionRunner.executeAction(context, IterableAction.actionCustomAction(actionName), IterableActionSource.IN_APP)
                }
                urlString.startsWith(IterableConstants.URL_SCHEME_ITBL) -> {
                    // Handle itbl:// URLs, pass that to the custom action handler for compatibility
                    val actionName = urlString.replace(IterableConstants.URL_SCHEME_ITBL, "")
                    IterableActionRunner.executeAction(context, IterableAction.actionCustomAction(actionName), IterableActionSource.IN_APP)
                }
                urlString.startsWith(IterableConstants.URL_SCHEME_ITERABLE) -> {
                    // Handle iterable:// URLs - reserved for actions defined by the SDK only
                    val actionName = urlString.replace(IterableConstants.URL_SCHEME_ITERABLE, "")
                    handleIterableCustomAction(actionName, message)
                }
                else -> {
                    IterableActionRunner.executeAction(context, IterableAction.actionOpenUrl(urlString), IterableActionSource.IN_APP)
                }
            }
        }
    }

    /**
     * Remove message from the queue
     * This will actually remove it from the local queue
     * This should only be called when a silent push is received
     * @param messageId messageId of the message to be removed
     */
    @Synchronized
    internal fun removeMessage(messageId: String) {
        val message = storage.getMessage(messageId)
        if (message != null) {
            storage.removeMessage(message)
        }
        notifyOnChange()
    }

    private fun isMessageExpired(message: IterableInAppMessage): Boolean {
        return if (message.getExpiresAt() != null) {
            IterableUtil.currentTimeMillis() > message.getExpiresAt()!!.time
        } else {
            false
        }
    }

    private fun syncWithRemoteQueue(remoteQueue: List<IterableInAppMessage>) {
        var changed = false
        val remoteQueueMap = HashMap<String, IterableInAppMessage>()

        for (message in remoteQueue) {
            remoteQueueMap[message.getMessageId()] = message

            val isInAppStored = storage.getMessage(message.getMessageId()) != null

            if (!isInAppStored) {
                storage.addMessage(message)
                onMessageAdded(message)

                changed = true
            }

            if (isInAppStored) {
                val localMessage = storage.getMessage(message.getMessageId())

                val shouldOverwriteInApp = !localMessage!!.isRead() && message.isRead()

                if (shouldOverwriteInApp) {
                    localMessage.setRead(message.isRead())

                    changed = true
                }
            }
        }

        for (localMessage in storage.getMessages()) {
            if (!remoteQueueMap.containsKey(localMessage.getMessageId())) {
                storage.removeMessage(localMessage)

                changed = true
            }
        }

        scheduleProcessing()

        if (changed) {
            notifyOnChange()
        }
    }

    private fun getMessagesSortedByPriorityLevel(messages: List<IterableInAppMessage>): List<IterableInAppMessage> {
        val messagesByPriorityLevel = messages.toMutableList()

        Collections.sort(messagesByPriorityLevel) { message1, message2 ->
            when {
                message1.getPriorityLevel() < message2.getPriorityLevel() -> -1
                message1.getPriorityLevel() == message2.getPriorityLevel() -> 0
                else -> 1
            }
        }

        return messagesByPriorityLevel
    }

    private fun processMessages() {
        if (!activityMonitor.isInForeground() || isShowingInApp() || !canShowInAppAfterPrevious() || isAutoDisplayPaused()) {
            return
        }

        IterableLogger.printInfo()

        val messages = getMessages()
        val messagesByPriorityLevel = getMessagesSortedByPriorityLevel(messages)

        for (message in messagesByPriorityLevel) {
            if (!message.isProcessed() && !message.isConsumed() && message.getTriggerType() == TriggerType.IMMEDIATE && !message.isRead()) {
                IterableLogger.d(TAG, "Calling onNewInApp on " + message.getMessageId())
                val response = handler.onNewInApp(message)
                IterableLogger.d(TAG, "Response: $response")
                message.setProcessed(true)

                if (message.isJsonOnly()) {
                    setRead(message, true, null, null)
                    message.setConsumed(true)
                    api.inAppConsume(message, null, null, null, null)
                    return
                }

                if (response == InAppResponse.SHOW) {
                    val consume = !message.isInboxMessage()
                    showMessage(message, consume, null)
                    return
                }
            }
        }
    }

    internal fun scheduleProcessing() {
        IterableLogger.printInfo()
        if (canShowInAppAfterPrevious()) {
            processMessages()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                processMessages()
            }, ((inAppDisplayInterval - getSecondsSinceLastInApp() + 2.0) * 1000).toLong())
        }
    }

    private fun onMessageAdded(message: IterableInAppMessage) {
        if (!message.isRead()) {
            api.trackInAppDelivery(message)
        }
    }

    private fun isShowingInApp(): Boolean {
        return displayer.isShowingInApp()
    }

    private fun getSecondsSinceLastInApp(): Double {
        return (IterableUtil.currentTimeMillis() - lastInAppShown) / 1000.0
    }

    private fun canShowInAppAfterPrevious(): Boolean {
        return getSecondsSinceLastInApp() >= inAppDisplayInterval
    }

    private fun handleIterableCustomAction(actionName: String, message: IterableInAppMessage) {
        if (IterableConstants.ITERABLE_IN_APP_ACTION_DELETE == actionName) {
            removeMessage(message, IterableInAppDeleteActionType.DELETE_BUTTON, IterableInAppLocation.IN_APP, null, null)
        }
    }

    override fun onSwitchToForeground() {
        if (IterableUtil.currentTimeMillis() - lastSyncTime > MOVE_TO_FOREGROUND_SYNC_INTERVAL_MS) {
            syncInApp()
        } else {
            scheduleProcessing()
        }
    }

    override fun onSwitchToBackground() {

    }

    fun addListener(@NonNull listener: Listener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(@NonNull listener: Listener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun notifyOnChange() {
        Handler(Looper.getMainLooper()).post {
            synchronized(listeners) {
                for (listener in listeners) {
                    listener.onInboxUpdated()
                }
            }
        }
    }
}