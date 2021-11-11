package com.iterable.iterableapi;

import android.util.Log;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableLogger {

    public static void d(String tag, String msg) {
        if (isLoggableLevel(Log.DEBUG)) {
            Log.d(tag, " 💚 " + msg);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (isLoggable(Log.DEBUG)) {
            Log.d(tag, " 💚 " + msg, tr);
        }
    }

    public static void v(String tag, String msg) {
        if (isLoggable(Log.VERBOSE)) {
            Log.v(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (isLoggable(Log.WARN)) {
            Log.w(tag, " 🧡️ " + msg);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (isLoggable(Log.WARN)) {
            Log.w(tag, " 🧡 " + msg, tr);
        }
    }

    public static void e(String tag, String msg) {
        if (isLoggable(Log.ERROR)) {
            Log.e(tag, " ❤️ " + msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (isLoggable(Log.ERROR)) {
            Log.e(tag, " ❤️ " + msg, tr);
        }
    }

    public static void printInfo() {
        try {
            IterableLogger.v("Iterable Call", " 💛 " + Thread.currentThread().getStackTrace()[3].getFileName() + " => " + Thread.currentThread().getStackTrace()[3].getClassName() + " => " + Thread.currentThread().getStackTrace()[3].getMethodName() + " => Line #" + Thread.currentThread().getStackTrace()[3].getLineNumber());
        } catch (Exception e) {
            IterableLogger.e("Iterable Call", "Couldn't print info");
        }
    }

    private static boolean isLoggable(int messageLevel) {
        boolean isDebug = ((IterableApi.getInstance().getMainActivityContext().getApplicationInfo().flags & IterableApi.getInstance().getMainActivityContext().getApplicationInfo().FLAG_DEBUGGABLE) != 0);
        if(isDebug){
            return isLoggableLevel(messageLevel);
        }
        // Log level will be set to WARNING and above if in release mode.
        return messageLevel >= Log.WARN;
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
