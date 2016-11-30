package com.iterable.iterableapi;

import android.util.Log;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableLogger {

    public static void d(String tag, String msg) {
        if (isDebugMode()) {
            Log.d(tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (isDebugMode()) {
            Log.d(tag, msg, tr);
        }
    }

    public static void w(String tag, String msg) {
        if (isDebugMode()) {
            Log.w(tag, msg);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (isDebugMode()) {
            Log.w(tag, msg, tr);
        }
    }

    public static void e(String tag, String msg) {
        if (isDebugMode()) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (isDebugMode()) {
            Log.e(tag, msg, tr);
        }
    }

    private static boolean isDebugMode() {
        return (IterableApi.sharedInstance != null) ? IterableApi.sharedInstance.getDebugMode() : false;
    }
}
