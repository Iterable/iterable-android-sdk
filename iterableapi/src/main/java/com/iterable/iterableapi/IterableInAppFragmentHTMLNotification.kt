package com.iterable.iterableapi

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.DialogFragment

class IterableInAppFragmentHTMLNotification : DialogFragment(), IterableWebView.HTMLNotificationCallbacks {

    private lateinit var webView: IterableWebView
    private var loaded = false
    private var orientationListener: OrientationEventListener? = null
    private var callbackOnCancel = false
    private var htmlString: String? = null
    private var messageId = ""

    private var backgroundAlpha = 0.0 //TODO: remove in a future version
    private lateinit var insetPadding: Rect
    private var shouldAnimate = false
    private var inAppBackgroundAlpha = 0.0
    private var inAppBackgroundColor: String? = null

    /**
     * HTML In-App Notification
     */
    init {
        this.loaded = false
        this.backgroundAlpha = 0.0
        this.messageId = ""
        insetPadding = Rect()
        this.setStyle(STYLE_NO_FRAME, androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments

        if (args != null) {
            htmlString = args.getString(HTML_STRING, null)
            callbackOnCancel = args.getBoolean(CALLBACK_ON_CANCEL, false)
            messageId = args.getString(MESSAGE_ID) ?: ""
            backgroundAlpha = args.getDouble(BACKGROUND_ALPHA)
            insetPadding = args.getParcelable(INSET_PADDING) ?: Rect()
            inAppBackgroundAlpha = args.getDouble(IN_APP_BG_ALPHA)
            inAppBackgroundColor = args.getString(IN_APP_BG_COLOR, null)
            shouldAnimate = args.getBoolean(IN_APP_SHOULD_ANIMATE)
        }

        notification = this
    }

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                this@IterableInAppFragmentHTMLNotification.onBackPressed()
                hideWebView()
            }
        }
        dialog.setOnCancelListener { _ ->
            if (callbackOnCancel && clickCallback != null) {
                clickCallback!!.execute(null)
            }
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (getInAppLayout(insetPadding) == InAppLayout.FULLSCREEN) {
            dialog.window!!.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else if (getInAppLayout(insetPadding) != InAppLayout.TOP) {
            // For TOP layout in-app, status bar will be opaque so that the in-app content does not overlap with translucent status bar.
            // For other non-fullscreen in-apps layouts (BOTTOM and CENTER), status bar will be translucent
            dialog.window!!.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        return dialog
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        requireDialog().window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if (getInAppLayout(insetPadding) == InAppLayout.FULLSCREEN) {
            requireDialog().window!!.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        webView = IterableWebView(requireContext())
        webView.id = R.id.webView
        webView.createWithHtml(this, htmlString)

        if (orientationListener == null) {
            orientationListener = object : OrientationEventListener(requireContext(), SensorManager.SENSOR_DELAY_NORMAL) {
                // Resize the webView on device rotation
                override fun onOrientationChanged(orientation: Int) {
                    if (loaded) {
                        val handler = Handler()
                        handler.postDelayed({
                            runResizeScript()
                        }, 1000)
                    }
                }
            }
        }

        orientationListener!!.enable()

        val relativeLayout = RelativeLayout(requireContext())
        val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        relativeLayout.gravity = getVerticalLocation(insetPadding)
        relativeLayout.addView(webView, layoutParams)

        if (savedInstanceState == null || !savedInstanceState.getBoolean(IN_APP_OPEN_TRACKED, false)) {
            IterableApi.sharedInstance.trackInAppOpen(messageId, location!!)
        }

        prepareToShowWebView()
        return relativeLayout
    }

    override fun setLoaded(loaded: Boolean) {
        this.loaded = loaded
    }

    /**
     * Sets up the webView and the dialog layout
     */
    override fun onSaveInstanceState(@NonNull outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IN_APP_OPEN_TRACKED, true)
    }

    /**
     * On Stop of the dialog
     */
    override fun onStop() {
        orientationListener?.disable()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (activity?.isChangingConfigurations == true) {
            return
        }

        notification = null
        clickCallback = null
        location = null
    }

    override fun onUrlClicked(url: String) {
        IterableApi.sharedInstance.trackInAppClick(messageId, url, location!!)
        IterableApi.sharedInstance.trackInAppClose(messageId, url, IterableInAppCloseAction.LINK, location!!)

        if (clickCallback != null) {
            clickCallback!!.execute(Uri.parse(url))
        }

        processMessageRemoval()
        hideWebView()
    }

    /**
     * Tracks a button click when the back button is pressed
     */
    fun onBackPressed() {
        IterableApi.sharedInstance.trackInAppClick(messageId, BACK_BUTTON)
        IterableApi.sharedInstance.trackInAppClose(messageId, BACK_BUTTON, IterableInAppCloseAction.BACK, location!!)

        processMessageRemoval()
    }

    private fun prepareToShowWebView() {
        try {
            webView.alpha = 0.0f
            webView.postDelayed({
                if (context != null && dialog != null && dialog!!.window != null) {
                    showInAppBackground()
                    showAndAnimateWebView()
                }
            }, DELAY_THRESHOLD_MS.toLong())
        } catch (e: NullPointerException) {
            IterableLogger.e(TAG, "View not present. Failed to hide before resizing inapp")
        }
    }

    private fun showInAppBackground() {
        animateBackground(ColorDrawable(Color.TRANSPARENT), getInAppBackgroundDrawable())
    }

    private fun hideInAppBackground() {
        animateBackground(getInAppBackgroundDrawable(), ColorDrawable(Color.TRANSPARENT))
    }

    private fun animateBackground(from: Drawable?, to: Drawable?) {
        if (from == null || to == null) {
            return
        }

        if (dialog == null || dialog!!.window == null) {
            IterableLogger.e(TAG, "Dialog or Window not present. Skipping background animation")
            return
        }

        val layers = arrayOfNulls<Drawable>(2)
        layers[0] = from
        layers[1] = to
        val transitionDrawable = TransitionDrawable(layers)
        transitionDrawable.isCrossFadeEnabled = true
        dialog!!.window!!.setBackgroundDrawable(transitionDrawable)
        transitionDrawable.startTransition(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ANIMATION_DURATION)
    }

    private fun getInAppBackgroundDrawable(): ColorDrawable? {
        if (inAppBackgroundColor == null) {
            IterableLogger.d(TAG, "Background Color does not exist. In App background animation will not be performed")
            return null
        }

        val backgroundColorWithAlpha: Int

        try {
            backgroundColorWithAlpha = ColorUtils.setAlphaComponent(Color.parseColor(inAppBackgroundColor), (inAppBackgroundAlpha * 255).toInt())
        } catch (e: IllegalArgumentException) {
            IterableLogger.e(TAG, "Background color could not be identified for input string \"$inAppBackgroundColor\". Failed to load in-app background.")
            return null
        }

        return ColorDrawable(backgroundColorWithAlpha)
    }

    private fun showAndAnimateWebView() {
        webView.alpha = 1.0f
        webView.visibility = View.VISIBLE

        if (shouldAnimate) {
            val animationResource: Int
            val inAppLayout = getInAppLayout(insetPadding)
            when (inAppLayout) {
                InAppLayout.TOP -> animationResource = R.anim.slide_down_custom
                InAppLayout.CENTER, InAppLayout.FULLSCREEN -> animationResource = R.anim.fade_in_custom
                InAppLayout.BOTTOM -> animationResource = R.anim.slide_up_custom
                else -> animationResource = R.anim.fade_in_custom
            }
            try {
                val anim = AnimationUtils.loadAnimation(requireContext(), animationResource)
                anim.duration = IterableConstants.ITERABLE_IN_APP_ANIMATION_DURATION.toLong()
                webView.startAnimation(anim)
            } catch (e: Exception) {
                IterableLogger.e(TAG, "Failed to show inapp with animation")
            }
        }
    }

    private fun hideWebView() {
        if (shouldAnimate) {
            val animationResource: Int
            val inAppLayout = getInAppLayout(insetPadding)

            when (inAppLayout) {
                InAppLayout.TOP -> animationResource = R.anim.top_exit
                InAppLayout.CENTER, InAppLayout.FULLSCREEN -> animationResource = R.anim.fade_out_custom
                InAppLayout.BOTTOM -> animationResource = R.anim.bottom_exit
                else -> animationResource = R.anim.fade_out_custom
            }

            try {
                val anim = AnimationUtils.loadAnimation(requireContext(), animationResource)
                anim.duration = IterableConstants.ITERABLE_IN_APP_ANIMATION_DURATION.toLong()
                webView.startAnimation(anim)
            } catch (e: Exception) {
                IterableLogger.e(TAG, "Failed to hide inapp with animation")
            }
        }

        hideInAppBackground()
        val dismissWebViewRunnable = Runnable {
            if (context != null && dialog != null && dialog!!.window != null) {
                dismissAllowingStateLoss()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.postOnAnimationDelayed(dismissWebViewRunnable, 400L)
        } else {
            webView.postDelayed(dismissWebViewRunnable, 400L)
        }
    }

    private fun processMessageRemoval() {
        val message = IterableApi.sharedInstance.inAppManager?.getMessageById(messageId)
        if (message == null) {
            IterableLogger.e(TAG, "Message with id $messageId does not exist")
            return
        }

        if (message.isMarkedForDeletion() && !message.isConsumed()) {
            IterableApi.sharedInstance.inAppManager?.removeMessage(message, IterableInAppDeleteActionType.DELETE_BUTTON, IterableInAppLocation.IN_APP)
        }
    }

    override fun runResizeScript() {
        resize(webView.contentHeight)
    }

    /**
     * Resizes the dialog window based upon the size of its webView HTML content
     * @param height
     */
    fun resize(height: Float) {
        val activity = requireActivity()

        activity.runOnUiThread {
            try {
                // Since this is run asynchronously, notification might've been dismissed already
                if (context == null || notification == null || notification!!.dialog == null ||
                    notification!!.dialog!!.window == null || !notification!!.dialog!!.isShowing
                ) {
                    return@runOnUiThread
                }

                val displayMetrics = activity.resources.displayMetrics
                val window = notification!!.dialog!!.window
                val insetPadding = notification!!.insetPadding

                val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display = wm.defaultDisplay
                val size = Point()

                // Get the correct screen size based on api level
                // https://stackoverflow.com/questions/35780980/getting-the-actual-screen-height-android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    display.getRealSize(size)
                } else {
                    display.getSize(size)
                }

                val webViewWidth = size.x
                val webViewHeight = size.y

                //Check if the dialog is full screen
                if (insetPadding.bottom == 0 && insetPadding.top == 0) {
                    //Handle full screen
                    window!!.setLayout(webViewWidth, webViewHeight)
                    requireDialog().window!!.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
                } else {
                    val relativeHeight = (height * resources.displayMetrics.density).toDouble()
                    val webViewLayout = RelativeLayout.LayoutParams(resources.displayMetrics.widthPixels, relativeHeight.toInt())
                    webView.layoutParams = webViewLayout
                }
            } catch (e: IllegalArgumentException) {
                IterableLogger.e(TAG, "Exception while trying to resize an in-app message", e)
            }
        }
    }

    /**
     * Returns the vertical position of the dialog for the given padding
     * @param padding
     * @return
     */
    internal fun getVerticalLocation(padding: Rect): Int {
        var gravity = Gravity.CENTER_VERTICAL
        if (padding.top == 0 && padding.bottom < 0) {
            gravity = Gravity.TOP
        } else if (padding.top < 0 && padding.bottom == 0) {
            gravity = Gravity.BOTTOM
        }
        return gravity
    }

    internal fun getInAppLayout(padding: Rect): InAppLayout {
        return if (padding.top == 0 && padding.bottom == 0) {
            InAppLayout.FULLSCREEN
        } else if (padding.top == 0 && padding.bottom < 0) {
            InAppLayout.TOP
        } else if (padding.top < 0 && padding.bottom == 0) {
            InAppLayout.BOTTOM
        } else {
            InAppLayout.CENTER
        }
    }

    companion object {
        private const val BACK_BUTTON = "itbl://backButton"
        private const val TAG = "IterableInAppFragmentHTMLNotification"
        private const val HTML_STRING = "HTML"
        private const val BACKGROUND_ALPHA = "BackgroundAlpha"
        private const val INSET_PADDING = "InsetPadding"
        private const val CALLBACK_ON_CANCEL = "CallbackOnCancel"
        private const val MESSAGE_ID = "MessageId"
        private const val IN_APP_OPEN_TRACKED = "InAppOpenTracked"
        private const val IN_APP_BG_ALPHA = "InAppBgAlpha"
        private const val IN_APP_BG_COLOR = "InAppBgColor"
        private const val IN_APP_SHOULD_ANIMATE = "ShouldAnimate"

        private const val DELAY_THRESHOLD_MS = 500

        @Nullable
        var notification: IterableInAppFragmentHTMLNotification? = null
        @Nullable
        var clickCallback: IterableHelper.IterableUrlCallback? = null
        @Nullable
        var location: IterableInAppLocation? = null

        @JvmStatic
        fun createInstance(
            @NonNull htmlString: String,
            callbackOnCancel: Boolean,
            @NonNull clickCallback: IterableHelper.IterableUrlCallback,
            @NonNull location: IterableInAppLocation,
            @NonNull messageId: String,
            @NonNull backgroundAlpha: Double,
            @NonNull padding: Rect
        ): IterableInAppFragmentHTMLNotification {
            return createInstance(htmlString, callbackOnCancel, clickCallback, location, messageId, backgroundAlpha, padding, false, IterableInAppMessage.InAppBgColor(null, 0.0))
        }

        @JvmStatic
        fun createInstance(
            @NonNull htmlString: String,
            callbackOnCancel: Boolean,
            @NonNull clickCallback: IterableHelper.IterableUrlCallback,
            @NonNull location: IterableInAppLocation,
            @NonNull messageId: String,
            @NonNull backgroundAlpha: Double,
            @NonNull padding: Rect,
            @NonNull shouldAnimate: Boolean,
            inAppBgColor: IterableInAppMessage.InAppBgColor
        ): IterableInAppFragmentHTMLNotification {
            notification = IterableInAppFragmentHTMLNotification()
            val args = Bundle()
            args.putString(HTML_STRING, htmlString)
            args.putBoolean(CALLBACK_ON_CANCEL, callbackOnCancel)
            args.putString(MESSAGE_ID, messageId)
            args.putDouble(BACKGROUND_ALPHA, backgroundAlpha)
            args.putParcelable(INSET_PADDING, padding)
            args.putString(IN_APP_BG_COLOR, inAppBgColor.bgHexColor)
            args.putDouble(IN_APP_BG_ALPHA, inAppBgColor.bgAlpha.toDouble())
            args.putBoolean(IN_APP_SHOULD_ANIMATE, shouldAnimate)

            this.clickCallback = clickCallback
            this.location = location
            notification!!.arguments = args
            return notification!!
        }

        /**
         * Returns the notification instance currently being shown
         * @return notification instance
         */
        @JvmStatic
        fun getInstance(): IterableInAppFragmentHTMLNotification? {
            return notification
        }
    }
}

enum class InAppLayout {
    TOP,
    BOTTOM,
    CENTER,
    FULLSCREEN
}