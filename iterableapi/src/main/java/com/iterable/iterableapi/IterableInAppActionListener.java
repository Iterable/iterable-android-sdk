package com.iterable.iterableapi;

import android.app.Dialog;
import android.view.View;

import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppActionListener implements View.OnClickListener {
    Dialog dialog;
    int index;
    String actionName;
    IterableInAppActionHandler onClickCallback;
    JSONObject trackParams;

    IterableInAppActionListener(Dialog dialog, int index, String actionName, JSONObject trackParams, IterableInAppActionHandler onClickCallback){
        this.index = index;
        this.actionName  = actionName;
        this.onClickCallback = onClickCallback;
        this.dialog = dialog;
        this.trackParams = trackParams;
    }

    @Override
    public void onClick(View v) {
        IterableApi.sharedInstance.trackInAppClick(trackParams);

        if (onClickCallback != null) {
            onClickCallback.execute(actionName);
        }
        dialog.dismiss();
    }

    public interface IterableInAppActionHandler {
        void execute(String data);
    }
}
