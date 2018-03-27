package com.iterable.iterableapi;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Created by David Truong dt@iterable.com.
 */

public class IterableLaunchActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        //TOOD: might not need the extra requestCode since the messageId is always in the original payload.
        int requestCode = extras.getInt(IterableConstants.REQUEST_CODE, 0);
        System.out.print("requestCode: "+requestCode);
        NotificationManager mNotificationManager = (NotificationManager)
                this.getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(requestCode);

        Class mainClass = (Class) extras.get(IterableConstants.MAIN_CLASS);

        //get the notification action here
        String actionName = intent.getAction();

        if(IterableConstants.ACTION_NOTIF_OPENED.equalsIgnoreCase(actionName)) {
            //Handles opens and deeplinks
            Intent mainIntent = new Intent(this, mainClass);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mainIntent.putExtras(extras);
            startActivity(mainIntent);
        }


        //TODO: if custom event - open up main class with custom event as the action name.

        //Check if the action should not open the application
        if (false) {
            //Don't open or foreground the app
            finish();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
            startActivity(browserIntent);

        } else {
            //Handles opens and deeplinks
            Intent mainIntent = new Intent(this, mainClass);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mainIntent.putExtras(extras);
            startActivity(mainIntent);
        }

        //TODO: How can i not bring the app to the foreground for a silent notification action.
    }
}
