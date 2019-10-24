package com.iterable.iterableapi;


import android.util.Log;

import java.io.File;

public class InAppFileContentStorage implements IterableInAppContentStorage {


    String FOLDER_PATH = "InAppContent";
    String TAG = "InAppFileContentStorage";

    @Override
    public void saveHTML(String messageID, String contentHTML) {
        //Check if folder named with messageID exists already
        // Skip if the folder already exists
        File folder = createFolderIfNecessary(messageID);

        if (folder == null) {
            return;
        }


        File file = new File(folder,"index.html");
        Boolean result = IterableUtil.instance.writeFile(file,contentHTML);
        if (result) {
            Log.d(TAG,"Successfully written on file");
        }else {
            Log.d(TAG,"Write fail");
        }

    }

    private File createFolderIfNecessary(String messageID) {
        File folder = getFolderForMessage(messageID);

        //Check if it is a directory
        if (folder.isDirectory()) {
            Log.d(TAG, "Directory exists already. No need to store again");
            return null;
        }

        Log.d(TAG, "No such directory exists. Creating a directory");
        Boolean result = folder.mkdir();
        String processName = "Directory creation ";

        printResult(result, processName);

        return folder;
    }

    private void printResult(Boolean result, String processName) {
        Log.d(TAG, processName);
        if (result) {
            Log.d(TAG, "Successful");
        } else {
            Log.d(TAG, "Failed");
        }
    }

    private File getFileForContent(String messageID) {
        File folder = getFolderForMessage(messageID);
        File file = new File(folder,"index.html");
        return file;
    }

    private File getFolderForMessage(String messageID) {
        return new File(getInAppStorageFile(), messageID);
    }


    @Override
    public String getHTML(String messageID) {
        File file = getFileForContent(messageID);
        String contentHTML = IterableUtil.instance.readFile(file);
        Log.d(TAG,contentHTML);
        return contentHTML;
    }

    @Override
    public void removeContent(String messageID) {
        File folder = getFolderForMessage(messageID);

        //Delete each files inside the directory before deleting the directory as required by Android File class
        File[] files = folder.listFiles();
        for (File file: files
        ) {
            file.delete();
        }
        Boolean result = folder.delete();
        printResult(result,"RemoveContent");
    }

    private File getInAppStorageFile() {
        //return new File(IterableUtil.getFileDir(IterableApi.getInstance().getMainActivityContext(), FOLDER_PATH));
        File context = IterableUtil.getFileDir(IterableApi.getInstance().getMainActivityContext(),FOLDER_PATH);
        return context;
    }

}
