package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class IterableInAppHTMLNotification extends Dialog implements IterableWebViewClient.HTMLNotificationCallbacks {

    static final String BACK_BUTTON = "itbl://backButton";
    static final String JAVASCRIPT_INTERFACE = "ITBL";
    private static final String TAG = "IterableInAppHTMLNotification";

    static IterableInAppHTMLNotification notification;

    Context context;
    IterableWebView webView;
    Boolean loaded;
    OrientationEventListener orientationListener;
    String htmlString;
    String messageId;
    double backgroundAlpha;
    Rect insetPadding;
    IterableHelper.IterableUrlCallback clickCallback;
    IterableInAppLocation location;

    /**
     * Creates a static instance of the notification
     * @param context
     * @param htmlString
     * @return
     */
    @NonNull
    public static IterableInAppHTMLNotification createInstance(@NonNull Context context, @NonNull String htmlString) {
        notification = new IterableInAppHTMLNotification(context, htmlString);
        return notification;
    }

    /**
     * Returns the notification instance currently being shown
     * @return notification instance
     */
    @Nullable
    public static IterableInAppHTMLNotification getInstance() {
        return notification;
    }

    /**
     * HTML In-App Notification
     * @param context
     * @param htmlString
     */
    private IterableInAppHTMLNotification(@NonNull Context context, @NonNull String htmlString) {
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
    public void setTrackParams(@NonNull String messageId) {
        this.messageId = messageId;
    }

    /**
     * Sets the clickCallback
     * @param clickCallback
     */
    public void setCallback(@NonNull IterableHelper.IterableUrlCallback clickCallback) {
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
    public void setPadding(@NonNull Rect insetPadding) {
        this.insetPadding = insetPadding;
    }

    public void setLocation(@NonNull IterableInAppLocation location) {
        this.location = location;
    }

    /**
     * Tracks a button click when the back button is pressed
     */
    @Override
    public void onBackPressed() {
        IterableApi.sharedInstance.trackInAppClick(messageId, BACK_BUTTON, location);
        IterableApi.sharedInstance.trackInAppClose(messageId, BACK_BUTTON, IterableInAppCloseAction.BACK, location);
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
        webView.setId(R.id.webView);
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
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        relativeLayout.addView(webView, layoutParams);
        setContentView(relativeLayout, layoutParams);

        IterableApi.sharedInstance.trackInAppOpen(messageId, location);
    }

    /**
     * On Stop of the dialog
     */
    @Override
    protected void onStop() {
        orientationListener.disable();
        notification = null;
    }

    @Override
    public void onUrlClicked(@NonNull String url) {
        IterableApi.sharedInstance.trackInAppClick(messageId, url, location);
        IterableApi.sharedInstance.trackInAppClose(messageId, url, IterableInAppCloseAction.LINK, location);
        clickCallback.execute(Uri.parse(url));
        dismiss();
    }

    /**
     * Resizes the dialog window based upon the size of its webview html content
     * @param height
     */
    @JavascriptInterface
    public void resize(final float height) {
        final Activity activity = getOwnerActivity();
        if (activity == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Since this is run asynchronously, notification might've been dismissed already
                    if (notification == null || notification.getWindow() == null || !notification.isShowing()) {
                        return;
                    }

                    DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
                    Window window = notification.getWindow();
                    Rect insetPadding = notification.insetPadding;

                    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Point size = new Point();

                    // getSize gives us the screen size minus the navigation bar, which is what we want
                    display.getSize(size);
                    int webViewWidth = size.x;
                    int webViewHeight = size.y;

                    //Check if the dialog is full screen
                    if (insetPadding.bottom == 0 && insetPadding.top == 0) {
                        //Handle full screen
                        window.setLayout(webViewWidth, webViewHeight);

                        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    } else {
                        // Calculates the dialog size
                        double notificationWidth = 100 - (insetPadding.left + insetPadding.right);
                        float widthPercentage = (float) notificationWidth / 100;
                        int maxHeight = Math.min((int) (height * displayMetrics.scaledDensity), webViewHeight);
                        int maxWidth = Math.min(webViewWidth, (int) (webViewWidth * widthPercentage));
                        window.setLayout(maxWidth, maxHeight);

                        //Calculates the horizontal position based on the dialog size
                        double center = (insetPadding.left + notificationWidth / 2f);
                        int offset = (int) ((center - 50) / 100f * webViewWidth);

                        //Set the window properties
                        WindowManager.LayoutParams wlp = window.getAttributes();
                        wlp.x = offset;
                        wlp.gravity = getVerticalLocation(insetPadding);
                        wlp.dimAmount = (float) notification.backgroundAlpha;
                        wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                        window.setAttributes(wlp);
                    }
                } catch (IllegalArgumentException e) {
                    IterableLogger.e(TAG, "Exception while trying to resize an in-app message", e);
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