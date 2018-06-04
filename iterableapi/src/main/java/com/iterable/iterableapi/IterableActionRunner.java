package com.iterable.iterableapi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

public class IterableActionRunner {
    static final String TAG = "IterableActionRunner";

    public static boolean executeAction(@NonNull Context context, @Nullable IterableAction action) {
        if (action == null)
            return false;

        if (action.isOfType(IterableAction.ACTION_TYPE_OPEN_URL)) {
            return openUri(context, Uri.parse(action.getData()), action);
        } else {
            return callCustomActionIfSpecified(action);
        }
    }

    private static boolean openUri(@NonNull Context context, @NonNull Uri uri, @NonNull IterableAction action) {
        if (IterableApi.sharedInstance.urlHandler != null) {
            if (IterableApi.sharedInstance.urlHandler.handleIterableURL(uri, action)) {
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

    private static boolean callCustomActionIfSpecified(@NonNull IterableAction action) {
        if (action.getType() != null && !action.getType().isEmpty()) {
            // Call custom action handler
            if (IterableApi.sharedInstance.customActionHandler != null) {
                return IterableApi.sharedInstance.customActionHandler.handleIterableCustomAction(action);
            }
        }
        return false;
    }

}
