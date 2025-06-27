package com.iterable.iterableapi.ui.inbox

import android.net.Uri
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient

import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableInAppLocation
import com.iterable.iterableapi.IterableInAppMessage
import com.iterable.iterableapi.ui.R

class IterableInboxMessageFragment : Fragment() {
    
    companion object {
        const val ARG_MESSAGE_ID = "messageId"
        const val STATE_LOADED = "loaded"
        
        @JvmStatic
        fun newInstance(messageId: String?): IterableInboxMessageFragment {
            val fragment = IterableInboxMessageFragment()
            val args = Bundle()
            args.putString(ARG_MESSAGE_ID, messageId)
            fragment.arguments = args
            return fragment
        }
    }

    private var messageId: String? = null
    private lateinit var webView: WebView
    private var message: IterableInAppMessage? = null
    private var loaded = false

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            messageId = it.getString(ARG_MESSAGE_ID)
        }
        savedInstanceState?.let {
            loaded = it.getBoolean(STATE_LOADED, false)
        }
    }

    override fun onSaveInstanceState(@NonNull outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LOADED, true)
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.iterable_inbox_message_fragment, container, false)
        webView = view.findViewById(R.id.webView)
        loadMessage()
        return view
    }

    private fun getMessageById(messageId: String?): IterableInAppMessage? {
        val messages = IterableApi.getInstance().inAppManager.messages
        for (message in messages) {
            if (message.messageId == messageId) {
                return message
            }
        }
        return null
    }

    private fun loadMessage() {
        message = getMessageById(messageId)
        message?.let { msg ->
            webView.loadDataWithBaseURL("", msg.content.html, "text/html", "UTF-8", "")
            webView.webViewClient = webViewClient
            if (!loaded) {
                IterableApi.getInstance().trackInAppOpen(msg, IterableInAppLocation.INBOX)
                loaded = true
            }
            activity?.setTitle(msg.inboxMetadata.title)
        }
    }

    private val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            message?.let { msg ->
                IterableApi.getInstance().trackInAppClick(msg, url, IterableInAppLocation.INBOX)
                IterableApi.getInstance().inAppManager.handleInAppClick(msg, Uri.parse(url))
                activity?.finish()
            }
            return true
        }
    }
}
