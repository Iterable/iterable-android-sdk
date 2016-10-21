package com.iterable.iterableapi.InApp;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
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
import android.widget.TextView;

/**
 * Created by David Truong dt@iterable.com.
 */
public class IterableInAppManager {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void showNotification(Context context) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Light); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout linearlayout = new LinearLayout(context);
        linearlayout.setOrientation(LinearLayout.VERTICAL);

        ViewGroup.LayoutParams linearlayoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        TextView title = new TextView(context);
        title.setText("test title 1");
        title.setLayoutParams(linearlayoutparams);
        linearlayout.addView(title);

        TextView title2 = new TextView(context);
        title2.setText("test title 2");
        title2.setLayoutParams(linearlayoutparams);
        linearlayout.addView(title2);


        dialog.setContentView(linearlayout);


        dialog.show();

    }
}

