package com.iterable.iterableapi;

import android.content.Context;
import android.graphics.Color;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import static com.iterable.iterableapi.IterableWebViewClient.RESIZE_SCRIPT;

/**
 * The custom html webView
 */
class IterableWebView extends WebView {
    static final String MIME_TYPE = "text/html";
    static final String ENCODING = "UTF-8";

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
        loadDataWithBaseURL("", html, MIME_TYPE, ENCODING, "");
        setWebViewClient(webViewClient);
        setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                loadUrl(RESIZE_SCRIPT);
            }
        });

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
