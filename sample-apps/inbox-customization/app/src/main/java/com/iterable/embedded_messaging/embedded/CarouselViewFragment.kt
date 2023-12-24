package com.iterable.embedded_messaging.embedded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iterable.inbox_customization.R
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableEmbeddedManager

class CarouselViewFragment: Fragment() {

    private lateinit var embeddedManager: IterableEmbeddedManager

    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var adapter: CarouselAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        embeddedManager = IterableApi.getInstance().embeddedManager
    }

    override fun onResume() {
        super.onResume()
        IterableApi.getInstance().embeddedManager.syncMessages()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.embedded_view_fragment, container, false)

        val placeholder: ViewGroup = rootView.findViewById(R.id.placeholder)

        val messages = embeddedManager.getMessages()

        placeholder.removeAllViews()

        val carouselView = inflater.inflate(R.layout.carousel_view, container, false)

        adapter = CarouselAdapter()
        val recyclerView = carouselView.findViewById<RecyclerView>(R.id.embedded_message_list)
        layoutManager = recyclerView.layoutManager as LinearLayoutManager

        recyclerView.adapter = adapter

        adapter.submitList(messages)

        placeholder.addView(carouselView)

        return rootView
    }


}