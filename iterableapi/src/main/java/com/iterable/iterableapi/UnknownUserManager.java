package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationManagerCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.iterable.iterableapi.util.CriteriaCompletionChecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class UnknownUserManager implements IterableActivityMonitor.AppStateCallback {

    private static final String TAG = "UnknownUserManager";
    private IterableApi iterableApi = IterableApi.sharedInstance;
    private final IterableActivityMonitor activityMonitor;
    long lastCriteriaFetch = 0;
    boolean isCriteriaMatched = false;

    UnknownUserManager(IterableApi iterableApi) {
        this(iterableApi,
            IterableActivityMonitor.getInstance());
    }

    @VisibleForTesting
    UnknownUserManager(IterableApi iterableApi,
                       IterableActivityMonitor activityMonitor) {
        this.iterableApi = iterableApi;
        this.activityMonitor = activityMonitor;
        this.activityMonitor.addCallback(this);
    }

    void updateUnknownSession() {
        IterableLogger.v(TAG, "updateUnknownSession");

        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String previousData = sharedPref.getString(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS, "");

        try {
            int sessionNo = 0;
            String firstSessionDate = "";

            //If previous session data exists, get previous session number and first session date
            if (!previousData.isEmpty()) {
                JSONObject previousDataJson = new JSONObject(previousData);
                JSONObject sessionObject = previousDataJson.getJSONObject(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS);
                sessionNo = sessionObject.getInt(IterableConstants.SHARED_PREFS_SESSION_NO);
                firstSessionDate = sessionObject.getString(IterableConstants.SHARED_PREFS_FIRST_SESSION);
            }

            //create new session data object and save it to local storage
            JSONObject newDataObject = createNewSessionData(sessionNo, firstSessionDate);
            saveUnknownSessionData(sharedPref, newDataObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject createNewSessionData(int sessionNo, String firstSessionDate) throws JSONException {
        JSONObject newDataObject = new JSONObject();
        newDataObject.put(IterableConstants.SHARED_PREFS_SESSION_NO, sessionNo + 1);
        newDataObject.put(IterableConstants.SHARED_PREFS_LAST_SESSION, IterableUtil.currentTimeMillis());

        if (firstSessionDate.isEmpty()) {
            newDataObject.put(IterableConstants.SHARED_PREFS_FIRST_SESSION, IterableUtil.currentTimeMillis());
        } else {
            newDataObject.put(IterableConstants.SHARED_PREFS_FIRST_SESSION, firstSessionDate);
        }

        return newDataObject;
    }

    private void saveUnknownSessionData(SharedPreferences sharedPref, JSONObject newDataObject) throws JSONException {
        JSONObject unknownSessionData = new JSONObject();
        unknownSessionData.put(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS, newDataObject);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS, unknownSessionData.toString());
        editor.apply();
    }

    void trackUnknownEvent(String eventName, JSONObject dataFields) {
        IterableLogger.v(TAG, "trackUnknownEvent");

        try {
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_EVENT_NAME, eventName);
            newDataObject.put(IterableConstants.KEY_CREATED_AT, IterableUtil.currentTimeMillis());
            newDataObject.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            newDataObject.put(IterableConstants.KEY_CREATE_NEW_FIELDS, true);
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_EVENT);
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackUnknownUpdateUser(JSONObject dataFields) {
        IterableLogger.v(TAG, "updateUnknownUser");
        try {
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.UPDATE_USER);
            storeUserUpdateToLocalStorage(newDataObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackUnknownTokenRegistration(String token) {
        IterableLogger.v(TAG, "trackUnknownTokenRegistration");
        try {
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_TOKEN, token);
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_TOKEN_REGISTRATION);
            storeEventListToLocalStorage(newDataObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackUnknownPurchaseEvent(double total, @NonNull List<CommerceItem> items, @Nullable JSONObject dataFields) {
        IterableLogger.v(TAG, "trackUnknownPurchaseEvent");
        try {
            JSONObject newDataObject = new JSONObject();
            Gson gson = new GsonBuilder().create();

            newDataObject.put(IterableConstants.KEY_ITEMS, gson.toJsonTree(items).getAsJsonArray().toString());
            newDataObject.put(IterableConstants.KEY_CREATED_AT, IterableUtil.currentTimeMillis());
            newDataObject.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            newDataObject.put(IterableConstants.KEY_TOTAL, total);
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_PURCHASE);
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackUnknownUpdateCart(@NonNull List<CommerceItem> items) {
        IterableLogger.v(TAG, "trackUnknownUpdateCart");

        try {
            Gson gson = new GsonBuilder().create();
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_ITEMS, gson.toJsonTree(items).getAsJsonArray().toString());
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_UPDATE_CART);
            newDataObject.put(IterableConstants.KEY_CREATED_AT, IterableUtil.currentTimeMillis());
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void getCriteria() {
        lastCriteriaFetch = System.currentTimeMillis();

        iterableApi.apiClient.getCriteriaList(data -> {
            if (data != null) {
                try {
                    JSONObject mockDataObject = new JSONObject(data);
                    SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(IterableConstants.SHARED_PREFS_CRITERIA, mockDataObject.toString());
                    editor.apply();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String checkCriteriaCompletion() {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String criteriaData = sharedPref.getString(IterableConstants.SHARED_PREFS_CRITERIA, "");

        JSONArray localStoredEventListAndUserUpdates = getEventListFromLocalStorage();
        JSONObject localStoredUserUpdateObj = getUserUpdateObjFromLocalStorage();

        localStoredEventListAndUserUpdates.put(localStoredUserUpdateObj);

        try {
            if (!criteriaData.isEmpty() && localStoredEventListAndUserUpdates.length() > 0) {
                CriteriaCompletionChecker checker = new CriteriaCompletionChecker();
                return checker.getMatchedCriteria(criteriaData, localStoredEventListAndUserUpdates);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void createUnknownUser(String criteriaId) {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        updateUnknownSession();

        //get session data and stored time of consent
        String userData = sharedPref.getString(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS, "");
        Long timeOfConsent = sharedPref.getLong(IterableConstants.SHARED_PREFS_VISITOR_USAGE_TRACKED_TIME, 0);

        //generate unknown user id
        String userId = UUID.randomUUID().toString();

        try {
            if (!userData.isEmpty()) {
                JSONObject updateUserObj = getUserUpdateObjFromLocalStorage();
                JSONObject updateUserDataFields = null;
                if (updateUserObj.has(IterableConstants.KEY_DATA_FIELDS)) {
                    updateUserDataFields = updateUserObj.getJSONObject(IterableConstants.KEY_DATA_FIELDS);
                }
                JSONObject userSessionDataJson = new JSONObject(userData);
                JSONObject userDataJson = userSessionDataJson.getJSONObject(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS);

                //update user data
                if (!getPushStatus().isEmpty()) {
                    userDataJson.put(IterableConstants.SHARED_PREFS_PUSH_OPT_IN, getPushStatus());
                }
                userDataJson.put(IterableConstants.SHARED_PREFS_CRITERIA_ID, Integer.valueOf(criteriaId));

                //track unknown user session with new user
                iterableApi.apiClient.trackUnknownUserSession(IterableUtil.currentTimeMillis(), userId, userDataJson, updateUserDataFields, data -> {
                    // success handler
                    if (IterableApi.getInstance().config.iterableUnknownUserHandler != null) {
                        IterableApi.getInstance().config.iterableUnknownUserHandler.onUnknownUserCreated(userId);
                    }
                    IterableApi.getInstance().setUnknownUser(userId);
                }, (reason, data) -> handleTrackFailure(data));
            }

        } catch (JSONException e) {
            isCriteriaMatched = false;
            e.printStackTrace();
        }
    }
    
    private void handleTrackFailure(JSONObject data) {
        isCriteriaMatched = false;
        if (data != null && data.has(IterableConstants.HTTP_STATUS_CODE)) {
            try {
                int statusCode = (int) data.get(IterableConstants.HTTP_STATUS_CODE);
                if (statusCode == 409) {
                    getCriteria(); // refetch the criteria
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    void syncEventsAndUserUpdate() {
        JSONArray trackEventList = getEventListFromLocalStorage();
        JSONObject updateUserObj = getUserUpdateObjFromLocalStorage();
        Gson gson = new GsonBuilder().create();

        if (trackEventList.length() == 0 && !updateUserObj.has(IterableConstants.KEY_DATA_FIELDS)) return;

        for (int i = 0; i < trackEventList.length(); i++) {
            try {
                JSONObject event = trackEventList.getJSONObject(i);
                String eventType = event.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE);
                switch (eventType) {
                    case IterableConstants.TRACK_EVENT: {
                        handleTrackEvent(event);
                        break;
                    }
                    case IterableConstants.TRACK_PURCHASE: {
                        handleTrackPurchase(event, gson);
                        break;
                    }
                    case IterableConstants.TRACK_UPDATE_CART: {
                        handleUpdateCart(event, gson);
                        break;
                    }
                    default:
                        break;
                }
            } catch (JSONException e) {
                IterableLogger.d(TAG, "Event Sync Failure");
            }
        }

        try {
            handleUpdateUser(updateUserObj);
        } catch (JSONException e) {
            IterableLogger.d(TAG, "Handle User Update Failure");
        }
    }

    private void handleTrackEvent(JSONObject event) throws JSONException {
        String createdAt = getStringValue(event);
        JSONObject dataFields = getDataFields(event);
        iterableApi.apiClient.track(event.getString(IterableConstants.KEY_EVENT_NAME), 0, 0, dataFields, createdAt);
    }

    private void handleTrackPurchase(JSONObject event, Gson gson) throws JSONException {
        Type listType = new TypeToken<List<CommerceItem>>() { }.getType();
        List<CommerceItem> list = gson.fromJson(event.getString(IterableConstants.KEY_ITEMS), listType);

        long createdAt = getLongValue(event);
        JSONObject dataFields = getDataFields(event);
        iterableApi.apiClient.trackPurchase(event.getDouble(IterableConstants.KEY_TOTAL), list, dataFields, createdAt);
    }

    private void handleUpdateCart(JSONObject event, Gson gson) throws JSONException {
        Type listType = new TypeToken<List<CommerceItem>>() { }.getType();
        List<CommerceItem> list = gson.fromJson(event.getString(IterableConstants.KEY_ITEMS), listType);

        long createdAt = getLongValue(event);
        iterableApi.apiClient.updateCart(list, createdAt);
    }

    private void handleUpdateUser(JSONObject event) throws JSONException {
        iterableApi.apiClient.updateUser(event.getJSONObject(IterableConstants.KEY_DATA_FIELDS), false);
    }

    private String getStringValue(JSONObject event) throws JSONException {
        return event.has(IterableConstants.KEY_CREATED_AT) ? event.getString(IterableConstants.KEY_CREATED_AT) : "";
    }

    private long getLongValue(JSONObject event) throws JSONException {
        return event.has(IterableConstants.KEY_CREATED_AT) ? Long.parseLong(event.getString(IterableConstants.KEY_CREATED_AT)) : 0L;
    }
    private JSONObject getDataFields(JSONObject event) throws JSONException {
        return event.has(IterableConstants.KEY_DATA_FIELDS) ? new JSONObject(event.getString(IterableConstants.KEY_DATA_FIELDS)) : null;
    }

    public void clearVisitorEventsAndUserData() {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_UNKNOWN_SESSIONS, "");
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
        editor.putString(IterableConstants.SHARED_PREFS_USER_UPDATE_OBJECT_KEY, "");
        editor.apply();
    }

    private void storeEventListToLocalStorage(JSONObject newDataObject) {
        if (!iterableApi.getVisitorUsageTracked()) {
            return;
        }
        JSONArray eventList = getEventListFromLocalStorage();

        eventList.put(newDataObject);

        eventList = enforceEventThresholdLimit(eventList);
        saveEventListToLocalStorage(eventList);

        String criteriaId = checkCriteriaCompletion();
        Log.i("TEST_USER", "criteriaId::" + String.valueOf(criteriaId));

        if (criteriaId != null && !isCriteriaMatched) {
            isCriteriaMatched = true;
            createUnknownUser(criteriaId);
        }
        Log.i("criteriaId::", String.valueOf(criteriaId != null));
    }

    private void storeUserUpdateToLocalStorage(JSONObject newDataObject) throws JSONException {
        if (!iterableApi.getVisitorUsageTracked()) {
            return;
        }

        JSONObject userUpdateObject = getUserUpdateObjFromLocalStorage();
        mergeUpdateUserObjects(userUpdateObject, newDataObject);

        saveUserUpdateObjectToLocalStorage(userUpdateObject);

        String criteriaId = checkCriteriaCompletion();
        Log.i("TEST_USER", "criteriaId::" + String.valueOf(criteriaId));

        if (criteriaId != null) {
            createUnknownUser(criteriaId);
        }
        Log.i("criteriaId::", String.valueOf(criteriaId != null));
    }

    private void mergeUpdateUserObjects(JSONObject target, JSONObject source) throws JSONException {
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = source.get(key);
            // if value is an object, recurse
            if (value instanceof JSONObject && target.has(key)) {
                mergeUpdateUserObjects(target.getJSONObject(key), (JSONObject) value);
            } else {
                // If the key doesn't exist in the target, just add it
                target.put(key, value);
            }
        }
    }

    private JSONArray enforceEventThresholdLimit(JSONArray eventDataArray) {
        int lengthOfData = eventDataArray.length();
        int eventThresholdLimit = iterableApi.config.eventThresholdLimit;

        if (lengthOfData > eventThresholdLimit) {
            int difference = lengthOfData - eventThresholdLimit;
            ArrayList<JSONObject> eventListData = new ArrayList<>();
            for (int i = difference; i < eventDataArray.length(); i++) {
                try {
                    eventListData.add(eventDataArray.getJSONObject(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            return new JSONArray(eventListData);
        }
        return eventDataArray;
    }

    private void saveEventListToLocalStorage(JSONArray eventDataArray) {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, eventDataArray.toString());
        editor.apply();
    }

    private void saveUserUpdateObjectToLocalStorage(JSONObject userUpdateObject) {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_USER_UPDATE_OBJECT_KEY, userUpdateObject.toString());
        editor.apply();
    }

    private JSONArray getEventListFromLocalStorage() {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String eventListJson = sharedPref.getString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
        JSONArray eventListArray = new JSONArray();
        try {
            if (!eventListJson.isEmpty()) {
                eventListArray = new JSONArray(eventListJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return eventListArray;
    }

    private JSONObject getUserUpdateObjFromLocalStorage() {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String userUpdateJson = sharedPref.getString(IterableConstants.SHARED_PREFS_USER_UPDATE_OBJECT_KEY, "");
        JSONObject userUpdateObject = new JSONObject();
        try {
            if (!userUpdateJson.isEmpty()) {
                userUpdateObject = new JSONObject(userUpdateJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return userUpdateObject;
    }

    private String getPushStatus() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(IterableApi.getInstance().getMainActivityContext());
        if (notificationManagerCompat.areNotificationsEnabled()) {
            ApplicationInfo applicationInfo = IterableApi.getInstance().getMainActivityContext().getApplicationInfo();
            int stringId = applicationInfo.labelRes;
            return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : IterableApi.getInstance().getMainActivityContext().getString(stringId);
        } else {
            return "";
        }
    }

    @Override
    public void onSwitchToForeground() {
        long currentTime = System.currentTimeMillis();

        // fetching unknown user criteria on foregrounding
        if (!iterableApi.checkSDKInitialization()
            && iterableApi._userIdUnknown == null
            && iterableApi.config.enableUnknownUserActivation
            && iterableApi.getVisitorUsageTracked()
            && iterableApi.config.enableForegroundCriteriaFetch
            && currentTime - lastCriteriaFetch >= IterableConstants.CRITERIA_FETCHING_COOLDOWN) {

            lastCriteriaFetch = currentTime;
            this.getCriteria();
            IterableLogger.d(TAG, "Fetching unknown user criteria - Foreground");
        }
    }

    @Override
    public void onSwitchToBackground() {

    }
}