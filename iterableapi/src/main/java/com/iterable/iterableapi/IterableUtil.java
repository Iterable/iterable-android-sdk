package com.iterable.iterableapi;

import android.content.SharedPreferences;

import org.json.JSONObject;

public class IterableUtil {

    private static final String TAG = "IterableUtil";

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
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
