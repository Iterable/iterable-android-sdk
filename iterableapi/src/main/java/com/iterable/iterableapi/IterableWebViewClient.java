package com.iterable.iterableapi;

import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Custom webViewClient which handles url clicks
 */
class IterableWebViewClient extends WebViewClient {
    static final String RESIZE_SCRIPT = "javascript:ITBL.resize(document.body.getBoundingClientRect().height)";

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
        view.setVisibility(View.GONE);
        return true;
    }

    /**
     * Resizes the view after the page has loaded
     * @param view
     * @param url
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        inAppHTMLNotification.setLoaded(true);
        view.loadUrl(RESIZE_SCRIPT);
    }

    interface HTMLNotificationCallbacks {
        void onUrlClicked(String url);
        void setLoaded(boolean loaded);
    }
}