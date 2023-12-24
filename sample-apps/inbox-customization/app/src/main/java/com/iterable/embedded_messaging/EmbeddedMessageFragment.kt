package com.iterable.embedded_messaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.iterable.embedded_messaging.embedded.EmbeddedMessageViewFragment
import com.iterable.inbox_customization.R

class EmbeddedMessageFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_message, container, false)

        val messageFragment: Fragment = EmbeddedMessageViewFragment()

        val ft: FragmentTransaction = childFragmentManager.beginTransaction()
        ft.replace(R.id.embedded_message, messageFragment)
        ft.commit()

        return view
    }
}