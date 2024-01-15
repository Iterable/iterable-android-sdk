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
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedViewConfig
import com.iterable.iterableapi.ui.embedded.IterableEmbeddedViewType

class EmbeddedMessageAllViewFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_message_all_view, container, false)

        val messages = IterableApi.getInstance().embeddedManager.getMessages(1)
        val message: IterableEmbeddedMessage? = messages?.get(0)
        var messageFragment: Fragment
        var ft: FragmentTransaction = childFragmentManager.beginTransaction()

        if (message != null) {
            messageFragment =
                IterableEmbeddedView(IterableEmbeddedViewType.BANNER, message, null)
            ft.replace(R.id.embedded_message_banner, messageFragment)
            ft.commit()
        }

        if (message != null) {
            val cardConfig = IterableEmbeddedViewConfig(
                resources.getColor(R.color.mustard),
                resources.getColor(R.color.red),
                1,
                100f,
                resources.getColor(R.color.colorAccent),
                resources.getColor(R.color.not_black),
                resources.getColor(R.color.green),
                resources.getColor(R.color.colorPrimaryDark),
                resources.getColor(R.color.title_color),
                resources.getColor(R.color.body_color),
            )
            messageFragment =
                IterableEmbeddedView(IterableEmbeddedViewType.CARD, message, cardConfig)
            ft = childFragmentManager.beginTransaction()
            ft.replace(R.id.embedded_message_card, messageFragment)
            ft.commit()
        }

        if (message != null) {
            val notificationConfig = IterableEmbeddedViewConfig(
                resources.getColor(R.color.lavender),
                resources.getColor(R.color.salmon),
                1,
                5f,
                resources.getColor(R.color.mustard),
                resources.getColor(R.color.not_black),
                resources.getColor(R.color.purple),
                resources.getColor(R.color.colorAccent),
                resources.getColor(R.color.colorPrimaryDark),
                resources.getColor(R.color.green),
            )
            messageFragment = IterableEmbeddedView(
                IterableEmbeddedViewType.NOTIFICATION,
                message,
                notificationConfig
            )
            ft = childFragmentManager.beginTransaction()
            ft.replace(R.id.embedded_message_notification, messageFragment)
            ft.commit()
        }

        return view
    }
}