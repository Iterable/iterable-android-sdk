package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by davidtruong on 4/4/16.
 */
public class IterableApi {

    static final String TAG = "IterableApi";
    static final String NOTIFICATION_ICON_NAME = "iterable_notification_icon";

    protected static IterableApi sharedInstance = null;

    private Context _context;
    private String _apiKey;
    private String _email;

    private IterableApi(Context context, String apiKey, String email){
        updateData(context, apiKey, email);
    }

    /**
     * Creates and returns the stored IterableApi instance.
     * @param context
     * @param apiKey
     * @param email
     * @return
     */
    public static IterableApi sharedInstanceWithApiKey(Context context, String apiKey, String email)
    {
        if (sharedInstance == null)
        {
            sharedInstance = new IterableApi(context, apiKey, email);
        } else{
            //TODO: check to see if we need to call updateEmail
            sharedInstance.updateData(context, apiKey, email);
        }

        Intent calledIntent = ((Activity) context).getIntent();
        sharedInstance.tryTrackNotifOpen(calledIntent);
        return sharedInstance;
    }

    private void updateData(Context context, String apiKey, String email) {
        this._context = context;
        this._apiKey = apiKey;
        this._email = email;
    }

    protected Context getApplicationContext() {
        return _context;
    }

    public void setNotificationIcon(String iconName) {
        setNotificationIcon(_context, iconName);
    }

    protected static void setNotificationIcon(Context context, String iconName) {
        SharedPreferences sharedPref = ((Activity) context).getSharedPreferences(NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(NOTIFICATION_ICON_NAME, iconName);
        editor.commit();
    }

    protected static String getNotificationIcon(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(NOTIFICATION_ICON_NAME, Context.MODE_PRIVATE);
        String iconName = sharedPref.getString(NOTIFICATION_ICON_NAME, "");
        return iconName;
    }

    public void registerForPush(String iterableAppId, String gcmProjectId) {
        //TODO: set this up as a callback then call registerDeviceToken
        Intent pushRegistrationIntent = new Intent(_context, IterablePushReceiver.class);
        pushRegistrationIntent.setAction(IterableConstants.ACTION_PUSH_REGISTRATION);
        pushRegistrationIntent.putExtra(IterableConstants.PUSH_APPID, iterableAppId);
        pushRegistrationIntent.putExtra(IterableConstants.PUSH_PROJECTID, gcmProjectId);
        _context.sendBroadcast(pushRegistrationIntent);
    }

    private void tryTrackNotifOpen(Intent calledIntent)
    {
        Bundle extras = calledIntent.getExtras();
        if (extras != null) {
            Intent intent = new Intent();
            intent.setClass(_context, IterablePushOpenReceiver.class);
            intent.setAction(IterableConstants.ACTION_NOTIF_OPENED);
            intent.putExtras(extras);
            _context.sendBroadcast(intent);
        }
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
        String platform = IterableConstants.MESSAGING_PLATFORM_GOOGLE;

        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            JSONObject device = new JSONObject();
            device.put(IterableConstants.KEY_TOKEN, token);
            device.put(IterableConstants.KEY_PLATFORM, platform);
            device.put(IterableConstants.KEY_APPLICATIONNAME, applicationName);
            requestJSON.put(IterableConstants.KEY_DEVICE, device);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_REGISTERDEVICETOKEN, requestJSON);
    }

    public void track(String eventName) {
        track(eventName, null, null, null);
    }

    public void track(String eventName, JSONObject dataFields) {
        track(eventName, null, null, dataFields);
    }

    public void track(String eventName, String campaignId, String templateId) {
        track(eventName, campaignId, templateId, null);
    }

    public void track(String eventName, String campaignId, String templateId, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            requestJSON.put(IterableConstants.KEY_EVENTNAME, eventName);

            requestJSON.put(IterableConstants.KEY_CAMPAIGNID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            requestJSON.put(IterableConstants.KEY_DATAFIELDS, dataFields);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_TRACK, requestJSON);
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
            requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            requestJSON.put(IterableConstants.KEY_CAMPAIGNID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);
            if (dataFields != null) {
                requestJSON.put(IterableConstants.KEY_DATAFIELDS, dataFields);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_TRACKCONVERSION, requestJSON);
    }

    /**
     * Track when a push notification is opened on device.
     * @param campaignId
     * @param templateId
     */
    protected void trackPushOpen(int campaignId, int templateId) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            requestJSON.put(IterableConstants.KEY_CAMPAIGNID, campaignId);
            requestJSON.put(IterableConstants.KEY_TEMPLATE_ID, templateId);

        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_TRACKPUSHOPEN, requestJSON);
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
    public void sendPush(String email, int campaignId, Date sendAt) {
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
    public void sendPush(String email, int campaignId, Date sendAt, JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put(IterableConstants.KEY_RECIPIENT_EMAIL, email);
            requestJSON.put(IterableConstants.KEY_CAMPAIGNID, campaignId);
            if (sendAt != null){
                String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String dateString = sdf.format(sendAt);
                requestJSON.put(IterableConstants.KEY_SEND_AT, dateString);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_PUSHTARGET, requestJSON);
    }

    // FIXME: 4/22/16 Not yet complete
    private void trackPurchase(CommerceItem[] commerceItems, double total) {
        JSONObject requestJSON = new JSONObject();

        JSONObject userJSON = new JSONObject();

        for(CommerceItem item : commerceItems) {
            //item.serialize();
        }

        try {
            userJSON.put(IterableConstants.KEY_EMAIL, _email);
            requestJSON.put(IterableConstants.KEY_USER, userJSON);
            requestJSON.put(IterableConstants.KEY_ITEMS, commerceItems.toString());
            requestJSON.put(IterableConstants.KEY_TOTAL, total);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_TRACKPURCHASE, requestJSON);
    }

    //TODO: reset current user profile
    private static void reset() {
        // clears all the current device

        //TODO:Require the app to re-initialize with the SDK

    }

    public void updateEmail(String newEmail) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put(IterableConstants.KEY_CURRENT_EMAIL, _email);
            requestJSON.put(IterableConstants.KEY_NEW_EMAIL, newEmail);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_UPDATEEMAIL, requestJSON);

        _email = newEmail;
    }

    //TODO: use adid or android ID
    private void getDeviceAdid() {
        //Reference - https://developer.android.com/google/play-services/id.html#example
    }

    /**
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    private void sendRequest(String resourcePath, JSONObject json) {
        IterableApiRequest request = new IterableApiRequest(_apiKey, resourcePath, json.toString());
        new IterableRequest().execute(request);
    }
}
