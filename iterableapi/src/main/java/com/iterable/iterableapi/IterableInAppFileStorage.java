package com.iterable.iterableapi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IterableInAppFileStorage implements IterableInAppStorage, IterableInAppMessage.OnChangeListener {
    private static final String TAG = "IterableInAppFileStorage";
    private static final String FOLDER_PATH = "IterableInAppFileStorage";
    private final Context context;
    private Map<String, IterableInAppMessage> messages =
            Collections.synchronizedMap(new LinkedHashMap<String, IterableInAppMessage>());
    private ArrayDeque<FileOperation> fileOperationQueue = new ArrayDeque<>();
    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    enum Operation {
        SAVE, LOAD, OTHER
    }

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
        message.setOnChangeListener(this);
        saveMessagesInBackground();
    }

    @Override
    public synchronized void removeMessage(IterableInAppMessage message) {
        message.setOnChangeListener(null);
        removeHTML(message.getMessageId());
        messages.remove(message.getMessageId());
        saveMessagesInBackground();
    }

    @Override
    public void onInAppMessageChanged(IterableInAppMessage message) {
        saveMessagesInBackground();
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
        return new File(getInAppContentFolder(), "itbl_inapp.json");
    }

    private File getInAppCacheStorageFile() {
        return new File(IterableUtil.getSdkCacheDir(context), "itbl_inapp.json");
    }

    private void load() {
        try {
            File inAppStorageFile = getInAppStorageFile();
            if (inAppStorageFile.exists()) {
                JSONObject jsonData = new JSONObject(IterableUtil.readFile(inAppStorageFile));
                loadMessagesFromJson(jsonData);
            } else if (getInAppCacheStorageFile().exists()) {
                JSONObject jsonData = new JSONObject(IterableUtil.readFile(getInAppCacheStorageFile()));
                loadMessagesFromJson(jsonData);
            }
        } catch (Exception e) {
            IterableLogger.e("IterableInAppFileStorage", "Error while loading in-app messages from file", e);
        }
    }

    private void saveMessagesInBackground() {
        //Add SaveRequest to Queue
        FileOperation operation = null;
        try {
            operation = fileOperationQueue.getLast();
        } catch (RuntimeException e) {
            operation = null;
        }
        if (operation == null || operation.operationType != Operation.SAVE) {
            FileOperation save = new FileOperation(Operation.SAVE, new Runnable() {
                @Override
                public void run() {
                    IterableLogger.e("Something", "Saving Message");
                    saveMessages();
                }
            });
            fileOperationQueue.add(save);
        }
        executeOperationQueueTasks();
    }


    private synchronized void saveMessages() {
        saveHTMLContent();
        saveMetadata();
    }

    private synchronized void saveHTMLContent() {
        for (IterableInAppMessage message : messages.values()) {
            if (message.hasLoadedHtmlFromJson()) {
                saveHTML(message.getMessageId(), message.getContent().html);
                message.setLoadedHtmlFromJson(false);
            }
        }
    }

    private synchronized void saveMetadata() {
        try {
            File inAppStorageFile = getInAppStorageFile();
            JSONObject jsonData = serializeMessages();
            IterableUtil.writeFile(inAppStorageFile, jsonData.toString());
        } catch (Exception e) {
            IterableLogger.e("IterableInAppFileStorage", "Error while saving in-app messages to file", e);
        }
    }

    @Override
    public void saveHTML(String messageID, String contentHTML) {
        File folder = createFolderForMessage(messageID);
        if (folder == null) {
            IterableLogger.e(TAG, "Failed to create folder for HTML content");
            return;
        }

        File file = new File(folder, "index.html");
        Boolean result = IterableUtil.writeFile(file, contentHTML);
        if (!result) {
            IterableLogger.e(TAG, "Failed to store HTML content");
        }
    }

    private File createFolderForMessage(String messageID) {
        File folder = getFolderForMessage(messageID);

        if (folder.isDirectory()) {
            if (new File(folder, "index.html").exists()) {
                IterableLogger.v(TAG, "Directory with file already exists. No need to store again");
                return null;
            }
        }

        Boolean result = folder.mkdir();
        if (result) {
            return folder;
        } else {
            return null;
        }
    }

    private void executeOperationQueueTasks() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                for (FileOperation operation : fileOperationQueue) {
                    singleThreadExecutor.submit(operation.backgroundMethod);
                    fileOperationQueue.removeFirst();
                }
            }
        }, 1000);
    }

    private File getInAppContentFolder() {
        File sdkFilesDirectory = IterableUtil.getSDKFilesDirectory(this.context);
        File inAppContentFolder = IterableUtil.getDirectory(sdkFilesDirectory, FOLDER_PATH);
        return inAppContentFolder;
    }

    private File getFolderForMessage(String messageID) {
        return new File(getInAppContentFolder(), messageID);
    }

    private File getFileForContent(String messageID) {
        File folder = getFolderForMessage(messageID);
        File file = new File(folder, "index.html");
        return file;
    }

    @Override
    public String getHTML(String messageID) {
        File file = getFileForContent(messageID);
        String contentHTML = IterableUtil.readFile(file);
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

    class FileOperation {
        Operation operationType;
        Runnable backgroundMethod;

        public FileOperation(Operation operationType, Runnable backgroundMethod) {
            this.operationType = operationType;
            this.backgroundMethod = backgroundMethod;
        }
    }
}
