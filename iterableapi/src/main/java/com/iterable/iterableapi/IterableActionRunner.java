package com.iterable.iterableapi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import android.util.Log;

import java.util.List;

class IterableActionRunner {

    /**
     * Internal action execution outcome used by {@link IterablePushNotificationUtil} to decide
     * whether to clear {@code pendingAction} and whether the {@code openApp} launcher fallback
     * should fire. Existing callers that only need a boolean use the {@link #executeAction} adapter.
     */
    enum ActionDispatchResult {
        /** The action was not handled — no handler configured or URL failed to open.
         *  Preserve the caller's existing retry / fallback behavior. */
        NOT_HANDLED,
        /** A custom action handler was invoked but returned {@code false} (documented as
         *  "Reserved for future use"). The action is consumed; allow the openApp fallback. */
        DISPATCHED_UNHANDLED,
        /** The action was handled successfully (handler returned {@code true} or URL opened). */
        HANDLED
    }

    @VisibleForTesting
    static IterableActionRunnerImpl instance = new IterableActionRunnerImpl();

    static boolean executeAction(@NonNull Context context, @Nullable IterableAction action, @NonNull IterableActionSource source) {
        return instance.executeAction(context, action, source);
    }

    /**
     * Dispatch an action and return a typed {@link ActionDispatchResult} so callers can
     * distinguish "handler was invoked but returned false" from "no handler was present".
     */
    static ActionDispatchResult dispatchAction(@NonNull Context context, @Nullable IterableAction action, @NonNull IterableActionSource source) {
        return instance.dispatchAction(context, action, source);
    }

    static class IterableActionRunnerImpl {
        private static final String TAG = "IterableActionRunner";

        /**
         * Execute an {@link IterableAction} as a response to push action
         *
         * @param context Context
         * @param action  The original action object
         * @return `true` if the action was handled, `false` if it was not
         */
        boolean executeAction(@NonNull Context context, @Nullable IterableAction action, @NonNull IterableActionSource source) {
            return dispatchAction(context, action, source) == ActionDispatchResult.HANDLED;
        }

        ActionDispatchResult dispatchAction(@NonNull Context context, @Nullable IterableAction action, @NonNull IterableActionSource source) {
            if (action == null) {
                return ActionDispatchResult.NOT_HANDLED;
            }

            IterableActionContext actionContext = new IterableActionContext(action, source);

            if (action.isOfType(IterableAction.ACTION_TYPE_OPEN_URL)) {
                return openUri(context, Uri.parse(action.getData()), actionContext)
                        ? ActionDispatchResult.HANDLED
                        : ActionDispatchResult.NOT_HANDLED;
            } else {
                return callCustomActionIfSpecified(action, actionContext);
            }
        }

        /**
         * Handle {@link IterableAction#ACTION_TYPE_OPEN_URL} action type
         * Calls {@link IterableUrlHandler} for custom handling by the app. If the handle does not exist
         * or returns `false`, the SDK tries to find an activity that can open this URL.
         *
         * @param context       Context
         * @param uri           The URL to open
         * @param actionContext The original action object
         * @return `true` if the action was handled, or an activity was found for this URL
         * `false` if the handler did not handle this URL and no activity was found to open it with
         */
        private boolean openUri(@NonNull Context context, @NonNull Uri uri, @NonNull IterableActionContext actionContext) {
            boolean uriHandled = false;
            // Handle URL: check for deep links within the app
            if (!IterableUtil.isUrlOpenAllowed(uri.toString())) {
                return false;
            }

            if (IterableApi.sharedInstance.config.urlHandler != null) {
                if (IterableApi.sharedInstance.config.urlHandler.handleIterableURL(uri, actionContext)) {
                    return true;
                }
            }

            // Handle URL: check for deep links within the app
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);

            if (context.getPackageManager() == null) {
                IterableLogger.e(TAG, "Could not find package manager to handle deep link:" + uri);
                return false;
            }

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
                uriHandled = true;
            } else {
                IterableLogger.e(TAG, "Could not find activities to handle deep link:" + uri);
            }
            return uriHandled;
        }

        /**
         * Handle custom actions passed from push notifications
         *
         * @param action {@link IterableAction} object that contains action payload
         * @return {@link ActionDispatchResult#HANDLED} if the handler returned {@code true},
         * {@link ActionDispatchResult#DISPATCHED_UNHANDLED} if the handler was invoked but
         * returned {@code false}, or {@link ActionDispatchResult#NOT_HANDLED} if no handler
         * was configured.
         */
        private ActionDispatchResult callCustomActionIfSpecified(@NonNull IterableAction action, @NonNull IterableActionContext actionContext) {
            if (action.getType() != null && !action.getType().isEmpty()) {
                if (IterableApi.sharedInstance.config.customActionHandler != null) {
                    boolean handled = IterableApi.sharedInstance.config.customActionHandler.handleIterableCustomAction(action, actionContext);
                    return handled ? ActionDispatchResult.HANDLED : ActionDispatchResult.DISPATCHED_UNHANDLED;
                }
            }
            return ActionDispatchResult.NOT_HANDLED;
        }
    }
}
