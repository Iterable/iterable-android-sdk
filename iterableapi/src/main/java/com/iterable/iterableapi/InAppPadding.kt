package com.iterable.iterableapi

import android.graphics.Rect

internal data class InAppPadding(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    companion object {
        @JvmStatic
        fun fromRect(rect: Rect): InAppPadding {
            return InAppPadding(
                left = rect.left,
                top = rect.top,
                right = rect.right,
                bottom = rect.bottom
            )
        }
    }

    fun toRect(): Rect {
        return Rect(left, top, right, bottom)
    }
}

