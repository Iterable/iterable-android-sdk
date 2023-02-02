package com.iterable.iterableapi.ui.flex

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.iterable.iterableapi.*
import com.iterable.iterableapi.ui.R

class IterableFlexViewFragment : Fragment() {

    private lateinit var viewModel: IterableFlexViewViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = ViewModelProvider(this).get(IterableFlexViewViewModel::class.java)

        var view = inflater.inflate(R.layout.iterable_flex_view_fragment, container, false)

        var flexMessageText = view.findViewById<TextView>(R.id.flexMessageBody)
        var flexMessageButton = view.findViewById<Button>(R.id.flexMessageButton)

        viewModel.flexMessage.observe(viewLifecycleOwner, Observer { newMessage ->
            flexMessageText.text = newMessage.elements.text[0].text
            flexMessageButton.text = newMessage.elements.buttons[0].title
        })

        return view
    }
}