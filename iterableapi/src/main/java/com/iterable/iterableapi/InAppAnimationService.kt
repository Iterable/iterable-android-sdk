package com.iterable.iterableapi

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.annotation.RestrictTo
import androidx.core.graphics.ColorUtils

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            transition.startTransition(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ANIMATION_DURATION)
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

    /**
     * Returns the enter animation resource for the given in-app layout, mirroring the
     * behavior of [IterableInAppFragmentHTMLNotification] so Compose/Dialog hosts get the
     * same slide/fade animations as Fragment hosts.
     */
    @AnimRes
    fun getEnterAnimationResource(layout: InAppLayoutService.InAppLayout): Int {
        return when (layout) {
            InAppLayoutService.InAppLayout.TOP -> R.anim.slide_down_custom
            InAppLayoutService.InAppLayout.BOTTOM -> R.anim.slide_up_custom
            InAppLayoutService.InAppLayout.CENTER,
            InAppLayoutService.InAppLayout.FULLSCREEN -> R.anim.fade_in_custom
        }
    }

    /**
     * Returns the exit animation resource for the given in-app layout, mirroring the
     * behavior of [IterableInAppFragmentHTMLNotification].
     */
    @AnimRes
    fun getExitAnimationResource(layout: InAppLayoutService.InAppLayout): Int {
        return when (layout) {
            InAppLayoutService.InAppLayout.TOP -> R.anim.top_exit
            InAppLayoutService.InAppLayout.BOTTOM -> R.anim.bottom_exit
            InAppLayoutService.InAppLayout.CENTER,
            InAppLayoutService.InAppLayout.FULLSCREEN -> R.anim.fade_out_custom
        }
    }

    fun showAndAnimateWebView(
        webView: View,
        shouldAnimate: Boolean,
        context: Context?,
        layout: InAppLayoutService.InAppLayout
    ) {
        webView.alpha = 1.0f
        webView.visibility = View.VISIBLE

        if (shouldAnimate && context != null) {
            try {
                val anim = AnimationUtils.loadAnimation(context, getEnterAnimationResource(layout))
                anim.duration = IterableConstants.ITERABLE_IN_APP_ANIMATION_DURATION.toLong()
                webView.startAnimation(anim)
            } catch (e: Exception) {
                IterableLogger.w(TAG, "Failed to start enter animation", e)
            }
        }
    }

    /**
     * Plays the layout-appropriate exit animation on the given view. Returns `true` when
     * an animation was started, `false` otherwise (either because [shouldAnimate] was
     * false or loading the animation failed). Callers should schedule dismissal
     * accordingly.
     */
    fun hideAndAnimateWebView(
        webView: View,
        shouldAnimate: Boolean,
        context: Context?,
        layout: InAppLayoutService.InAppLayout
    ): Boolean {
        if (!shouldAnimate || context == null) {
            return false
        }
        return try {
            val anim = AnimationUtils.loadAnimation(context, getExitAnimationResource(layout))
            anim.duration = IterableConstants.ITERABLE_IN_APP_ANIMATION_DURATION.toLong()
            webView.startAnimation(anim)
            true
        } catch (e: Exception) {
            IterableLogger.w(TAG, "Failed to start exit animation", e)
            false
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
        private const val TAG = "InAppAnimService"
    }
}

