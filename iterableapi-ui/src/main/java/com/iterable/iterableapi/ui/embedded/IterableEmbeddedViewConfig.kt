package com.iterable.iterableapi.ui.embedded

import android.widget.ImageView

data class IterableEmbeddedViewConfig @JvmOverloads constructor(
    val backgroundColor: Int? = null,
    val borderColor: Int? = null,
    val borderWidth: Int? = null,
    val borderCornerRadius: Float? = null,
    val primaryBtnBackgroundColor: Int? = null,
    val primaryBtnTextColor: Int? = null,
    val secondaryBtnBackgroundColor: Int? = null,
    val secondaryBtnTextColor: Int? = null,
    val titleTextColor: Int? = null,
    val bodyTextColor: Int? = null,
    /** Image scale type applied to CARD and BANNER views. */
    val imageScaleType: ImageView.ScaleType = DEFAULT_IMAGE_SCALE_TYPE
) {
    companion object {
        @JvmField
        val DEFAULT_IMAGE_SCALE_TYPE: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
    }
}