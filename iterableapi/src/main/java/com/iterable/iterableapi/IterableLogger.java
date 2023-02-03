package com.iterable.iterableapi;

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.iterable.iterableapi.util.LogLevel;

/**
 * Created by David Truong dt@iterable.com.
 */
public final class IterableLogger {

    private IterableLogger() { }

    public static void d(String tag, String msg) {
        if (isLoggableLevel(LogLevel.DEBUG)) {
            Log.d(tag, " 💚 " + msg);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (isLoggableLevel(LogLevel.DEBUG)) {
            Log.d(tag, " 💚 " + msg, tr);
        }
    }

    public static void v(String tag, String msg) {
        if (isLoggableLevel(LogLevel.VERBOSE)) {
            Log.v(tag, " 💛 " + msg);
        }
    }

    public static void w(String tag, String msg) {
        if (isLoggableLevel(LogLevel.WARN)) {
            Log.w(tag, " 🧡️ " + msg);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (isLoggableLevel(LogLevel.WARN)) {
            Log.w(tag, " 🧡 " + msg, tr);
        }
    }

    public static void e(String tag, String msg) {
        if (isLoggableLevel(LogLevel.ERROR)) {
            Log.e(tag, " ❤️ " + msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (isLoggableLevel(LogLevel.ERROR)) {
            Log.e(tag, " ❤️ " + msg, tr);
        }
    }

    public static void printInfo() {
        try {
            IterableLogger.v("Iterable Call", Thread.currentThread().getStackTrace()[3].getFileName() + " => " + Thread.currentThread().getStackTrace()[3].getClassName() + " => " + Thread.currentThread().getStackTrace()[3].getMethodName() + " => Line #" + Thread.currentThread().getStackTrace()[3].getLineNumber());
        } catch (Exception e) {
            IterableLogger.e("Iterable Call", "Couldn't print info");
        }
    }

    @VisibleForTesting
    protected static boolean isLoggableLevel(@LogLevel.Level int messageLevel) {
        return messageLevel >= getLogLevel();
    }

    @LogLevel.Level
    private static int getLogLevel() {
        if (IterableApi.sharedInstance != null) {
            if (IterableApi.sharedInstance.getDebugMode()) {
                return LogLevel.VERBOSE;
            } else {
                return IterableApi.sharedInstance.config.logLevel;
            }
        }
        return LogLevel.NONE;
    }
}
