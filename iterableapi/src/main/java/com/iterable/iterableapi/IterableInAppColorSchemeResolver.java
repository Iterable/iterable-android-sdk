package com.iterable.iterableapi;

import androidx.annotation.NonNull;

final class IterableInAppColorSchemeResolver {
    private static final String TAG = "IterableInAppColorScheme";

    private IterableInAppColorSchemeResolver() {
    }

    @NonNull
    static IterableInAppColorScheme resolve() {
        IterableConfig config = IterableApi.sharedInstance == null
                ? null
                : IterableApi.sharedInstance.config;
        if (config == null) {
            return IterableInAppColorScheme.AUTOMATIC;
        }

        IterableInAppColorSchemeProvider provider = config.inAppColorSchemeProvider;
        if (provider == null) {
            return config.inAppColorScheme;
        }

        try {
            IterableInAppColorScheme colorScheme = provider.getInAppColorScheme();
            if (colorScheme != null) {
                return colorScheme;
            }
            IterableLogger.e(TAG, "Color scheme provider returned null; using AUTOMATIC");
        } catch (Exception e) {
            IterableLogger.e(TAG, "Color scheme provider failed; using AUTOMATIC", e);
        }
        return IterableInAppColorScheme.AUTOMATIC;
    }

    static int resolveDialogTheme() {
        switch (resolve()) {
            case LIGHT:
                return R.style.Iterable_InAppDialog_Light;
            case DARK:
                return R.style.Iterable_InAppDialog_Dark;
            case AUTOMATIC:
            default:
                return R.style.Iterable_InAppDialog;
        }
    }

    static int resolveFragmentTheme() {
        switch (resolve()) {
            case LIGHT:
                return R.style.Iterable_InAppFragment_Light;
            case DARK:
                return R.style.Iterable_InAppFragment_Dark;
            case AUTOMATIC:
            default:
                return R.style.Iterable_InAppFragment;
        }
    }
}
