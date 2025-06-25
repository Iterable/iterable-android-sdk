package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IterableAPIMobileFrameworkInfo {
    @NonNull private final IterableAPIMobileFrameworkType frameworkType;
    @Nullable private final String iterableSdkVersion;

    public IterableAPIMobileFrameworkInfo(@NonNull IterableAPIMobileFrameworkType frameworkType, @Nullable String iterableSdkVersion) {
        this.frameworkType = frameworkType;
        this.iterableSdkVersion = iterableSdkVersion;
    }

    @NonNull
    public IterableAPIMobileFrameworkType getFrameworkType() {
        return frameworkType;
    }

    @Nullable
    public String getIterableSdkVersion() {
        return iterableSdkVersion;
    }
}