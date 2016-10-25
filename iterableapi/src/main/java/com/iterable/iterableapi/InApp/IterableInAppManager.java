package com.iterable.iterableapi.InApp;

import android.app.Dialog;
import android.content.Context;

import android.graphics.Color;

import android.view.View;
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

        LinearLayout verticalLayout = new LinearLayout(context);
        verticalLayout.setOrientation(LinearLayout.VERTICAL);
        verticalLayout.setBackgroundColor(Color.BLUE);
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

        //Adds in the bottom buttons
        View bottomButtons = createButtons(context);
        verticalLayout.addView(bottomButtons);

        dialog.setContentView(verticalLayout);
        dialog.setCancelable(false);
        dialog.show();

    }

    public static View createButtons(Context context) {
        RelativeLayout bottomButtons = new RelativeLayout(context);
        bottomButtons.setBackgroundColor(Color.CYAN);

        LinearLayout.LayoutParams equalParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
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

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        bottomButtons.addView(linearlayout, lp);
        return bottomButtons;
    }
}

