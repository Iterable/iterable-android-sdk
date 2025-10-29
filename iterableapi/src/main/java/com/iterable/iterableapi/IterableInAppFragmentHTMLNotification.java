package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

public class IterableInAppFragmentHTMLNotification extends DialogFragment implements IterableWebView.HTMLNotificationCallbacks {
    private static final String BACK_BUTTON = "itbl://backButton";
    private static final String TAG = "IterableInAppFragmentHTMLNotification";
    private static final String HTML_STRING = "HTML";
    private static final String BACKGROUND_ALPHA = "BackgroundAlpha";
    private static final String INSET_PADDING = "InsetPadding";
    private static final String CALLBACK_ON_CANCEL = "CallbackOnCancel";
    private static final String MESSAGE_ID = "MessageId";
    private static final String IN_APP_OPEN_TRACKED = "InAppOpenTracked";
    private static final String IN_APP_BG_ALPHA = "InAppBgAlpha";
    private static final String IN_APP_BG_COLOR = "InAppBgColor";
    private static final String IN_APP_SHOULD_ANIMATE = "ShouldAnimate";

    private static final int DELAY_THRESHOLD_MS = 500;

    @Nullable
    static IterableInAppFragmentHTMLNotification notification;
    @Nullable
    static IterableHelper.IterableUrlCallback clickCallback;
    @Nullable
    static IterableInAppLocation location;

    private IterableWebView webView;
    private boolean loaded;
    private OrientationEventListener orientationListener;
    private boolean callbackOnCancel = false;
    private String htmlString;
    private String messageId;

    // Resize debouncing fields
    private Handler resizeHandler = new Handler();
    private Runnable pendingResizeRunnable;
    private float lastContentHeight = -1;
    private static final int RESIZE_DEBOUNCE_DELAY_MS = 200;

    private double backgroundAlpha; //TODO: remove in a future version
    private Rect insetPadding;
    private boolean shouldAnimate;
    private double inAppBackgroundAlpha;
    private String inAppBackgroundColor;

    public static IterableInAppFragmentHTMLNotification createInstance(@NonNull String htmlString, boolean callbackOnCancel, @NonNull IterableHelper.IterableUrlCallback clickCallback, @NonNull IterableInAppLocation location, @NonNull String messageId, @NonNull Double backgroundAlpha, @NonNull Rect padding) {
        return IterableInAppFragmentHTMLNotification.createInstance(htmlString, callbackOnCancel, clickCallback, location, messageId, backgroundAlpha, padding, false, new IterableInAppMessage.InAppBgColor(null, 0.0f));
    }

    public static IterableInAppFragmentHTMLNotification createInstance(@NonNull String htmlString, boolean callbackOnCancel, @NonNull IterableHelper.IterableUrlCallback clickCallback, @NonNull IterableInAppLocation location, @NonNull String messageId, @NonNull Double backgroundAlpha, @NonNull Rect padding, @NonNull boolean shouldAnimate, IterableInAppMessage.InAppBgColor inAppBgColor) {
        notification = new IterableInAppFragmentHTMLNotification();
        Bundle args = new Bundle();
        args.putString(HTML_STRING, htmlString);
        args.putBoolean(CALLBACK_ON_CANCEL, callbackOnCancel);
        args.putString(MESSAGE_ID, messageId);
        args.putDouble(BACKGROUND_ALPHA, backgroundAlpha);
        args.putParcelable(INSET_PADDING, padding);
        args.putString(IN_APP_BG_COLOR, inAppBgColor.bgHexColor);
        args.putDouble(IN_APP_BG_ALPHA, inAppBgColor.bgAlpha);
        args.putBoolean(IN_APP_SHOULD_ANIMATE, shouldAnimate);

        IterableInAppFragmentHTMLNotification.clickCallback = clickCallback;
        IterableInAppFragmentHTMLNotification.location = location;
        notification.setArguments(args);
        return notification;
    }

