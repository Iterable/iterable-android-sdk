package com.iterable.iterableapi.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

import com.iterable.iterableapi.IterableConstants
import com.iterable.iterableapi.IterableUtil
import com.iterable.iterableapi.IterableAPIMobileFrameworkInfo

import org.json.JSONException
import org.json.JSONObject

object DeviceInfoUtils {

    fun isFireTV(packageManager: PackageManager): Boolean {
        val amazonFireTvHardware = "amazon.hardware.fire_tv"
        val amazonModel = Build.MODEL
        return amazonModel.matches("AFTN".toRegex()) || packageManager.hasSystemFeature(amazonFireTvHardware)
    }

    @Throws(JSONException::class)
    fun populateDeviceDetails(dataFields: JSONObject, context: Context, deviceId: String, frameworkInfo: IterableAPIMobileFrameworkInfo?) {
        dataFields.put(IterableConstants.DEVICE_BRAND, Build.BRAND) //brand: google
        dataFields.put(IterableConstants.DEVICE_MANUFACTURER, Build.MANUFACTURER) //manufacturer: samsung
        dataFields.put(IterableConstants.DEVICE_SYSTEM_NAME, Build.DEVICE) //device name: toro
        dataFields.put(IterableConstants.DEVICE_SYSTEM_VERSION, Build.VERSION.RELEASE) //version: 4.0.4
        dataFields.put(IterableConstants.DEVICE_MODEL, Build.MODEL) //device model: Galaxy Nexus
        dataFields.put(IterableConstants.DEVICE_SDK_VERSION, Build.VERSION.SDK_INT) //sdk version/api level: 15

        dataFields.put(IterableConstants.DEVICE_ID, deviceId) // Random UUID
        dataFields.put(IterableConstants.DEVICE_APP_PACKAGE_NAME, context.packageName)
        dataFields.put(IterableConstants.DEVICE_APP_VERSION, IterableUtil.getAppVersion(context))
        dataFields.put(IterableConstants.DEVICE_APP_BUILD, IterableUtil.getAppVersionCode(context))
        dataFields.put(IterableConstants.DEVICE_ITERABLE_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER)

        if (frameworkInfo?.frameworkType != null) {
            val mobileFrameworkJson = JSONObject()
            mobileFrameworkJson.put(IterableConstants.DEVICE_FRAMEWORK_TYPE, frameworkInfo.frameworkType.value)
            mobileFrameworkJson.put(IterableConstants.DEVICE_ITERABLE_SDK_VERSION,
                frameworkInfo.iterableSdkVersion ?: "unknown")
            dataFields.put(IterableConstants.DEVICE_MOBILE_FRAMEWORK_INFO, mobileFrameworkJson)
        }
    }
}
