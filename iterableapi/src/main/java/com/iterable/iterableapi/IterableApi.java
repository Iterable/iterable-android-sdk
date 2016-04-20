package com.iterable.iterableapi;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
    static final String iterableBaseUrl = "http://192.168.86.102:9000/api/";

    static final int devTestCampaignId = 2;

    static IterableApi sharedInstance = null;

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

    public void track(JSONObject dataFields) {

        String jsonString = dataFields.toString();
        sendRequest("events/trackPushOpen", jsonString);
    }

    //TODO: setup test pushes with a given campaignID
    public void sendPush(String email) {
        JSONObject requestJSON = new JSONObject();
        int campaignId = devTestCampaignId;

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

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            //TODO: read input stream to validate request status

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}