    /**
     * Returns the notification instance currently being shown
     *
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
        this.setStyle(DialogFragment.STYLE_NO_FRAME, androidx.appcompat.R.style.Theme_AppCompat_NoActionBar);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set dialog positioning after the dialog is created and shown
        Dialog dialog = getDialog();
        if (dialog != null) {
            applyWindowGravity(dialog.getWindow(), "onStart");
        }
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
            inAppBackgroundAlpha = args.getDouble(IN_APP_BG_ALPHA);
            inAppBackgroundColor = args.getString(IN_APP_BG_COLOR, null);
            shouldAnimate = args.getBoolean(IN_APP_SHOULD_ANIMATE);
        }

        notification = this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                IterableInAppFragmentHTMLNotification.this.onBackPressed();
                hideWebView();
            }
        };
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (callbackOnCancel && clickCallback != null) {
                    clickCallback.execute(null);
                }
            }
        });
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set window gravity for the dialog
        applyWindowGravity(dialog.getWindow(), "onCreateDialog");

        if (getInAppLayout(insetPadding) == InAppLayout.FULLSCREEN) {
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else if (getInAppLayout(insetPadding) != InAppLayout.TOP) {
            // For TOP layout in-app, status bar will be opaque so that the in-app content does not overlap with translucent status bar.
            // For other non-fullscreen in-apps layouts (BOTTOM and CENTER), status bar will be translucent
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (getInAppLayout(insetPadding) == InAppLayout.FULLSCREEN) {
            getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Set initial window gravity based on inset padding
        applyWindowGravity(getDialog().getWindow(), "onCreateView");

        webView = new IterableWebView(getContext());
        webView.setId(R.id.webView);

        // Debug the HTML content
        IterableLogger.d(TAG, "HTML content preview: " + (htmlString.length() > 200 ? htmlString.substring(0, 200) + "..." : htmlString));

        webView.createWithHtml(this, htmlString);

        if (orientationListener == null) {
            orientationListener = new OrientationEventListener(getContext(), SensorManager.SENSOR_DELAY_NORMAL) {
                private int lastOrientation = -1;

                // Resize the webView on device rotation
                public void onOrientationChanged(int orientation) {
                    if (loaded && webView != null) {
                        // Only trigger on significant orientation changes (90 degree increments)
                        int currentOrientation = ((orientation + 45) / 90) * 90;
                        if (currentOrientation != lastOrientation && lastOrientation != -1) {
                            lastOrientation = currentOrientation;

                            // Use longer delay for orientation changes to allow layout to stabilize
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    IterableLogger.d(TAG, "Orientation changed, triggering resize");
                                    runResizeScript();
                                }
                            }, 1500); // Increased delay for better stability
                        } else if (lastOrientation == -1) {
                            lastOrientation = currentOrientation;
                        }
                    }
                }
            };
        }

        orientationListener.enable();

        // Create a FrameLayout as the main container for better positioning control
        FrameLayout frameLayout = new FrameLayout(this.getContext());

        // Create a RelativeLayout as a wrapper for the WebView
        RelativeLayout webViewContainer = new RelativeLayout(this.getContext());

        int gravity = getVerticalLocation(insetPadding);
        IterableLogger.d(TAG, "Initial setup - gravity: " + gravity + " for inset padding: " + insetPadding);

        // Set FrameLayout gravity based on positioning
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );

        if (gravity == Gravity.CENTER_VERTICAL) {
            containerParams.gravity = Gravity.CENTER;
            IterableLogger.d(TAG, "Applied CENTER gravity to container");
        } else if (gravity == Gravity.TOP) {
            containerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            IterableLogger.d(TAG, "Applied TOP gravity to container");
        } else if (gravity == Gravity.BOTTOM) {
            containerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            IterableLogger.d(TAG, "Applied BOTTOM gravity to container");
        }

        // Add WebView to the RelativeLayout container with WRAP_CONTENT for proper sizing
        RelativeLayout.LayoutParams webViewParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        webViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        webViewContainer.addView(webView, webViewParams);

        IterableLogger.d(TAG, "Added WebView with WRAP_CONTENT and CENTER_IN_PARENT rule");

        // Add the container to the FrameLayout
        frameLayout.addView(webViewContainer, containerParams);

        IterableLogger.d(TAG, "Created FrameLayout with positioned RelativeLayout container");

        if (savedInstanceState == null || !savedInstanceState.getBoolean(IN_APP_OPEN_TRACKED, false)) {
            IterableApi.sharedInstance.trackInAppOpen(messageId, location);
        }

        prepareToShowWebView();
        return frameLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Handle edge-to-edge insets with modern approach
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, sysBars.top, 0, sysBars.bottom);
            return insets;
        });
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    /**
     * Sets up the webView and the dialog layout
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IN_APP_OPEN_TRACKED, true);
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
    public void onDestroy() {
        super.onDestroy();

        // Clean up pending resize operations
        if (resizeHandler != null && pendingResizeRunnable != null) {
            resizeHandler.removeCallbacks(pendingResizeRunnable);
            pendingResizeRunnable = null;
        }

        if (this.getActivity() != null && this.getActivity().isChangingConfigurations()) {
            return;
        }

        notification = null;
        clickCallback = null;
        location = null;
    }

    @Override
    public void onUrlClicked(String url) {
        IterableApi.sharedInstance.trackInAppClick(messageId, url, location);
        IterableApi.sharedInstance.trackInAppClose(messageId, url, IterableInAppCloseAction.LINK, location);

        if (clickCallback != null) {
            clickCallback.execute(Uri.parse(url));
        }

        processMessageRemoval();
        hideWebView();
    }

    /**
     * Tracks a button click when the back button is pressed
     */
    public void onBackPressed() {
        IterableApi.sharedInstance.trackInAppClick(messageId, BACK_BUTTON);
        IterableApi.sharedInstance.trackInAppClose(messageId, BACK_BUTTON, IterableInAppCloseAction.BACK, location);

        processMessageRemoval();
    }

