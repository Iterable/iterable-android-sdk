package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.iterable.iterableapi.util.IOUtils;

import org.json.JSONException;
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

    /**
     * Gets the advertisingId if available
     * @return
     */
    static String getAdvertisingId(Context context) {
        return instance.getAdvertisingId(context);
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

        String getAdvertisingId(Context context) {
            String advertisingId = null;
            try {
                Class adClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                if (adClass != null) {
                    Object advertisingIdInfo = adClass.getMethod("getAdvertisingIdInfo", Context.class).invoke(null, context);
                    if (advertisingIdInfo != null) {
                        advertisingId = (String) advertisingIdInfo.getClass().getMethod("getId").invoke(advertisingIdInfo);
                    }
                }
            } catch (ClassNotFoundException e) {
                IterableLogger.d(TAG, "ClassNotFoundException: Can't track ADID. " +
                        "Check that play-services-ads is added to the dependencies.", e);
            } catch (Exception e) {
                IterableLogger.w(TAG, "Error while fetching advertising ID", e);
            }
            return advertisingId;
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
    }

    static boolean isUrlOpenAllowed(@NonNull String url) {
        String urlProtocol = url.split("://")[0];
        if (urlProtocol.equals("https")) {
            return true;
        }

        for (String allowedProtocol : IterableApi.getInstance().config.allowedProtocols) {
            if (urlProtocol.equals(allowedProtocol)) {
                return true;
            }
        }

        IterableLogger.d(TAG, urlProtocol + " is not in the allowed protocols");
        return false;
    }
}
