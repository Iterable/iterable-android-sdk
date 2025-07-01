package com.iterable.iterableapi

import android.util.Log

/**
 * Created by David Truong dt@iterable.com.
 */
object IterableLogger {
    private const val TAG = "IterableApi"

    fun v(tag: String, msg: String) {
        if (IterableApi.getInstance().getDebugMode()) {
            Log.v(tag, msg)
        }
    }

    fun d(tag: String, msg: String) {
        if (IterableApi.getInstance().getDebugMode()) {
            Log.d(tag, msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (IterableApi.getInstance().getDebugMode()) {
            Log.i(tag, msg)
        }
    }

    fun w(tag: String, msg: String) {
        if (IterableApi.getInstance().getDebugMode()) {
            Log.w(tag, msg)
        }
    }

    fun w(tag: String, msg: String, tr: Throwable) {
        if (IterableApi.getInstance().getDebugMode()) {
            Log.w(tag, msg, tr)
        }
    }

    fun e(tag: String, msg: String) {
        if (IterableApi.getInstance().getDebugMode()) {
            Log.e(tag, msg)
        }
    }

    fun e(tag: String, msg: String, tr: Throwable) {
        if (IterableApi.getInstance().getDebugMode()) {
            Log.e(tag, msg, tr)
        }
    }

    fun printInfo() {
        if (IterableApi.getInstance().getDebugMode()) {
            val stackTrace = Thread.currentThread().stackTrace
            if (stackTrace.size >= 4) {
                val callingClass = stackTrace[3].className.split(".").last()
                val callingMethod = stackTrace[3].methodName
                d(callingClass, callingMethod)
            }
        }
    }
}
