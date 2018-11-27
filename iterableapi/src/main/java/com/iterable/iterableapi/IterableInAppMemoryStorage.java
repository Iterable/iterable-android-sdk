package com.iterable.iterableapi;

import java.util.ArrayList;
import java.util.List;

class IterableInAppMemoryStorage implements IterableInAppStorage {
    private List<IterableInAppMessage> messages = new ArrayList<>();

    IterableInAppMemoryStorage() {

    }

    @Override
    public List<IterableInAppMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    @Override
    public IterableInAppMessage getMessage(String messageId) {
        for (IterableInAppMessage message : messages) {
            if (message.getMessageId().equals(messageId)) {
                return message;
            }
        }
        return null;
    }

    @Override
    public void addMessage(IterableInAppMessage message) {
        messages.add(message);
    }

    @Override
    public void removeMessage(IterableInAppMessage message) {
        messages.remove(message);
    }
}
