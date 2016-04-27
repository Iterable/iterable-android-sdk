package com.iterable.iterableapi;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyPair;
import java.util.Map;

/**
 * Created by davidtruong on 4/4/16.
 */
public class IterableApi {

    /**
     * Configuration URLs for different enviornment endpoints.
     * TODO: Should this be moved into IterableRequest?
     */
    //static final String iterableBaseUrl = "https://api.iterable.com/api/";
    //static final String iterableBaseUrl = "https://canary.iterable.com/api/";
    //static final String iterableBaseUrl = "http://staging.iterable.com/api/";
    static final String iterableBaseUrl = "http://Davids-MBP-2.lan:9000/api/"; //Local Dev endpoint

    protected static IterableApi sharedInstance = null;

    static Application application;
    static Context applicationContext;

    private Context _context;
    private String _apiKey;
    private String _email;

    public IterableApi(Context context, String apiKey, String email){
        //TODO: add in data validation

        this._context = context;
        this._apiKey = apiKey;
        this._email = email;
    }

    /**
     * Creates and returns the stored IterableApi instance.
     * @param context
     * @param apikey
     * @param email
     * @return the singleton instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKey(Context context, String apikey, String email)
    {
        //TODO: what if the singleton is called with different init params?
        if (sharedInstance == null)
        {
            sharedInstance = new IterableApi(context, apikey, email);
        }

        return sharedInstance;
    }

    /**
     * Registers the GCM registration ID with Iterable.
     * @param token
     */
    public void registerDeviceToken(String applicationName, String token) {
        registerDeviceToken(applicationName, token, null);
    }

    /**
     * Registers the GCM registration ID with Iterable.
     * @param token
     * @param dataFields
     */
    public void registerDeviceToken(String applicationName, String token, JSONObject dataFields) {
        //TODO: Update thie platform flag for Kindle support based upon device type or store build
        String platform = "GCM";

        //TODO: Investigate create a self service page on our site to create the push integration application
//        int stringId = _context.getApplicationInfo().labelRes;
//        applicationName  = _context.getString(stringId);

        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put("email", _email);
            JSONObject device = new JSONObject();
            device.put("token", token);
            device.put("platform", platform);
            device.put("applicationName", applicationName);

            if (dataFields != null) {
                device.put("dataFields", dataFields);
            }
            requestJSON.put("device", device);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("users/registerDeviceToken", requestJSON);
    }

    public void track(String eventName, JSONObject dataFields) {
        track(eventName, dataFields, null);
    }

    public void track(String eventName, JSONObject dataFields, Map<String, Object> additionalParams) {
        String[] optArgs = {"createdAt", "dataFields", "campaignId", "templateId"};

        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("email", _email);
            requestJSON.put("eventName", eventName);

            if (dataFields != null) {
                requestJSON.put("dataFields", dataFields);
            }

            if (additionalParams != null) {
                for (String optArgsKey : optArgs) {
                    if (additionalParams.containsKey(optArgsKey)) {
                        requestJSON.put(optArgsKey, additionalParams.get(optArgsKey));
                    }
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("events/track", requestJSON);
    }


    public void trackConversion(int campaignId, JSONObject dataFields) {

        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("email", _email);
            requestJSON.put("campaignId", campaignId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("events/trackConversion", requestJSON);
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

        sendRequest("events/trackPushOpen", requestJSON);
    }

    public void sendPush(String email, int campaignId) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("recipientEmail", email);
            requestJSON.put("campaignId", campaignId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("push/target", requestJSON);
    }

    // FIXME: 4/22/16
    public void trackPurchase(CommerceItem[] commerceItems, double total) {
        JSONObject requestJSON = new JSONObject();

        //TODO: optional user
        JSONObject userJSON = new JSONObject();

        for(CommerceItem item : commerceItems) {
            //item.serialize();
        }

        try {
            userJSON.put("email", _email);
            requestJSON.put("user", userJSON);
            requestJSON.put("items", commerceItems.toString());
            requestJSON.put("total", total);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("commerce/trackPurchase", requestJSON);
    }

    // Performs network operations on an async thread instead of the main thread.
    private void sendRequest(String uri, JSONObject json) {
        new IterableRequest().execute(iterableBaseUrl, _apiKey, uri, json.toString());
    }
}
