package com.iterable.iterableapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

interface IterableInAppStorage {
    @NonNull
    List<IterableInAppMessage> getMessages();
    @Nullable
    IterableInAppMessage getMessage(String messageId);
    void addMessage(@NonNull IterableInAppMessage message);
    void removeMessage(@NonNull IterableInAppMessage message);

    void saveHTML(@NonNull String messageID, @NonNull String contentHTML);
    @Nullable
    String getHTML(@NonNull String messageID);
    void removeHTML(@NonNull String messageID);
}
