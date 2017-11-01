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
    String messageId;

    /**
     * A custom onClickListener which stores data about the dialog context.
     * @param dialog
     * @param index
     * @param actionName
     * @param messageId
     * @param onClickCallback
     */
    public IterableInAppActionListener(Dialog dialog, int index, String actionName, String messageId, IterableHelper.IterableActionHandler onClickCallback){
        this.index = index;
        this.actionName  = actionName;
        this.onClickCallback = onClickCallback;
        this.dialog = dialog;
        this.messageId = messageId;
    }

    /**
     * Dismisses the dialog when a click is processed.
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (onClickCallback != null) {
            onClickCallback.execute(actionName);
        }
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
