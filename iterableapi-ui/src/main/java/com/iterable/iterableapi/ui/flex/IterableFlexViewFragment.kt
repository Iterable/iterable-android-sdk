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
        IterableFlexMessageText("body", "CATS RULE!!!")
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

        val binding: FragmentIterableFlexViewBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_iterable_flex_view, container, false)
        binding.flexMessage = flexMessage
        return binding.root
    }
}