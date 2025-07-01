package com.iterable.iterableapi

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting

import android.util.Log

internal object IterableActionRunner {

    @VisibleForTesting
    @JvmStatic
    var instance: IterableActionRunnerImpl = IterableActionRunnerImpl()

    @JvmStatic
    fun executeAction(@NonNull context: Context, action: IterableAction?, @NonNull source: IterableActionSource): Boolean {
        return instance.executeAction(context, action, source)
    }

    internal class IterableActionRunnerImpl {
        
        companion object {
            private const val TAG = "IterableActionRunner"
        }

        /**
         * Execute an [IterableAction] as a response to push action
         *
         * @param context Context
         * @param action  The original action object
         * @return `true` if the action was handled, `false` if it was not
         */
        fun executeAction(@NonNull context: Context, action: IterableAction?, @NonNull source: IterableActionSource): Boolean {
            if (action == null) {
                return false
            }

            val actionContext = IterableActionContext(action, source)

            return if (action.isOfType(IterableAction.ACTION_TYPE_OPEN_URL)) {
                openUri(context, Uri.parse(action.getData()), actionContext)
            } else {
                callCustomActionIfSpecified(action, actionContext)
            }
        }

        /**
         * Handle [IterableAction.ACTION_TYPE_OPEN_URL] action type
         * Calls [IterableUrlHandler] for custom handling by the app. If the handle does not exist
         * or returns `false`, the SDK tries to find an activity that can open this URL.
         *
         * @param context       Context
         * @param uri           The URL to open
         * @param actionContext The original action object
         * @return `true` if the action was handled, or an activity was found for this URL
         * `false` if the handler did not handle this URL and no activity was found to open it with
         */
        private fun openUri(@NonNull context: Context, @NonNull uri: Uri, @NonNull actionContext: IterableActionContext): Boolean {
            var uriHandled = false
            // Handle URL: check for deep links within the app
            if (!IterableUtil.isUrlOpenAllowed(uri.toString())) {
                return false
            }

            if (IterableApi.getInstance().config.urlHandler != null) {
                if (IterableApi.getInstance().config.urlHandler!!.handleIterableURL(uri, actionContext)) {
                    return true
                }
            }

            // Handle URL: check for deep links within the app
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri

            val packageManager = context.packageManager
            if (packageManager == null) {
                IterableLogger.e(TAG, "Could not find package manager to handle deep link:$uri")
                return false
            }

            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            if (resolveInfos.size > 1) {
                for (resolveInfo in resolveInfos) {
                    if (resolveInfo.activityInfo.packageName == context.packageName) {
                        Log.d(TAG, "The deep link will be handled by the app: " + resolveInfo.activityInfo.packageName)
                        intent.setPackage(resolveInfo.activityInfo.packageName)
                        break
                    }
                }
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                uriHandled = true
            } else {
                IterableLogger.e(TAG, "Could not find activities to handle deep link:$uri")
            }
            return uriHandled
        }

        /**
         * Handle custom actions passed from push notifications
         *
         * @param action [IterableAction] object that contains action payload
         * @return `true` if the action is valid and was handled by the handler
         * `false` if the action is invalid or the handler returned `false`
         */
        private fun callCustomActionIfSpecified(@NonNull action: IterableAction, @NonNull actionContext: IterableActionContext): Boolean {
            if (!action.getType().isNullOrEmpty()) {
                // Call custom action handler
                if (IterableApi.getInstance().config.customActionHandler != null) {
                    return IterableApi.getInstance().config.customActionHandler!!.handleIterableCustomAction(action, actionContext)
                }
            }
            return false
        }
    }
}
