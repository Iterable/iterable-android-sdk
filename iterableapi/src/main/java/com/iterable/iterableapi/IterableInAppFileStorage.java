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

public class IterableInAppFileStorage implements IterableInAppStorage {
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
        save();
    }

    @Override
    public synchronized void removeMessage(IterableInAppMessage message) {
        messages.remove(message.getMessageId());
        save();
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
        } catch (JSONException ignored) {}

        return jsonData;
    }

    private void loadMessagesFromJson(JSONObject jsonData) {
        messages.clear();
        JSONArray messagesJson = jsonData.optJSONArray("inAppMessages");
        if (messagesJson != null) {
            for (int i = 0; i < messagesJson.length(); i++) {
                JSONObject messageJson = messagesJson.optJSONObject(i);
                if (messageJson != null) {
                    IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson);
                    if (message != null) {
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
            JSONObject jsonData = serializeMessages();
            File inAppStorageFile = getInAppStorageFile();
            IterableUtil.writeFile(inAppStorageFile, jsonData.toString());
        } catch (Exception e) {
            IterableLogger.e("IterableInAppFileStorage", "Error while saving in-app messages to file", e);
        }
    }
}
