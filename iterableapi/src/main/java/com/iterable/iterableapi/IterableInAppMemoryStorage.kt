package com.iterable.iterableapi

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList

internal class IterableInAppMemoryStorage : IterableInAppStorage {
    private val messages = ArrayList<IterableInAppMessage>()

    //region IterableInAppStorage interface implementation
    @NonNull
    @Synchronized
    override fun getMessages(): List<IterableInAppMessage> {
        return ArrayList(messages)
    }

    @Nullable
    @Synchronized
    override fun getMessage(messageId: String): IterableInAppMessage? {
        for (message in messages) {
            if (message.messageId == messageId) {
                return message
            }
        }
        return null
    }

    @Synchronized
    override fun addMessage(@NonNull message: IterableInAppMessage) {
        messages.add(message)
    }

    @Synchronized
    override fun removeMessage(@NonNull message: IterableInAppMessage) {
        messages.remove(message)
    }

    override fun saveHTML(@NonNull messageID: String, @NonNull contentHTML: String) {

    }

    override fun getHTML(@NonNull messageID: String): String? {
        return null
    }

    override fun removeHTML(@NonNull messageID: String) {

    }
    //endregion
}
