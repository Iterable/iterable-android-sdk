package com.iterable.iterableapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableApi {

    static final String TAG = "IterableApi";
    static final String NOTIFICATION_ICON_NAME = "iterable_notification_icon";

    protected static IterableApi sharedInstance = null;

    private Context _context;
    private String _apiKey;
    private String _email;

    private Bundle _payloadData;
    private IterableNotificationData _notificationData;
    private String _pushToken;

    private IterableApi(Context context, String apiKey, String email){
        updateData(context, apiKey, email);
    }

    /**
     * Returns a shared instance of IterableApi. Updates the client data if an instance already exists.
     * Should be called whenever the app is opened.
     * @param context The current activity
     * @return stored instance of IterableApi
     */
    public static IterableApi sharedInstanceWithApiKey(Context context, String apiKey, String email)
    {
        if (sharedInstance == null)
        {
            sharedInstance = new IterableApi(context, apiKey, email);
        } else{
            sharedInstance.updateData(context, apiKey, email);
        }

        if (context instanceof Activity) {
            Activity currentActivity = (Activity) context;
            Intent calledIntent = currentActivity.getIntent();
            sharedInstance.tryTrackNotifOpen(calledIntent);
        }
        else {
            Log.d(TAG, "Notification Opens will not be tracked: "+
                    "sharedInstanceWithApiKey called with a Context that is not an instance of Activity. " +
                    "Pass in an Activity to IterableApi.sharedInstanceWithApiKey to enable open tracking.");
        }

        return sharedInstance;
    }

    private void updateData(Context context, String apiKey, String email) {
        this._context = context;
        this._apiKey = apiKey;
        this._email = email;
    }

    protected Context getMainActivityContext() {
        return _context;
    }

    /**
     * Sets the icon to be displayed in notifications.
     * The icon name should match the resource name stored in the /res/drawable directory.
     * @param iconName
     */
    public void setNotificationIcon(String iconName) {
        setNotificationIcon(_context, iconName);
    }

    protected void setPushToken(String token) { _pushToken = token; }

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

    /**
     * Automatically generates a GCM token and registers it with Iterable.
     * @param iterableAppId The applicationId of the Iterable Push Integration
     *                      - https://app.iterable.com/integrations/mobilePush
     * @param gcmProjectId  The Google Project Number
     *                       - https://console.developers.google.com/iam-admin/settings
     */
    public void registerForPush(String iterableAppId, String gcmProjectId) {
        Intent pushRegistrationIntent = new Intent(_context, IterablePushReceiver.class);
        pushRegistrationIntent.setAction(IterableConstants.ACTION_PUSH_REGISTRATION);
        pushRegistrationIntent.putExtra(IterableConstants.PUSH_APPID, iterableAppId);
        pushRegistrationIntent.putExtra(IterableConstants.PUSH_PROJECTID, gcmProjectId);
        _context.sendBroadcast(pushRegistrationIntent);
    }

    private void tryTrackNotifOpen(Intent calledIntent) {
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
     * Registers an existing GCM device token with Iterable.
     * Recommended to use registerForPush if you do not already have a deviceToken
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
    private void registerDeviceToken(String applicationName, String token, JSONObject dataFields) {
        String platform = IterableConstants.MESSAGING_PLATFORM_GOOGLE;

        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            if (dataFields == null) {
                dataFields = new JSONObject();
            }

            JSONObject device = new JSONObject();
                device.put(IterableConstants.KEY_TOKEN, token);
                device.put(IterableConstants.KEY_PLATFORM, platform);
                device.put(IterableConstants.KEY_APPLICATIONNAME, applicationName);
                device.put(IterableConstants.KEY_DATAFIELDS, dataFields);
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

    public void trackConversion(int campaignId, int templateId) {
        trackConversion(campaignId, templateId, null);
    }

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

    public void sendPush(String email, int campaignId) {
        sendPush(email, campaignId, null, null);
    }

    /**
     * Sends a push campaign to an email address at the given time.
     * @param sendAt Schedule the message for up to 365 days in the future.
     *               If set in the past, message is sent immediately.
     *               Format is YYYY-MM-DD HH:MM:SS in UTC
     */
    public void sendPush(String email, int campaignId, Date sendAt) {
        sendPush(email, campaignId, sendAt, null);
    }

    public void sendPush(String email, int campaignId, JSONObject dataFields) {
        sendPush(email, campaignId, null, dataFields);
    }

    /**
     * Sends a push campaign to an email address at the given time.
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

    public void updateUser(JSONObject dataFields) {
        JSONObject requestJSON = new JSONObject();

        try {
            requestJSON.put(IterableConstants.KEY_EMAIL, _email);
            requestJSON.put(IterableConstants.KEY_DATAFIELDS, dataFields);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        sendRequest(IterableConstants.ENDPOINT_UPDATEUSER, requestJSON);
    }

    public void disablePush(String iterableAppId, String gcmProjectId) {
        registerForPush(iterableAppId, gcmProjectId);

        JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.put(IterableConstants.KEY_TOKEN, _pushToken);
            requestJSON.put(IterableConstants.KEY_EMAIL, _email);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        sendRequest(IterableConstants.ENDPOINT_DISABLEDEVICE, requestJSON);

    }

    /**
     * Retrieves the payload string for a given key.
     * Used for deeplinking and retrieving extra data passed down along with a campaign.
     * @param key
     * @return Returns the requested payload data from the current push campaign if it exists.
     */
    public String getPayloadData(String key) {
        String dataString = null;
        if (_payloadData != null){
            dataString = _payloadData.getString(key, null);
        }
        return dataString;
    }

    void setPayloadData(Bundle bundle) {
        _payloadData = bundle;
    }
    void setNotificationData(IterableNotificationData data) {
        _notificationData = data;
    }

    /**
     * Gets the current Template ID.
     * @return returns 0 if the current templateId does not exist.
     */
    public int getTemplateId() {
        int returnId = 0;
        if (_notificationData != null){
            returnId = _notificationData.getTemplateId();
        }
        return returnId;
    }

    /**
     * Gets the current Campaign ID.
     * @return 0 if the current templateId does not exist.
     */
    public int getCampaignId() {
        int returnId = 0;
        if (_notificationData != null){
            returnId = _notificationData.getCampaignId();
        }
        return returnId;
    }

    public static void initDebugMode(String url) {
        IterableRequest.overrideUrl = url;
    }

    /**
     * Sends the request to Iterable.
     * Performs network operations on an async thread instead of the main thread.
     * @param resourcePath
     * @param json
     */
    private void sendRequest(String resourcePath, JSONObject json) {
        IterableApiRequest request = new IterableApiRequest(_apiKey, resourcePath, json.toString());
        new IterableRequest().execute(request);
    }
}
