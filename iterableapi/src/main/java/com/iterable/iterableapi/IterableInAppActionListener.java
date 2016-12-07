package com.iterable.iterableapi;

import android.app.Dialog;
import android.view.View;

import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com.
 *
 *  IterableInAppActionListener is a custom OnClickListener
 */
public class IterableInAppActionListener implements View.OnClickListener {
    Dialog dialog;
    int index;
    String actionName;
    IterableHelper.IterableActionHandler onClickCallback;
    JSONObject trackParams;

    /**
     * A custom onClickListener which stores data about the dialog context.
     * @param dialog
     * @param index
     * @param actionName
     * @param trackParams
     * @param onClickCallback
     */
    IterableInAppActionListener(Dialog dialog, int index, String actionName, JSONObject trackParams, IterableHelper.IterableActionHandler onClickCallback){
        this.index = index;
        this.actionName  = actionName;
        this.onClickCallback = onClickCallback;
        this.dialog = dialog;
        this.trackParams = trackParams;
    }

    /**
     * Dismisses the dialog when a click is processed.
     * @param v
     */
    @Override
    public void onClick(View v) {
        IterableApi.sharedInstance.trackInAppClick(trackParams);

        if (onClickCallback != null) {
            onClickCallback.execute(actionName);
        }
        dialog.dismiss();
    }
}
