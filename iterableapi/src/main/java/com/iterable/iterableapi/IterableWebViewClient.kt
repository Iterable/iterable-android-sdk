package com.iterable.iterableapi

import android.webkit.WebView
import android.webkit.WebViewClient

internal class IterableWebViewClient(
    private val inAppHTMLNotification: IterableWebView.HTMLNotificationCallbacks
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        inAppHTMLNotification.onUrlClicked(url)
        return true
    }

    override fun onPageFinished(view: WebView, url: String) {
        inAppHTMLNotification.setLoaded(true)
        view.postDelayed({ inAppHTMLNotification.runResizeScript() }, 100)
    }
}