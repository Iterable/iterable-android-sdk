package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting

import org.json.JSONObject

import java.io.File

object IterableUtil {
    @VisibleForTesting
    @JvmStatic
    internal var instance: IterableUtilImpl = IterableUtilImpl()

    @JvmStatic
    fun currentTimeMillis(): Long {
        return instance.currentTimeMillis()
    }

    @JvmStatic
    fun getAppVersion(context: Context): String {
        return instance.getAppVersion(context)
    }

    @JvmStatic
    fun getAppVersionCode(context: Context): String {
        return instance.getAppVersionCode(context)
    }

    /**
     * Maps a version string like "x.y.z" onto an integer like xxxyyyzzz
     * Example: "1.2.3" -> 1002003
     * @param versionString Version string
     * @return Integer representation of the version
     */
    @JvmStatic
    fun convertVersionStringToInt(versionString: String): Int {
        return instance.convertVersionStringToInt(versionString)
    }

    @JvmStatic
    fun saveExpirableJsonObject(preferences: SharedPreferences, key: String, obj: JSONObject, expirationInterval: Long) {
        instance.saveExpirableJsonObject(preferences, key, obj, expirationInterval)
    }

    @JvmStatic
    fun saveExpirableValue(preferences: SharedPreferences, key: String, value: String, expirationInterval: Long) {
        instance.saveExpirableValue(preferences, key, value, expirationInterval)
    }

    @JvmStatic
    fun retrieveExpirableValue(preferences: SharedPreferences, key: String): String? {
        return instance.retrieveExpirableValue(preferences, key)
    }

    @JvmStatic
    fun retrieveExpirableJsonObject(preferences: SharedPreferences, key: String): JSONObject? {
        return instance.retrieveExpirableJsonObject(preferences, key)
    }

    @Nullable
    @JvmStatic
    fun retrieveValidCampaignIdOrNull(json: JSONObject, key: String): Long? {
        return instance.retrieveValidCampaignIdOrNull(json, key)
    }

    @JvmStatic
    fun isValidCampaignId(campaignId: Long): Boolean {
        return instance.isValidCampaignId(campaignId)
    }

    @JvmStatic
    fun getSdkCacheDir(context: Context): File {
        return instance.getSdkCacheDir(context)
    }

    @JvmStatic
    fun getSDKFilesDirectory(context: Context): File {
        return instance.getSDKFilesDirectory(context)
    }

    @JvmStatic
    fun getDirectory(folder: File, subFolder: String): File {
        return instance.getDirectory(folder, subFolder)
    }

    @Nullable
    @JvmStatic
    fun readFile(file: File): String? {
        return instance.readFile(file)
    }

    @JvmStatic
    fun writeFile(file: File, content: String): Boolean {
        return instance.writeFile(file, content)
    }

    @JvmStatic
    fun isUrlOpenAllowed(@NonNull url: String): Boolean {
        return instance.isUrlOpenAllowed(url)
    }
}
