package com.iterable.iterableapi

import android.util.Log

/**
 * Created by David Truong dt@iterable.com.
 */
object IterableLogger {

    fun d(tag: String, msg: String) {
        if (isLoggableLevel(Log.DEBUG)) {
            Log.d(tag, " 💚 $msg")
        }
    }

    fun d(tag: String, msg: String, tr: Throwable) {
        if (isLoggableLevel(Log.DEBUG)) {
            Log.d(tag, " 💚 $msg", tr)
        }
    }

    fun v(tag: String, msg: String) {
        if (isLoggableLevel(Log.VERBOSE)) {
            Log.v(tag, " 💛 $msg")
        }
    }

    fun w(tag: String, msg: String) {
        if (isLoggableLevel(Log.WARN)) {
            Log.w(tag, " 🧡️ $msg")
        }
    }

    fun w(tag: String, msg: String, tr: Throwable) {
        if (isLoggableLevel(Log.WARN)) {
            Log.w(tag, " 🧡 $msg", tr)
        }
    }

    fun e(tag: String, msg: String) {
        if (isLoggableLevel(Log.ERROR)) {
            Log.e(tag, " ❤️ $msg")
        }
    }

    fun e(tag: String, msg: String, tr: Throwable) {
        if (isLoggableLevel(Log.ERROR)) {
            Log.e(tag, " ❤️ $msg", tr)
        }
    }

    fun printInfo() {
        try {
            val stackTrace = Thread.currentThread().stackTrace[3]
            v("Iterable Call", "${stackTrace.fileName} => ${stackTrace.className} => ${stackTrace.methodName} => Line #${stackTrace.lineNumber}")
        } catch (e: Exception) {
            e("Iterable Call", "Couldn't print info")
        }
    }

    private fun isLoggableLevel(messageLevel: Int): Boolean {
        return messageLevel >= getLogLevel()
    }

    private fun getLogLevel(): Int {
        return if (IterableApi.sharedInstance != null) {
            if (IterableApi.sharedInstance.getDebugMode()) {
                Log.VERBOSE
            } else {
                IterableApi.sharedInstance.config.logLevel
            }
        } else {
            Log.ERROR
        }
    }
}
