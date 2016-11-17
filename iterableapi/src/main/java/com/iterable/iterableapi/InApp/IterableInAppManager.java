package com.iterable.iterableapi.InApp;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
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

import com.iterable.iterableapi.IterableApi;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import iterable.com.iterableapi.R;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppManager {

    public static int colorCol;

    public static void showNotification(Context context) {
        testRounded(context);
    }

    public static void testRounded(Context context) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Material_NoActionBar); //Theme_Material_NoActionBar_Overscan
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        //dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//        lp.dimAmount = .8f;
        window.setAttributes(lp);

        /*ImageView NormalImageView;
        Bitmap ImageBit;
        float ImageRadius = 10.0f;


        NormalImageView = new ImageView(context);

        //ImageBit = BitmapFactory.decodeResource(context.getResources(), R.drawable.common_full_open_on_phone);
        ImageBit = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        NormalImageView.setImageBitmap(ImageBit);
        RoundedBitmapDrawable RBD = RoundedBitmapDrawableFactory.create(context.getResources(),ImageBit);
        RBD.setCornerRadius(ImageRadius);
        RBD.setAntiAlias(true);
        NormalImageView.setImageDrawable(RBD);
        //NormalImageView.setBackgroundColor(Color.RED);
//        dialog.setContentView(NormalImageView);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.RED);
        gd.setCornerRadius(20);
        gd.setStroke(2, Color.GREEN);*/

        ImageView imageView = new ImageView(context);

        GradientDrawable gd = new GradientDrawable();

        // Set the color array to draw gradient
        gd.setColor(Color.RED);

        // Set the GradientDrawable gradient type linear gradient
        //gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        // Set GradientDrawable shape is a rectangle
        gd.setShape(GradientDrawable.RECTANGLE);

        gd.setStroke(3, Color.BLACK);

        gd.setCornerRadius(50);

        // Set GradientDrawable width and in pixels
        //gd.setSize(450, 150); // Width 450 pixels and height 150 pixels

        // Set GradientDrawable as ImageView source image
        imageView.setImageDrawable(gd);

        dialog.setContentView(imageView);
        dialog.show();
    }

    public static void showNotificationDialog(Context context) {
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
        lp.gravity = Gravity.TOP; //update this for the location
        window.setAttributes(lp);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setBackgroundColor(Color.GREEN);

        TextView title = new TextView(context);
        title.setText("Iterable \nand the future of growth marketing");
        //        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant/16);
        title.setGravity(Gravity.CENTER);
        title.setPadding(100,100,100,100);
        title.setTextColor(Color.BLACK);
        title.setBackgroundColor(Color.MAGENTA);
        verticalLayout.addView(title);

        /*View bottomButtons = createButtons(context);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.height = 50;
        verticalLayout.addView(bottomButtons);*/



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
        buttonLeft.setHeight(100);

        Button buttonRight = new Button(context);
        buttonRight.setBackgroundColor(colorCol);
        buttonRight.setTextColor(Color.WHITE);
        buttonRight.setText("ORDER NOW");
        buttonRight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Toast.makeText(v.getContext(), "You clicked on Right", Toast.LENGTH_SHORT).show();
            }
        });
        buttonRight.setHeight(100);

        LinearLayout linearlayout = new LinearLayout(context);
        linearlayout.setOrientation(LinearLayout.HORIZONTAL);
//        linearlayout.addView(buttonLeft);
//        linearlayout.addView(buttonRight);
        LinearLayout.LayoutParams linearlayoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        verticalLayout.addView(linearlayout);
        verticalLayout.addView(createButtons(context));


        ImageView lineColorCode = (ImageView)new ImageView(context);
        int color = Color.parseColor("#AE6118"); //The color u want
        lineColorCode.setColorFilter(color);

        linearlayout.addView(lineColorCode);
        verticalLayout.addView(linearlayout);

        dialog.setContentView(verticalLayout);
        dialog.show();
    }

    public static void showFullScreenDialog(Context context, String dialogOptions) {
        JSONObject dialogParameters =  getNextMessageFromPayload(dialogOptions);

        Dialog dialog = new Dialog(context, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);

        int col = Color.parseColor("#FF352400");
        int r = ((col & 0xFF000000) >> 24);
        int g = ((col & 0xFF0000) >>  16);
        int b = ((col & 0xFF00) >> 8);
        int a = ((col & 0xFF));
        colorCol = Color.argb(a, r, g, b);

        verticalLayout.setBackgroundColor(colorCol);
        LinearLayout.LayoutParams linearlayoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        verticalLayout.setLayoutParams(linearlayoutparams);

        LinearLayout.LayoutParams equalParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParam.weight = 1;
        equalParam.height = 0;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int dialogWidth = size.x;
        int dialogHeight = size.y;
        int fontConstant = (dialogWidth > dialogHeight) ? dialogWidth : dialogHeight;

        JSONObject titleJson = dialogParameters.optJSONObject("title");

        if (titleJson != null) {
            //Title Text
            TextView title = new TextView(context);
            title.setText(titleJson.optString("text"));
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 16);
            title.setGravity(Gravity.CENTER);
            title.setTextColor(Color.WHITE);
            //title.setBackgroundColor(colorCol);
            verticalLayout.addView(title, equalParam);
        }

        //Main Image
        ImageView imageContent = new ImageView(context);
        verticalLayout.addView(imageContent);

        ImageView imageView = new ImageView(context);
        //TODO: add runtime checks if picasso is loaded
        Picasso.
                with(context.getApplicationContext()).
                load(dialogParameters.optString("mainImage")).
                resize(dialogWidth, 0). //TOOD: if in landscape mode use the height to be 1/2 of the screen height
                into(imageView);

        verticalLayout.addView(imageView);

        JSONObject bodyJson = dialogParameters.optJSONObject("body");

        if (bodyJson != null) {
            //Body Text
            TextView bodyText = new TextView(context);
            bodyText.setText(bodyJson.optString("text"));
            bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant / 30);
            bodyText.setGravity(Gravity.CENTER);
            bodyText.setTextColor(Color.BLACK);
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
}

class InAppParameters {

    public InAppParameters(String jsonString) {
        JSONObject mainObject = null;
        try {
            mainObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mainObject.optString("");
    }
}
