package com.iterable.iterableapi;

import android.graphics.Rect;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

/**
 * Service class for in-app message layout calculations and window configuration.
 * Centralizes layout detection logic shared between Fragment and Dialog implementations.
 */
class InAppLayoutService {

    /**
     * Layout types for in-app messages based on padding configuration
     */
    enum InAppLayout {
        TOP,
        BOTTOM,
        CENTER,
        FULLSCREEN
    }

    /**
     * Determines the layout type based on inset padding
     * @param padding The inset padding (top/bottom) that defines the layout
     * @return The corresponding InAppLayout type
     */
    @NonNull
    public InAppLayout getInAppLayout(@NonNull Rect padding) {
        if (padding.top == 0 && padding.bottom == 0) {
            return InAppLayout.FULLSCREEN;
        } else if (padding.top > 0 && padding.bottom <= 0) {
            return InAppLayout.TOP;
        } else if (padding.top <= 0 && padding.bottom > 0) {
            return InAppLayout.BOTTOM;
        } else {
            return InAppLayout.CENTER;
        }
    }

    /**
     * Gets the vertical gravity for positioning based on padding
     * @param padding The inset padding that defines positioning
     * @return Gravity constant (TOP, BOTTOM, or CENTER_VERTICAL)
     */
    public int getVerticalLocation(@NonNull Rect padding) {
        InAppLayout layout = getInAppLayout(padding);
        
        switch (layout) {
            case TOP:
                return Gravity.TOP;
            case BOTTOM:
                return Gravity.BOTTOM;
            case CENTER:
                return Gravity.CENTER_VERTICAL;
            case FULLSCREEN:
            default:
                return Gravity.CENTER_VERTICAL;
        }
    }

    /**
     * Configures window flags based on layout type
     * @param window The window to configure
     * @param layout The layout type
     */
    public void configureWindowFlags(Window window, @NonNull InAppLayout layout) {
        if (window == null) {
            return;
        }

        if (layout == InAppLayout.FULLSCREEN) {
            // Fullscreen: hide status bar
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        } else if (layout != InAppLayout.TOP) {
            // BOTTOM and CENTER: translucent status bar
            // TOP layout keeps status bar opaque (no flags needed)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            );
        }
    }

    /**
     * Sets window size to fill the screen
     * This is necessary for both fullscreen and positioned layouts
     * @param window The window to configure
     */
    public void setWindowToFullScreen(Window window) {
        if (window != null) {
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            );
        }
    }

    /**
     * Applies window gravity for positioned layouts (non-fullscreen)
     * @param window The window to configure
     * @param padding The inset padding
     * @param source Debug string indicating where this is called from
     */
    public void applyWindowGravity(Window window, @NonNull Rect padding, String source) {
        if (window == null) {
            return;
        }

        int verticalGravity = getVerticalLocation(padding);
        WindowManager.LayoutParams params = window.getAttributes();

        switch (verticalGravity) {
            case Gravity.CENTER_VERTICAL:
                params.gravity = Gravity.CENTER;
                break;
            case Gravity.TOP:
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                break;
            case Gravity.BOTTOM:
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                break;
            default:
                params.gravity = Gravity.CENTER;
                break;
        }

        window.setAttributes(params);
        
        if (source != null) {
            IterableLogger.d("InAppLayoutService", "Applied window gravity from " + source + ": " + params.gravity);
        }
    }
}

