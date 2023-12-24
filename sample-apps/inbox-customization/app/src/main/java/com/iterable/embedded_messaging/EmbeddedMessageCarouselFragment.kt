package com.iterable.embedded_messaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.iterable.embedded_messaging.embedded.CarouselViewFragment
import com.iterable.inbox_customization.R

class EmbeddedMessageCarouselFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_carousel, container, false)

        val flexViewFragment: Fragment = CarouselViewFragment()

        val ft: FragmentTransaction = childFragmentManager.beginTransaction()
        ft.replace(R.id.embedded_message_carousel, flexViewFragment)
        ft.commit()

        return view
    }
}