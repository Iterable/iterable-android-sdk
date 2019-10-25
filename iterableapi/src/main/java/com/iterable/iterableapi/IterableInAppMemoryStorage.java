package com.iterable.iterableapi;

import java.util.ArrayList;
import java.util.List;

class IterableInAppMemoryStorage implements IterableInAppStorage {
    private List<IterableInAppMessage> messages = new ArrayList<>();

    IterableInAppMemoryStorage() {

    }

    @Override
    public synchronized List<IterableInAppMessage> getMessages() {
        return new ArrayList<>(messages);
    }

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
    public synchronized void addMessage(IterableInAppMessage message) {
        messages.add(message);
    }

    @Override
    public synchronized void removeMessage(IterableInAppMessage message) {
        messages.remove(message);
    }

    @Override
    public void save() {

    }

    @Override
    public void saveHTML(String messageID, String contentHTML) {

    }

    @Override
    public String getHTML(String messageID) {
        return null;
    }

    @Override
    public void removeHTML(String messageID) {

    }
}
