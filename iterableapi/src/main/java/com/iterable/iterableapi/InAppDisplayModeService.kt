package com.iterable.iterableapi

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RestrictTo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Resolves [IterableInAppDisplayMode] and applies the corresponding window/system-bar
 * configuration. Shared by both [IterableInAppFragmentHTMLNotification] (FragmentActivity
 * hosts) and [IterableInAppDialogNotification] (ComponentActivity / Compose hosts) so the
 * two renderers stay aligned on display-mode behavior.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InAppDisplayModeService {

    fun resolveDisplayMode(): IterableInAppDisplayMode {
        return try {
            IterableApi.sharedInstance.config?.inAppDisplayMode ?: DEFAULT_MODE
        } catch (e: Exception) {
            IterableLogger.w(TAG, "Could not resolve display mode from config, using default")
            DEFAULT_MODE
        }
    }

    fun configureSystemBarsForMode(
        window: Window?,
        mode: IterableInAppDisplayMode,
        hostActivity: Activity?,
        hostIsEdgeToEdge: Boolean
    ) {
        if (window == null) return

        when (mode) {
            IterableInAppDisplayMode.FORCE_EDGE_TO_EDGE -> applyEdgeToEdge(window)
            IterableInAppDisplayMode.FORCE_FULLSCREEN -> hideStatusBar(window, hostActivity)
            IterableInAppDisplayMode.FORCE_RESPECT_BOUNDS -> applyRespectBounds(window, hostActivity)
            IterableInAppDisplayMode.FOLLOW_APP_LAYOUT ->
                configureSystemBarsFollowingApp(window, hostActivity, hostIsEdgeToEdge)
        }
    }

    fun shouldApplySystemBarInsets(
        mode: IterableInAppDisplayMode,
        isFullscreenLayout: Boolean,
        hostIsEdgeToEdge: Boolean
    ): Boolean {
        return when (mode) {
            IterableInAppDisplayMode.FORCE_EDGE_TO_EDGE,
            IterableInAppDisplayMode.FORCE_FULLSCREEN -> false
            IterableInAppDisplayMode.FORCE_RESPECT_BOUNDS -> true
            IterableInAppDisplayMode.FOLLOW_APP_LAYOUT ->
                !isFullscreenLayout && hostIsEdgeToEdge
        }
    }

    fun isHostActivityEdgeToEdge(activity: Activity?): Boolean {
        if (activity == null || activity.window == null) return false

        if (hasEdgeToEdgeLegacyFlags(activity)) {
            return true
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isContentDrawnBehindSystemBars(activity)
        } else {
            false
        }
    }

    private fun applyEdgeToEdge(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    @Suppress("DEPRECATION")
    private fun hideStatusBar(window: Window, hostActivity: Activity?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        hideSystemBarsOnWindow(window)
        hideHostSystemBars(hostActivity)
    }

    private fun hideSystemBarsOnWindow(window: Window) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun hideHostSystemBars(hostActivity: Activity?) {
        val hostWindow = hostActivity?.window ?: return
        val controller = WindowCompat.getInsetsController(hostWindow, hostWindow.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun restoreHostSystemBars(hostActivity: Activity?) {
        val hostWindow = hostActivity?.window ?: return
        val controller = WindowCompat.getInsetsController(hostWindow, hostWindow.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    fun applyContentInsetsForMode(
        contentView: View,
        mode: IterableInAppDisplayMode,
        isFullscreenLayout: Boolean,
        hostActivity: Activity?,
        hostIsEdgeToEdge: Boolean
    ) {
        if (!shouldApplySystemBarInsets(mode, isFullscreenLayout, hostIsEdgeToEdge)) return
        if (hostActivity == null) return
        val (top, bottom) = resolveSystemBarInsets(hostActivity)
        contentView.setPadding(0, top, 0, bottom)
    }

    private fun resolveSystemBarInsets(hostActivity: Activity): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sysBars = hostActivity.windowManager.currentWindowMetrics
                .windowInsets
                .getInsets(android.view.WindowInsets.Type.systemBars())
            if (sysBars.top > 0 || sysBars.bottom > 0) {
                return sysBars.top to sysBars.bottom
            }
        }
        val resources = hostActivity.resources
        return resourceDimen(resources, "status_bar_height") to
            resourceDimen(resources, "navigation_bar_height")
    }

    private fun resourceDimen(resources: android.content.res.Resources, name: String): Int {
        val resId = resources.getIdentifier(name, "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    private fun applyRespectBounds(window: Window, hostActivity: Activity?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        copyHostSystemBarAppearance(window, hostActivity)
    }

    private fun copyHostSystemBarAppearance(window: Window, hostActivity: Activity?) {
        val hostWindow = hostActivity?.window ?: return
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = hostWindow.statusBarColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = hostWindow.navigationBarColor
        }
        val hostController = WindowCompat.getInsetsController(hostWindow, hostWindow.decorView)
        val ourController = WindowCompat.getInsetsController(window, window.decorView)
        ourController.isAppearanceLightStatusBars = hostController.isAppearanceLightStatusBars
        ourController.isAppearanceLightNavigationBars = hostController.isAppearanceLightNavigationBars
    }

    private fun configureSystemBarsFollowingApp(
        window: Window,
        hostActivity: Activity?,
        hostIsEdgeToEdge: Boolean
    ) {
        if (hostActivity == null || hostActivity.window == null) return

        if (hostIsEdgeToEdge) {
            applyEdgeToEdge(window)
        } else if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = hostActivity.window.statusBarColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = hostActivity.window.navigationBarColor
        }
    }

    @Suppress("DEPRECATION")
    private fun hasEdgeToEdgeLegacyFlags(activity: Activity): Boolean {
        val flags = activity.window.decorView.systemUiVisibility
        return (flags and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) != 0
    }

    private fun isContentDrawnBehindSystemBars(activity: Activity): Boolean {
        val contentView = activity.findViewById<View>(android.R.id.content) ?: return false
        val position = IntArray(2)
        contentView.getLocationInWindow(position)
        val statusBarPushesContentDown = position[1] > 0
        return !statusBarPushesContentDown
    }

    companion object {
        private const val TAG = "InAppDisplayModeSvc"

        @JvmField
        internal val DEFAULT_MODE: IterableInAppDisplayMode = IterableInAppDisplayMode.FORCE_EDGE_TO_EDGE
    }
}
