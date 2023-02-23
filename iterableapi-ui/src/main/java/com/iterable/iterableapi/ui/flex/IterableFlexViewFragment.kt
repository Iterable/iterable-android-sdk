package com.iterable.iterableapi.ui.flex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.iterable.iterableapi.*
import com.iterable.iterableapi.ui.R
import com.iterable.iterableapi.ui.databinding.FragmentIterableFlexViewBinding
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

        val binding: FragmentIterableFlexViewBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_iterable_flex_view, container, false)
        binding.flexMessage = flexMessage
        return binding.root
    }
}