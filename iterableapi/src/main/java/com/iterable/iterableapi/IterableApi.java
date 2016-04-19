package com.iterable.iterableapi;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
    static final String iterableBaseUrl = "http://192.168.86.103:9000/api/";

    static IterableApi sharedInstance = null;

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

    public void track(JSONObject dataFields) {

        String jsonString = dataFields.toString();
        sendRequest("events/trackPushOpen", jsonString);
    }

    protected void sendRequest(String uri, String json) {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(iterableBaseUrl + uri);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");

            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Content-type", "application/json");
            urlConnection.setRequestProperty("api_key", _apiKey);

            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(json);
            writer.flush();
            writer.close();
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}
