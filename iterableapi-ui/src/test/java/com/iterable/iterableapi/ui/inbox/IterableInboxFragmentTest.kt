package com.iterable.iterableapi.ui.inbox

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Defends the unchanged-path contract for `IterableInboxFragment`. The default
 * `InboxToolbarOption.None` (used by every pre-toolbar caller) must render exactly as
 * the fragment did before the toolbar feature was added: list fills the parent, no
 * toolbar inflated, no AppCompat-or-above theme requirement beyond what already existed.
 *
 * These tests exist specifically because PR review caught two regressions on this path:
 *  - The list collapsed to 0dp height under a `RelativeLayout`+`GONE` constraint chain.
 *  - The toolbar layout was inflated even for `None`, requiring AppCompat unconditionally.
 *
 * Both are regression-test material; do not delete without proof the contract changed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IterableInboxFragmentTest {

    /** Plain AppCompat host; no Material attrs available. */
    class PlainAppCompatActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
            super.onCreate(savedInstanceState)
        }
    }

    @Before
    fun setUp() {
        // The fragment's onCreateView reads inbox messages from IterableApi.
        // Initialize with a stub key so it doesn't NPE during view setup.
        IterableApi.initialize(RuntimeEnvironment.getApplication(), "test-key")
    }

    private fun mountDefaultFragment(): Pair<PlainAppCompatActivity, IterableInboxFragment> {
        val activity = Robolectric.buildActivity(PlainAppCompatActivity::class.java).setup().get()
        val fragment = IterableInboxFragment.newInstance()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        return activity to fragment
    }

    /**
     * Default-path layout contract: under `InboxToolbarOption.None` (no args),
     * the list fills the parent and the toolbar takes zero space.
     *
     * Catches: PR-review blocker #1 (RelativeLayout+GONE collapsed the list to 0dp).
     */
    @Test
    fun newInstance_default_listFillsParentAndToolbarTakesNoSpace() {
        val (_, fragment) = mountDefaultFragment()
        val root = fragment.requireView()

        val toolbar = root.findViewById<View>(R.id.iterable_inbox_toolbar)
        assertEquals("Toolbar must be GONE under InboxToolbarOption.None",
            View.GONE, toolbar.visibility)
        assertEquals("Toolbar must inflate no children under None " +
            "(otherwise we pay an AppCompat-or-above cost the host didn't opt into)",
            0, (toolbar as ViewGroup).childCount)

        // Robolectric does not lay out fragments inserted via commitNow until something
        // triggers a layout pass. Drive one explicitly with realistic dimensions so we
        // can assert measured size of the RecyclerView.
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        val list = root.findViewById<RecyclerView>(R.id.list)
        assertNotNull(list)
        assertTrue("RecyclerView must take vertical space under None " +
            "(would have caught the layout_below=GONE-view collapse regression). " +
            "Got measuredHeight=${list.measuredHeight}",
            list.measuredHeight > 0)
    }

    /**
     * AppCompat-host inflation: under `InboxToolbarOption.None`, no Material widgets
     * may be inflated, so even hosts on plain `Theme.AppCompat.*` (no Material attrs)
     * must not throw.
     *
     * Catches: PR-review blocker #2 (always-inflated MaterialToolbar required AppCompat
     * even when the toolbar feature was disabled).
     */
    @Test
    fun newInstance_default_inflatesUnderPlainAppCompatTheme() {
        // The act of mounting the fragment is the test: if the toolbar layout inflates
        // unconditionally, MaterialToolbar fails to resolve Material attributes here
        // and throws InflateException at view creation.
        val (_, fragment) = mountDefaultFragment()
        assertNotNull("Fragment view must inflate under plain AppCompat",
            fragment.view)
    }
}
