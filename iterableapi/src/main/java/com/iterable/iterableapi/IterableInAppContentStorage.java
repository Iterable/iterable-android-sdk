package com.iterable.iterableapi;

public interface IterableInAppContentStorage {

    void saveHTML(String messageID, String contentHTML);
    String getHTML(String messageID);
    void removeContent(String messageID);

}
