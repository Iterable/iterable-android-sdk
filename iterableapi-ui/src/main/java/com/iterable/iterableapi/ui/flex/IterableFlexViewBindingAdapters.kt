package com.iterable.iterableapi.ui.flex

import android.media.Image
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
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

@BindingAdapter("flexMessageImage")
fun ImageView.setFlexMessageImage(item: IterableFlexMessage?) {
    item?.let {
        setImageResource(R.drawable.coffee_cappuccino)
    }
}