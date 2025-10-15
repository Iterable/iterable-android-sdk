package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONObject;

import java.io.File;

public class IterableUtil {
    @VisibleForTesting
    static IterableUtilImpl instance = new IterableUtilImpl();

    private IterableUtil() { }

    static long currentTimeMillis() {
        return instance.currentTimeMillis();
    }

    public static String getAppVersion(Context context) {
        return instance.getAppVersion(context);
    }

    public static String getAppVersionCode(Context context) {
        return instance.getAppVersionCode(context);
    }

    /**
     * Maps a version string like "x.y.z" onto an integer like xxxyyyzzz
     * Example: "1.2.3" -> 1002003
     * @param versionString Version string
     * @return Integer representation of the version
     */
    static int convertVersionStringToInt(String versionString) {
        return instance.convertVersionStringToInt(versionString);
    }

    static void saveExpirableJsonObject(SharedPreferences preferences, String key, JSONObject object, long expirationInterval) {
        instance.saveExpirableJsonObject(preferences, key, object, expirationInterval);
    }

    static void saveExpirableValue(SharedPreferences preferences, String key, String value, long expirationInterval) {
        instance.saveExpirableValue(preferences, key, value, expirationInterval);
    }

    static String retrieveExpirableValue(SharedPreferences preferences, String key) {
        return instance.retrieveExpirableValue(preferences, key);
    }

    static JSONObject retrieveExpirableJsonObject(SharedPreferences preferences, String key) {
        return instance.retrieveExpirableJsonObject(preferences, key);
    }

    @Nullable
    static Long retrieveValidCampaignIdOrNull(final JSONObject json, final String key) {
        return instance.retrieveValidCampaignIdOrNull(json, key);
    }

    static boolean isValidCampaignId(final long campaignId) {
        return instance.isValidCampaignId(campaignId);
    }

    static File getSdkCacheDir(Context context) {
        return instance.getSdkCacheDir(context);
    }

    static File getSDKFilesDirectory(Context context) {
        return instance.getSDKFilesDirectory(context);
    }

    static File getDirectory(File folder, String subFolder) {
        return instance.getDirectory(folder, subFolder);
    }

    @Nullable
    static String readFile(File file) {
        return instance.readFile(file);
    }

    static boolean writeFile(File file, String content) {
        return instance.writeFile(file, content);
    }

    static boolean isUrlOpenAllowed(@NonNull String url) {
        return instance.isUrlOpenAllowed(url);
    }

    static String getWebViewBaseUrl() {
        return instance.getWebViewBaseUrl();
    }
}
