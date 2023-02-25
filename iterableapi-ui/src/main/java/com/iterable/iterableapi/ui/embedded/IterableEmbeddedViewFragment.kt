package com.iterable.iterableapi.ui.embedded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.iterable.iterableapi.*
import com.iterable.iterableapi.ui.R
import org.json.JSONObject

class IterableEmbeddedViewFragment : Fragment() {
    // example flex message data from a payload
    private val embeddedMessageMetaData = EmbeddedMessageMetadata(
        "doibjo4590340oidiobnw",
        "mbn8489b7ehycy",
        "noj9iyjthfvhs",
        false
    )

    private val embeddedMessageDefaultAction = EmbeddedMessageElementsDefaultAction(
        "someType", "someAction"
    )

    private val embeddedMessageButtons: List<EmbeddedMessageElementsButton> = listOf(
        EmbeddedMessageElementsButton("reward-button", "REDEEM MEOW", "success")
    )


    private val embeddedMessageText: List<EmbeddedMessageElementsText> = listOf(
        EmbeddedMessageElementsText("body", "CATS RULE!!!", "label")
    )

    private val embeddedMessageElements = EmbeddedMessageElements(
        "Iterable Coffee Shoppe",
        "Get 15% OFF",
        "http://placekitten.com/200/300",
        embeddedMessageDefaultAction,
        embeddedMessageButtons,
        embeddedMessageText
    )

    val payload = JSONObject()

    private var embeddedMessage: IterableEmbeddedMessage = IterableEmbeddedMessage(embeddedMessageMetaData, embeddedMessageElements, payload)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.iterable_embedded_view_fragment, container, false)

        var flexMessageText = view.findViewById<TextView>(R.id.embeddedMessageBody)
        var flexMessageButton = view.findViewById<Button>(R.id.embeddedMessageButton)

        flexMessageText.text = embeddedMessage.elements?.text?.get(0)?.text
        flexMessageButton.text = embeddedMessage.elements?.buttons?.get(0)?.title

        return view
    }
}