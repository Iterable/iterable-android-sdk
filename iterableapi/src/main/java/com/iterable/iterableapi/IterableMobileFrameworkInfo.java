package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IterableMobileFrameworkInfo {
    @NonNull private final IterableMobileFrameworkType frameworkType;
    @Nullable private final String iterableSdkVersion;

    public IterableMobileFrameworkInfo(@NonNull IterableMobileFrameworkType frameworkType, @Nullable String iterableSdkVersion) {
        this.frameworkType = frameworkType;
        this.iterableSdkVersion = iterableSdkVersion;
    }

    @NonNull
    public IterableMobileFrameworkType getFrameworkType() {
        return frameworkType;
    }

    @Nullable
    public String getIterableSdkVersion() {
        return iterableSdkVersion;
    }
} 