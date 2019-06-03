package com.iterable.iterableapi.util;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
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
    private final List<WeakReference<SuccessCallback<T>>> successCallbacks = new ArrayList<>();
    private final List<WeakReference<FailureCallback>> failureCallbacks = new ArrayList<>();

    private Future(final Callable<T> callable) {
        // Set up a Handler for the callback based on the current thread
        Looper looper = Looper.myLooper();
        if (looper == null) {
            looper = Looper.getMainLooper();
        }
        callbackHandler = new Handler(looper);

        EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    T result = callable.call();
                    handleSuccess(result);
                } catch (Exception e) {
                    handleFailure(e);
                }
            }
        });
    }

    private void handleSuccess(final T result) {
        callbackHandler.post(new Runnable() {
            @Override
            public void run() {
                List<WeakReference<SuccessCallback<T>>> callbacks;
                synchronized (successCallbacks) {
                    callbacks = new ArrayList<>(successCallbacks);
                }
                for (WeakReference<SuccessCallback<T>> weakCallback : callbacks) {
                    SuccessCallback<T> callback = weakCallback.get();
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                }
            }
        });
    }

    private void handleFailure(final Throwable t) {
        callbackHandler.post(new Runnable() {
            @Override
            public void run() {
                List<WeakReference<FailureCallback>> callbacks;
                synchronized (failureCallbacks) {
                    callbacks = new ArrayList<>(failureCallbacks);
                }
                for (WeakReference<FailureCallback> weakCallback : callbacks) {
                    FailureCallback callback = weakCallback.get();
                    if (callback != null) {
                        callback.onFailure(t);
                    }
                }
            }
        });
    }

    public Future<T> onSuccess(SuccessCallback<T> successCallback) {
        synchronized (successCallbacks) {
            successCallbacks.add(new WeakReference<>(successCallback));
        }
        return this;
    }

    public Future<T> onFailure(FailureCallback failureCallback) {
        synchronized (failureCallbacks) {
            failureCallbacks.add(new WeakReference<>(failureCallback));
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
