package com.iterable.iterableapi.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableUtil;

import org.json.JSONException;
import org.json.JSONObject;

public final class DeviceInfoUtils {

    private DeviceInfoUtils() {
    }

    public static boolean isFireTV(PackageManager packageManager) {
        String amazonFireTvHardware = "amazon.hardware.fire_tv";
        String amazonModel = Build.MODEL;
        if (amazonModel.matches("AFTN") || packageManager.hasSystemFeature(amazonFireTvHardware)) {
            return true;
        } else {
            return false;
        }
    }
    public static JSONObject populateDeviceDetails(JSONObject dataFields, Context context, String deviceId) throws JSONException {
        dataFields.put(IterableConstants.DEVICE_BRAND, Build.BRAND); //brand: google
        dataFields.put(IterableConstants.DEVICE_MANUFACTURER, Build.MANUFACTURER); //manufacturer: samsung
        dataFields.put(IterableConstants.DEVICE_SYSTEM_NAME, Build.DEVICE); //device name: toro
        dataFields.put(IterableConstants.DEVICE_SYSTEM_VERSION, Build.VERSION.RELEASE); //version: 4.0.4
        dataFields.put(IterableConstants.DEVICE_MODEL, Build.MODEL); //device model: Galaxy Nexus
        dataFields.put(IterableConstants.DEVICE_SDK_VERSION, Build.VERSION.SDK_INT); //sdk version/api level: 15

        dataFields.put(IterableConstants.DEVICE_ID, deviceId); // Random UUID
        dataFields.put(IterableConstants.DEVICE_APP_PACKAGE_NAME, context.getPackageName());
        dataFields.put(IterableConstants.DEVICE_APP_VERSION, IterableUtil.getAppVersion(context));
        dataFields.put(IterableConstants.DEVICE_APP_BUILD, IterableUtil.getAppVersionCode(context));
        dataFields.put(IterableConstants.DEVICE_ITERABLE_SDK_VERSION, IterableConstants.ITBL_KEY_SDK_VERSION_NUMBER);
        dataFields.put(IterableConstants.DEVICE_NOTIFICATIONS_ENABLED, !isFireTV(context.getPackageManager()) && NotificationManagerCompat.from(context).areNotificationsEnabled());
    }
}
