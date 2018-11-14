package com.iterable.iterableapi;

import java.util.List;

class IterableInAppMemoryStorage implements IterableInAppStorage {
    private List<IterableInAppMessage> messages;

    IterableInAppMemoryStorage() {

    }

    @Override
    public List<IterableInAppMessage> getMessages() {
        return messages;
    }

    @Override
    public void putMessages(List<IterableInAppMessage> messages) {
        this.messages = messages;
    }

    @Override
    public void removeMessage(IterableInAppMessage message) {
        messages.remove(message);
    }
}
