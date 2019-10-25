package com.iterable.iterableapi;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IterableInAppFileStorage implements IterableInAppStorage, IterableInAppMessage.OnChangeListener {
    private static final String TAG = "InAppFileStorage";
    String FOLDER_PATH = "InAppContent";
    private final Context context;
    private Map<String, IterableInAppMessage> messages =
            Collections.synchronizedMap(new LinkedHashMap<String, IterableInAppMessage>());

    IterableInAppFileStorage(Context context) {
        this.context = context;
        load();
    }

    @Override
    public synchronized List<IterableInAppMessage> getMessages() {
        return new ArrayList<>(messages.values());
    }

    @Override
    public synchronized IterableInAppMessage getMessage(String messageId) {
        return messages.get(messageId);
    }

    @Override
    public synchronized void addMessage(IterableInAppMessage message) {
        messages.put(message.getMessageId(), message);
        saveHTML(message.getMessageId(),message.getContent().html);
        message.setOnChangeListener(this);
        save();
    }

    @Override
    public synchronized void removeMessage(IterableInAppMessage message) {
        message.setOnChangeListener(null);
        removeHTML(message.getMessageId());
        messages.remove(message.getMessageId());
        save();
    }

    @Override
    public void onInAppMessageChanged(IterableInAppMessage message) {
        save();
    }

    private synchronized void clearMessages() {
        for (Map.Entry<String, IterableInAppMessage> entry : messages.entrySet()) {
            IterableInAppMessage message = entry.getValue();
            message.setOnChangeListener(null);
        }
        messages.clear();
    }

    private JSONObject serializeMessages() {
        JSONObject jsonData = new JSONObject();
        JSONArray messagesJson = new JSONArray();

        try {
            for (Map.Entry<String, IterableInAppMessage> entry : messages.entrySet()) {
                IterableInAppMessage message = entry.getValue();
                messagesJson.put(message.toJSONObject());
            }
            jsonData.putOpt("inAppMessages", messagesJson);
        } catch (JSONException e) {
            IterableLogger.e(TAG, "Error while serializing messages", e);
        }

        return jsonData;
    }

    private void loadMessagesFromJson(JSONObject jsonData) {
        clearMessages();
        JSONArray messagesJson = jsonData.optJSONArray("inAppMessages");
        if (messagesJson != null) {
            for (int i = 0; i < messagesJson.length(); i++) {
                JSONObject messageJson = messagesJson.optJSONObject(i);
                if (messageJson != null) {
                    IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson, this);
                    if (message != null) {
                        message.setOnChangeListener(this);
                        messages.put(message.getMessageId(), message);
                    }
                }
            }
        }
    }

    private File getInAppStorageFile() {
        return new File(IterableUtil.getSdkCacheDir(context), "itbl_inapp.json");
    }

    private void load() {
        try {
            File inAppStorageFile = getInAppStorageFile();
            if (inAppStorageFile.exists()) {
                JSONObject jsonData = new JSONObject(IterableUtil.readFile(inAppStorageFile));
                loadMessagesFromJson(jsonData);
            }
        } catch (Exception e) {
            IterableLogger.e("IterableInAppFileStorage", "Error while loading in-app messages from file", e);
        }
    }


    public synchronized void save() {
        try {
            //TODO: Serilize messages. but keep HTML part seperate.
            JSONObject jsonData = serializeMessages();
            File inAppStorageFile = getInAppStorageFile();
            IterableUtil.writeFile(inAppStorageFile, jsonData.toString());
        } catch (Exception e) {
            IterableLogger.e("IterableInAppFileStorage", "Error while saving in-app messages to file", e);
        }
    }

    @Override
    public void saveHTML(String messageID, String contentHTML) {
        File folder = createFolderIfNecessary(messageID);
        if (folder == null) {
            return;
        }

        File file = new File(folder,"index.html");
        Boolean result = IterableUtil.instance.writeFile(file,contentHTML);
        if (!result) {
            IterableLogger.e(TAG,"Write fail");
        }
    }

    private File createFolderIfNecessary(String messageID) {
        File folder = getFolderForMessage(messageID);

        if (folder.isDirectory()) {
            IterableLogger.v(TAG, "Directory exists already. No need to store again");
            return null;
        }

        Boolean result = folder.mkdir();
        if (result){
            return folder;
        }else {
            return null;
        }
    }

    private File getInAppContentFolder() {
        File context = IterableUtil.getFileDir(IterableApi.getInstance().getMainActivityContext(),FOLDER_PATH);
        return context;
    }

    private File getFolderForMessage(String messageID) {
        return new File(getInAppContentFolder(), messageID);
    }

    private File getFileForContent(String messageID) {
        File folder = getFolderForMessage(messageID);
        File file = new File(folder,"index.html");
        return file;
    }

    @Override
    public String getHTML(String messageID) {
        File file = getFileForContent(messageID);
        String contentHTML = IterableUtil.instance.readFile(file);
        return contentHTML;
    }

    @Override
    public void removeHTML(String messageID) {
        File folder = getFolderForMessage(messageID);
        if (folder == null) {
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            file.delete();
        }
        folder.delete();
    }
}
