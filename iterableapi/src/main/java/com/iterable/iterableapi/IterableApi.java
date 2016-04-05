package com.iterable.iterableapi;

import android.util.Log;

/**
 * Created by davidtruong on 4/4/16.
 */
public class IterableApi {

    static IterableApi sharedInstance = null;

    //Singleton
    public static IterableApi sharedInstanceWithApiKey(String apikey, String email)
    {
        //TODO: what if the app is already running and the notif is pressed?
        if (sharedInstance == null)
        {
            sharedInstance = new IterableApi();
            //Create instance
            //sharedInstance.trackPushOpen();
        }

        return sharedInstance;
    }

    public static void connect() {
        Log.d("test", "msg");
    }

    public static void semee(){

    }

}
