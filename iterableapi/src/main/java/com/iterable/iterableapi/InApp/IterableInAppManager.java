package com.iterable.iterableapi.InApp;

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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableLogger;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppManager {
    static final String TAG = "IterableInAppManager";

    public static void showNotification(Context context, JSONObject dialogOptions) {
        showFullScreenDialog(context, dialogOptions);
    }

    public static void showNotificationDialog(Context context, JSONObject dialogParameters) {
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
        lp.gravity = Gravity.TOP;
        window.setAttributes(lp);

        Point screenSize = getScreenSize(context);
        int fontConstant = getFontConstant(screenSize);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);

        int colorCol = Color.parseColor(dialogParameters.optString("backgroundColor"));
        verticalLayout.setBackgroundColor(colorCol);

        TextView title = new TextView(context);
        title.setText("Iterable");
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant/24);
        title.setGravity(Gravity.CENTER);
        //TODO: update padding to be orientation relative
        title.setPadding(screenSize.x/30,screenSize.y/30,screenSize.x/30, 0);
        title.setTextColor(Color.BLACK);
        verticalLayout.addView(title);

        JSONObject bodyJson = dialogParameters.optJSONObject("body");
        if (bodyJson != null) {
            //Body Text
            TextView bodyText = new TextView(context);
            bodyText.setText(bodyJson.optString("text"));
            bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 36);
            bodyText.setGravity(Gravity.CENTER);
            bodyText.setTextColor(Color.parseColor(bodyJson.optString("color")));
            bodyText.setPadding(screenSize.x/60,0,screenSize.x/60,screenSize.y/60);
            verticalLayout.addView(bodyText);
        }

        verticalLayout.addView(createButtons(context));

        dialog.setContentView(verticalLayout);
        dialog.show();
    }

    public static void showFullScreenDialog(Context context, JSONObject dialogParameters) {

        Dialog dialog = new Dialog(context, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);

        int colorCol = Color.parseColor(dialogParameters.optString("backgroundColor"));

        verticalLayout.setBackgroundColor(colorCol);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        verticalLayout.setLayoutParams(linearLayoutParams);

        LinearLayout.LayoutParams equalParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParam.weight = 1;
        equalParam.height = 0;

        Point size = getScreenSize(context);
        int dialogWidth = size.x;
        int dialogHeight = size.y;
        int fontConstant = getFontConstant(size);

        JSONObject titleJson = dialogParameters.optJSONObject("title");
        if (titleJson != null) {
            //Title Text
            TextView title = new TextView(context);
            title.setText(titleJson.optString("text"));
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 24);
            title.setGravity(Gravity.CENTER);
            title.setTextColor(Color.parseColor((titleJson.optString("color"))));
            verticalLayout.addView(title, equalParam);
        }

        ImageView imageView = new ImageView(context);
        try {
            Class picassoClass = Class.forName(IterableConstants.PICASSO_CLASS);
            if (picassoClass != null) {

                Picasso.
                        with(context.getApplicationContext()).
                        load(dialogParameters.optString("mainImage")).
                        resize(dialogWidth, dialogHeight/2).
                        centerInside().
                        into(imageView);
            }
        } catch (ClassNotFoundException e) {
            IterableLogger.e(TAG, "ClassNotFoundException: Check that picasso is added " +
                    "to the build dependencies", e);
        }

        verticalLayout.addView(imageView);

        JSONObject bodyJson = dialogParameters.optJSONObject("body");
        if (bodyJson != null) {
            //Body Text
            TextView bodyText = new TextView(context);
            bodyText.setText(bodyJson.optString("text"));
            bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 36);
            bodyText.setGravity(Gravity.CENTER);

            bodyText.setTextColor(Color.parseColor((bodyJson.optString("color"))));
            verticalLayout.addView(bodyText, equalParam);
        }

        //Adds in the bottom buttons
        View bottomButtons = createButtons(context);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.height = dialogHeight/10;
        verticalLayout.addView(bottomButtons, buttonParams);

        dialog.setContentView(verticalLayout);
        //dialog.setCancelable(false);
        dialog.show();
    }

    public static View createButtons(Context context) {
        RelativeLayout bottomButtons = new RelativeLayout(context);
        bottomButtons.setBackgroundColor(Color.CYAN);

        LinearLayout.LayoutParams equalParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParam.weight = 1;
        equalParam.width = 0;

        //TODO: Loop through buttons
        Button buttonLeft = new Button(context);
        buttonLeft.setBackgroundColor(Color.LTGRAY);
        buttonLeft.setTextColor(Color.WHITE);
        buttonLeft.setText("CLOSE");
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Toast.makeText(v.getContext(), "You clicked on Left", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonRight = new Button(context);
        buttonRight.setBackgroundColor(Color.BLACK);
        buttonRight.setTextColor(Color.WHITE);
        buttonRight.setText("ORDER NOW");
        buttonRight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Toast.makeText(v.getContext(), "You clicked on Right", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout linearlayout = new LinearLayout(context);
        linearlayout.setOrientation(LinearLayout.HORIZONTAL);
        linearlayout.addView(buttonLeft, equalParam);
        linearlayout.addView(buttonRight, equalParam);

        return linearlayout;
    }

    public static JSONObject getNextMessageFromPayload(String s) {
        JSONObject mainObject = null;
        try {
            mainObject = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = mainObject.optJSONArray("inAppMessages");
        JSONObject message = jsonArray.optJSONObject(0);
        return message.optJSONObject("content");
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

    private static LinearLayout.LayoutParams equalParam() {
        LinearLayout.LayoutParams equalParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParam.weight = 1;
        equalParam.width = 0;
        return equalParam;
    }
}