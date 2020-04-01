package com.iterable.iterableapi;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Custom webViewClient which handles url clicks
 */
class IterableWebViewClient extends WebViewClient {
    static final String resizeScript = "javascript:ITBL.resize(document.body.getBoundingClientRect().height)";

    HTMLNotificationCallbacks inAppHTMLNotification;

    IterableWebViewClient(HTMLNotificationCallbacks inAppHTMLNotification) {
        this.inAppHTMLNotification = inAppHTMLNotification;
    }

    /**
     * Handles url clicks
     * @param view
     * @param url
     * @return
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        inAppHTMLNotification.onUrlClicked(url);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        inAppHTMLNotification.setWebViewStatus(HTMLNotificationCallbacks.PageStatus.INITALIZED);
    }

    /**
     * Resizes the view after the page has loaded
     * @param view
     * @param url
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        inAppHTMLNotification.setWebViewStatus(HTMLNotificationCallbacks.PageStatus.LOADED);
        view.loadUrl(resizeScript);
    }

    interface HTMLNotificationCallbacks {
        enum PageStatus {
            NOT_INITIALIZED,
            INITALIZED,
            LOADED
        }
        void onUrlClicked(String url);

        void setWebViewStatus(PageStatus status);
    }
}
