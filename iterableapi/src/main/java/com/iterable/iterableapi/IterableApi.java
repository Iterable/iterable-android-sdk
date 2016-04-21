package com.iterable.iterableapi;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by davidtruong on 4/4/16.
 */
public class IterableApi {
    private String _apiKey;
    private String _email;


    //Configs for endpoints
    //static final String iterableBaseUrl = "https://api.iterable.com/api/";
    //static final String iterableBaseUrl = "https://canary.iterable.com/api/";
    //static final String iterableBaseUrl = "http://staging.iterable.com/api/";
    static final String iterableBaseUrl = "http://Davids-MBP-2.lan:9000/api/"; //Local Dev endpoint

    protected static IterableApi sharedInstance = null;

    static Application application;
    static Context applicationContext;

    //Singleton
    public static IterableApi sharedInstanceWithApiKey(String apikey, String email)
    {
        //TODO: what if the app is already running and the notif is pressed?
        if (sharedInstance == null)
        {
            sharedInstance = new IterableApi(apikey, email);

            //Create instance and track pushOpen
            //sharedInstance.trackPushOpen();
        }

        return sharedInstance;
    }

    public IterableApi(String apiKey, String email){
        //TODO: add in data validation
        this._apiKey = apiKey;
        this._email = email;
    }

    public static void init(Context applicationContext){
        IterableApi.applicationContext = applicationContext;
        Class applicationClass = applicationContext.getClass();
        Log.d("class", applicationClass.toString());
    }

    public void registerDeviceToken(String email, String regid) {
        String platform = "GCM";
        //TODO: update this application name
        String applicationName = "iterableapp";

        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put("email", email);
            JSONObject device = new JSONObject();
            device.put("token", regid);
            device.put("platform", platform);
            device.put("applicationName", applicationName);
            JSONObject dataFields = new JSONObject();

            device.put("dataFields", dataFields);
            requestJSON.put("device", device);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String jsonString = requestJSON.toString();
        sendRequest("users/registerDeviceToken", jsonString);
    }

    public void track(String eventName, JSONObject dataFields) {

        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("email", _email);
            requestJSON.put("eventName", eventName);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        String jsonString = requestJSON.toString();
        sendRequest("events/track", jsonString);
    }

    public void trackConversion(String email, int campaignId, JSONObject dataFields) {

        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("email", email);
            requestJSON.put("campaignId", campaignId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        String jsonString = requestJSON.toString();
        sendRequest("events/trackConversion", jsonString);
    }

    public void trackPushOpen(int campaignId, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("email", _email);
            requestJSON.put("campaignId", campaignId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        String jsonString = requestJSON.toString();
        sendRequest("events/trackPushOpen", jsonString);
    }


    //TODO: setup test pushes with a given campaignID
    public void sendPush(String email, int campaignId) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("recipientEmail", email);
            requestJSON.put("campaignId", campaignId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        String jsonString = requestJSON.toString();
        sendRequest("push/target", jsonString);
    }

    // Performs network operations on an async thread instead of the main thread.
    protected void sendRequest(String uri, String json) {
        new IterableRequest().execute(iterableBaseUrl, _apiKey, uri, json);
    }

}
