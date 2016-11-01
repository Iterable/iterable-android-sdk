package com.iterable.iterableapi.InApp;

import android.app.Dialog;
import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.graphics.Point;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.InputStream;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppManager {

    public static void showNotification(Context context) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
//        verticalLayout.setBackgroundColor(Color.BLUE);
//        verticalLayout.setBackgroundColor(0x333333EE);
        int col = 0x004C99FF;
        int r = ((col & 0xFF000000) >> 24);
        int g = ((col & 0xFF0000) >>  16);
        int b = ((col & 0xFF00) >> 8);
        int a = ((col & 0xFF));
//        verticalLayout.setBackgroundColor(Color.rgb(51, 51,51));//a, r, g, b));
        verticalLayout.setBackgroundColor(Color.argb(a, r, g, b));
        LinearLayout.LayoutParams linearlayoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        verticalLayout.setLayoutParams(linearlayoutparams);

        LinearLayout.LayoutParams equalParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        equalParam.weight = 1;
        equalParam.height = 0;

        //NSInteger fontConstant = (self.view.frame.size.width > self.view.frame.size.height) ? self.view.frame.size.width : self.view.frame.size.height;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int dialogWidth = size.x;
        int dialogHeight = size.y;
//        int dialogWidth = dialog.getWindow().getDecorView().getWidth();
//        int dialogHeight = dialog.getWindow().getDecorView().getHeight();
        int fontConstant = (dialogWidth > dialogHeight) ? dialogWidth : dialogHeight;

        //Title Text
        TextView title = new TextView(context);
        title.setText("SEATGEEK");
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant/16);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);
        verticalLayout.addView(title, equalParam);

        //Main Image
        ImageView imageContent = new ImageView(context);
        verticalLayout.addView(imageContent);

        ImageView imageView = new ImageView(context);
        //TODO: add runtime checks if picasso is loaded
        Picasso.
                with(context.getApplicationContext()).
                load("https://www.gstatic.com/images/branding/googlelogo/2x/googlelogo_color_284x96dp.png").
//                load("http://i.dailymail.co.uk/i/pix/2016/01/04/21/2FA753A200000578-3384303-image-a-38_1451941484917.jpg").
                resize(dialogWidth, 0). //TOOD: if in landscape mode use the height to be 1/2 of the screen height
                into(imageView);

        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        //imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        verticalLayout.addView(imageView);

        //Body Text
        TextView bodyText = new TextView(context);
        bodyText.setText("Sample Image above. Plus a description which can be long and multi line");
        bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontConstant/30);
        bodyText.setGravity(Gravity.CENTER);
        bodyText.setTextColor(Color.WHITE);
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
        buttonLeft.setBackgroundColor(Color.GREEN);
        buttonLeft.setText("button click Left");
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Toast.makeText(v.getContext(), "You clicked on Left", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonRight = new Button(context);
        buttonRight.setBackgroundColor(Color.RED);
        buttonRight.setText("button click Right");
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

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        bottomButtons.addView(linearlayout, lp);
        return bottomButtons;
    }
}


