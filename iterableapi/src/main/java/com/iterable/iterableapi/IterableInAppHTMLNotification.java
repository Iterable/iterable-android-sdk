package com.iterable.iterableapi;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

/**
 * Created by David Truong dt@iterable.com.
 */

public class IterableInAppHTMLNotification extends Dialog {

    static final String BACK_BUTTON = "itbl://backButton";
    static final String JAVASCRIPT_INTERFACE = "ITBL";

    static IterableInAppHTMLNotification notification;

    Context context;
    IterableWebView webView;
    Boolean loaded;
    OrientationEventListener orientationListener;
    String htmlString;
    String messageId;
    double backgroundAlpha;
    Rect insetPadding;
    IterableHelper.IterableActionHandler clickCallback;

    /**
     * Creates a static instance of the notification
     * @param context
     * @param htmlString
     * @return
     */
    public static IterableInAppHTMLNotification createInstance(Context context, String htmlString) {
        notification = new IterableInAppHTMLNotification(context, htmlString);
        return notification;
    }

    /**
     * Returns the notification instance currently being shown
     * @return notification instance
     */
    public static IterableInAppHTMLNotification getInstance() {
        return notification;
    }

    /**
     * HTML In-App Notification
     * @param context
     * @param htmlString
     */
    private IterableInAppHTMLNotification(Context context, String htmlString) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);

        this.context = context;
        this.htmlString = htmlString;
        this.loaded = false;
        this.backgroundAlpha = 0;
        this.messageId = "";
        insetPadding = new Rect();
    }

    /**
     * Sets the trackParams
     * @param messageId
     */
    public void setTrackParams(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Sets the clickCallback
     * @param clickCallback
     */
    public void setCallback(IterableHelper.IterableActionHandler clickCallback) {
        this.clickCallback = clickCallback;
    }

    /**
     * Sets the loaded flag
     * @param loaded
     */
    public void setLoaded(Boolean loaded) {
        this.loaded = loaded;
    }

    /**
     * Sets the backgroundAlpha
     * @param backgroundAlpha
     */
    public void setBackgroundAlpha(double backgroundAlpha) {
        this.backgroundAlpha = backgroundAlpha;
    }

    /**
     * Sets the padding
     * @param insetPadding
     */
    public void setPadding(Rect insetPadding) {
        this.insetPadding = insetPadding;
    }

    /**
     * Tracks a button click when the back button is pressed
     */
    @Override
    public void onBackPressed() {
        IterableApi.sharedInstance.trackInAppClick(messageId, BACK_BUTTON);

        super.onBackPressed();
    }

    /**
     * Sets up the webview and the dialog layout
     */
    @Override
    protected void onStart() {
        super.onStart();
        this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        webView = new IterableWebView(context);
        webView.createWithHtml(this, htmlString);
        webView.addJavascriptInterface(this, JAVASCRIPT_INTERFACE);

        if (orientationListener == null) {
            orientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                public void onOrientationChanged(int orientation) {
                    // Resize the webview on device rotation
                    if (loaded) {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl(IterableWebViewClient.resizeScript);
                            }
                        }, 1000);
                    }
                }
            };
        }
        orientationListener.enable();

        RelativeLayout relativeLayout = new RelativeLayout(this.getContext());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
        relativeLayout.addView(webView,layoutParams);
        setContentView(relativeLayout,layoutParams);

        IterableApi.sharedInstance.trackInAppOpen(messageId);
    }

    /**
     * On Stop of the dialog
     */
    @Override
    protected void onStop() {
        orientationListener.disable();
        notification = null;
    }

    /**
     * Resizes the dialog window based upon the size of its webview html content
     * @param height
     */
    @JavascriptInterface
    public void resize(final float height) {
        getOwnerActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics displayMetrics = getOwnerActivity().getResources().getDisplayMetrics();
                Window window = notification.getWindow();
                Rect insetPadding = notification.insetPadding;

                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();

                // Get the correct screen size based on api level
                // https://stackoverflow.com/questions/35780980/getting-the-actual-screen-height-android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    display.getRealSize(size);
                } else {
                    display.getSize(size);
                }
                int webViewWidth = size.x;
                int webViewHeight = size.y;

                //Check if the dialog is full screen
                if (insetPadding.bottom == 0 && insetPadding.top == 0) {
                    //Handle full screen
                    window.setLayout(webViewWidth, webViewHeight);

                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
                } else {
                    // Calculates the dialog size
                    double notificationWidth = 100-(insetPadding.left +insetPadding.right);
                    float widthPercentage = (float) notificationWidth/100;
                    int maxHeight = Math.min((int) (height * displayMetrics.scaledDensity), webViewHeight);
                    int maxWidth = Math.min(webViewWidth, (int) (webViewWidth * widthPercentage));
                    window.setLayout(maxWidth, maxHeight);

                    //Calculates the horizontal position based on the dialog size
                    double center = (insetPadding.left + notificationWidth/2f);
                    int offset = (int) ((center - 50)/100f * webViewWidth);

                    //Set the window properties
                    WindowManager.LayoutParams wlp = window.getAttributes();
                    wlp.x = offset;
                    wlp.gravity = getVerticalLocation(insetPadding);
                    wlp.dimAmount = (float) notification.backgroundAlpha;
                    wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    window.setAttributes(wlp);
                }
            }
        });
    }

    /**
     * Returns the vertical position of the dialog for the given padding
     * @param padding
     * @return
     */
    int getVerticalLocation(Rect padding) {
        int gravity = Gravity.CENTER_VERTICAL;
        if (padding.top == 0 && padding.bottom < 0) {
            gravity = Gravity.TOP;
        } else if (padding.top < 0 && padding.bottom == 0) {
            gravity = Gravity.BOTTOM;
        }
        return gravity;
    }
}

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
    void createWithHtml(IterableInAppHTMLNotification notificationDialog, String html) {
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

/**
 * Custom webViewClient which handles url clicks
 */
class IterableWebViewClient extends WebViewClient {
    static final String resizeScript = "javascript:ITBL.resize(document.body.getBoundingClientRect().height)";
    static final String itblUrlScheme = "itbl://";

    IterableInAppHTMLNotification inAppHTMLNotification;

    IterableWebViewClient(IterableInAppHTMLNotification inAppHTMLNotification) {
        this.inAppHTMLNotification = inAppHTMLNotification;
    }

    /**
     * Handles url clicks
     * @param view
     * @param url
     * @return
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView  view, String  url) {
        String callbackURL = url;

        //Removes the itbl:// scheme from the callbackUrl
        if (url.startsWith(itblUrlScheme)) {
            callbackURL = url.replace(itblUrlScheme, "");
        }

        IterableApi.sharedInstance.trackInAppClick(inAppHTMLNotification.messageId, url);
        inAppHTMLNotification.clickCallback.execute(callbackURL);
        inAppHTMLNotification.dismiss();

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
        view.loadUrl(resizeScript);
    }
}