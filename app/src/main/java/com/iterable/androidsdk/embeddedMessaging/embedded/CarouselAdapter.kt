package com.iterable.androidsdk.embeddedMessaging.embedded

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedView
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedViewType

class CarouselAdapter(fm:FragmentManager, private val messages: List<IterableEmbeddedMessage>) : FragmentPagerAdapter(fm) {

    override fun getCount(): Int {
        return messages.size
    }

    override fun getItem(position: Int): Fragment {
        return IterableEmbeddedView(
            IterableEmbeddedViewType.CARD,
            messages[position],
            null
        )
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return super.getPageTitle(position)
    }

}
