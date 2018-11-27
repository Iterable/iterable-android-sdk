package com.iterable.iterableapi;

import java.util.List;

interface IterableInAppStorage {
    List<IterableInAppMessage> getMessages();
    IterableInAppMessage getMessage(String messageId);
    void addMessage(IterableInAppMessage message);
    void removeMessage(IterableInAppMessage message);
}
