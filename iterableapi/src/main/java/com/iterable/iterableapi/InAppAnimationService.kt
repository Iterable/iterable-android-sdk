package com.iterable.iterableapi

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.view.Window
import androidx.core.graphics.ColorUtils

internal class InAppAnimationService {

    fun createInAppBackgroundDrawable(hexColor: String?, alpha: Double): ColorDrawable? {
        val backgroundColor = try {
            if (!hexColor.isNullOrEmpty()) {
                Color.parseColor(hexColor)
            } else {
                Color.BLACK
            }
        } catch (e: IllegalArgumentException) {
            IterableLogger.w(TAG, "Invalid background color: $hexColor. Using BLACK.", e)
            Color.BLACK
        }

        val backgroundWithAlpha = ColorUtils.setAlphaComponent(
            backgroundColor,
            (alpha * 255).toInt()
        )

        return ColorDrawable(backgroundWithAlpha)
    }

    fun animateWindowBackground(window: Window, from: Drawable, to: Drawable, shouldAnimate: Boolean) {
        if (shouldAnimate) {
            val layers = arrayOf(from, to)
            val transition = TransitionDrawable(layers)
            window.setBackgroundDrawable(transition)
            transition.startTransition(ANIMATION_DURATION_MS)
        } else {
            window.setBackgroundDrawable(to)
        }
    }

    fun showInAppBackground(window: Window, hexColor: String?, alpha: Double, shouldAnimate: Boolean) {
        val backgroundDrawable = createInAppBackgroundDrawable(hexColor, alpha)

        if (backgroundDrawable == null) {
            IterableLogger.w(TAG, "Failed to create background drawable")
            return
        }

        if (shouldAnimate) {
            val transparentDrawable = ColorDrawable(Color.TRANSPARENT)
            animateWindowBackground(window, transparentDrawable, backgroundDrawable, true)
        } else {
            window.setBackgroundDrawable(backgroundDrawable)
        }
    }

    fun showAndAnimateWebView(webView: View, shouldAnimate: Boolean, context: Context?) {
        if (shouldAnimate && context != null) {
            webView.alpha = 0f
            webView.visibility = View.VISIBLE
            webView.animate()
                .alpha(1.0f)
                .setDuration(ANIMATION_DURATION_MS.toLong())
                .start()
        } else {
            webView.alpha = 1.0f
            webView.visibility = View.VISIBLE
        }
    }

    fun hideInAppBackground(window: Window, hexColor: String?, alpha: Double, shouldAnimate: Boolean) {
        if (shouldAnimate) {
            val backgroundDrawable = createInAppBackgroundDrawable(hexColor, alpha)
            val transparentDrawable = ColorDrawable(Color.TRANSPARENT)

            if (backgroundDrawable != null) {
                animateWindowBackground(window, backgroundDrawable, transparentDrawable, true)
            }
        } else {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    fun prepareViewForDisplay(view: View) {
        view.alpha = 0f
        view.visibility = View.INVISIBLE
    }

    companion object {
        private const val ANIMATION_DURATION_MS = 300
        private const val TAG = "InAppAnimService"
    }
}

