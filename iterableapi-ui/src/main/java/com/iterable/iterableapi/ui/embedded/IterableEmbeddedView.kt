package com.iterable.iterableapi.ui.embedded

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.ui.R

class IterableEmbeddedView(
    private var viewType: IterableEmbeddedViewType,
    private var message: IterableEmbeddedMessage,
    private var config: IterableEmbeddedViewConfig?
): Fragment() {

    private val defaultBackgroundColor = "#FFFFFF"
    private val defaultBorderWidth = 1
    private val defaultBorderColor = "#E0DEDF"
    private val defaultBorderCornerRadius = 8f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = when (viewType) {
            IterableEmbeddedViewType.BANNER -> inflater.inflate(R.layout.banner_view, container, false)
            IterableEmbeddedViewType.CARD -> inflater.inflate(R.layout.card_view, container, false)
            IterableEmbeddedViewType.NOTIFICATION -> inflater.inflate(R.layout.notification_view, container, false)
        }

        bind(view, message)

        configure(view, config)

        return view
    }

    private fun configure(view: View, config: IterableEmbeddedViewConfig?) {

        val backgroundColor = config?.backgroundColor.takeIf { it?.isNotEmpty() == true } ?: defaultBackgroundColor
        val borderWidth = config?.borderWidth.takeIf { it != null && it > 0 } ?: defaultBorderWidth
        val borderColor = config?.borderColor.takeIf { it?.isNotEmpty() == true } ?: defaultBorderColor
        val borderCornerRadius = config?.borderCornerRadius.takeIf { it != null && it > 0 } ?: defaultBorderCornerRadius

        val gradientDrawable = GradientDrawable()

        gradientDrawable.setColor(Color.parseColor(backgroundColor))
        gradientDrawable.setStroke(borderWidth, Color.parseColor(borderColor))
        gradientDrawable.cornerRadius = borderCornerRadius
        view.setBackgroundDrawable(gradientDrawable)
    }

    private fun bind(view: View, message: IterableEmbeddedMessage): View  {
        val embeddedMessageViewTitle: TextView = view.findViewById(R.id.embedded_message_title)
        val embeddedMessageViewBody: TextView = view.findViewById(R.id.embedded_message_body)
        val embeddedMessageViewButton: TextView = view.findViewById(R.id.embedded_message_first_button)
        val embeddedMessageViewButton2: TextView = view.findViewById(R.id.embedded_message_second_button)

        embeddedMessageViewTitle.text = message.elements?.title
        embeddedMessageViewBody.text = message.elements?.body

        val buttons = message.elements?.buttons

        if (buttons != null) {
            embeddedMessageViewButton.visibility = if (buttons.getOrNull(0)?.title == null) View.GONE else View.VISIBLE
            embeddedMessageViewButton.text = buttons.getOrNull(0)?.title.orEmpty()

            embeddedMessageViewButton.setOnClickListener {
                IterableApi.getInstance().embeddedManager.handleEmbeddedClick(
                    message,
                    message.elements?.buttons?.get(0)?.id,
                    message.elements?.buttons?.get(0)?.action?.data
                )

                IterableApi.getInstance().trackEmbeddedClick(
                    message,
                    message.elements?.buttons?.get(0)?.id,
                    message.elements?.buttons?.get(0)?.action?.data
                )
            }

            if (buttons.size > 1) {
                embeddedMessageViewButton2.visibility = if (buttons[1].title == null) View.GONE else View.VISIBLE
                embeddedMessageViewButton2.text = buttons[1].title.orEmpty()

                embeddedMessageViewButton2.setOnClickListener {
                    IterableApi.getInstance().embeddedManager.handleEmbeddedClick(
                        message,
                        message.elements?.buttons?.get(1)?.id,
                        message.elements?.buttons?.get(1)?.action?.data
                    )

                    IterableApi.getInstance().trackEmbeddedClick(
                        message,
                        message.elements?.buttons?.get(1)?.id,
                        message.elements?.defaultAction?.data
                    )
                }

            } else {
                embeddedMessageViewButton2.visibility = View.GONE
            }

        } else {
            embeddedMessageViewButton.visibility = View.GONE
            embeddedMessageViewButton2.visibility = View.GONE
        }

        return view
    }
}