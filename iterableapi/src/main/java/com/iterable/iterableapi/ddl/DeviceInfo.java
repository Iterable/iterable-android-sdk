package com.iterable.iterableapi.ddl;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DeviceInfo {
    String mobileDeviceType = "Android";
    DeviceFp deviceFp;

    private DeviceInfo(DeviceFp deviceFp) {
        this.deviceFp = deviceFp;
    }

    static class DeviceFp {
        String screenWidth;
        String screenHeight;
        String screenScale;
        String version;
        String timezoneOffsetMinutes;
        String language;

        JSONObject toJSONObject() throws JSONException {
            JSONObject json = new JSONObject();
            json.putOpt("screenWidth", screenWidth);
            json.putOpt("screenHeight", screenHeight);
            json.putOpt("screenScale", screenScale);
            json.putOpt("version", version);
            json.putOpt("timezoneOffsetMinutes", timezoneOffsetMinutes);
            json.putOpt("language", language);
            return json;
        }
    }

    public static DeviceInfo createDeviceInfo(Context context) {
        return new DeviceInfo(createDeviceFp(context));
    }

    private static DeviceFp createDeviceFp(Context context) {
        DeviceFp fp = new DeviceFp();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealMetrics(displayMetrics);
        } else {
            display.getMetrics(displayMetrics);
        }
        fp.screenWidth = Long.toString(Math.round(Math.ceil(displayMetrics.widthPixels / displayMetrics.density)));
        fp.screenHeight = Long.toString(Math.round(Math.ceil(displayMetrics.heightPixels / displayMetrics.density)));
        fp.screenScale = Float.toString(displayMetrics.density);

        fp.version = Build.VERSION.RELEASE;

        // We're comparing with Javascript timezone offset, which is the difference between the
        // local time and UTC, so we need to flip the sign
        TimeZone timezone = TimeZone.getDefault();
        int seconds = -1 * timezone.getOffset(new Date().getTime()) / 1000;
        int offsetMinutes = seconds / 60;
        fp.timezoneOffsetMinutes = Integer.toString(offsetMinutes);

        String countryCode = Locale.getDefault().getCountry();
        String languageCode = Locale.getDefault().getLanguage();
        fp.language = languageCode + "_" + countryCode;

        return fp;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("mobileDeviceType", mobileDeviceType);
        json.put("deviceFp", deviceFp.toJSONObject());
        return json;
    }

}
