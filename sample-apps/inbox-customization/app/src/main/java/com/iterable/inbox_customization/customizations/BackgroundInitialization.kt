package com.iterable.inbox_customization.customizations

import androidx.fragment.app.Fragment
import com.iterable.inbox_customization.BackgroundInitFragment
import com.iterable.inbox_customization.util.SingleFragmentActivity

fun Fragment.onBackgroundInitializationClicked() {
    val intent = SingleFragmentActivity.createIntentWithFragment(requireActivity(), BackgroundInitFragment::class.java)
    startActivity(intent)
}



