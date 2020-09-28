package com.iterable.iterableapi;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Custom webViewClient which handles url clicks
 */
class IterableWebViewClient extends WebViewClient {
    enum Layout{
        TOP,
        BOTTOM,
        CENTER,
        FULLSCREEN
    }

    Layout inAppLayout;
    WebView htmlView;
    boolean shouldAnimate;
    static final String RESIZE_SCRIPT = "javascript:ITBL.resize(document.body.getBoundingClientRect().height)";

    HTMLNotificationCallbacks inAppHTMLNotification;

    IterableWebViewClient(HTMLNotificationCallbacks inAppHTMLNotification, boolean shouldAnimate) {
        this.inAppHTMLNotification = inAppHTMLNotification;
        this.shouldAnimate = shouldAnimate;
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
        IterableLogger.v("Progress", "Page Load Finished Triggered");
        htmlView = view;
        inAppHTMLNotification.setLoaded(true);
        IterableLogger.v("Progress", "Calling resize script from onpageload");
        view.loadUrl(RESIZE_SCRIPT);
    }

    void showWebView() {
        if (htmlView != null) {
            if(shouldAnimate) {
                animate(htmlView);
            }
            htmlView.setVisibility(View.VISIBLE);
        }
    }
    
    private void animate(final WebView view) {
        Animation anim = AnimationUtils.loadAnimation(IterableApi.getInstance().getMainActivityContext(),
                R.anim.fade_in_custom);
        view.startAnimation(anim);
    }

    interface HTMLNotificationCallbacks {
        void onUrlClicked(String url);
        void setLoaded(boolean loaded);
    }
}
