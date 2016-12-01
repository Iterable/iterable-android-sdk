package com.iterable.iterableapi.InApp;

import android.view.View;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppActionListener implements View.OnClickListener {
    int index;
    String actionName;
    IterableOnClick onClickCallback;

    IterableInAppActionListener(int index, String actionName, IterableOnClick onClickCallback){
        this.index = index;
        this.actionName  = actionName;
        this.onClickCallback = onClickCallback;
    }

    @Override
    public void onClick(View v) {
        //track which button was clicked

        if (onClickCallback != null) {
            onClickCallback.onClick(actionName);
        }
    }

    public interface IterableOnClick {
        void onClick(String result);
    }
}
