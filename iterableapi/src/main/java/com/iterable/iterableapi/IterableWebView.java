package com.iterable.iterableapi;

import android.content.Context;
import android.graphics.Color;
import android.webkit.WebView;

/**
 * The custom html webView
 */
class IterableWebView extends WebView {
    private static final String TAG = "IterableWebView";
    static final String MIME_TYPE = "text/html";
    static final String ENCODING = "UTF-8";

    /**
     * Instantiate the WebView. Note: if WebView initialization fails (e.g., due to
     * android.util.AndroidRuntimeException from WebViewFactory when the WebView process is
     * unavailable), the exception propagates to the caller and should be caught there (#1013).
     */
    IterableWebView(Context context) {
        super(context);
    }

    void createWithHtml(IterableWebView.HTMLNotificationCallbacks notificationDialog, String html) {
        try {
            // set up web view clients
            IterableWebViewClient webViewClient = new IterableWebViewClient(notificationDialog);
            IterableWebChromeClient webChromeClient = new IterableWebChromeClient(notificationDialog);

            setWebViewClient(webViewClient);
            setWebChromeClient(webChromeClient);

            // don't overscroll
            setOverScrollMode(WebView.OVER_SCROLL_NEVER);

            // transparent
            setBackgroundColor(Color.TRANSPARENT);

            // fixes the webView to be the size of the screen
            getSettings().setLoadWithOverviewMode(true);

            // disallow unnecessary access
            getSettings().setAllowFileAccess(false);
            getSettings().setAllowFileAccessFromFileURLs(false);
            getSettings().setAllowUniversalAccessFromFileURLs(false);
            getSettings().setAllowContentAccess(false);

            // disallow javascript
            getSettings().setJavaScriptEnabled(false);

            // start loading the in-app
            // specifically use loadDataWithBaseURL and not loadData, as mentioned in https://stackoverflow.com/a/58181704/13111386
            // Use configured base URL to enable CORS for external resources (e.g., custom fonts)
            loadDataWithBaseURL(IterableUtil.getWebViewBaseUrl(), html, MIME_TYPE, ENCODING, "");
        } catch (Exception e) {
            IterableLogger.e(TAG, "Failed to configure WebView - WebView may be in an invalid state (#1013)", e);
        }
    }

    interface HTMLNotificationCallbacks {
        void onUrlClicked(String url);
        void setLoaded(boolean loaded);
        void runResizeScript();
    }
}
