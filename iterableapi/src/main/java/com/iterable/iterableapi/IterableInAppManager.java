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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

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

    public static void showIterableNotificationHTML(Context context, String htmlString, String messageId, IterableHelper.IterableActionHandler clickCallback, double backgroundAlpha, Rect padding) {
        if (context instanceof Activity) {
            Activity currentActivity = (Activity) context;
            if (htmlString != null) {
                IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.createInstance(context, htmlString); //and data
                notification.setTrackParams(messageId);
                notification.setCallback(clickCallback);
                notification.setBackgroundAlpha(backgroundAlpha);
                notification.setPadding(padding);
                notification.setOwnerActivity(currentActivity);
                notification.show();
            }
        } else {
            IterableLogger.w(TAG, "To display in-app notifications, the context must be of an instance of: Activity");
        }
    }

    /**
     * Displays an InApp Notification from the dialogOptions; with a click callback handler.
     * @param context
     * @param dialogOptions
     * @param clickCallback
     */
    public static void showNotification(Context context, JSONObject dialogOptions, String messageId, IterableHelper.IterableActionHandler clickCallback) {
        if(dialogOptions != null) {
            String type = dialogOptions.optString(IterableConstants.ITERABLE_IN_APP_TYPE);
            if (type.equalsIgnoreCase(IterableConstants.ITERABLE_IN_APP_TYPE_FULL)) {
                showFullScreenDialog(context, dialogOptions, messageId, clickCallback);
            } else {
                showNotificationDialog(context, dialogOptions, messageId, clickCallback);
            }
        } else {
            IterableLogger.d(TAG, "In-App notification not displayed: showNotification must contain valid dialogOptions");
        }
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
     * Creates and shows a Full Screen InApp Notification; with a click callback handler.
     * @param context
     * @param dialogParameters
     * @param clickCallback
     */
    static void showFullScreenDialog(Context context, JSONObject dialogParameters, String messageId, IterableHelper.IterableActionHandler clickCallback) {
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
            String imageUrl = dialogParameters.optString(IterableConstants.ITERABLE_IN_APP_MAIN_IMAGE);
            if (picassoClass != null && !imageUrl.isEmpty()) {
                Picasso.
                        with(context.getApplicationContext()).
                        load(imageUrl).
                        resize(dialogWidth, dialogHeight/2).
                        centerInside().
                        into(imageView);
            }
        } catch (ClassNotFoundException e) {
            IterableLogger.w(TAG, "ClassNotFoundException: Check that picasso is added " +
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
            View buttons = createButtons(context, dialog, buttonJson, messageId, clickCallback);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.height = dialogHeight / 10;
            verticalLayout.addView(buttons, buttonParams);
        }

        dialog.setContentView(verticalLayout);
        //dialog.setCancelable(false);
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

    public static Rect getPaddingFromPayload(JSONObject paddingOptions) {
        Rect rect = new Rect();
        rect.top = decodePadding(paddingOptions.optJSONObject("top"));
        rect.left = decodePadding(paddingOptions.optJSONObject("left"));
        rect.bottom = decodePadding(paddingOptions.optJSONObject("bottom"));
        rect.right = decodePadding(paddingOptions.optJSONObject("right"));

        return rect;
    }

    static int decodePadding(JSONObject jsonObject) {
        int returnPadding = 0;
        if (jsonObject != null) {
            if ((jsonObject.optString("displayOption") == "AutoExpand")) {
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