    private void prepareToShowWebView() {
        try {
            webView.setAlpha(0.0f);
            webView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getContext() != null && getDialog() != null && getDialog().getWindow() != null) {
                        showInAppBackground();
                        showAndAnimateWebView();
                    }
                }
            }, DELAY_THRESHOLD_MS);
        } catch (NullPointerException e) {
            IterableLogger.e(TAG, "View not present. Failed to hide before resizing inapp");
        }
    }

    private void showInAppBackground() {
        animateBackground(new ColorDrawable(Color.TRANSPARENT), getInAppBackgroundDrawable());
    }

    private void hideInAppBackground() {
        animateBackground(getInAppBackgroundDrawable(), new ColorDrawable(Color.TRANSPARENT));
    }

    private void animateBackground(Drawable from, Drawable to) {
        if (from == null || to == null) {
            return;
        }

        if (getDialog() == null || getDialog().getWindow() == null) {
            IterableLogger.e(TAG, "Dialog or Window not present. Skipping background animation");
            return;
        }

        Drawable[] layers = new Drawable[2];
        layers[0] = from;
        layers[1] = to;
        TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
        transitionDrawable.setCrossFadeEnabled(true);
        getDialog().getWindow().setBackgroundDrawable(transitionDrawable);
        transitionDrawable.startTransition(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ANIMATION_DURATION);
    }

    private ColorDrawable getInAppBackgroundDrawable() {
        if (inAppBackgroundColor == null) {
            IterableLogger.d(TAG, "Background Color does not exist. In App background animation will not be performed");
            return null;
        }

        int backgroundColorWithAlpha;

        try {
            backgroundColorWithAlpha = ColorUtils.setAlphaComponent(Color.parseColor(inAppBackgroundColor), (int) (inAppBackgroundAlpha * 255));
        } catch (IllegalArgumentException e) {
            IterableLogger.e(TAG, "Background color could not be identified for input string \"" + inAppBackgroundColor + "\". Failed to load in-app background.");
            return null;
        }

        ColorDrawable backgroundColorDrawable = new ColorDrawable(backgroundColorWithAlpha);
        return backgroundColorDrawable;
    }

    private void showAndAnimateWebView() {
        webView.setAlpha(1.0f);
        webView.setVisibility(View.VISIBLE);

        if (shouldAnimate) {
            int animationResource;
            InAppLayout inAppLayout = getInAppLayout(insetPadding);
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
            try {
                Animation anim = AnimationUtils.loadAnimation(getContext(), animationResource);
                anim.setDuration(IterableConstants.ITERABLE_IN_APP_ANIMATION_DURATION);
                webView.startAnimation(anim);
            } catch (Exception e) {
                IterableLogger.e(TAG, "Failed to show inapp with animation");
            }
        }
    }

    private void hideWebView() {
        if (shouldAnimate) {
            int animationResource;
            InAppLayout inAppLayout = getInAppLayout(insetPadding);

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

            try {
                Animation anim = AnimationUtils.loadAnimation(getContext(),
                    animationResource);
                anim.setDuration(IterableConstants.ITERABLE_IN_APP_ANIMATION_DURATION);
                webView.startAnimation(anim);
            } catch (Exception e) {
                IterableLogger.e(TAG, "Failed to hide inapp with animation");
            }

        }

        hideInAppBackground();
        Runnable dismissWebViewRunnable = new Runnable() {
            @Override
            public void run() {
                if (getContext() != null && getDialog() != null && getDialog().getWindow() != null) {
                    dismissAllowingStateLoss();
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.postOnAnimationDelayed(dismissWebViewRunnable, 400);
        } else {
            webView.postDelayed(dismissWebViewRunnable, 400);
        }
    }

    private void processMessageRemoval() {
        IterableInAppMessage message = IterableApi.sharedInstance.getInAppManager().getMessageById(messageId);
        if (message == null) {
            IterableLogger.e(TAG, "Message with id " + messageId + " does not exist");
            return;
        }

        if (message.isMarkedForDeletion() && !message.isConsumed()) {
            IterableApi.sharedInstance.getInAppManager().removeMessage(message, null, null);
        }
    }

    @Override
    public void runResizeScript() {
        // Cancel any pending resize operation
        if (pendingResizeRunnable != null) {
            resizeHandler.removeCallbacks(pendingResizeRunnable);
        }

        // Schedule a debounced resize operation
        pendingResizeRunnable = new Runnable() {
            @Override
            public void run() {
                performResizeWithValidation();
            }
        };

        resizeHandler.postDelayed(pendingResizeRunnable, RESIZE_DEBOUNCE_DELAY_MS);
    }

    private void performResizeWithValidation() {
        if (webView == null) {
            IterableLogger.w(TAG, "WebView is null, skipping resize");
            return;
        }

        float currentHeight = webView.getContentHeight();

        // Validate content height
        if (currentHeight <= 0) {
            IterableLogger.w(TAG, "Invalid content height: " + currentHeight + "dp, skipping resize");
            return;
        }

        // Check if height has stabilized (avoid unnecessary resizes for same height)
        if (Math.abs(currentHeight - lastContentHeight) < 1.0f) {
            IterableLogger.d(TAG, "Content height unchanged (" + currentHeight + "dp), skipping resize");
            return;
        }

        lastContentHeight = currentHeight;

        IterableLogger.d(
            TAG,
            "💚 Resizing in-app to height: " + currentHeight + "dp"
        );

        resize(currentHeight);
    }

    /**
     * Resizes the dialog window based upon the size of its webView HTML content
     *
     * @param height
     */
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
                    if (getContext() == null || notification == null || notification.getDialog() == null ||
                        notification.getDialog().getWindow() == null || !notification.getDialog().isShowing()) {
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
                        // Resize the WebView directly with explicit size
                        float relativeHeight = height * getResources().getDisplayMetrics().density;
                        int newWebViewWidth = getResources().getDisplayMetrics().widthPixels;
                        int newWebViewHeight = (int) relativeHeight;

                        // Set WebView to explicit size
                        RelativeLayout.LayoutParams webViewParams = new RelativeLayout.LayoutParams(newWebViewWidth, newWebViewHeight);

                        // Apply positioning based on gravity
                        int resizeGravity = getVerticalLocation(insetPadding);
                        IterableLogger.d(TAG, "Resizing WebView directly - gravity: " + resizeGravity + " size: " + newWebViewWidth + "x" + newWebViewHeight + "px for inset padding: " + insetPadding);

                        if (resizeGravity == Gravity.CENTER_VERTICAL) {
                            webViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                            IterableLogger.d(TAG, "Applied CENTER_IN_PARENT to WebView");
                        } else if (resizeGravity == Gravity.TOP) {
                            webViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                            webViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                            IterableLogger.d(TAG, "Applied TOP alignment to WebView");
                        } else if (resizeGravity == Gravity.BOTTOM) {
                            webViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                            webViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                            IterableLogger.d(TAG, "Applied BOTTOM alignment to WebView");
                        }

                        // Make dialog full screen to allow proper positioning
                        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

                        // Apply the new layout params to WebView
                        webView.setLayoutParams(webViewParams);

                        // Force layout updates
                        webView.requestLayout();
                        if (webView.getParent() instanceof ViewGroup) {
                            ((ViewGroup) webView.getParent()).requestLayout();
                        }

                        IterableLogger.d(TAG, "Applied explicit size and positioning to WebView: " + newWebViewWidth + "x" + newWebViewHeight);
                    }
                } catch (IllegalArgumentException e) {
                    IterableLogger.e(TAG, "Exception while trying to resize an in-app message", e);
                }
            }
        });
    }

    /**
     * Returns the vertical position of the dialog for the given padding
     *
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

    /**
     * Sets the window gravity based on inset padding
     *
     * @param window  The dialog window to configure
     * @param context Debug context string for logging
     */
    private void applyWindowGravity(Window window, String context) {
        if (window == null) {
            return;
        }

        WindowManager.LayoutParams windowParams = window.getAttributes();
        int gravity = getVerticalLocation(insetPadding);

        if (gravity == Gravity.CENTER_VERTICAL) {
            windowParams.gravity = Gravity.CENTER;
        } else if (gravity == Gravity.TOP) {
            windowParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        } else if (gravity == Gravity.BOTTOM) {
            windowParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        }

        window.setAttributes(windowParams);
        IterableLogger.d(TAG, "Set window gravity in " + context + ": " + windowParams.gravity);
    }

    InAppLayout getInAppLayout(Rect padding) {
        if (padding.top == 0 && padding.bottom == 0) {
            return InAppLayout.FULLSCREEN;
        } else if (padding.top == 0 && padding.bottom < 0) {
            return InAppLayout.TOP;
        } else if (padding.top < 0 && padding.bottom == 0) {
            return InAppLayout.BOTTOM;
        } else {
            return InAppLayout.CENTER;
        }
    }
}

enum InAppLayout {
    TOP,
    BOTTOM,
    CENTER,
    FULLSCREEN
}
