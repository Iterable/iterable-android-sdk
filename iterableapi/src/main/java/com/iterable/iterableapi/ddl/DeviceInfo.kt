package com.iterable.iterableapi.ddl

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager

import androidx.annotation.RestrictTo

import org.json.JSONException
import org.json.JSONObject

import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.round

@RestrictTo(RestrictTo.Scope.LIBRARY)
class DeviceInfo private constructor(private val deviceFp: DeviceFp) {

    companion object {
        private const val MOBILE_DEVICE_TYPE = "Android"

        fun createDeviceInfo(context: Context): DeviceInfo {
            return DeviceInfo(createDeviceFp(context))
        }

        private fun createDeviceFp(context: Context): DeviceFp {
            val fp = DeviceFp()

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            val displayMetrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= 17) {
                display.getRealMetrics(displayMetrics)
            } else {
                display.getMetrics(displayMetrics)
            }
            fp.screenWidth = round(ceil(displayMetrics.widthPixels / displayMetrics.density)).toLong().toString()
            fp.screenHeight = round(ceil(displayMetrics.heightPixels / displayMetrics.density)).toLong().toString()
            fp.screenScale = displayMetrics.density.toString()

            fp.version = Build.VERSION.RELEASE

            // We're comparing with Javascript timezone offset, which is the difference between the
            // local time and UTC, so we need to flip the sign
            val timezone = TimeZone.getDefault()
            val seconds = -1 * timezone.getOffset(Date().time) / 1000
            val offsetMinutes = seconds / 60
            fp.timezoneOffsetMinutes = offsetMinutes.toString()

            val countryCode = Locale.getDefault().country
            val languageCode = Locale.getDefault().language
            fp.language = "${languageCode}_$countryCode"

            return fp
        }
    }

    internal class DeviceFp {
        var screenWidth: String? = null
        var screenHeight: String? = null
        var screenScale: String? = null
        var version: String? = null
        var timezoneOffsetMinutes: String? = null
        var language: String? = null

        @Throws(JSONException::class)
        fun toJSONObject(): JSONObject {
            val json = JSONObject()
            json.putOpt("screenWidth", screenWidth)
            json.putOpt("screenHeight", screenHeight)
            json.putOpt("screenScale", screenScale)
            json.putOpt("version", version)
            json.putOpt("timezoneOffsetMinutes", timezoneOffsetMinutes)
            json.putOpt("language", language)
            return json
        }
    }

    @Throws(JSONException::class)
    fun toJSONObject(): JSONObject {
        val json = JSONObject()
        json.put("mobileDeviceType", MOBILE_DEVICE_TYPE)
        json.put("deviceFp", deviceFp.toJSONObject())
        return json
    }

}
