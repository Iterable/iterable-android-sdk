package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class IterableActivityMonitor {

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> currentActivity;
    private static int numStartedActivities = 0;
    private static boolean inForeground = false;
    private static List<AppStateCallback> callbacks = new ArrayList<>();
    private static Runnable backgroundTransitionRunnable = new Runnable() {
        @Override
        public void run() {
            inForeground = false;
            for (AppStateCallback callback : callbacks) {
                callback.onSwitchToBackground();
            }
        }
    };
    private static final int BACKGROUND_DELAY_MS = 1000;

    public static Activity getCurrentActivity() {
        return currentActivity != null ? currentActivity.get() : null;
    }

    public static boolean isInForeground() {
        return getCurrentActivity() != null;
    }

    public static void init(Context context) {
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                handler.removeCallbacks(backgroundTransitionRunnable);
                numStartedActivities++;
                if (!inForeground) {
                    inForeground = true;
                    for (AppStateCallback callback : callbacks) {
                        callback.onSwitchToForeground();
                    }
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                currentActivity = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                currentActivity = null;
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
        });
    }

    public static void addCallback(AppStateCallback callback) {
        callbacks.add(callback);
    }

    public static void removeCallback(AppStateCallback callback) {
        callbacks.remove(callback);
    }

    public interface AppStateCallback {
        void onSwitchToForeground();
        void onSwitchToBackground();
    }

}
