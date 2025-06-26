package com.iterable.iterableapi

import android.webkit.WebChromeClient
import android.webkit.WebView

internal class IterableWebChromeClient(
    private val inAppHTMLNotification: IterableWebView.HTMLNotificationCallbacks
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        inAppHTMLNotification.runResizeScript()
    }
}
