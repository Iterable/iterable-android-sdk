package com.iterable.iterableapi.ui.flex

import android.media.Image
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.iterable.iterableapi.IterableFlexMessage
import com.iterable.iterableapi.ui.R

@BindingAdapter("flexMessageBody")
fun TextView.setFlexMessageBodyText(item: IterableFlexMessage?) {
    item?.let {
        text = item.elements.text[0].text
    }
}
@BindingAdapter("flexMessageButton")
fun Button.setFlexMessageButtonText(item: IterableFlexMessage?) {
    item?.let {
        text = item.elements.buttons[0].title
    }
}
@BindingAdapter("imageUrl")
fun bindImage(imgView: ImageView, imgUrl: String?) {
    imgUrl?.let {
        val imgUri = it.toUri().buildUpon().scheme("http").build()
        Glide.with(imgView.context)
            .load(imgUri)
            .into(imgView)
    }
}