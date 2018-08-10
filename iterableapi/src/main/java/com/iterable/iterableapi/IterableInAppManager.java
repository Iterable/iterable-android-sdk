package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

import android.graphics.Color;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com.
 *
 * The IterableInAppManager handles creating and rendering different types of InApp Notifications received from the IterableApi
 */
public class IterableInAppManager {
    static final String TAG = "IterableInAppManager";

    /**
     * Displays an html rendered InApp Notification
     * @param context
     * @param htmlString
     * @param messageId
     * @param clickCallback
     * @param backgroundAlpha
     * @param padding
     */
    public static boolean showIterableNotificationHTML(Context context, String htmlString, String messageId, IterableHelper.IterableActionHandler clickCallback, double backgroundAlpha, Rect padding) {
        if (context instanceof Activity) {
            Activity currentActivity = (Activity) context;
            if (htmlString != null) {
                if (IterableInAppHTMLNotification.getInstance() != null) {
                    IterableLogger.w(TAG, "Skipping the in-app notification: another notification is already being displayed");
                    return false;
                }

                IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.createInstance(context, htmlString);
                notification.setTrackParams(messageId);
                notification.setCallback(clickCallback);
                notification.setBackgroundAlpha(backgroundAlpha);
                notification.setPadding(padding);
                notification.setOwnerActivity(currentActivity);
                notification.show();
                return true;
            }
        } else {
            IterableLogger.w(TAG, "To display in-app notifications, the context must be of an instance of: Activity");
        }
        return false;
    }

    /**
     * Creates and shows a pop-up InApp Notification; with a click callback handler.
     * @param context
     * @param dialogParameters
     * @param clickCallback
     */
    static void showNotificationDialog(Context context, JSONObject dialogParameters, String messageId, IterableHelper.IterableActionHandler clickCallback) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Material_NoActionBar);
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
            title.setPadding(10, 5, 10, 5);
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
            bodyText.setPadding(10,0,10,10);
            verticalLayout.addView(bodyText);
        }

        //Buttons
        JSONArray buttonJson = dialogParameters.optJSONArray(IterableConstants.ITERABLE_IN_APP_BUTTONS);
        if (buttonJson != null) {
            verticalLayout.addView(createButtons(context, dialog, buttonJson, messageId, clickCallback));
        }

        dialog.setContentView(verticalLayout);
        dialog.show();
    }

    /**
     * Gets the next message from the payload
     * @param payload
     * @return
     */
    public static JSONObject getNextMessageFromPayload(String payload) {
        JSONObject returnObject = null;
        if (payload != null && payload != "") {
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

    /**
     * Returns a Rect containing the paddingOptions
     * @param paddingOptions
     * @return
     */
    public static Rect getPaddingFromPayload(JSONObject paddingOptions) {
        Rect rect = new Rect();
        rect.top = decodePadding(paddingOptions.optJSONObject("top"));
        rect.left = decodePadding(paddingOptions.optJSONObject("left"));
        rect.bottom = decodePadding(paddingOptions.optJSONObject("bottom"));
        rect.right = decodePadding(paddingOptions.optJSONObject("right"));

        return rect;
    }

    /**
     * Retrieves the padding percentage
     * @discussion -1 is returned when the padding percentage should be auto-sized
     * @param jsonObject
     * @return
     */
    static int decodePadding(JSONObject jsonObject) {
        int returnPadding = 0;
        if (jsonObject != null) {
            if ("AutoExpand".equalsIgnoreCase(jsonObject.optString("displayOption"))) {
                returnPadding = -1;
            } else {
                returnPadding = jsonObject.optInt("percentage", 0);
            }
        }
        return returnPadding;
    }

    /**
     * Creates the button for an InApp Notification
     * @param context
     * @param dialog
     * @param buttons
     * @param messageId
     * @param clickCallback
     * @return
     */
    private static View createButtons(Context context, Dialog dialog, JSONArray buttons, String messageId, IterableHelper.IterableActionHandler clickCallback) {
        LinearLayout.LayoutParams equalParamWidth = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParamWidth.weight = 1;
        equalParamWidth.width = 0;

        LinearLayout linearlayout = new LinearLayout(context);
        linearlayout.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < buttons.length(); i++) {
            JSONObject buttonJson = buttons.optJSONObject(i);
            if (buttonJson != null) {
                final Button button = new Button(context);
                button.setBackgroundColor(getIntColorFromJson(buttonJson, IterableConstants.ITERABLE_IN_APP_BACKGROUND_COLOR, Color.LTGRAY));
                String action = buttonJson.optString(IterableConstants.ITERABLE_IN_APP_BUTTON_ACTION);
                button.setOnClickListener(new IterableInAppActionListener(dialog, i, action, messageId, clickCallback));

                JSONObject textJson = buttonJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
                button.setTextColor(getIntColorFromJson(textJson, IterableConstants.ITERABLE_IN_APP_COLOR, Color.BLACK));
                button.setText(textJson.optString(IterableConstants.ITERABLE_IN_APP_TEXT));

                linearlayout.addView(button, equalParamWidth);
            }
        }
        return linearlayout;
    }

    /**
     * Gets the portrait height of the screen to use as a constant
     * @param size
     * @return
     */
    private static int getFontConstant(Point size) {
        int fontConstant = (size.x > size.y) ? size.x : size.y;
        return fontConstant;
    }

    /**
     * Gets the dimensions of the device
     * @param context
     * @return
     */
    private static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    /**
     * Gets the int value of the color from the payload
     * @param payload
     * @param key
     * @param defaultColor
     * @return
     */
    private static int getIntColorFromJson(JSONObject payload, String key, int defaultColor) {
        int backgroundColor = defaultColor;
        if (payload != null) {
            String backgroundColorParam = payload.optString(key);
            if (!backgroundColorParam.isEmpty()) {
                backgroundColor = Color.parseColor(backgroundColorParam);
            }
        }
        return backgroundColor;
    }

    /**
     * Returns the gravity for a given displayType location
     * @param location
     * @return
     */
    private static int getNotificationLocation(String location){
        int locationValue;
        switch(location.toUpperCase()) {
            case IterableConstants.ITERABLE_IN_APP_TYPE_BOTTOM:
                locationValue = Gravity.BOTTOM;
                break;
            case IterableConstants.ITERABLE_IN_APP_TYPE_TOP:
                locationValue = Gravity.TOP;
                break;
            case IterableConstants.ITERABLE_IN_APP_TYPE_CENTER:
            default: locationValue = Gravity.CENTER;
        }
        return locationValue;
    }
}