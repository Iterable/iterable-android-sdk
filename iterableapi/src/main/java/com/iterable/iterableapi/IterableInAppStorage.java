package com.iterable.iterableapi;

import java.util.List;

interface IterableInAppStorage {
    List<IterableInAppMessage> getMessages();
    void putMessages(List<IterableInAppMessage> messages);
    void removeMessage(IterableInAppMessage message);
}
