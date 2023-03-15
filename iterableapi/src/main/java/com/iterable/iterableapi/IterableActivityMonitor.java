package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iterable.iterableapi.util.BrandUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IterableActivityMonitor {

    private static boolean initialized = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> currentActivity;
    private int numStartedActivities = 0;
    private boolean inForeground = false;
    private List<WeakReference<AppStateCallback>> callbacks = new ArrayList<>();
    private Runnable backgroundTransitionRunnable = new Runnable() {
        @Override
        public void run() {
            inForeground = false;
            for (WeakReference<AppStateCallback> callback : callbacks) {
                if (callback.get() != null) {
                    callback.get().onSwitchToBackground();
                }
            }
        }
    };
    private static final int BACKGROUND_DELAY_MS = 1000;
    static IterableActivityMonitor instance = new IterableActivityMonitor();

    @NonNull
    public static IterableActivityMonitor getInstance() {
        return instance;
    }

    private Application.ActivityLifecycleCallbacks lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            handler.removeCallbacks(backgroundTransitionRunnable);
            numStartedActivities++;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            currentActivity = new WeakReference<>(activity);

            if (!inForeground || BrandUtils.isFireTV(activity.getPackageManager())) {
                inForeground = true;
                for (WeakReference<AppStateCallback> callback : callbacks) {
                    if (callback.get() != null) {
                        callback.get().onSwitchToForeground();
                    }
                }
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            if (getCurrentActivity() == activity) {
                currentActivity = null;
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (numStartedActivities > 0) {
                numStartedActivities--;
            }

            if (numStartedActivities == 0 && inForeground) {
                handler.postDelayed(backgroundTransitionRunnable, BACKGROUND_DELAY_MS);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };

    public void registerLifecycleCallbacks(@NonNull Context context) {
        if (!initialized) {
            initialized = true;
            ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(lifecycleCallbacks);
        }
    }

    public void unregisterLifecycleCallbacks(@NonNull Context context) {
        if (initialized) {
            initialized = false;
            ((Application) context.getApplicationContext()).unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
        }
    }

    @Nullable
    public Activity getCurrentActivity() {
        return currentActivity != null ? currentActivity.get() : null;
    }

    public boolean isInForeground() {
        return getCurrentActivity() != null;
    }

    public void addCallback(@NonNull AppStateCallback callback) {
        // Don't insert again if the same callback already exists
        for (WeakReference<AppStateCallback> existingCallback : callbacks) {
            if (existingCallback.get() == callback) {
                return;
            }
        }
        callbacks.add(new WeakReference<>(callback));
    }

    public void removeCallback(@NonNull AppStateCallback callback) {
        Iterator<WeakReference<AppStateCallback>> iterator = callbacks.iterator();
        while (iterator.hasNext()) {
            WeakReference<AppStateCallback> callbackRef = iterator.next();
            if (callbackRef.get() == callback) {
                iterator.remove();
            }
        }
    }

    public interface AppStateCallback {
        void onSwitchToForeground();
        void onSwitchToBackground();
    }

}
