package com.iterable.embedded_messaging.embedded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.iterable.inbox_customization.R
import com.iterable.iterableapi.EmbeddedMessageElementsButton
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableEmbeddedManager
import com.iterable.iterableapi.IterableEmbeddedMessage

class EmbeddedMessageViewFragment: Fragment() {

    private lateinit var embeddedManager: IterableEmbeddedManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        embeddedManager = IterableApi.getInstance().embeddedManager
    }

    override fun onResume() {
        super.onResume()
        IterableApi.getInstance().embeddedManager.syncMessages()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.embedded_view_fragment, container, false)
        val placeholder: ViewGroup = rootView.findViewById(R.id.placeholder)

        placeholder.removeAllViews()

        val messages = embeddedManager.getMessages()

        var message: IterableEmbeddedMessage? = messages[0]

        val messageView = inflater.inflate(R.layout.banner_view, container, false)

        if(message !== null) {
            bind(messageView, message)
        }

        placeholder.addView(messageView)

        return rootView
    }

    private fun bind(view: View, message: IterableEmbeddedMessage): View {
        val embeddedMessageViewTitle: TextView = view.findViewById(R.id.embedded_message_title)
        val embeddedMessageViewBody: TextView = view.findViewById(R.id.embedded_message_body)
        val embeddedMessageViewButton: Button = view.findViewById(R.id.embedded_message_first_button)
        val embeddedMessageViewButton2: Button = view.findViewById(R.id.embedded_message_second_button)

        val embeddedMessageImageView: ImageView = view.findViewById(R.id.embedded_message_image)

        if(message.elements?.mediaURL?.isEmpty() == true) {
            embeddedMessageImageView.visibility = View.GONE
        } else {
            Glide.with(view.context).load(message.elements?.mediaURL).into(embeddedMessageImageView)
        }

        embeddedMessageViewTitle.text = message.elements?.title
        embeddedMessageViewBody.text = message.elements?.body

        val buttons = message.elements?.buttons

        if (buttons != null) {
            setButton(embeddedMessageViewButton, buttons.getOrNull(0), message)

            if (buttons.size > 1) {
                setButton(embeddedMessageViewButton2, buttons.getOrNull(1), message)
            } else {
                embeddedMessageViewButton2.visibility = View.GONE
            }

        } else {
            embeddedMessageViewButton.visibility = View.GONE
            embeddedMessageViewButton2.visibility = View.GONE
        }

        return view
    }

    private fun setButton(buttonView: Button, button: EmbeddedMessageElementsButton?, message: IterableEmbeddedMessage) {
        buttonView.visibility = if (button?.title == null) View.GONE else View.VISIBLE
        buttonView.text = button?.title.orEmpty()
    }
}