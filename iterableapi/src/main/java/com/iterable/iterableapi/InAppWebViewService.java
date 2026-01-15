package com.iterable.iterableapi;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Service class for in-app message WebView management.
 * Centralizes WebView creation and configuration logic shared between 
 * Fragment and Dialog implementations.
 */
class InAppWebViewService {

    private static final String TAG = "InAppWebViewService";

    /**
     * Creates and configures a WebView for in-app message display
     * @param context The context for WebView creation
     * @param callbacks The callback interface for WebView events
     * @param htmlContent The HTML content to load
     * @return Configured IterableWebView
     */
    @NonNull
    public IterableWebView createConfiguredWebView(
            @NonNull Context context,
            @NonNull IterableWebView.HTMLNotificationCallbacks callbacks,
            @NonNull String htmlContent) {
        
        IterableWebView webView = new IterableWebView(context);
        webView.setId(R.id.webView);
        webView.createWithHtml(callbacks, htmlContent);
        
        IterableLogger.d(TAG, "Created and configured WebView with HTML content");
        return webView;
    }

    /**
     * Creates layout parameters for WebView based on layout type
     * @param isFullScreen Whether this is a fullscreen in-app message
     * @return Appropriate LayoutParams for the WebView
     */
    @NonNull
    public FrameLayout.LayoutParams createWebViewLayoutParams(boolean isFullScreen) {
        if (isFullScreen) {
            // Fullscreen: WebView fills entire container
            return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            );
        } else {
            // Non-fullscreen: WebView wraps content for proper sizing
            return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /**
     * Creates layout parameters for WebView container (RelativeLayout) in positioned layouts
     * @return RelativeLayout.LayoutParams for WebView centering
     */
    @NonNull
    public RelativeLayout.LayoutParams createCenteredWebViewParams() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        return params;
    }

    /**
     * Creates layout parameters for the WebView container based on layout type
     * @param layout The layout type (TOP, BOTTOM, CENTER, FULLSCREEN)
     * @return FrameLayout.LayoutParams with appropriate gravity
     */
    @NonNull
    public FrameLayout.LayoutParams createContainerLayoutParams(@NonNull InAppLayoutService.InAppLayout layout) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );

        switch (layout) {
            case TOP:
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
                break;
            case BOTTOM:
                params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
                break;
            case CENTER:
                params.gravity = android.view.Gravity.CENTER;
                break;
            case FULLSCREEN:
                // Fullscreen doesn't use container positioning
                params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                );
                break;
        }

        return params;
    }

    /**
     * Properly cleans up and destroys a WebView
     * @param webView The WebView to clean up (nullable)
     */
    public void cleanupWebView(@Nullable IterableWebView webView) {
        if (webView != null) {
            try {
                webView.destroy();
                IterableLogger.d(TAG, "WebView cleaned up and destroyed");
            } catch (Exception e) {
                IterableLogger.w(TAG, "Error cleaning up WebView", e);
            }
        }
    }

    /**
     * Triggers the resize script on the WebView
     * This is typically called after orientation changes or content updates
     * @param webView The WebView to resize
     */
    public void runResizeScript(@Nullable IterableWebView webView) {
        if (webView != null) {
            try {
                webView.evaluateJavascript("window.resize()", null);
                IterableLogger.d(TAG, "Triggered WebView resize script");
            } catch (Exception e) {
                IterableLogger.w(TAG, "Error running resize script", e);
            }
        }
    }
}

