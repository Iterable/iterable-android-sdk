package com.iterable.androidsdk.embeddedMessaging.embedded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableEmbeddedManager
import com.iterable.iterableapi.testapp.R

class CarouselViewFragment : Fragment() {

    private lateinit var embeddedManager: IterableEmbeddedManager

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
        placeholder.removeAllViews()
        val carouselView = inflater.inflate(R.layout.carousel_view, container, false)
        val viewPager = carouselView.findViewById<ViewPager>(R.id.viewPager)

        val messages = embeddedManager.getMessages(1)
        if (messages != null) {
            viewPager.adapter = CarouselAdapter(parentFragmentManager, messages)
        }

        placeholder.addView(carouselView)
        return rootView
    }
}