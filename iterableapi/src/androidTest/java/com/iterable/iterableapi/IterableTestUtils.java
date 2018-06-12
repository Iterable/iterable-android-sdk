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
}
