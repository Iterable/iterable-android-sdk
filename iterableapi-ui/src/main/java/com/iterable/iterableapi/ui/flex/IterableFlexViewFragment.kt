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

class IterableFlexViewFragment : Fragment() {
    // example flex message data from a payload
    private val flexMessageMetaData: IterableFlexMessageMetaData = IterableFlexMessageMetaData(
        "doibjo4590340oidiobnw",
        "mbn8489b7ehycy",
        "noj9iyjthfvhs",
        false
    )

    private val flexMessageButtons: List<IterableFlexMessageButton> = listOf(
        IterableFlexMessageButton("reward-button", "REDEEM MEOW", "success")
    )

    private val flexMessageImages: List<IterableFlexMessageImage> = listOf(
        IterableFlexMessageImage("coffee-image", "https://example-image-url.com/first-image")
    )

    private val flexMessageText: List<IterableFlexMessageText> = listOf(
        IterableFlexMessageText("title", "CATS RULE!!!"),
        IterableFlexMessageText("body", "GET FRESH COFFEE")
    )

    private val flexMessageElements: IterableFlexMessageElements = IterableFlexMessageElements(
        "hero",
        flexMessageButtons,
        flexMessageImages,
        flexMessageText
    )

    private var flexMessage: IterableFlexMessage = IterableFlexMessage(flexMessageMetaData, flexMessageElements,  "")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.iterable_flex_view_fragment, container, false)
        val flexMessageText: TextView  = view.findViewById<TextView>(R.id.body)
        val flexMessageButton: Button = view.findViewById<Button>(R.id.flexMessageButton)

        flexMessageText.text = flexMessage.elements.text.find(it)

        return view
    }
}