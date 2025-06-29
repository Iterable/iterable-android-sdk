package com.iterable.iterableapi

import androidx.annotation.NonNull
import androidx.annotation.Nullable

internal interface IterableInAppStorage {
    @NonNull
    fun getMessages(): List<IterableInAppMessage>

    @Nullable
    fun getMessage(messageId: String): IterableInAppMessage?

    fun addMessage(@NonNull message: IterableInAppMessage)

    fun removeMessage(@NonNull message: IterableInAppMessage)

    fun saveHTML(@NonNull messageID: String, @NonNull contentHTML: String)

    @Nullable
    fun getHTML(@NonNull messageID: String): String?

    fun removeHTML(@NonNull messageID: String)
}
