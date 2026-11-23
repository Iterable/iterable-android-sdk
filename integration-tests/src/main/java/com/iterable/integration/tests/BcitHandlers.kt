package com.iterable.integration.tests

import android.content.Context
import android.content.Intent
import android.util.Log
import com.iterable.integration.tests.activities.DeepLinkTestActivity
import com.iterable.iterableapi.IterableCustomActionHandler
import com.iterable.iterableapi.IterableUrlHandler

/**
 * The URL and custom-action handlers, allowed protocols and in-app interval the BCIT app runs
 * project A with, shared so a project switched to behaves the same as A at launch.
 */
object BcitHandlers {

    private const val TAG = "BcitHandlers"

    /** BCIT campaigns use `tester://` deep links, which the SDK drops unless allowed. */
    val ALLOWED_PROTOCOLS = arrayOf("tester", "https", "http")

    const val IN_APP_DISPLAY_INTERVAL_SECONDS = 2.0

    /** Opens deep links in [DeepLinkTestActivity]; the config outlives any one activity. */
    fun urlHandler(context: Context): IterableUrlHandler {
        val appContext = context.applicationContext
        return IterableUrlHandler { url, _ ->
            Log.d(TAG, "Deep link handled: $url")
            appContext.startActivity(
                Intent(appContext, DeepLinkTestActivity::class.java)
                    .putExtra(MainActivity.EXTRA_DEEP_LINK_URL, url.toString())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            false
        }
    }

    val customActionHandler = IterableCustomActionHandler { action, _ ->
        Log.d(TAG, "Custom action received: type=${action.getType()}, data=${action.getData()}")
        false
    }
}
