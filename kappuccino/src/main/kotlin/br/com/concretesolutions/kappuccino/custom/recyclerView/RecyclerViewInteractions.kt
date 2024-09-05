package br.com.concretesolutions.kappuccino.custom.recyclerView

import androidx.annotation.IdRes

object RecyclerViewInteractions {
    fun recyclerView(@IdRes recyclerViewId: Int, func: RecyclerViewMethods.() -> RecyclerViewMethods): RecyclerViewMethods {
        return RecyclerViewMethods(recyclerViewId).apply { func() }
    }
}