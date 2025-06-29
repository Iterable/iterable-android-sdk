package com.iterable.iterableapi.util

import android.os.Handler
import android.os.Looper

import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Future<T> private constructor(callable: Callable<T>) {
    
    companion object {
        private val EXECUTOR: ExecutorService = Executors.newCachedThreadPool()

        fun <T> runAsync(callable: Callable<T>): Future<T> {
            return Future(callable)
        }
    }

    private val callbackHandler: Handler
    private val successCallbacks = ArrayList<SuccessCallback<T>>()
    private val failureCallbacks = ArrayList<FailureCallback>()

    init {
        // Set up a Handler for the callback based on the current thread
        var looper = Looper.myLooper()
        if (looper == null) {
            looper = Looper.getMainLooper()
        }
        callbackHandler = Handler(looper)

        EXECUTOR.submit {
            try {
                val result = callable.call()
                handleSuccess(result)
            } catch (e: Exception) {
                handleFailure(e)
            }
        }
    }

    private fun handleSuccess(result: T) {
        callbackHandler.post {
            val callbacks: List<SuccessCallback<T>>
            synchronized(successCallbacks) {
                callbacks = ArrayList(successCallbacks)
            }
            for (callback in callbacks) {
                callback?.onSuccess(result)
            }
        }
    }

    private fun handleFailure(t: Throwable) {
        callbackHandler.post {
            val callbacks: List<FailureCallback>
            synchronized(failureCallbacks) {
                callbacks = ArrayList(failureCallbacks)
            }
            for (callback in callbacks) {
                callback?.onFailure(t)
            }
        }
    }

    fun onSuccess(successCallback: SuccessCallback<T>): Future<T> {
        synchronized(successCallbacks) {
            successCallbacks.add(successCallback)
        }
        return this
    }

    fun onFailure(failureCallback: FailureCallback): Future<T> {
        synchronized(failureCallbacks) {
            failureCallbacks.add(failureCallback)
        }
        return this
    }

    interface SuccessCallback<T> {
        fun onSuccess(result: T)
    }

    interface FailureCallback {
        fun onFailure(throwable: Throwable)
    }
}
