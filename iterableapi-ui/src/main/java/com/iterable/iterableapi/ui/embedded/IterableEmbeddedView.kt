package com.iterable.iterableapi.ui.embedded

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.ui.R

class IterableEmbeddedView(
    private var viewType: String,
    private var message: IterableEmbeddedMessage,
    private var config: IterableEmbeddedViewConfig?
): Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = when (viewType) {
            "banner" -> inflater.inflate(R.layout.banner_view, container, false)
            "card" -> inflater.inflate(R.layout.card_view, container, false)
            "notification" -> inflater.inflate(R.layout.notification_view, container, false)
            else -> inflater.inflate(R.layout.banner_view, container, false)
        }

        bind(view, message)

        configure(view, config)

        return view
    }

    private fun configure(view: View, config: IterableEmbeddedViewConfig?) {

        val backgroundColor = config?.backgroundColor ?: "#FFFFFF"
        val borderWidth = config?.borderWidth ?: 1
        val borderColor = config?.borderColor ?: "#E0DEDF"
        val borderCornerRadius = config?.borderCornerRadius ?: 8f

        val gradientDrawable = GradientDrawable()

        gradientDrawable.setColor(Color.parseColor(backgroundColor))
        gradientDrawable.setStroke(borderWidth, Color.parseColor(borderColor))
        gradientDrawable.cornerRadius = borderCornerRadius
        view.setBackgroundDrawable(gradientDrawable)
    }

    private fun bind(view: View, message: IterableEmbeddedMessage): View  {
        val embeddedMessageViewTitle: TextView = view.findViewById(R.id.embedded_message_title)
        val embeddedMessageViewBody: TextView = view.findViewById(R.id.embedded_message_body)
        var embeddedMessageViewButton1: TextView? = view.findViewById(R.id.embedded_message_first_button)
        var embeddedMessageViewButton2: TextView? = view.findViewById(R.id.embedded_message_second_button)

        embeddedMessageViewTitle.text = message.elements?.title
        embeddedMessageViewBody.text = message.elements?.body
        //embeddedMessageViewButton1?.text = message.elements?.buttons?.get(0)?.title
        //embeddedMessageViewButton2?.text = message.elements?.buttons?.get(1)?.title

        return view
    }
}