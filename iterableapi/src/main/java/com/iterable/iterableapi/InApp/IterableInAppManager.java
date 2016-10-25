package com.iterable.iterableapi.InApp;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppManager {

    public static void showNotification(Context context) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        RelativeLayout centerLayout = new RelativeLayout(context);
        centerLayout.setBackgroundColor(Color.GREEN);
        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        //LinearLayout.LayoutParams linearlayoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams linearlayoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        verticalLayout.setLayoutParams(linearlayoutparams);



        TextView title = new TextView(context);
        title.setText("test title 1");
        //title.setLayoutParams(linearlayoutparams);
        verticalLayout.addView(title);

        TextView title2 = new TextView(context);
        title2.setText("test title 2");
        //title2.setLayoutParams(linearlayoutparams);
        verticalLayout.addView(title2);

        View bottomButtons = createButtons(context);
        verticalLayout.addView(bottomButtons);

        verticalLayout.setBackgroundColor(Color.BLUE);

        centerLayout.addView(verticalLayout);

        dialog.setContentView(centerLayout, centerParams);

        dialog.setCancelable(false);
        dialog.show();

    }

    public static View createButtons(Context context) {
        RelativeLayout bottomButtons = new RelativeLayout(context);
        bottomButtons.setBackgroundColor(Color.CYAN);

        Button buttonLeft = new Button(context);
        //b.setTextColor(65793);
        //b.setBackgroundColor(13107200);
        buttonLeft.setText("button click Left");
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Toast.makeText(v.getContext(), "You clicked on Left", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonRight = new Button(context);
        //b.setTextColor(65793);
        //b.setBackgroundColor(13107200);
        buttonRight.setText("button click Right");
        buttonRight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Toast.makeText(v.getContext(), "You clicked on Right", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout linearlayout = new LinearLayout(context);
        linearlayout.setOrientation(LinearLayout.HORIZONTAL);
        linearlayout.addView(buttonLeft);
        linearlayout.addView(buttonRight);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT); // You might want to tweak these to WRAP_CONTENT
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        bottomButtons.addView(linearlayout, lp);
        return bottomButtons;
    }
}

