package com.iterable.iterableapi

import android.content.Context
import android.net.Uri

class IterableManagerHelper {
    public fun handleClick(url: Uri?, context: Context, actionSource: IterableActionSource) {
        val urlString: String = url.toString()
        if ((urlString != null) && urlString.toString().isNotEmpty()) {
            if (urlString.startsWith(IterableConstants.URL_SCHEME_ACTION)) {
                // This is an action:// URL, pass that to the custom action handler
                val actionName: String = urlString.replace(IterableConstants.URL_SCHEME_ACTION, "")
                IterableActionRunner.executeAction(
                    context,
                    IterableAction.actionCustomAction(actionName),
                    actionSource
                )
            } else if (urlString.startsWith(IterableConstants.URL_SCHEME_ITBL)) {
                // Handle itbl:// URLs, pass that to the custom action handler for compatibility
                val actionName: String = urlString.replace(IterableConstants.URL_SCHEME_ITBL, "")
                IterableActionRunner.executeAction(
                    context,
                    IterableAction.actionCustomAction(actionName),
                    actionSource
                )
            } else {
                IterableActionRunner.executeAction(
                    context,
                    IterableAction.actionOpenUrl(urlString),
                    actionSource
                )
            }
        }

    }
}