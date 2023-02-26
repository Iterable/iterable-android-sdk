package com.iterable.iterableapi.ui.embedded

import android.os.Bundle
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
import com.iterable.iterableapi.ui.flex.IterableEmbeddedViewViewModel
import org.json.JSONObject

class IterableEmbeddedViewFragment : Fragment() {

    private lateinit var viewModel: IterableEmbeddedViewViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this).get(IterableEmbeddedViewViewModel::class.java)

        var view = inflater.inflate(R.layout.iterable_embedded_view_fragment, container, false)

        var flexMessageText = view.findViewById<TextView>(R.id.embeddedMessageBody)
        var flexMessageButton = view.findViewById<Button>(R.id.embeddedMessageButton)

        viewModel.embeddedMessage.observe(viewLifecycleOwner, Observer { newMessage ->
            flexMessageText.text = newMessage.elements?.text?.get(0)?.text
            flexMessageButton.text = newMessage.elements?.buttons?.get(0)?.title
        })

        return view
    }
}