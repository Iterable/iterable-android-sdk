package com.iterable.iterableapi;

/**
 * Controls how in-app messages interact with the system bars (status bar, navigation bar).
 * <p>
 * This setting is configured via {@link IterableConfig.Builder#setInAppDisplayMode(IterableInAppDisplayMode)}
 * and applies globally to all in-app messages displayed by the SDK.
 */
public enum IterableInAppDisplayMode {

    /**
     * Default. The in-app message follows the host app's current layout configuration.
     * If the app is edge-to-edge, the in-app will display edge-to-edge.
     * If the app respects system bar bounds, the in-app will too.
     */
    FOLLOW_APP_LAYOUT,

    /**
     * Forces in-app messages to display edge-to-edge, drawing content behind system bars.
     * The in-app content will extend behind the status bar and navigation bar.
     */
    FORCE_EDGE_TO_EDGE,

    /**
     * Forces in-app messages to display in fullscreen mode, hiding the status bar entirely.
     * Uses legacy FLAG_FULLSCREEN on API &lt; 30 and WindowInsetsController on API 30+.
     */
    FORCE_FULLSCREEN,

    /**
     * Forces in-app messages to respect system bar boundaries.
     * Content will never draw behind the status bar or navigation bar,
     * ensuring UI elements like the close button are always accessible.
     */
    FORCE_RESPECT_BOUNDS
}
