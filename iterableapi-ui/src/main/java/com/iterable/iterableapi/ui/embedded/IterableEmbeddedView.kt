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
import com.iterable.iterableapi.EmbeddedMessageElementsButton
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.IterableLogger
import com.iterable.iterableapi.ui.R

class IterableEmbeddedView(
    private var viewType: IterableEmbeddedViewType,
    private var message: IterableEmbeddedMessage,
    private var config: IterableEmbeddedViewConfig?
): Fragment() {

    private val defaultBackgroundColor : Int by lazy  { getDefaultColor(viewType, R.color.notification_background_color, R.color.banner_background_color) }
    private val defaultBorderColor : Int by lazy { getDefaultColor(viewType, R.color.notification_border_color, R.color.banner_border_color) }
    private val defaultFirstButtonBackgroundColor: Int by lazy { getDefaultColor(viewType, R.color.white, R.color.banner_button_color) }
    private val defaultFirstButtonBorderColor: Int by lazy { getDefaultColor(viewType, R.color.notification_button_border_color, R.color.banner_button_color) }
    private val defaultFirstButtonTextColor: Int by lazy { getDefaultColor(viewType, R.color.notification_text_color, R.color.white) }
    private val defaultSecondButtonTextColor: Int by lazy { getDefaultColor(viewType, R.color.notification_text_color, R.color.banner_button_color) }
    private val defaultTitleTextColor: Int by lazy { getDefaultColor(viewType, R.color.notification_text_color, R.color.title_text_color) }
    private val defaultBodyTextColor: Int by lazy { getDefaultColor(viewType, R.color.notification_text_color, R.color.body_text_color) }
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
                bind(viewType, bannerView, message)
                bannerView
            }
            IterableEmbeddedViewType.CARD -> {
                val cardView = inflater.inflate(R.layout.card_view, container, false)
                bind(viewType, cardView, message)
                cardView
            }
            IterableEmbeddedViewType.NOTIFICATION -> {
                val notificationView = inflater.inflate(R.layout.notification_view, container, false)
                bind(viewType, notificationView, message)
                notificationView
            }
        }

        setDefaultAction(view, message)
        configure(view, config)

        return view
    }

    private fun configure(view: View, config: IterableEmbeddedViewConfig?) {

        val backgroundColor = config?.backgroundColor.takeIf { it != null } ?: defaultBackgroundColor
        val borderColor = config?.borderColor.takeIf { it != null } ?: defaultBorderColor
        val borderWidth = config?.borderWidth.takeIf { it != null } ?: defaultBorderWidth
        val borderCornerRadius = config?.borderCornerRadius.takeIf { it != null } ?: defaultBorderCornerRadius

        val firstButtonBackgroundColor = config?.firstButtonBackgroundColor.takeIf { it != null } ?: defaultFirstButtonBackgroundColor
        val firstButtonBorderColor = config?.firstButtonBorderColor.takeIf { it != null } ?: defaultFirstButtonBorderColor
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

        if(config?.firstButtonBackgroundColor != null || config?.firstButtonBorderColor != null) {
            val buttonBackgroundDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.banner_button_background) as? GradientDrawable
            buttonBackgroundDrawable?.setColor(firstButtonBackgroundColor)
            buttonBackgroundDrawable?.setStroke(1, firstButtonBorderColor)

            firstButton.setBackgroundDrawable(buttonBackgroundDrawable)
        }

        firstButton.setTextColor(firstButtonTextColor)
        secondButton.setTextColor(secondButtonTextColor)

        titleText.setTextColor(titleTextColor)
        bodyText.setTextColor(bodyTextColor)
    }

    private fun bind(viewType: IterableEmbeddedViewType, view: View, message: IterableEmbeddedMessage): View  {
        val embeddedMessageViewTitle: TextView = view.findViewById(R.id.embedded_message_title)
        val embeddedMessageViewBody: TextView = view.findViewById(R.id.embedded_message_body)
        val embeddedMessageViewButton: Button = view.findViewById(R.id.embedded_message_first_button)
        val embeddedMessageViewButton2: Button = view.findViewById(R.id.embedded_message_second_button)

        if(viewType != IterableEmbeddedViewType.NOTIFICATION) {
            val embeddedMessageImageView: ImageView = view.findViewById(R.id.embedded_message_image)
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

    private fun setDefaultAction(view: View, message: IterableEmbeddedMessage) {
        if(message.elements?.defaultAction != null) {
            val clickedUrl = message.elements?.defaultAction?.data.takeIf { it?.isNotEmpty() == true } ?: message.elements?.defaultAction?.type

            view.setOnClickListener {
                IterableApi.getInstance().embeddedManager.handleEmbeddedClick(message, null, clickedUrl)
                IterableApi.getInstance().trackEmbeddedClick(message, null, clickedUrl)
            }
        }
    }

    private fun setButton(buttonView: Button, button: EmbeddedMessageElementsButton?, message: IterableEmbeddedMessage) {
        buttonView.visibility = if (button?.title == null) View.GONE else View.VISIBLE
        buttonView.text = button?.title.orEmpty()

        val clickedUrl = if (button?.action?.data?.isNotEmpty() == true) button.action?.data else button?.action?.type

        buttonView.setOnClickListener {
            IterableApi.getInstance().embeddedManager.handleEmbeddedClick(message, button?.id, clickedUrl)
            IterableApi.getInstance().trackEmbeddedClick(message, button?.id, clickedUrl)
        }
    }

    private fun getDefaultColor(viewType: IterableEmbeddedViewType, notificationColor: Int, bannerColor: Int): Int {
        return when (viewType) {
            IterableEmbeddedViewType.NOTIFICATION -> ContextCompat.getColor(requireContext(), notificationColor)
            else -> ContextCompat.getColor(requireContext(), bannerColor)
        }
    }
}