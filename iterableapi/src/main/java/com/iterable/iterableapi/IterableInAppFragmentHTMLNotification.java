package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class IterableInAppFragmentHTMLNotification extends DialogFragment implements IterableWebViewClient.HTMLNotificationCallbacks {

    private static final String BACK_BUTTON = "itbl://backButton";
    private static final String JAVASCRIPT_INTERFACE = "ITBL";
    private static final String TAG = "IterableInAppFragmentHTMLNotification";
    private static final String HTML_STRING = "HTML";
    private static final String BACKGROUND_ALPHA = "BackgroundAlpha";
    private static final String INSET_PADDING = "InsetPadding";
    private static final String CALLBACK_ON_CANCEL = "CallbackOnCancel";
    private static final String MESSAGE_ID = "MessageId";
    private static final String INAPP_OPEN_TRACKED = "InAppOpenTracked";

    static IterableInAppFragmentHTMLNotification notification;
    static IterableHelper.IterableUrlCallback clickCallback;
    static IterableInAppLocation location;

    private IterableWebView webView;
    private boolean loaded;
    private OrientationEventListener orientationListener;
    private boolean callbackOnCancel = false;
    private String htmlString;
    private String messageId;
    private double backgroundAlpha;
    private Rect insetPadding;

    /**
     * Creates a static instance of the notification
     * @param context
     * @param htmlString
     * @return
     */
    public static IterableInAppFragmentHTMLNotification createInstance(String htmlString, boolean callbackOnCancel, IterableHelper.IterableUrlCallback clickCallback, IterableInAppLocation location, String messageId, Double backgroundAlpha, Rect padding) {

        notification = new IterableInAppFragmentHTMLNotification();
        Bundle args = new Bundle();
        args.putString(HTML_STRING, htmlString);
        args.putBoolean(CALLBACK_ON_CANCEL, callbackOnCancel);
        args.putString(MESSAGE_ID, messageId);
        args.putDouble(BACKGROUND_ALPHA, backgroundAlpha);
        args.putParcelable(INSET_PADDING, padding);
        IterableInAppFragmentHTMLNotification.clickCallback = clickCallback;
        IterableInAppFragmentHTMLNotification.location = location;
        notification.setArguments(args);
        return notification;
    }

    /**
     * Returns the notification instance currently being shown
     * @return notification instance
     */
    public static IterableInAppFragmentHTMLNotification getInstance() {
        return notification;
    }

    /**
     * HTML In-App Notification
     */
    public IterableInAppFragmentHTMLNotification() {
        this.loaded = false;
        this.backgroundAlpha = 0;
        this.messageId = "";
        insetPadding = new Rect();
        this.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_AppCompat_NoActionBar);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            htmlString = args.getString(HTML_STRING, null);
            callbackOnCancel = args.getBoolean(CALLBACK_ON_CANCEL, false);
            messageId = args.getString(MESSAGE_ID);
            backgroundAlpha = args.getDouble(BACKGROUND_ALPHA);
            insetPadding = args.getParcelable(INSET_PADDING);
        }
        notification = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.getActivity() != null && this.getActivity().isChangingConfigurations()) {
            return;
        }
        notification = null;
        clickCallback = null;
        location = null;
    }


    /**
     * Sets the loaded flag
     * @param loaded
     */
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), getTheme()){
            @Override
            public void onBackPressed() {
                IterableInAppFragmentHTMLNotification.this.onBackPressed();
                super.onBackPressed();
            }
        };
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override public void onCancel(DialogInterface dialog) {
                if (callbackOnCancel) {
                    clickCallback.execute(null);
                }
            }
        });
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return dialog;
    }

    /**
     * Tracks a button click when the back button is pressed
     */
    public void onBackPressed() {
        IterableApi.sharedInstance.trackInAppClick(messageId, BACK_BUTTON);
        IterableApi.sharedInstance.trackInAppClose(messageId, BACK_BUTTON, IterableInAppCloseAction.BACK, location);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        webView = new IterableWebView(getContext());
        webView.setId(R.id.webView);
        webView.createWithHtml(this, htmlString);
        webView.addJavascriptInterface(this, JAVASCRIPT_INTERFACE);

        if (orientationListener == null) {
            orientationListener = new OrientationEventListener(getContext(), SensorManager.SENSOR_DELAY_NORMAL) {
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
        relativeLayout.addView(webView, layoutParams);

        if (savedInstanceState == null || !savedInstanceState.getBoolean(INAPP_OPEN_TRACKED, false)) {
            IterableApi.sharedInstance.trackInAppOpen(messageId, location);
        }
        return relativeLayout;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(INAPP_OPEN_TRACKED, true);
    }

    /**
     * Sets up the webview and the dialog layout
     */
    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /**
     * On Stop of the dialog
     */
    @Override
    public void onStop() {
        orientationListener.disable();
        super.onStop();
    }

    @Override
    public void onUrlClicked(String url) {
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
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Since this is run asynchronously, notification might've been dismissed already
                    if (notification == null || notification.getDialog() == null || notification.getDialog().getWindow() == null || !notification.getDialog().isShowing()) {
                        return;
                    }

                    DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
                    Window window = notification.getDialog().getWindow();
                    Rect insetPadding = notification.insetPadding;

                    WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
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

                        getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
