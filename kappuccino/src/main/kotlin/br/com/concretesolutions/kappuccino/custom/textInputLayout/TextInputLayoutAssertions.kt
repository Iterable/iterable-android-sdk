package br.com.concretesolutions.kappuccino.custom.textInputLayout

import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description

fun textInputLayout(@IdRes idTextInputLayout: Int, assertion: TextInputLayoutAssertions.() -> TextInputLayoutAssertions): TextInputLayoutAssertions {
    return TextInputLayoutAssertions(idTextInputLayout).apply {
        assertion()
    }
}

class TextInputLayoutAssertions(@IdRes private val idTextInputLayout: Int) {

    fun hasTextError(): TextInputLayoutAssertions {
        onView(withId(idTextInputLayout)).check(matches(matcherHasTextError()))
        return this
    }

    fun withTextError(@StringRes textToCheck: Int): TextInputLayoutAssertions {
        val targetContext = InstrumentationRegistry.getTargetContext()
        val matcherToCheckErrorText = boundedMatcherToCheckErrorText(targetContext.resources.getString(textToCheck))
        onView(withId(idTextInputLayout)).check(matches(matcherToCheckErrorText))
        return this
    }

    fun withTextError(textToCheck: String): TextInputLayoutAssertions {
        val matcherToCheckErrorText = boundedMatcherToCheckErrorText(textToCheck)
        onView(withId(idTextInputLayout)).check(matches(matcherToCheckErrorText))
        return this
    }

    private fun matcherHasTextError(): BoundedMatcher<View, TextInputLayout> {
        return object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {
            override fun describeTo(description: Description) {
                description.appendText(" has text error")
            }

            override fun matchesSafely(item: TextInputLayout): Boolean {
                return item.error != null
            }
        }
    }

    private fun boundedMatcherToCheckErrorText(textToCheck: String): BoundedMatcher<View, TextInputLayout> {
        return object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {
            override fun describeTo(description: Description) {
                description.appendText(" has text error")
            }

            override fun matchesSafely(item: TextInputLayout): Boolean {
                return item.error != null
            }
        }
    }
}