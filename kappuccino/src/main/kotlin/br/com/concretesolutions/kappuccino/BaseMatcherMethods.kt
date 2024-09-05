package br.com.concretesolutions.kappuccino

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import br.com.concretesolutions.kappuccino.matchers.drawable.DrawablePosition
import org.hamcrest.Matcher

interface BaseMatcherMethods {
    fun id(@IdRes viewId: Int): Any
    fun text(@StringRes textId: Int): Any
    fun text(text: String): Any
    /**
     * Matches a view if it has text that matches the Matcher
     */
    fun text(textMatcher: Matcher<String>): Any
    fun contentDescription(@StringRes contentDescriptionId: Int): Any
    fun contentDescription(contentDescription: String): Any
    fun image(@DrawableRes imageId: Int): Any
    /**
     * Match background of a View.
     * This background must be a VectorDrawable, ColorDrawable, or BitmapDrawable
     */
    fun background(@IdRes drawableId: Int): Any
    fun textColor(@ColorRes colorId: Int): Any
    fun textCompoundDrawable(drawablePosition: DrawablePosition): Any
    fun parent(@IdRes parentId: Int = -1, func: BaseMatchersImpl.() -> BaseMatchersImpl): Any
    fun descendant(@IdRes descendantId: Int = -1, func: BaseMatchersImpl.() -> BaseMatchersImpl): Any
    fun custom(viewMatcher: Matcher<View>): Any
}