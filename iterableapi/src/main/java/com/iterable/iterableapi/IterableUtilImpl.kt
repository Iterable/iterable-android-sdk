package com.iterable.iterableapi

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import com.iterable.iterableapi.util.IOUtils

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.math.pow

internal class IterableUtilImpl {
    companion object {
        private const val TAG = "IterableUtilImpl"

        fun isUrlOpenAllowed(@NonNull url: String): Boolean {
            val urlProtocol = url.split("://")[0]

            if (urlProtocol == "https") {
                return true
            }

            for (allowedProtocol in IterableApi.sharedInstance.config.allowedProtocols) {
                if (urlProtocol == allowedProtocol) {
                    return true
                }
            }

            IterableLogger.d(TAG, "$urlProtocol is not in the allowed protocols")
            return false
        }
    }

    fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    internal fun getAppVersion(context: Context): String? {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            IterableLogger.e(TAG, "Error while retrieving app version", e)
        }
        return null
    }

    internal fun getAppVersionCode(context: Context): String? {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionCode.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            IterableLogger.e(TAG, "Error while retrieving app version code", e)
        }
        return null
    }

    /**
     * Maps a version string like "x.y.z" onto an integer like xxxyyyzzz
     * Example: "1.2.3" -> 1002003
     * @param versionString Version string
     * @return Integer representation of the version
     */
    internal fun convertVersionStringToInt(versionString: String): Int {
        var version = 0
        val versionNumbers = versionString.split("\\.".toRegex()).toTypedArray()
        for (i in versionNumbers.indices) {
            version += (10.0.pow(3 * (2 - i)).toInt()) * versionNumbers[i].toInt()
        }
        return version
    }

    internal fun saveExpirableJsonObject(preferences: SharedPreferences, key: String, obj: JSONObject, expirationInterval: Long) {
        saveExpirableValue(preferences, key, obj.toString(), expirationInterval)
    }

    internal fun saveExpirableValue(preferences: SharedPreferences, key: String, value: String, expirationInterval: Long) {
        val editor = preferences.edit()
        editor.putString(key + IterableConstants.SHARED_PREFS_OBJECT_SUFFIX, value)
        editor.putLong(key + IterableConstants.SHARED_PREFS_EXPIRATION_SUFFIX, currentTimeMillis() + expirationInterval)
        editor.apply()
    }

    internal fun retrieveExpirableValue(preferences: SharedPreferences, key: String): String? {
        val value = preferences.getString(key + IterableConstants.SHARED_PREFS_OBJECT_SUFFIX, null)
        val expirationTime = preferences.getLong(key + IterableConstants.SHARED_PREFS_EXPIRATION_SUFFIX, 0)

        return if (value == null || expirationTime < currentTimeMillis()) {
            null
        } else {
            value
        }
    }

    internal fun retrieveExpirableJsonObject(preferences: SharedPreferences, key: String): JSONObject? {
        try {
            val encodedObject = retrieveExpirableValue(preferences, key)
            if (encodedObject != null) {
                return JSONObject(encodedObject)
            }
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error while parsing an expirable object for key: $key", e)
        }
        return null
    }

    @Nullable
    internal fun retrieveValidCampaignIdOrNull(json: JSONObject, key: String): Long? {
        return try {
            val id = json.getLong(key)
            if (isValidCampaignId(id)) {
                id
            } else {
                null
            }
        } catch (ex: JSONException) {
            null
        }
    }

    internal fun isValidCampaignId(campaignId: Long): Boolean {
        return campaignId >= 0
    }

    internal fun getSdkCacheDir(context: Context): File {
        val sdkCacheDir = File(context.cacheDir, "com.iterable.sdk")
        if (!sdkCacheDir.exists()) {
            sdkCacheDir.mkdirs()
        }
        return sdkCacheDir
    }

    internal fun getSDKFilesDirectory(context: Context): File {
        val iterableSDKRootDirectory = File(context.filesDir, "com.iterable.sdk")
        if (!iterableSDKRootDirectory.exists()) {
            iterableSDKRootDirectory.mkdirs()
        }
        return iterableSDKRootDirectory
    }

    internal fun getDirectory(folder: File, subFolder: String): File {
        val applicationRootDirectory = File(folder, subFolder)
        if (!applicationRootDirectory.exists()) {
            applicationRootDirectory.mkdirs()
        }
        return applicationRootDirectory
    }

    @Nullable
    internal fun readFile(file: File): String? {
        var inputStream: FileInputStream? = null
        var streamReader: InputStreamReader? = null
        var bufferedReader: BufferedReader? = null
        try {
            inputStream = FileInputStream(file)
            streamReader = InputStreamReader(inputStream)
            bufferedReader = BufferedReader(streamReader)
            val stringBuilder = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            return stringBuilder.toString()
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error while reading file: $file", e)
        } finally {
            IOUtils.closeQuietly(inputStream)
            IOUtils.closeQuietly(streamReader)
            IOUtils.closeQuietly(bufferedReader)
        }
        return null
    }

    internal fun writeFile(file: File, content: String): Boolean {
        try {
            val outputStream = FileOutputStream(file)
            val outputStreamWriter = OutputStreamWriter(outputStream)
            outputStreamWriter.write(content)
            outputStreamWriter.close()
            return true
        } catch (e: Exception) {
            IterableLogger.e(TAG, "Error while writing to file: $file", e)
        }
        return false
    }
}