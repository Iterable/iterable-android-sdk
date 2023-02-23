package com.iterable.iterableapi.ui.flex

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

class IterableFlexViewFragment : Fragment() {
    // example flex message data from a payload
    private val flexMessageMetaData = FlexMessageMetadata(
        "doibjo4590340oidiobnw",
        "mbn8489b7ehycy",
        "noj9iyjthfvhs",
        false
    )

    private val flexMessageDefaultAction = FlexMessageElementsDefaultAction(
        "someType", "someAction"
    )

    private val flexMessageButtons: List<FlexMessageElementsButton> = listOf(
        FlexMessageElementsButton("reward-button", "REDEEM MEOW", "success")
    )


    private val flexMessageText: List<FlexMessageElementsText> = listOf(
        FlexMessageElementsText("body", "CATS RULE!!!", "label")
    )

    private val flexMessageElements = FlexMessageElements(
        "Iterable Coffee Shoppe",
        "Get 15% OFF",
        "http://placekitten.com/200/300",
        flexMessageDefaultAction,
        flexMessageButtons,
        flexMessageText
    )

    val payload = JSONObject()

    private var flexMessage: IterableFlexMessage = IterableFlexMessage(flexMessageMetaData, flexMessageElements, payload)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.iterable_flex_view_fragment, container, false)

        var flexMessageText = view.findViewById<TextView>(R.id.flexMessageBody)
        var flexMessageButton = view.findViewById<Button>(R.id.flexMessageButton)

        flexMessageText.text = flexMessage.elements?.text?.get(0)?.text
        flexMessageButton.text = flexMessage.elements?.buttons?.get(0)?.title

        return view
    }
}