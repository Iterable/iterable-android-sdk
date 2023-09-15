package com.iterable.iterableapi.ui.embedded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.iterable.iterableapi.ui.R

class IterableCardView(): Fragment() {
//    var style = style
//    var message = message

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.card_view, container, false)

//        val rootView = when (style) {
//            "banner" -> inflater.inflate(R.layout.banner_view, container, false)
//            "card" -> inflater.inflate(R.layout.card_view, container, false)
//            "notification" -> inflater.inflate(R.layout.notification_view, container, false)
//            else -> inflater.inflate(R.layout.banner_view, container, false)
//        }

//        bind(rootView, message)

        return rootView
    }

//    fun bind(view: View, message: IterableEmbeddedMessage): View  {
//        var embeddedMessageViewTitle: TextView = view.findViewById(R.id.embedded_message_title)
//        var embeddedMessageViewBody: TextView = view.findViewById(R.id.embedded_message_body)
//        var embeddedMessageViewButton1: TextView? = view.findViewById(R.id.embedded_message_first_button)
//        var embeddedMessageViewButton2: TextView? = view.findViewById(R.id.embedded_message_second_button)
//
//        embeddedMessageViewTitle.text = message.elements?.title
//        embeddedMessageViewBody.text = message.elements?.body
//        embeddedMessageViewButton1?.text = message.elements?.buttons?.get(0)?.title
//        embeddedMessageViewButton2?.text = message.elements?.buttons?.get(1)?.title
//
//        return view
//    }
}