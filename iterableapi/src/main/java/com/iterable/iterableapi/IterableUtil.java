package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.VisibleForTesting;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

class IterableUtil {
    private static final String TAG = "IterableUtil";

    @VisibleForTesting
    static IterableUtilImpl instance = new IterableUtilImpl();

    static long currentTimeMillis() {
        return instance.currentTimeMillis();
    }

    static String getAppVersion(Context context) {
        return instance.getAppVersion(context);
    }

    static String getAppVersionCode(Context context) {
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

    static File getSdkCacheDir(Context context) {
        return instance.getSdkCacheDir(context);
    }

    static String readFile(File file) {
        return instance.readFile(file);
    }

    static boolean writeFile(File file, String content) {
        return instance.writeFile(file, content);
    }


    static class IterableUtilImpl {

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

        File getSdkCacheDir(Context context) {
            File sdkCacheDir = new File(context.getCacheDir(), "com.iterable.sdk");
            if (!sdkCacheDir.exists()) {
                sdkCacheDir.mkdirs();
            }
            return sdkCacheDir;
        }

        String readFile(File file) {
            try {
                FileInputStream inputStream = new FileInputStream(file);
                InputStreamReader streamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return stringBuilder.toString();
            } catch (Exception e) {
                IterableLogger.e(TAG, "Error while reading file: " + file.toString(), e);
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
    }
}
