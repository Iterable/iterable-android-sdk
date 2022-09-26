package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

class IterableUtilImpl {
    private static final String TAG = "IterableUtilImpl";

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    String getAppVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            IterableLogger.e(TAG, "Error while retrieving app version", e);
        }
        return null;
    }

    String getAppVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return Integer.toString(pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            IterableLogger.e(TAG, "Error while retrieving app version code", e);
        }
        return null;
    }

    /**
     * Maps a version string like "x.y.z" onto an integer like xxxyyyzzz
     * Example: "1.2.3" -> 1002003
     * @param versionString Version string
     * @return Integer representation of the version
     */
    int convertVersionStringToInt(String versionString) {
        int version = 0;
        String[] versionNumbers = versionString.split("\\.");
        for (int i = 0; i < versionNumbers.length; i++) {
            version += ((int) Math.pow(10, 3 * (2 - i))) * Integer.parseInt(versionNumbers[i]);
        }
        return version;
    }

    void saveExpirableJsonObject(SharedPreferences preferences, String key, JSONObject object, long expirationInterval) {
        saveExpirableValue(preferences, key, object.toString(), expirationInterval);
    }

    void saveExpirableValue(SharedPreferences preferences, String key, String value, long expirationInterval) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key + IterableConstants.SHARED_PREFS_OBJECT_SUFFIX, value);
        editor.putLong(key + IterableConstants.SHARED_PREFS_EXPIRATION_SUFFIX, currentTimeMillis() + expirationInterval);
        editor.apply();
    }

    String retrieveExpirableValue(SharedPreferences preferences, String key) {
        String value = preferences.getString(key + IterableConstants.SHARED_PREFS_OBJECT_SUFFIX, null);
        long expirationTime = preferences.getLong(key + IterableConstants.SHARED_PREFS_EXPIRATION_SUFFIX, 0);

        if (value == null || expirationTime < currentTimeMillis()) {
            return null;
        } else {
            return value;
        }
    }

    JSONObject retrieveExpirableJsonObject(SharedPreferences preferences, String key) {
        try {
            String encodedObject = retrieveExpirableValue(preferences, key);
            if (encodedObject != null) {
                return new JSONObject(encodedObject);
            }
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing an expirable object for key: " + key, e);
        }
        return null;
    }

    @Nullable
    Long retrieveValidCampaignIdOrNull(final JSONObject json, final String key) {
        try {
            final long id = json.getLong(key);
            if (isValidCampaignId(id)) {
                return id;
            } else {
                return null;
            }
        } catch (final JSONException ex) {
            return null;
        }
    }

    boolean isValidCampaignId(final long campaignId) {
        return campaignId >= 0;
    }

    File getSdkCacheDir(Context context) {
        File sdkCacheDir = new File(context.getCacheDir(), "com.iterable.sdk");
        if (!sdkCacheDir.exists()) {
            sdkCacheDir.mkdirs();
        }
        return sdkCacheDir;
    }

    File getSDKFilesDirectory(Context context) {
        File iterableSDKRootDirectory = new File(context.getFilesDir(), "com.iterable.sdk");
        if (!iterableSDKRootDirectory.exists()) {
            iterableSDKRootDirectory.mkdirs();
        }
        return iterableSDKRootDirectory;
    }

    File getDirectory(File folder, String subFolder) {
        File applicationRootDirectory = new File(folder, subFolder);
        if (!applicationRootDirectory.exists()) {
            applicationRootDirectory.mkdirs();
        }
        return applicationRootDirectory;
    }

    @Nullable
    String readFile(File file) {
        FileInputStream inputStream = null;
        InputStreamReader streamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = new FileInputStream(file);
            streamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while reading file: " + file.toString(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(streamReader);
            IOUtils.closeQuietly(bufferedReader);
        }
        return null;
    }

    boolean writeFile(File file, String content) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(content);
            outputStreamWriter.close();
            return true;
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while writing to file: " + file.toString(), e);
        }
        return false;
    }

    static boolean isUrlOpenAllowed(@NonNull String url) {
        String urlProtocol = url.split("://")[0];

        if (urlProtocol.equals("https")) {
            return true;
        }

        for (String allowedProtocol : IterableApi.sharedInstance.config.allowedProtocols) {
            if (urlProtocol.equals(allowedProtocol)) {
                return true;
            }
        }

        IterableLogger.d(TAG, urlProtocol + " is not in the allowed protocols");

        return false;
    }
}