package com.iterable.iterableapi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.List;

public class IterableTestUtils {
    public static void createIterableApi() {
        IterableApi.sharedInstanceWithApiKey(InstrumentationRegistry.getTargetContext(), "fake_key", "test_email");
    }

    interface BroadcastReceiverCallback {
        void onReceive(Context context, Intent intent);
    }

    private static List<BroadcastReceiver> installedBroadcastReceivers = new ArrayList<BroadcastReceiver>();

    public static void installBroadcastReceiver(String action, final BroadcastReceiverCallback callback) {
        IntentFilter intentFilter = new IntentFilter(IterableConstants.ACTION_PUSH_ACTION);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (callback != null) {
                    callback.onReceive(context, intent);
                }
                setResult(Activity.RESULT_OK, null, null);
            }
        };
        InstrumentationRegistry.getTargetContext().registerReceiver(receiver, intentFilter);
    }

    public static void uninstallBroadcastReceivers() {
        for (BroadcastReceiver receiver : installedBroadcastReceivers) {
            InstrumentationRegistry.getTargetContext().unregisterReceiver(receiver);
        }
        installedBroadcastReceivers.clear();
    }
}
