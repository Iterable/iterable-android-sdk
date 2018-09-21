package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONObject;

public class IterableUtil {

    private static final String TAG = "IterableUtil";

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    static String getAppVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            IterableLogger.e(TAG, "Error while retrieving app version", e);
        }
        return null;
    }

    static String getAppVersionCode(Context context) {
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
    static int convertVersionStringToInt(String versionString) {
        int version = 0;
        String[] versionNumbers = versionString.split("\\.");
        for (int i = 0; i < versionNumbers.length; i++) {
            version += ((int) Math.pow(10, 3 * (2 - i))) * Integer.parseInt(versionNumbers[i]);
        }
        return version;
    }

    static void saveExpirableJsonObject(SharedPreferences preferences, String key, JSONObject object, long expirationInterval) {
        saveExpirableValue(preferences, key, object.toString(), expirationInterval);
    }

    static void saveExpirableValue(SharedPreferences preferences, String key, String value, long expirationInterval) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key + IterableConstants.SHARED_PREFS_OBJECT_SUFFIX, value);
        editor.putLong(key + IterableConstants.SHARED_PREFS_EXPIRATION_SUFFIX, currentTimeMillis() + expirationInterval);
        editor.apply();
    }

    static String retrieveExpirableValue(SharedPreferences preferences, String key) {
        String value = preferences.getString(key + IterableConstants.SHARED_PREFS_OBJECT_SUFFIX, null);
        long expirationTime = preferences.getLong(key + IterableConstants.SHARED_PREFS_EXPIRATION_SUFFIX, 0);

        if (value == null || expirationTime < currentTimeMillis()) {
            return null;
        } else {
            return value;
        }
    }

    static JSONObject retrieveExpirableJsonObject(SharedPreferences preferences, String key) {
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

}
