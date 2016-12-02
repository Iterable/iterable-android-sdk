package com.iterable.iterableapi;

import android.view.View;
import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppActionListener implements View.OnClickListener {
    int index;
    String actionName;
    IterableOnClick onClickCallback;
    JSONObject trackParams;

    IterableInAppActionListener(int index, String actionName, JSONObject trackParams, IterableOnClick onClickCallback){
        this.index = index;
        this.actionName  = actionName;
        this.onClickCallback = onClickCallback;
        this.trackParams = trackParams;
    }

    @Override
    public void onClick(View v) {
        //track which button was clicked
        IterableApi.sharedInstance.trackInAppClick(trackParams);

        if (onClickCallback != null) {
            onClickCallback.onClick(actionName);
        }
    }

    public interface IterableOnClick {
        void onClick(String result);
    }
}
