package com.iterable.iterableapi

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class IterableInAppFileStorage(private val context: Context) : IterableInAppStorage, IterableInAppMessage.OnChangeListener {
    
    companion object {
        private const val TAG = "IterableInAppFileStorage"
        private const val FOLDER_PATH = "IterableInAppFileStorage"
        private const val INDEX_FILE = "index.html"
        private const val OPERATION_SAVE = 100
    }

    private val messages: MutableMap<String, IterableInAppMessage> =
        Collections.synchronizedMap(LinkedHashMap<String, IterableInAppMessage>())

    private val fileOperationThread = HandlerThread("FileOperationThread")

    @VisibleForTesting
    lateinit var fileOperationHandler: FileOperationHandler

    init {
        fileOperationThread.start()
        fileOperationHandler = FileOperationHandler(fileOperationThread.looper)
        load()
    }

    //region IterableInAppStorage interface implementation
    @NonNull
    @Synchronized
    override fun getMessages(): List<IterableInAppMessage> {
        return ArrayList(messages.values)
    }

    @Nullable
    @Synchronized
    override fun getMessage(@NonNull messageId: String): IterableInAppMessage? {
        return messages[messageId]
    }

    @Synchronized
    override fun addMessage(@NonNull message: IterableInAppMessage) {
        messages[message.messageId] = message
        message.setOnChangeListener(this)
        saveMessagesInBackground()
    }

    @Synchronized
    override fun removeMessage(@NonNull message: IterableInAppMessage) {
        message.setOnChangeListener(null)
        removeHTML(message.messageId)
        messages.remove(message.messageId)
        saveMessagesInBackground()
    }

    override fun saveHTML(@NonNull messageID: String, @NonNull contentHTML: String) {
        val folder = createFolderForMessage(messageID)
        if (folder == null) {
            IterableLogger.e(TAG, "Failed to create folder for HTML content")
            return
        }

        val file = File(folder, INDEX_FILE)
        val result = IterableUtil.writeFile(file, contentHTML)
        if (!result) {
            IterableLogger.e(TAG, "Failed to store HTML content")
        }
    }

    @Nullable
    override fun getHTML(@NonNull messageID: String): String? {
        val file = getFileForContent(messageID)
        return IterableUtil.readFile(file)
    }

    override fun removeHTML(@NonNull messageID: String) {
        val folder = getFolderForMessage(messageID)

        val files = folder.listFiles() ?: return

        for (file in files) {
            file.delete()
        }
        folder.delete()
    }
    //endregion

    //region In-App Lifecycle
    override fun onInAppMessageChanged(@NonNull message: IterableInAppMessage) {
        saveMessagesInBackground()
    }

    @Synchronized
    private fun clearMessages() {
        for ((_, message) in messages) {
            message.setOnChangeListener(null)
        }
        messages.clear()
    }
    //endregion

    //region JSON Parsing
    @NonNull
    private fun serializeMessages(): JSONObject {
        val jsonData = JSONObject()
        val messagesJson = JSONArray()

        try {
            for ((_, message) in messages) {
                messagesJson.put(message.toJSONObject())
            }
            jsonData.putOpt("inAppMessages", messagesJson)
        } catch (e: JSONException) {
            IterableLogger.e(TAG, "Error while serializing messages", e)
        }

        return jsonData
    }

    private fun loadMessagesFromJson(jsonData: JSONObject) {
        clearMessages()
        val messagesJson = jsonData.optJSONArray("inAppMessages")
        if (messagesJson != null) {
            for (i in 0 until messagesJson.length()) {
                val messageJson = messagesJson.optJSONObject(i)

                if (messageJson != null) {
                    val message = IterableInAppMessage.fromJSONObject(messageJson, this)
                    if (message != null) {
                        message.setOnChangeListener(this)
                        messages[message.messageId] = message
                    }
                }
            }
        }
    }
    //endregion

    //region File Saving/Loading
    private fun load() {
        try {
            val inAppStorageFile = getInAppStorageFile()
            if (inAppStorageFile.exists()) {
                val jsonData = JSONObject(IterableUtil.readFile(inAppStorageFile))
                loadMessagesFromJson(jsonData)
            } else if (getInAppCacheStorageFile().exists()) {
                val jsonData = JSONObject(IterableUtil.readFile(getInAppCacheStorageFile()))
                loadMessagesFromJson(jsonData)
            }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error while loading in-app messages from file", e)
        }
    }

    private fun saveMessagesInBackground() {
        if (!fileOperationHandler.hasMessages(OPERATION_SAVE)) {
            fileOperationHandler.sendEmptyMessageDelayed(OPERATION_SAVE, 100)
        }
    }

    @Synchronized
    private fun saveMessages() {
        saveHTMLContent()
        saveMetadata()
    }

    @Synchronized
    private fun saveHTMLContent() {
        for (message in messages.values) {
            if (message.hasLoadedHtmlFromJson()) {
                saveHTML(message.messageId, message.content.html ?: "")
                message.setLoadedHtmlFromJson(false)
            }
        }
    }

    @Synchronized
    private fun saveMetadata() {
        try {
            val inAppStorageFile = getInAppStorageFile()
            val jsonData = serializeMessages()
            IterableUtil.writeFile(inAppStorageFile, jsonData.toString())
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error while saving in-app messages to file", e)
        }
    }
    //endregion

    //region File Management
    private fun getInAppStorageFile(): File {
        return File(getInAppContentFolder(), "itbl_inapp.json")
    }

    private fun getInAppCacheStorageFile(): File {
        return File(IterableUtil.getSdkCacheDir(context), "itbl_inapp.json")
    }

    @Nullable
    private fun createFolderForMessage(messageID: String): File? {
        val folder = getFolderForMessage(messageID)

        if (folder.isDirectory && File(folder, INDEX_FILE).exists()) {
            IterableLogger.v(TAG, "Directory with file already exists. No need to store again")
            return null
        }

        val result = folder.mkdir()
        return if (result) {
            folder
        } else {
            null
        }
    }

    private fun getInAppContentFolder(): File {
        val sdkFilesDirectory = IterableUtil.getSDKFilesDirectory(this.context)
        return IterableUtil.getDirectory(sdkFilesDirectory, FOLDER_PATH)
    }

    @NonNull
    private fun getFolderForMessage(messageID: String): File {
        return File(getInAppContentFolder(), messageID)
    }

    @NonNull
    private fun getFileForContent(messageID: String): File {
        val folder = getFolderForMessage(messageID)
        return File(folder, INDEX_FILE)
    }
    //endregion

    inner class FileOperationHandler(threadLooper: Looper) : Handler(threadLooper) {
        override fun handleMessage(msg: Message) {
            if (msg.what == OPERATION_SAVE) {
                saveMessages()
            }
        }
    }
}