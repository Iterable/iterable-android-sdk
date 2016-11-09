package com.iterable.iterableapi.InApp;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
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

import com.squareup.picasso.Picasso;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppManager {

    public static int colorCol;

    public static void showNotification(Context context) {
        showNotificationDialog(context);
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
        window.setAttributes(lp);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setBackgroundColor(Color.GREEN);

        TextView title = new TextView(context);
        title.setText("Iterable \nand the future of growth marketing");
        //        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant/16);
        title.setGravity(Gravity.CENTER);
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

        dialog.setContentView(verticalLayout);
        dialog.show();
    }

    public static void showFullScreenDialog(Context context) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);

        int col = 0xEC3524FF;
        int r = ((col & 0xFF000000) >> 24);
        int g = ((col & 0xFF0000) >>  16);
        int b = ((col & 0xFF00) >> 8);
        int a = ((col & 0xFF));
        colorCol = Color.argb(a, r, g, b);
        verticalLayout.setBackgroundColor(Color.WHITE);
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

        //Title Text
        TextView title = new TextView(context);
        title.setText("FIVE GUYS \nBURGERS and FRIES");
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant/16);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);
        title.setBackgroundColor(colorCol);
        verticalLayout.addView(title, equalParam);

        //Main Image
        ImageView imageContent = new ImageView(context);
        verticalLayout.addView(imageContent);

        ImageView imageView = new ImageView(context);
        //TODO: add runtime checks if picasso is loaded
        Picasso.
                with(context.getApplicationContext()).
                load("http://blogs-images.forbes.com/benkepes/files/2014/10/workflows-1940x1233.png").

                resize(dialogWidth, 0). //TOOD: if in landscape mode use the height to be 1/2 of the screen height
                into(imageView);

        verticalLayout.addView(imageView);

        //Body Text
        TextView bodyText = new TextView(context);
        bodyText.setText("Handcrafted BURGERS & FRIES since 1986.");
        bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant/30);
        bodyText.setGravity(Gravity.CENTER);
        bodyText.setTextColor(Color.BLACK);
        verticalLayout.addView(bodyText, equalParam);

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
        buttonRight.setBackgroundColor(colorCol);
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

}


