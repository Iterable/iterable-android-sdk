package com.iterable.iterableapi

import android.graphics.Rect
import android.view.Gravity
import android.view.Window
import android.view.WindowManager

internal class InAppLayoutService {
    internal enum class InAppLayout {
        TOP,
        BOTTOM,
        CENTER,
        FULLSCREEN
    }

    fun getInAppLayout(padding: Rect): InAppLayout {
        return getInAppLayout(InAppPadding.fromRect(padding))
    }

    fun getInAppLayout(padding: InAppPadding): InAppLayout {
        if (padding.top == 0 && padding.bottom == 0) {
            return InAppLayout.FULLSCREEN
        } else if (padding.top > 0 && padding.bottom <= 0) {
            return InAppLayout.TOP
        } else if (padding.top <= 0 && padding.bottom > 0) {
            return InAppLayout.BOTTOM
        } else {
            return InAppLayout.CENTER
        }
    }

    fun getVerticalLocation(padding: Rect): Int {
        return getVerticalLocation(InAppPadding.fromRect(padding))
    }

    fun getVerticalLocation(padding: InAppPadding): Int {
        val layout = getInAppLayout(padding)

        when (layout) {
            InAppLayout.TOP -> return Gravity.TOP
            InAppLayout.BOTTOM -> return Gravity.BOTTOM
            InAppLayout.CENTER -> return Gravity.CENTER_VERTICAL
            InAppLayout.FULLSCREEN -> return Gravity.CENTER_VERTICAL
        }
    }

    fun configureWindowFlags(window: Window?, layout: InAppLayout) {
        if (window == null) {
            return
        }

        if (layout == InAppLayout.FULLSCREEN) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else if (layout != InAppLayout.TOP) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
        }
    }

    fun setWindowToFullScreen(window: Window?) {
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    fun applyWindowGravity(window: Window?, padding: Rect, source: String?) {
        if (window == null) {
            return
        }

        val verticalGravity = getVerticalLocation(padding)
        val params = window.attributes

        when (verticalGravity) {
            Gravity.CENTER_VERTICAL -> params.gravity = Gravity.CENTER
            Gravity.TOP -> params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            Gravity.BOTTOM -> params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            else -> params.gravity = Gravity.CENTER
        }

        window.attributes = params

        if (source != null) {
            IterableLogger.d(
                "InAppLayoutService",
                "Applied window gravity from " + source + ": " + params.gravity
            )
        }
    }
}

