package com.iterable.iterableapi;

import androidx.annotation.NonNull;

/**
 * Supplies the color scheme to use when an HTML in-app message is created.
 */
@FunctionalInterface
public interface IterableInAppColorSchemeProvider {
    /**
     * Returns the current color scheme for HTML in-app messages.
     *
     * @return the color scheme to use
     */
    @NonNull
    IterableInAppColorScheme getInAppColorScheme();
}
