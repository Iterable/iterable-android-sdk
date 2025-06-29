package com.iterable.iterableapi

import android.content.Context
import android.graphics.Color
import android.webkit.WebView

/**
 * The custom html webView
 */
internal class IterableWebView(context: Context) : WebView(context) {
    
    companion object {
        const val MIME_TYPE = "text/html"
        const val ENCODING = "UTF-8"
    }

    fun createWithHtml(notificationDialog: HTMLNotificationCallbacks, html: String) {
        // set up web view clients
        val webViewClient = IterableWebViewClient(notificationDialog)
        val webChromeClient = IterableWebChromeClient(notificationDialog)

        setWebViewClient(webViewClient)
        setWebChromeClient(webChromeClient)

        // don't overscroll
        overScrollMode = WebView.OVER_SCROLL_NEVER

        // transparent
        setBackgroundColor(Color.TRANSPARENT)

        // fixes the webView to be the size of the screen
        settings.loadWithOverviewMode = true

        // disallow unnecessary access
        settings.allowFileAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        settings.allowContentAccess = false

        // disallow javascript
        settings.javaScriptEnabled = false

        // start loading the in-app
        // specifically use loadDataWithBaseURL and not loadData, as mentioned in https://stackoverflow.com/a/58181704/13111386
        loadDataWithBaseURL("", html, MIME_TYPE, ENCODING, "")
    }

    interface HTMLNotificationCallbacks {
        fun onUrlClicked(url: String)
        fun setLoaded(loaded: Boolean)
        fun runResizeScript()
    }
}
