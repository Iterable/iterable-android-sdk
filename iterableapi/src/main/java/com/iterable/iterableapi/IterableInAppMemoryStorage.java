package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class IterableInAppMemoryStorage implements IterableInAppStorage {
    private List<IterableInAppMessage> messages = new ArrayList<>();

    IterableInAppMemoryStorage() {

    }

    @NonNull
    @Override
    public synchronized List<IterableInAppMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    @Nullable
    @Override
    public synchronized IterableInAppMessage getMessage(String messageId) {
        for (IterableInAppMessage message : messages) {
            if (message.getMessageId().equals(messageId)) {
                return message;
            }
        }
        return null;
    }

    @Override
    public synchronized void addMessage(@NonNull IterableInAppMessage message) {
        messages.add(message);
    }

    @Override
    public synchronized void removeMessage(@NonNull IterableInAppMessage message) {
        messages.remove(message);
    }

    @Override
    public void saveHTML(@NonNull String messageID, @NonNull String contentHTML) {

    }

    @Override
    public String getHTML(@NonNull String messageID) {
        return null;
    }

    @Override
    public void removeHTML(@NonNull String messageID) {

    }
}
