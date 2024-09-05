package br.com.concretesolutions.kappuccino.counters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.viewpager.widget.ViewPager
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert

class CountAssertion(private val expectedCount: Int) : ViewAssertion {

    override fun check(view: View, noViewFoundException: NoMatchingViewException?) {
        if (noViewFoundException != null) {
            throw noViewFoundException
        }

        when (view) {
            is RecyclerView -> recyclerViewSize(view)
            is ViewPager -> viewPagerSize(view)
        }
    }

    private fun recyclerViewSize(recyclerView: RecyclerView) {
        val adapter = recyclerView.adapter
        MatcherAssert.assertThat(adapter?.itemCount, CoreMatchers.`is`(expectedCount))
    }

    private fun viewPagerSize(viewPager: ViewPager) {
        val adapter = viewPager.adapter
        MatcherAssert.assertThat(adapter?.count, CoreMatchers.`is`(expectedCount))
    }
}