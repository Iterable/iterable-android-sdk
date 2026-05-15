package com.iterable.iterableapi.ui.inbox

import android.view.ContextThemeWrapper
import android.view.View
import androidx.activity.ComponentActivity
import com.google.android.material.appbar.MaterialToolbar
import com.iterable.iterableapi.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowView

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IterableInboxToolbarViewTest {

    private fun newToolbar(): IterableInboxToolbarView {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        // MaterialToolbar requires an AppCompat-derived theme.
        val themed = ContextThemeWrapper(
            activity,
            androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar
        )
        return IterableInboxToolbarView(themed)
    }

    private fun materialToolbar(view: IterableInboxToolbarView): MaterialToolbar =
        view.findViewById(R.id.iterableInboxMaterialToolbar)

    @Test
    fun applyNone_hidesTheView() {
        val view = newToolbar()
        view.apply(InboxToolbarOption.None, title = "ignored")
        assertEquals(View.GONE, view.visibility)
    }

    @Test
    fun applyDefault_showsTitleWithoutNavigationIcon() {
        val view = newToolbar()
        view.apply(InboxToolbarOption.Default, title = "My Inbox")
        val toolbar = materialToolbar(view)
        assertEquals(View.VISIBLE, view.visibility)
        assertEquals("My Inbox", toolbar.title)
        assertNull("Default should not set a navigation icon", toolbar.navigationIcon)
    }

    @Test
    fun applyDefault_withNullTitle_fallsBackToDefaultString() {
        val view = newToolbar()
        view.apply(InboxToolbarOption.Default, title = null)
        val expected = view.context.getString(R.string.iterable_inbox_default_title)
        assertEquals(expected, materialToolbar(view).title)
    }

    @Test
    fun applyWithBackButton_setsNavigationIconAndClickListener() {
        val view = newToolbar()
        view.apply(InboxToolbarOption.WithBackButton, title = "Inbox")
        val toolbar = materialToolbar(view)
        assertEquals(View.VISIBLE, view.visibility)
        assertEquals("Inbox", toolbar.title)
        assertNotNull("WithBackButton must set a navigation icon", toolbar.navigationIcon)
    }

    @Test
    fun setOnBackClickListener_isInvokedWhenNavigationIconClicked() {
        val view = newToolbar()
        view.apply(InboxToolbarOption.WithBackButton, title = null)

        var overrideFired = false
        view.setOnBackClickListener { overrideFired = true }

        // MaterialToolbar exposes the click via the listener registered with
        // setNavigationOnClickListener. Robolectric's ShadowView.innerText is brittle
        // for the nav icon child; instead we read back the listener and invoke it.
        materialToolbar(view).navigationOnClickListener().onClick(view)

        assertTrue("Override back-click listener was not invoked", overrideFired)
    }

    @Test
    fun setOnBackClickListener_clearedWithNull_fallsBackToDefault() {
        val view = newToolbar()
        view.apply(InboxToolbarOption.WithBackButton, title = null)

        var overrideFired = false
        view.setOnBackClickListener { overrideFired = true }
        view.setOnBackClickListener(null)

        materialToolbar(view).navigationOnClickListener().onClick(view)

        assertFalse("Override should have been cleared", overrideFired)
    }

    @Test
    fun customToDefaultTransition_restoresSdkToolbar() {
        val view = newToolbar()

        // Start in Default - the SDK's MaterialToolbar should be present.
        view.apply(InboxToolbarOption.Default, title = "Inbox")
        assertNotNull(view.findViewById<MaterialToolbar>(R.id.iterableInboxMaterialToolbar))

        // Switch to Custom using a layout we know exists in the SDK. After this,
        // the integrator-supplied layout replaces the SDK's toolbar tree.
        view.apply(InboxToolbarOption.Custom(R.layout.iterable_inbox_item), title = null)
        assertEquals(View.VISIBLE, view.visibility)

        // Switch back to Default - the SDK's MaterialToolbar must be re-inflated
        // and the new title must be applied.
        view.apply(InboxToolbarOption.Default, title = "Back")
        val restored: MaterialToolbar? = view.findViewById(R.id.iterableInboxMaterialToolbar)
        assertNotNull("Default after Custom must restore the SDK toolbar", restored)
        assertEquals("Back", restored!!.title)
    }
}

/**
 * Reads back the navigation OnClickListener that was registered via
 * [androidx.appcompat.widget.Toolbar.setNavigationOnClickListener]. Toolbar stores
 * the listener on its internal navigation button child; the cleanest way under
 * Robolectric is to attach our own click to `null`-safe traverse via a shadow.
 */
private fun MaterialToolbar.navigationOnClickListener(): View.OnClickListener {
    // Toolbar always creates an internal ImageButton for the nav icon when one is
    // set; locate it and return its registered click listener via Robolectric shadow.
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is android.widget.ImageButton) {
            val shadow = org.robolectric.Shadows.shadowOf(child) as ShadowView
            return shadow.onClickListener
                ?: error("Navigation icon has no click listener attached")
        }
    }
    error("Toolbar has no navigation icon child")
}
