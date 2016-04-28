package com.iterable.iterableapi;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidtruong on 4/4/16.
 */
public class IterableApi {

    /**
     * Configuration URLs for different enviornment endpoints.
     * TODO: Should this be moved into IterableRequest or into an xml/constants file?
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
     * @param apiKey
     * @param email
     * @return the singleton instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKey(Context context, String apiKey, String email)
    {
        //TODO: what if the singleton is called with different init params?
        // Should we call users/update or require the app to do so?
        if (sharedInstance == null)
        {
            sharedInstance = new IterableApi(context, apiKey, email);
        }

        return sharedInstance;
    }

    /**
     * Registers the GCM registration ID with Iterable.
     * @param applicationName
     * @param token
     */
    public void registerDeviceToken(String applicationName, String token) {
        registerDeviceToken(applicationName, token, null);
    }

    /**
     * Registers the GCM registration ID with Iterable.
     * @param applicationName
     * @param token
     * @param dataFields
     */
    public void registerDeviceToken(String applicationName, String token, JSONObject dataFields) {
        //TODO: Update thie platform flag for Kindle support based upon device type or store build
        String platform = "GCM";

        // TODO: Investigate creating a self service page on our site to
        // create the push integration application
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

    public void track(String eventName) {
        track(eventName, null, null, null);
    }

    public void track(String eventName, JSONObject dataFields) {
        track(eventName, null, null, dataFields);
    }

    public void track(String eventName, String campaignID, String templateId) {
        track(eventName, campaignID, templateId, null);
    }

    public void track(String eventName, String campaignID, String templateId, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put("email", _email);
            requestJSON.put("eventName", eventName);

            if (campaignID != null) {
                requestJSON.put("campaignID", campaignID);
            }
            if (templateId != null) {
                requestJSON.put("templateId", templateId);
            }
            if (dataFields != null) {
                requestJSON.put("dataFields", dataFields);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("events/track", requestJSON);
    }

    /**
     * Track a custom conversion event.
     * @param campaignId
     * @param templateId
     */
    public void trackConversion(int campaignId, int templateId) {
        trackConversion(campaignId, templateId, null);
    }

    /**
     * Track a custom conversion event.
     * @param campaignId
     * @param templateId
     * @param dataFields
     */
    public void trackConversion(int campaignId, int templateId, JSONObject dataFields) {

        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("email", _email);
            requestJSON.put("campaignId", campaignId);
            requestJSON.put("templateId", templateId);
            if (dataFields != null) {
                requestJSON.put("dataFields", dataFields);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("events/trackConversion", requestJSON);
    }

    /**
     * Track when a push notification is opened on device.
     * @param campaignId
     * @param templateId
     */
    protected void trackPushOpen(int campaignId, int templateId) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("email", _email);
            requestJSON.put("campaignId", campaignId);
            requestJSON.put("templateId", templateId);

        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("events/trackPushOpen", requestJSON);
    }

    /**
     * Sends a push campaign to the given email address
     * @param email
     * @param campaignId
     */
    public void sendPush(String email, int campaignId) {
        sendPush(email, campaignId, null, null);
    }

    /**
     * Sends a push campaign to the given email address at the given time
     * @param email
     * @param campaignId
     * @param sendAt Schedule the message for up to 365 days in the future.
     *               If set in the past, message is sent immediately.
     *               Format is YYYY-MM-DD HH:MM:SS in UTC
     */
    public void sendPush(String email, int campaignId, String sendAt) {
        sendPush(email, campaignId, sendAt, null);
    }

    /**
     * Sends a push campaign to the given email address at the given time
     * @param email
     * @param campaignId
     * @param dataFields
     */
    public void sendPush(String email, int campaignId, JSONObject dataFields) {
        sendPush(email, campaignId, null, dataFields);
    }

    /**
     * Sends a push campaign to the given email address at the given time
     * @param email
     * @param campaignId
     * @param sendAt Schedule the message for up to 365 days in the future.
     *               If set in the past, message is sent immediately.
     *               Format is YYYY-MM-DD HH:MM:SS in UTC
     */
    public void sendPush(String email, int campaignId, String sendAt, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put("recipientEmail", email);
            requestJSON.put("campaignId", campaignId);
            if (sendAt != null){
                requestJSON.put("sendAt", sendAt);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest("push/target", requestJSON);
    }

    // FIXME: 4/22/16 Not yet complete
    public void trackPurchase(CommerceItem[] commerceItems, double total) {
        JSONObject requestJSON = new JSONObject();

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

    /**
     * Performs network operations on an async thread instead of the main thread.
     * @param uri
     * @param json
     */
    private void sendRequest(String uri, JSONObject json) {
        new IterableRequest().execute(iterableBaseUrl, _apiKey, uri, json.toString());
    }
}
