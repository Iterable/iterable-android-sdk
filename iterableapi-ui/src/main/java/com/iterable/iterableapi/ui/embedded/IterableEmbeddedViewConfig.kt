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
    val imageScaleType: ImageView.ScaleType? = null
)