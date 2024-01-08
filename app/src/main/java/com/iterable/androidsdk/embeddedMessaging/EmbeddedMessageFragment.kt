package com.iterable.androidsdk.embeddedMessaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.testapp.R
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedView
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedViewType

class EmbeddedMessageFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_message, container, false)

        val messages = IterableApi.getInstance().embeddedManager.getMessages(2)
        val message: IterableEmbeddedMessage? = messages?.get(0)

        if (message != null) {
            val messageFragment: Fragment = IterableEmbeddedView(IterableEmbeddedViewType.BANNER, message, null)
            val ft: FragmentTransaction = childFragmentManager.beginTransaction()
            ft.replace(R.id.embedded_message, messageFragment)
            ft.commit()
        }

        return view
    }
}