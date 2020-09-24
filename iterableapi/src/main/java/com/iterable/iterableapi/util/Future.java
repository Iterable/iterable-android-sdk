package com.iterable.iterableapi.util;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Future<T> {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static <T> Future<T> runAsync(Callable<T> callable) {
        return new Future<>(callable);
    }

    private Handler callbackHandler;
    private final List<SuccessCallback<T>> successCallbacks = new ArrayList<>();
    private final List<FailureCallback> failureCallbacks = new ArrayList<>();

    private Future(final Callable<T> callable) {
        // Set up a Handler for the callback based on the current thread
        Looper looper = Looper.myLooper();
        if (looper == null) {
            looper = Looper.getMainLooper();
        }
        callbackHandler = new Handler(looper);

        EXECUTOR.submit(() -> {
            try {
                T result = callable.call();
                handleSuccess(result);
            } catch (Exception e) {
                handleFailure(e);
            }
        });
    }

    private void handleSuccess(final T result) {
        callbackHandler.post(() -> {
            List<SuccessCallback<T>> callbacks;
            synchronized (successCallbacks) {
                callbacks = new ArrayList<>(successCallbacks);
            }
            for (SuccessCallback<T> callback : callbacks) {
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }
        });
    }

    private void handleFailure(final Throwable t) {
        callbackHandler.post(() -> {
            List<FailureCallback> callbacks;
            synchronized (failureCallbacks) {
                callbacks = new ArrayList<>(failureCallbacks);
            }
            for (FailureCallback callback : callbacks) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    public Future<T> onSuccess(SuccessCallback<T> successCallback) {
        synchronized (successCallbacks) {
            successCallbacks.add(successCallback);
        }
        return this;
    }

    public Future<T> onFailure(FailureCallback failureCallback) {
        synchronized (failureCallbacks) {
            failureCallbacks.add(failureCallback);
        }
        return this;
    }

    public interface SuccessCallback<T> {
        void onSuccess(T result);
    }

    public interface FailureCallback {
        void onFailure(Throwable throwable);
    }
}
