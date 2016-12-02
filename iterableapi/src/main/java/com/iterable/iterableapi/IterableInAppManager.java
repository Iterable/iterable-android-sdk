package com.iterable.iterableapi;

import android.app.Dialog;
import android.content.Context;

import android.graphics.Color;

import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppManager {
    static final String TAG = "IterableInAppManager";

    public static void showNotification(Context context, JSONObject dialogOptions, IterableInAppActionListener.IterableOnClick clickCallback) {
        if(dialogOptions != null) {
            String type = dialogOptions.optString(IterableConstants.ITERABLE_IN_APP_TYPE);
            if (type.equalsIgnoreCase(IterableConstants.ITERABLE_IN_APP_TYPE_FULL)) {
                showFullScreenDialog(context, dialogOptions, clickCallback);
            } else {
                showNotificationDialog(context, dialogOptions, clickCallback);
            }
        } else {
            IterableLogger.d(TAG, "In-App notification not displayed: showNotification must contain valid dialogOptions");
        }
    }

    public static void showNotificationDialog(Context context, JSONObject dialogParameters, IterableInAppActionListener.IterableOnClick clickCallback) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Material_NoActionBar); //Theme_Material_NoActionBar_Overscan
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = .8f;
        lp.gravity = getNotificationLocation(dialogParameters.optString(IterableConstants.ITERABLE_IN_APP_TYPE));
        window.setAttributes(lp);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setBackgroundColor(getIntColorFromJson(dialogParameters, IterableConstants.ITERABLE_IN_APP_BACKGROUND_COLOR, Color.WHITE));

        Point screenSize = getScreenSize(context);
        int fontConstant = getFontConstant(screenSize);

        //Title
        JSONObject titleJson = dialogParameters.optJSONObject(IterableConstants.ITERABLE_IN_APP_TITLE);
        if (titleJson != null) {
            TextView title = new TextView(context);
            title.setText(titleJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 24);
            title.setGravity(Gravity.CENTER);
            //TODO: update padding to be orientation relative
            title.setPadding(screenSize.x / 30, screenSize.y / 30, screenSize.x / 30, 0);
            title.setTextColor(getIntColorFromJson(titleJson, IterableConstants.ITERABLE_IN_APP_COLOR, Color.BLACK));
            verticalLayout.addView(title);
        }

        //Body
        JSONObject bodyJson = dialogParameters.optJSONObject(IterableConstants.ITERABLE_IN_APP_BODY);
        if (bodyJson != null) {
            TextView bodyText = new TextView(context);
            bodyText.setText(bodyJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));
            bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 36);
            bodyText.setGravity(Gravity.CENTER);
            bodyText.setTextColor(getIntColorFromJson(bodyJson, IterableConstants.ITERABLE_IN_APP_COLOR, Color.BLACK));
            bodyText.setPadding(screenSize.x/60,0,screenSize.x/60,screenSize.y/60);
            verticalLayout.addView(bodyText);
        }

        //Buttons
        JSONArray buttonJson = dialogParameters.optJSONArray(IterableConstants.ITERABLE_IN_APP_BUTTONS);
        if (buttonJson != null) {
            verticalLayout.addView(createButtons(context, buttonJson, null, clickCallback));
        }

        dialog.setContentView(verticalLayout);
        dialog.show();
    }

    public static void showFullScreenDialog(Context context, JSONObject dialogParameters, IterableInAppActionListener.IterableOnClick clickCallback) {

        Dialog dialog = new Dialog(context, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);

        verticalLayout.setBackgroundColor(getIntColorFromJson(dialogParameters, IterableConstants.ITERABLE_IN_APP_BACKGROUND_COLOR, Color.WHITE));
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        verticalLayout.setLayoutParams(linearLayoutParams);

        LinearLayout.LayoutParams equalParamHeight = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParamHeight.weight = 1;
        equalParamHeight.height = 0;

        Point size = getScreenSize(context);
        int dialogWidth = size.x;
        int dialogHeight = size.y;
        int fontConstant = getFontConstant(size);

        //Title
        JSONObject titleJson = dialogParameters.optJSONObject(IterableConstants.ITERABLE_IN_APP_TITLE);
        if (titleJson != null) {
            TextView title = new TextView(context);
            title.setText(titleJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 24);
            title.setGravity(Gravity.CENTER);
            title.setTextColor(getIntColorFromJson(titleJson, IterableConstants.ITERABLE_IN_APP_COLOR, Color.BLACK));
            verticalLayout.addView(title, equalParamHeight);
        }

        //Image
        ImageView imageView = new ImageView(context);
        verticalLayout.addView(imageView);
        try {
            Class picassoClass = Class.forName(IterableConstants.PICASSO_CLASS);
            if (picassoClass != null) {

                Picasso.
                        with(context.getApplicationContext()).
                        load(dialogParameters.optString(IterableConstants.ITERABLE_IN_APP_MAIN_IMAGE)).
                        resize(dialogWidth, dialogHeight/2).
                        centerInside().
                        into(imageView);
            }
        } catch (ClassNotFoundException e) {
            IterableLogger.e(TAG, "ClassNotFoundException: Check that picasso is added " +
                    "to the build dependencies", e);
        }

        //Body
        JSONObject bodyJson = dialogParameters.optJSONObject(IterableConstants.ITERABLE_IN_APP_BODY);
        if (bodyJson != null) {
            TextView bodyText = new TextView(context);
            bodyText.setText(bodyJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));
            bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 36);
            bodyText.setGravity(Gravity.CENTER);
            bodyText.setTextColor(getIntColorFromJson(bodyJson, IterableConstants.ITERABLE_IN_APP_COLOR, Color.BLACK));
            verticalLayout.addView(bodyText, equalParamHeight);
        }

        //Buttons
        JSONArray buttonJson = dialogParameters.optJSONArray(IterableConstants.ITERABLE_IN_APP_BUTTONS);
        if (buttonJson != null) {
            View bottomButtons = createButtons(context, buttonJson, dialogParameters, clickCallback);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.height = dialogHeight / 10;
            verticalLayout.addView(bottomButtons, buttonParams);
        }

        dialog.setContentView(verticalLayout);
        //dialog.setCancelable(false);
        dialog.show();
    }

    public static JSONObject getNextMessageFromPayload(String payload) {
        JSONObject returnObject = null;
        if (payload != null) {
            try {
                JSONObject mainObject = new JSONObject(payload);
                JSONArray jsonArray = mainObject.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
                if (jsonArray != null) {
                    returnObject = jsonArray.optJSONObject(0);
                }
            } catch (JSONException e) {
                IterableLogger.e(TAG, e.toString());
            }
        }
        return returnObject;
    }

    private static View createButtons(Context context, JSONArray buttons, JSONObject dataFields, IterableInAppActionListener.IterableOnClick clickCallback) {
        LinearLayout.LayoutParams equalParamWidth = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParamWidth.weight = 1;
        equalParamWidth.width = 0;

        LinearLayout linearlayout = new LinearLayout(context);
        linearlayout.setOrientation(LinearLayout.HORIZONTAL);

        JSONObject trackParams = new JSONObject();
        try {
            trackParams.put(IterableConstants.KEY_CAMPAIGNID, dataFields.optString(IterableConstants.KEY_CAMPAIGNID, null));
            trackParams.put(IterableConstants.KEY_TEMPLATE_ID, dataFields.optString(IterableConstants.KEY_TEMPLATE_ID, null));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < buttons.length(); i++) {
            JSONObject buttonJson = buttons.optJSONObject(i);
            if (buttonJson != null) {
                final Button button = new Button(context);
                button.setBackgroundColor(getIntColorFromJson(buttonJson, IterableConstants.ITERABLE_IN_APP_BACKGROUND_COLOR, Color.LTGRAY));
                String action = buttonJson.optString(IterableConstants.ITERABLE_IN_APP_BUTTON_ACTION);
                if (!action.isEmpty()) {
                    button.setOnClickListener(new IterableInAppActionListener(i, action, trackParams, clickCallback));
                }

                JSONObject textJson = buttonJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
                button.setTextColor(getIntColorFromJson(textJson, IterableConstants.ITERABLE_IN_APP_COLOR, Color.BLACK));
                button.setText(textJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));

                linearlayout.addView(button, equalParamWidth);
            }
        }
        return linearlayout;
    }

    private static int getFontConstant(Point size) {
        int fontConstant = (size.x > size.y) ? size.x : size.y;
        return fontConstant;
    }

    private static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private static int getIntColorFromJson(JSONObject json, String key, int defaultColor) {
        int backgroundColor = defaultColor;
        if (json != null) {
            String backgroundColorParam = json.optString(key);
            if (!backgroundColorParam.isEmpty()) {
                backgroundColor = Color.parseColor(backgroundColorParam);
            }
        }
        return backgroundColor;
    }

    private static int getNotificationLocation(String location){
        int locationValue;
        switch(location) {
            case IterableConstants.ITERABLE_IN_APP_TYPE_BOTTOM:
                locationValue = Gravity.BOTTOM;
                break;
            case IterableConstants.ITERABLE_IN_APP_TYPE_TOP:
                locationValue = Gravity.TOP;
                break;
            case IterableConstants.ITERABLE_IN_APP_TYPE_CENTER:
                locationValue = Gravity.CENTER;
                break;
            default: locationValue = Gravity.TOP;
        }
        return locationValue;
    }
}