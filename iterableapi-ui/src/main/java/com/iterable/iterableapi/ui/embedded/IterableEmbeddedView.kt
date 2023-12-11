package com.iterable.iterableapi.ui.embedded

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.ui.R

class IterableEmbeddedView(
    private var viewType: IterableEmbeddedViewType,
    private var message: IterableEmbeddedMessage,
    private var config: IterableEmbeddedViewConfig?
): Fragment() {

    private val defaultBackgroundColor : Int by lazy  {
        when(viewType) {
            IterableEmbeddedViewType.NOTIFICATION -> ContextCompat.getColor(requireContext(), R.color.notification_background_color)
            else -> ContextCompat.getColor(requireContext(), R.color.banner_background_color)
        }
    }

    private val defaultBorderColor : Int by lazy {
        when(viewType) {
            IterableEmbeddedViewType.NOTIFICATION -> ContextCompat.getColor(requireContext(), R.color.notification_border_color)
            else -> ContextCompat.getColor(requireContext(), R.color.banner_border_color)
        }
    }

    private val defaultFirstButtonBackgroundColor: Int by lazy {
        when(viewType) {
            IterableEmbeddedViewType.NOTIFICATION -> ContextCompat.getColor(requireContext(), R.color.white)
            else -> ContextCompat.getColor(requireContext(), R.color.banner_button_color)
        }
    }

    private val defaultFirstButtonTextColor: Int by lazy {
        when(viewType) {
            IterableEmbeddedViewType.NOTIFICATION -> ContextCompat.getColor(requireContext(), R.color.notification_text_color)
            else -> ContextCompat.getColor(requireContext(), R.color.white)
        }
    }

    private val defaultSecondButtonTextColor: Int by lazy {
        when(viewType) {
            IterableEmbeddedViewType.NOTIFICATION -> ContextCompat.getColor(requireContext(), R.color.notification_text_color)
            else -> ContextCompat.getColor(requireContext(), R.color.banner_button_color)
        }
    }

    private val defaultTitleTextColor: Int by lazy {
        when(viewType) {
            IterableEmbeddedViewType.NOTIFICATION -> ContextCompat.getColor(requireContext(), R.color.notification_text_color)
            else -> ContextCompat.getColor(requireContext(), R.color.title_text_color)
        }
    }

    private val defaultBodyTextColor: Int by lazy {
        when(viewType) {
            IterableEmbeddedViewType.NOTIFICATION -> ContextCompat.getColor(requireContext(), R.color.notification_text_color)
            else -> ContextCompat.getColor(requireContext(), R.color.body_text_color)
        }
    }

    private val defaultBorderWidth = 1
    private val defaultBorderCornerRadius = 8f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

         val view = when (viewType) {
            IterableEmbeddedViewType.BANNER -> {
                val bannerView = inflater.inflate(R.layout.banner_view, container, false)
                bind(bannerView, message)
                bannerView
            }
            IterableEmbeddedViewType.CARD -> {
                val cardView = inflater.inflate(R.layout.card_view, container, false)
                bind(cardView, message)
                cardView
            }
            IterableEmbeddedViewType.NOTIFICATION -> {
                val notificationView = inflater.inflate(R.layout.notification_view, container, false)
                bind(notificationView, message)
                notificationView
            }
        }

        configure(view, config)

        return view
    }

    private fun configure(view: View, config: IterableEmbeddedViewConfig?) {

        val backgroundColor = config?.backgroundColor.takeIf { it != null } ?: defaultBackgroundColor
        val borderColor = config?.borderColor.takeIf { it != null } ?: defaultBorderColor
        val borderWidth = config?.borderWidth.takeIf { it != null } ?: defaultBorderWidth
        val borderCornerRadius = config?.borderCornerRadius.takeIf { it != null } ?: defaultBorderCornerRadius

        val firstButtonBackgroundColor = config?.firstButtonBackgroundColor.takeIf { it != null } ?: defaultFirstButtonBackgroundColor
        val firstButtonTextColor = config?.firstButtonTextColor.takeIf { it != null } ?: defaultFirstButtonTextColor
        val secondButtonTextColor = config?.secondButtonTextColor.takeIf { it != null } ?: defaultSecondButtonTextColor

        val titleTextColor = config?.titleTextColor.takeIf { it != null } ?: defaultTitleTextColor
        val bodyTextColor = config?.bodyTextColor.takeIf { it != null } ?: defaultBodyTextColor

        val gradientDrawable = GradientDrawable()

        gradientDrawable.setColor(backgroundColor)
        gradientDrawable.setStroke(borderWidth, borderColor)
        gradientDrawable.cornerRadius = borderCornerRadius
        view.setBackgroundDrawable(gradientDrawable)

        val firstButton = view.findViewById<Button>(R.id.embedded_message_first_button)
        val secondButton = view.findViewById<Button>(R.id.embedded_message_second_button)

        val titleText = view.findViewById<TextView>(R.id.embedded_message_title)
        val bodyText = view.findViewById<TextView>(R.id.embedded_message_body)

        val buttonBackgroundDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.banner_button_background) as? GradientDrawable
        buttonBackgroundDrawable?.setColor(firstButtonBackgroundColor)

        firstButton.setBackgroundDrawable(buttonBackgroundDrawable)
        firstButton.setTextColor(firstButtonTextColor)
        secondButton.setTextColor(secondButtonTextColor)

        titleText.setTextColor(titleTextColor)
        bodyText.setTextColor(bodyTextColor)
    }

    private fun bind(view: View, message: IterableEmbeddedMessage): View  {
        val embeddedMessageViewTitle: TextView = view.findViewById(R.id.embedded_message_title)
        val embeddedMessageViewBody: TextView = view.findViewById(R.id.embedded_message_body)
        val embeddedMessageViewButton: TextView = view.findViewById(R.id.embedded_message_first_button)
        val embeddedMessageViewButton2: TextView = view.findViewById(R.id.embedded_message_second_button)

        val embeddedMessageImageView: ImageView = view.findViewById(R.id.embedded_message_image)

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

        Glide.with(view.context)
                .load(message.elements?.mediaURL)
                .into(embeddedMessageImageView)

        return view
    }
}