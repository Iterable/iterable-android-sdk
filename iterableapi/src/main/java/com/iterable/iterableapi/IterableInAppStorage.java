package com.iterable.iterableapi;

import java.util.List;

interface IterableInAppStorage {
    List<IterableInAppMessage> getMessages();
    IterableInAppMessage getMessage(String messageId);
    void addMessage(IterableInAppMessage message);
    void removeMessage(IterableInAppMessage message);

    void saveHTML(String messageID, String contentHTML);
    String getHTML(String messageID);
    void removeHTML(String messageID);
}
