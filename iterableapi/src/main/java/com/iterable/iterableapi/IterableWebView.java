package com.iterable.iterableapi;

import android.content.Context;
import android.graphics.Color;
import android.webkit.WebView;

/**
 * The custom html webView
 */
class IterableWebView extends WebView {
    static final String mimeType = "text/html";
    static final String encoding = "UTF-8";

    IterableWebView(Context context) {
        super(context);
    }

    /**
     * Loads the html into the webView
     * @param notificationDialog
     * @param html
     */
    void createWithHtml(IterableWebViewClient.HTMLNotificationCallbacks notificationDialog, String html) {
        IterableWebViewClient webViewClient = new IterableWebViewClient(notificationDialog);
        loadDataWithBaseURL("", html, mimeType, encoding, "");
        setWebViewClient(webViewClient);

        //don't overscroll
        setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        //transparent
        setBackgroundColor(Color.TRANSPARENT);

        //Fixes the webView to be the size of the screen
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setUseWideViewPort(true);

        //resize:
        getSettings().setJavaScriptEnabled(true);
    }
}
