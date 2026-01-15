package com.iterable.iterableapi;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

/**
 * Service class for in-app message animations.
 * Centralizes animation logic shared between Fragment and Dialog implementations.
 */
class InAppAnimationService {

    private static final int ANIMATION_DURATION_MS = 300;
    private static final String TAG = "InAppAnimService";

    /**
     * Creates a background drawable with the specified color and alpha
     * @param hexColor The background color in hex format (e.g., "#000000")
     * @param alpha The alpha value (0.0 to 1.0)
     * @return ColorDrawable with the specified color and alpha, or null if parsing fails
     */
    @Nullable
    public ColorDrawable createInAppBackgroundDrawable(@Nullable String hexColor, double alpha) {
        int backgroundColor;
        
        try {
            if (hexColor != null && !hexColor.isEmpty()) {
                backgroundColor = Color.parseColor(hexColor);
            } else {
                backgroundColor = Color.BLACK;
            }
        } catch (IllegalArgumentException e) {
            IterableLogger.w(TAG, "Invalid background color: " + hexColor + ". Using BLACK.", e);
            backgroundColor = Color.BLACK;
        }

        int backgroundWithAlpha = ColorUtils.setAlphaComponent(
            backgroundColor,
            (int) (alpha * 255)
        );

        return new ColorDrawable(backgroundWithAlpha);
    }

    /**
     * Animates the window background from one drawable to another
     * @param window The window to animate
     * @param from The starting drawable
     * @param to The ending drawable
     * @param shouldAnimate If false, sets the background immediately without animation
     */
    public void animateWindowBackground(@NonNull Window window, @NonNull Drawable from, @NonNull Drawable to, boolean shouldAnimate) {
        if (shouldAnimate) {
            Drawable[] layers = new Drawable[]{from, to};
            TransitionDrawable transition = new TransitionDrawable(layers);
            window.setBackgroundDrawable(transition);
            transition.startTransition(ANIMATION_DURATION_MS);
        } else {
            window.setBackgroundDrawable(to);
        }
    }

    /**
     * Shows the in-app background with optional fade-in animation
     * @param window The window to set the background on
     * @param hexColor The background color in hex format
     * @param alpha The background alpha (0.0 to 1.0)
     * @param shouldAnimate Whether to animate the background fade-in
     */
    public void showInAppBackground(@NonNull Window window, @Nullable String hexColor, double alpha, boolean shouldAnimate) {
        ColorDrawable backgroundDrawable = createInAppBackgroundDrawable(hexColor, alpha);
        
        if (backgroundDrawable == null) {
            IterableLogger.w(TAG, "Failed to create background drawable");
            return;
        }

        if (shouldAnimate) {
            // Animate from transparent to the target background
            ColorDrawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);
            animateWindowBackground(window, transparentDrawable, backgroundDrawable, true);
        } else {
            window.setBackgroundDrawable(backgroundDrawable);
        }
    }

    /**
     * Shows and optionally animates a WebView
     * @param webView The WebView to show
     * @param shouldAnimate Whether to animate the appearance
     * @param context Context for loading animation resources (only needed if shouldAnimate is true)
     */
    public void showAndAnimateWebView(@NonNull View webView, boolean shouldAnimate, @Nullable Context context) {
        if (shouldAnimate && context != null) {
            // Animate with alpha fade-in
            webView.setAlpha(0f);
            webView.setVisibility(View.VISIBLE);
            webView.animate()
                .alpha(1.0f)
                .setDuration(ANIMATION_DURATION_MS)
                .start();
        } else {
            // Show immediately
            webView.setAlpha(1.0f);
            webView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides the in-app background with optional fade-out animation
     * @param window The window to modify
     * @param hexColor The current background color
     * @param alpha The current background alpha
     * @param shouldAnimate Whether to animate the background fade-out
     */
    public void hideInAppBackground(@NonNull Window window, @Nullable String hexColor, double alpha, boolean shouldAnimate) {
        if (shouldAnimate) {
            ColorDrawable backgroundDrawable = createInAppBackgroundDrawable(hexColor, alpha);
            ColorDrawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);
            
            if (backgroundDrawable != null) {
                animateWindowBackground(window, backgroundDrawable, transparentDrawable, true);
            }
        } else {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    /**
     * Prepares a view to be shown by hiding it initially
     * This is typically called before the resize operation
     * @param view The view to hide
     */
    public void prepareViewForDisplay(@NonNull View view) {
        view.setAlpha(0f);
        view.setVisibility(View.INVISIBLE);
    }
}

