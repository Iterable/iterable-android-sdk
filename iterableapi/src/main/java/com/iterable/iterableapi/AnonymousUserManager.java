package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class AnonymousUserManager {

    private static final String TAG = "AnonymousUserManager";
    private final IterableApi iterableApi = IterableApi.sharedInstance;

    void updateAnonSession() {
        IterableLogger.v(TAG, "updateAnonSession");

        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String previousData = sharedPref.getString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, "");

        try {
            int sessionNo = 0;
            String firstSessionDate = "";

            //If previous session data exists, get previous session number and first session date
            if (!previousData.isEmpty()) {
                JSONObject previousDataJson = new JSONObject(previousData);
                JSONObject sessionObject = previousDataJson.getJSONObject(IterableConstants.SHARED_PREFS_ANON_SESSIONS);
                sessionNo = sessionObject.getInt(IterableConstants.SHARED_PREFS_SESSION_NO);
                firstSessionDate = sessionObject.getString(IterableConstants.SHARED_PREFS_FIRST_SESSION);
            }

            //create new session data object and save it to local storage
            JSONObject newDataObject = createNewSessionData(sessionNo, firstSessionDate);
            saveAnonSessionData(sharedPref, newDataObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject createNewSessionData(int sessionNo, String firstSessionDate) throws JSONException {
        JSONObject newDataObject = new JSONObject();
        newDataObject.put(IterableConstants.SHARED_PREFS_SESSION_NO, sessionNo + 1);
        newDataObject.put(IterableConstants.SHARED_PREFS_LAST_SESSION, getCurrentTime());

        if (firstSessionDate.isEmpty()) {
            newDataObject.put(IterableConstants.SHARED_PREFS_FIRST_SESSION, getCurrentTime());
        } else {
            newDataObject.put(IterableConstants.SHARED_PREFS_FIRST_SESSION, firstSessionDate);
        }

        return newDataObject;
    }

    private void saveAnonSessionData(SharedPreferences sharedPref, JSONObject newDataObject) throws JSONException {
        JSONObject anonSessionData = new JSONObject();
        anonSessionData.put(IterableConstants.SHARED_PREFS_ANON_SESSIONS, newDataObject);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, anonSessionData.toString());
        editor.apply();
    }

    void trackAnonEvent(String eventName, JSONObject dataFields) {
        IterableLogger.v(TAG, "trackAnonEvent");

        try {
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_EVENT_NAME, eventName);
            newDataObject.put(IterableConstants.KEY_CREATED_AT, getCurrentTime());
            newDataObject.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            newDataObject.put(IterableConstants.KEY_CREATE_NEW_FIELDS, true);
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_EVENT);
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackAnonUpdateUser(JSONObject dataFields) {
        IterableLogger.v(TAG, "updateAnonUser");
        try {
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.UPDATE_USER);
            storeEventListToLocalStorage(newDataObject, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackAnonTokenRegistration(String token) {
        IterableLogger.v(TAG, "trackAnonTokenRegistration");
        try {
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_TOKEN, token);
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_TOKEN_REGISTRATION);
            storeEventListToLocalStorage(newDataObject, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackAnonPurchaseEvent(double total, @NonNull List<CommerceItem> items, @Nullable JSONObject dataFields) {

        IterableLogger.v(TAG, "trackAnonPurchaseEvent");
        try {
            JSONObject newDataObject = new JSONObject();
            Gson gson = new GsonBuilder().create();

            newDataObject.put(IterableConstants.KEY_ITEMS, gson.toJsonTree(items).getAsJsonArray().toString());
            newDataObject.put(IterableConstants.KEY_CREATED_AT, getCurrentTime());
            newDataObject.put(IterableConstants.KEY_DATA_FIELDS, dataFields);
            newDataObject.put(IterableConstants.KEY_TOTAL, total);
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_PURCHASE);
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void trackAnonUpdateCart(@NonNull List<CommerceItem> items) {

        IterableLogger.v(TAG, "trackAnonUpdateCart");

        try {
            Gson gson = new GsonBuilder().create();
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_ITEMS, gson.toJsonTree(items).getAsJsonArray().toString());
            newDataObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_UPDATE_CART);
            newDataObject.put(IterableConstants.KEY_CREATED_AT, getCurrentTime());
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void getCriteria() {
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
        JSONArray localStoredEventList = getEventListFromLocalStorage();

        try {
            if (!criteriaData.isEmpty() && localStoredEventList.length() > 0) {
                CriteriaCompletionChecker checker = new CriteriaCompletionChecker();
                return checker.getMatchedCriteria(criteriaData, localStoredEventList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void createKnownUser(String criteriaId) {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        updateAnonSession();

        //get session data
        String userData = sharedPref.getString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, "");

        //generate anon user id
        String userId = UUID.randomUUID().toString();

        try {
            if (!userData.isEmpty()) {
                JSONArray trackEventList = getEventListFromLocalStorage();
                JSONObject updateUserTrack = null;
                int updateUserTrackPosition = 0;

                //find last update user event data fields and position
                for (int i = 0; i < trackEventList.length(); i++) {
                    JSONObject trackEvent = trackEventList.getJSONObject(i);
                    if ((trackEvent.has(IterableConstants.SHARED_PREFS_EVENT_TYPE)
                            && trackEvent.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(IterableConstants.KEY_USER))
                            && trackEvent.has(IterableConstants.KEY_DATA_FIELDS)) {
                        updateUserTrackPosition = i;
                        updateUserTrack = trackEvent.getJSONObject(IterableConstants.KEY_DATA_FIELDS);
                        break;
                    }
                }

                //remove update user event from local event list
                if (updateUserTrack != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        trackEventList.remove(updateUserTrackPosition);
                    }
                }
                saveEventListToLocalStorage(trackEventList);

                JSONObject userSessionDataJson = new JSONObject(userData);
                JSONObject userDataJson = userSessionDataJson.getJSONObject(IterableConstants.SHARED_PREFS_ANON_SESSIONS);

                //update user data
                if (!getPushStatus().isEmpty()) {
                    userDataJson.put(IterableConstants.SHARED_PREFS_PUSH_OPT_IN, getPushStatus());
                }
                userDataJson.put(IterableConstants.SHARED_PREFS_CRITERIA_ID, Integer.valueOf(criteriaId));

                //track anon session with new user
                iterableApi.apiClient.trackAnonSession(getCurrentTime(), userId, userDataJson, updateUserTrack, data -> {
                    // success handler
                    IterableApi.getInstance().setAnonUser(userId);
                    syncEvents();
                }, (reason, data) -> handleTrackFailure(data));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleTrackFailure(JSONObject data) {
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

    void syncEvents() {
        JSONArray trackEventList = getEventListFromLocalStorage();
        Gson gson = new GsonBuilder().create();

        if (trackEventList.length() == 0) return;

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
                    case IterableConstants.UPDATE_USER: {
                        handleUpdateUser(event);
                        break;
                    }
                    default:
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        clearAnonEventsData();
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

    public void clearAnonEventsData() {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, "");
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
        editor.apply();
    }

    private void storeEventListToLocalStorage(JSONObject newDataObject) {
        storeEventListToLocalStorage(newDataObject, false);
    }

    private void storeEventListToLocalStorage(JSONObject newDataObject, boolean shouldOverWrite) {
        JSONArray previousDataArray = getEventListFromLocalStorage();
        if (shouldOverWrite) {
            previousDataArray = removeDuplicateEvent(previousDataArray, newDataObject);
        }

        previousDataArray.put(newDataObject);

        previousDataArray = enforceEventThresholdLimit(previousDataArray);
        saveEventListToLocalStorage(previousDataArray);

        String criteriaId = checkCriteriaCompletion();
        Log.i("TEST_USER", "criteriaId::" + String.valueOf(criteriaId));

        if (criteriaId != null) {
            createKnownUser(criteriaId);
        }
        Log.i("criteriaId::", String.valueOf(criteriaId != null));
    }

    private JSONArray removeDuplicateEvent(JSONArray previousDataArray, JSONObject newDataObject) {
        try {
            String trackingType = newDataObject.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE);
            for (int i = 0; i < previousDataArray.length(); i++) {
                JSONObject jsonObject = previousDataArray.getJSONObject(i);
                if (jsonObject.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(trackingType)) {
                    return removeEventAtIndex(previousDataArray, i);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return previousDataArray;
    }

    private JSONArray removeEventAtIndex(JSONArray array, int indexToRemove) throws JSONException {
        JSONArray newArray = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            if (i != indexToRemove) {
                newArray.put(array.get(i));
            }
        }
        return newArray;
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

    private long getCurrentTime() {
        return Calendar.getInstance().getTimeInMillis() / 1000;
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
}