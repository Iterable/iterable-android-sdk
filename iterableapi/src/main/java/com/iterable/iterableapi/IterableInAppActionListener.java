package com.iterable.iterableapi;

import android.app.Dialog;
import android.view.View;

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
    int campaignId;
    int templateId;

    /**
     * A custom onClickListener which stores data about the dialog context.
     * @param dialog
     * @param index
     * @param actionName
     * @param campaignId
     * @param templateId
     * @param onClickCallback
     */
    IterableInAppActionListener(Dialog dialog, int index, String actionName, int campaignId, int templateId, IterableHelper.IterableActionHandler onClickCallback){
        this.index = index;
        this.actionName  = actionName;
        this.onClickCallback = onClickCallback;
        this.dialog = dialog;
        this.campaignId = campaignId;
        this.templateId = templateId;
    }

    /**
     * Dismisses the dialog when a click is processed.
     * @param v
     */
    @Override
    public void onClick(View v) {
        IterableApi.sharedInstance.trackInAppClick(campaignId, templateId, index);
        if (onClickCallback != null) {
            onClickCallback.execute(actionName);
        }
        dialog.dismiss();
    }
}
