package com.iterable.iterableapi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

class IterableActionRunner {
    static final String TAG = "IterableActionRunner";

    /**
     * Execute an {@link IterableAction} as a response to push action
     * @param context Context
     * @param action The original action object
     * @return `true` if the action was handled, `false` if it was not
     */
    public static boolean executeAction(@NonNull Context context, @Nullable IterableAction action, @NonNull IterableActionSource source) {
        if (action == null) {
            return false;
        }

        // Do not handle actions and try to open URLs unless the SDK is initialized with a new init method
        if (IterableApi.sharedInstance.sdkCompatEnabled) {
            return false;
        }

        IterableActionContext actionContext = new IterableActionContext(action, source);

        if (action.isOfType(IterableAction.ACTION_TYPE_OPEN_URL)) {
            return openUri(context, Uri.parse(action.getData()), actionContext);
        } else {
            return callCustomActionIfSpecified(action, actionContext);
        }
    }

    /**
     * Handle {@link IterableAction#ACTION_TYPE_OPEN_URL} action type
     * Calls {@link IterableUrlHandler} for custom handling by the app. If the handle does not exist
     * or returns `false`, the SDK tries to find an activity that can open this URL.
     * @param context Context
     * @param uri The URL to open
     * @param actionContext The original action object
     * @return `true` if the action was handled, or an activity was found for this URL
     * `false` if the handler did not handle this URL and no activity was found to open it with
     */
    private static boolean openUri(@NonNull Context context, @NonNull Uri uri, @NonNull IterableActionContext actionContext) {
        if (IterableApi.sharedInstance.config.urlHandler != null) {
            if (IterableApi.sharedInstance.config.urlHandler.handleIterableURL(uri, actionContext)) {
                return true;
            }
        }

        // Handle URL: check for deep links within the app
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);

        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        if (resolveInfos.size() > 1) {
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.activityInfo.packageName.equals(context.getPackageName())) {
                    Log.d(TAG, "The deep link will be handled by the app: " + resolveInfo.activityInfo.packageName);
                    intent.setPackage(resolveInfo.activityInfo.packageName);
                    break;
                }
            }
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else {
            IterableLogger.e(TAG, "Could not find activities to handle deep link:" + uri);
            return false;
        }
    }

    /**
     * Handle custom actions passed from push notifications
     * @param action {@link IterableAction} object that contains action payload
     * @return `true` if the action is valid and was handled by the handler
     * `false` if the action is invalid or the handler returned `false`
     */
    private static boolean callCustomActionIfSpecified(@NonNull IterableAction action, @NonNull IterableActionContext actionContext) {
        if (action.getType() != null && !action.getType().isEmpty()) {
            // Call custom action handler
            if (IterableApi.sharedInstance.config.customActionHandler != null) {
                return IterableApi.sharedInstance.config.customActionHandler.handleIterableCustomAction(action);
            }
        }
        return false;
    }

}
