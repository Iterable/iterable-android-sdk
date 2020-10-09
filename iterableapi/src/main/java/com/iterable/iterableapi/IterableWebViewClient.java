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

    InAppLayout inAppLayout;
    WebView htmlView;
    boolean shouldAnimate;
    static final String RESIZE_SCRIPT = "javascript:ITBL.resize(document.body.getBoundingClientRect().height)";

    HTMLNotificationCallbacks inAppHTMLNotification;

    IterableWebViewClient(HTMLNotificationCallbacks inAppHTMLNotification, boolean shouldAnimate, InAppLayout layout) {
        this.inAppHTMLNotification = inAppHTMLNotification;
        this.shouldAnimate = shouldAnimate;
        this.inAppLayout = layout;
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
        htmlView = view;
        inAppHTMLNotification.setLoaded(true);
        view.loadUrl(RESIZE_SCRIPT);
    }

    void showWebView() {
        if (htmlView != null) {
            if (shouldAnimate) {
                animate(htmlView);
            } else {
                htmlView.setVisibility(View.VISIBLE);
            }
            //Might need this line.
            htmlView.setVisibility(View.VISIBLE);
        }
    }

    private void animate(final WebView view) {
        int animationResource;
        switch (inAppLayout) {
            case TOP:
                animationResource = R.anim.slide_down_custom;
                break;
            case CENTER:
            case FULLSCREEN:
                animationResource = R.anim.fade_in_custom;
                break;
            case BOTTOM:
                animationResource = R.anim.slide_up_custom;
                break;
            default:
                animationResource = R.anim.fade_in_custom;
        }
        Animation anim = AnimationUtils.loadAnimation(IterableApi.getInstance().getMainActivityContext(),
                animationResource);
        view.startAnimation(anim);
    }

    public void animateClose(final WebView view, Runnable parentViewDismissRunnable) {
        if (!shouldAnimate) {
            parentViewDismissRunnable.run();
            return;
        }

        int animationResource;
        switch (inAppLayout) {
            case TOP:
                animationResource = R.anim.top_exit;
                break;
            case CENTER:
            case FULLSCREEN:
                animationResource = R.anim.fade_out_custom;
                break;
            case BOTTOM:
                animationResource = R.anim.bottom_exit;
                break;
            default:
                animationResource = R.anim.fade_out_custom;
        }
        Animation anim = AnimationUtils.loadAnimation(IterableApi.getInstance().getMainActivityContext(),
                animationResource);
        view.startAnimation(anim);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.postOnAnimationDelayed(parentViewDismissRunnable, 400);
        } else {
            parentViewDismissRunnable.run();
        }
    }

    interface HTMLNotificationCallbacks {
        void onUrlClicked(String url);

        void setLoaded(boolean loaded);
    }

}

enum InAppLayout {
    TOP,
    BOTTOM,
    CENTER,
    FULLSCREEN
}