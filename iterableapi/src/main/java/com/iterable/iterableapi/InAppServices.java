package com.iterable.iterableapi;

/**
 * Central access point for all in-app message services.
 * Provides singleton instances of each service for convenient access.
 */
final class InAppServices {

    /**
     * Layout detection and window configuration service
     */
    public static final InAppLayoutService layout = new InAppLayoutService();

    /**
     * Animation and visual effects service
     */
    public static final InAppAnimationService animation = new InAppAnimationService();

    /**
     * Event tracking and analytics service
     */
    public static final InAppTrackingService tracking = new InAppTrackingService();

    /**
     * WebView creation and management service
     */
    public static final InAppWebViewService webView = new InAppWebViewService();

    /**
     * Orientation change detection service
     */
    public static final InAppOrientationService orientation = new InAppOrientationService();

    /**
     * Private constructor to prevent instantiation
     */
    private InAppServices() {
        throw new UnsupportedOperationException("InAppServices is a static utility class and cannot be instantiated");
    }
}

