package com.iterable.iterableapi;

import android.content.Context;

import androidx.annotation.NonNull;

class ExternalContext {
    private Context _applicationContext;
    static volatile ExternalContext sharedInstance = new ExternalContext();

    public static void initialize(@NonNull Context context) {
        sharedInstance._applicationContext = context;
    }

    Context getExternalContext() {
        return _applicationContext;
    }
}
