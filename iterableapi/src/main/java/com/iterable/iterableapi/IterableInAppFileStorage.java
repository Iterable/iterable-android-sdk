package com.iterable.iterableapi;

import android.content.Context;

import com.iterable.iterableapi.util.Future;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IterableInAppFileStorage implements IterableInAppStorage, IterableInAppMessage.OnChangeListener{

    private static final String TAG = "IterableInAppFileStorage";
    ExecutorService saveMessageService;

    private final Context context;
    private Map<String, IterableInAppMessage> messages =
            Collections.synchronizedMap(new LinkedHashMap<String, IterableInAppMessage>());

    IterableInAppFileStorage(Context context) {
        this.context = context;
        backGroundLoad();
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
        message.setOnChangeListener(this);
        backGroundSave();
    }

    @Override
    public synchronized void removeMessage(IterableInAppMessage message) {
        message.setOnChangeListener(null);
        messages.remove(message.getMessageId());
        backGroundSave();
    }

    @Override
    public void save() {

    }

    @Override
    public void onInAppMessageChanged(IterableInAppMessage message) {
        backGroundSave();
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
                    IterableInAppMessage message = IterableInAppMessage.fromJSONObject(messageJson);
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

    private void backGroundLoad() {

        Future.runAsync(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                File inAppStorageFile = getInAppStorageFile();
                if (inAppStorageFile.exists()) {
                    JSONObject jsonData = new JSONObject(IterableUtil.readFile(inAppStorageFile));
                    loadMessagesFromJson(jsonData);
                }
                return null;
            }
        }).onSuccess(new Future.SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                IterableApi.getInstance().getInAppManager().notifyOnChange();
            }
        }).onFailure(new Future.FailureCallback() {
            @Override
            public void onFailure(Throwable throwable) {
                IterableLogger.e(TAG, "Error loading file");
            }
        });
    }

    public void backGroundSave() {

        if (saveMessageService == null) {
            saveMessageService =  Executors.newSingleThreadExecutor();
        }

        Future.runAsync(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                JSONObject jsonData = serializeMessages();
                File inAppStorageFile = getInAppStorageFile();
                IterableUtil.writeFile(inAppStorageFile, jsonData.toString());
                return null;
            }
        }, saveMessageService)
                .onSuccess(new Future.SuccessCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        IterableApi.getInstance().getInAppManager().notifyOnChange();
                    }
                })
                .onFailure(new Future.FailureCallback() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        IterableLogger.e(TAG, "Error saving file");
                    }
                });

    }
}