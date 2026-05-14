package com.iterable.iterableapi.ui.inbox

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.LayoutRes
import com.google.android.material.appbar.MaterialToolbar
import com.iterable.iterableapi.ui.R

/**
 * Opt-in toolbar for [IterableInboxFragment]. Configure via [apply] with an
 * [InboxToolbarOption].
 *
 * The view is marked `gone` in the fragment layout by default, so `InboxToolbarOption.None`
 * (the SDK default) renders no toolbar and takes no space.
 *
 * **Theme requirement:** the host activity must use a `Theme.AppCompat` (or
 * `Theme.MaterialComponents` / `Theme.Material3`) descendant - `MaterialToolbar` will throw
 * an `InflateException` otherwise. If using the [IterableInboxActivity] this is a non-concern
 */
class IterableInboxToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var materialToolbar: MaterialToolbar
    private var backClickListener: OnClickListener? = null
    private var isCustomLayout = false

    init {
        LayoutInflater.from(context).inflate(R.layout.iterable_inbox_toolbar, this, true)
        materialToolbar = findViewById(R.id.iterableInboxMaterialToolbar)
    }

    /** Configure the toolbar. Safe to call multiple times (e.g. on config change). */
    fun apply(option: InboxToolbarOption, title: String?) {
        when (option) {
            InboxToolbarOption.None -> {
                visibility = View.GONE
            }
            InboxToolbarOption.Default -> {
                showDefaultLayout()
                visibility = View.VISIBLE
                materialToolbar.title = resolveTitle(title)
                materialToolbar.navigationIcon = null
                materialToolbar.setNavigationOnClickListener(null)
            }
            InboxToolbarOption.WithBackButton -> {
                showDefaultLayout()
                visibility = View.VISIBLE
                materialToolbar.title = resolveTitle(title)
                materialToolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
                materialToolbar.setNavigationOnClickListener { v -> dispatchBackClick(v) }
            }
            is InboxToolbarOption.Custom -> {
                showCustomLayout(option.layoutRes)
                visibility = View.VISIBLE
                findViewById<View>(R.id.iterable_inbox_back_button)?.setOnClickListener { v ->
                    dispatchBackClick(v)
                }
                // `as? TextView` also matches subclasses like Button/EditText - documented behavior.
                (findViewById<View>(R.id.iterable_inbox_title) as? TextView)?.text = resolveTitle(title)
            }
        }
    }

    /**
     * Override the default back-click behavior. Honored for
     * `InboxToolbarOption.WithBackButton` and for `InboxToolbarOption.Custom` layouts that
     * include a view with id `@id/iterable_inbox_back_button`. Pass `null` to clear a
     * previously set override and fall back to the host activity's `OnBackPressedDispatcher`.
     *
     * When this view is hosted by [IterableInboxFragment], the fragment always installs
     * a default listener, so this fallback path is only reached for standalone XML usage.
     */
    fun setOnBackClickListener(listener: OnClickListener?) {
        backClickListener = listener
    }

    private fun dispatchBackClick(v: View) {
        val override = backClickListener
        if (override != null) {
            override.onClick(v)
            return
        }
        (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
    }

    /** Swap in the SDK's default toolbar layout. No-op when it's already showing. */
    private fun showDefaultLayout() {
        if (!isCustomLayout) return
        removeAllViews()
        LayoutInflater.from(context).inflate(R.layout.iterable_inbox_toolbar, this, true)
        materialToolbar = findViewById(R.id.iterableInboxMaterialToolbar)
        isCustomLayout = false
    }

    /** Swap in the integrator's custom toolbar layout. Always re-inflates. */
    private fun showCustomLayout(@LayoutRes layoutRes: Int) {
        removeAllViews()
        LayoutInflater.from(context).inflate(layoutRes, this, true)
        isCustomLayout = true
    }

    private fun resolveTitle(title: String?): String =
        title ?: context.getString(R.string.iterable_inbox_default_title)
}
