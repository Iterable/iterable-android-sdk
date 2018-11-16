package com.iterable.iterableapi;

import android.util.Log;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableLogger {

    public static void d(String tag, String msg) {
        if (isLoggableLevel(Log.DEBUG)) {
            Log.d(tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (isLoggableLevel(Log.DEBUG)) {
            Log.d(tag, msg, tr);
        }
    }

    public static void w(String tag, String msg) {
        if (isLoggableLevel(Log.WARN)) {
            Log.w(tag, msg);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (isLoggableLevel(Log.WARN)) {
            Log.w(tag, msg, tr);
        }
    }

    public static void e(String tag, String msg) {
        if (isLoggableLevel(Log.ERROR)) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (isLoggableLevel(Log.ERROR)) {
            Log.e(tag, msg, tr);
        }
    }

    private static boolean isLoggableLevel(int messageLevel) {
        return messageLevel >= getLogLevel();
    }

    private static int getLogLevel() {
        if (IterableApi.sharedInstance != null) {
            if (IterableApi.sharedInstance.getDebugMode()) {
                return Log.VERBOSE;
            } else {
                return IterableApi.sharedInstance.config.logLevel;
            }
        }
        return Log.ERROR;
    }
}
