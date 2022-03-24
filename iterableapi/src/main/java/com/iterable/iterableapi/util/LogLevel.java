package com.iterable.iterableapi.util;

import android.util.Log;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class LogLevel {

    /**
     * Priority constant to use instead of Log.v.
     */
    public static final int VERBOSE = Log.VERBOSE;

    /**
     * Priority constant to use instead of Log.d.
     */
    public static final int DEBUG = Log.DEBUG;

    /**
     * Priority constant to use instead of Log.i.
     */
    public static final int INFO = Log.INFO;

    /**
     * Priority constant to use instead of Log.w.
     */
    public static final int WARN = Log.WARN;

    /**
     * Priority constant to use instead of Log.e.
     */
    public static final int ERROR = Log.ERROR;

    /**
     * Priority constant to use to disable logging.
     */
    public static final int NONE = Integer.MAX_VALUE;

    @IntDef({VERBOSE, DEBUG, INFO, WARN, ERROR, NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Level {}

    private LogLevel() { }
}
