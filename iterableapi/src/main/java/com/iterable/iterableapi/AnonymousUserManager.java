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

            if (!previousData.isEmpty()) {
                JSONObject previousDataJson = new JSONObject(previousData);
                JSONObject sessionObject = previousDataJson.getJSONObject(IterableConstants.SHARED_PREFS_ANON_SESSIONS);
                sessionNo = sessionObject.getInt(IterableConstants.SHARED_PREFS_SESSION_NO);
                firstSessionDate = sessionObject.getString(IterableConstants.SHARED_PREFS_FIRST_SESSION);
            }

            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.SHARED_PREFS_SESSION_NO, sessionNo + 1);
            newDataObject.put(IterableConstants.SHARED_PREFS_LAST_SESSION, getCurrentTime());

            if (firstSessionDate.isEmpty()) {
                newDataObject.put(IterableConstants.SHARED_PREFS_FIRST_SESSION, getCurrentTime());
            } else {
                newDataObject.put(IterableConstants.SHARED_PREFS_FIRST_SESSION, firstSessionDate);
            }

            JSONObject anonSessionData = new JSONObject();
            anonSessionData.put(IterableConstants.SHARED_PREFS_ANON_SESSIONS, newDataObject);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, anonSessionData.toString());
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
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
        String userData = sharedPref.getString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, "");
        String userId = UUID.randomUUID().toString();
        try {
            if (!userData.isEmpty()) {
                JSONArray trackEventList = getEventListFromLocalStorage();
                JSONObject updateUserTrack = null;
                int updateUserTrackPosition = 0;
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
                if (updateUserTrack != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        trackEventList.remove(updateUserTrackPosition);
                    }
                }
                storeEventListToLocalStorage(trackEventList);

                JSONObject userSessionDataJson = new JSONObject(userData);
                JSONObject userDataJson = userSessionDataJson.getJSONObject(IterableConstants.SHARED_PREFS_ANON_SESSIONS);
                if (!getPushStatus().isEmpty()) {
                    userDataJson.put(IterableConstants.SHARED_PREFS_PUSH_OPT_IN, getPushStatus());
                }
                userDataJson.put(IterableConstants.SHARED_PREFS_CRITERIA_ID, Integer.valueOf(criteriaId));
                iterableApi.apiClient.trackAnonSession(getCurrentTime(), userId, userDataJson, updateUserTrack, data -> {
                    // success handler
                    IterableApi.getInstance().setAnonUser(userId);
                    syncEvents();
                }, (reason, data) -> {
                    if (data != null && data.has(IterableConstants.HTTP_STATUS_CODE)) {
                        try {
                            int statusCode = (int) data.get(IterableConstants.HTTP_STATUS_CODE);
                            if (statusCode == 409) {
                                getCriteria(); // refetch the criteria
                            }
                        } catch (JSONException e) {}
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void syncEvents() {

        JSONArray trackEventList = getEventListFromLocalStorage();
        if (trackEventList.length() > 0) {
            for (int i = 0; i < trackEventList.length(); i++) {
                try {
                    JSONObject event = trackEventList.getJSONObject(i);
                    String eventType = event.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE);
                    switch (eventType) {
                        case IterableConstants.TRACK_EVENT: {
                            String createdAt = "";
                            if (event.has(IterableConstants.KEY_CREATED_AT)) {
                                createdAt = event.getString(IterableConstants.KEY_CREATED_AT);
                            }
                            JSONObject dataFields = null;
                            if (event.has(IterableConstants.KEY_DATA_FIELDS)) {
                                dataFields = new JSONObject(event.getString(IterableConstants.KEY_DATA_FIELDS));
                            }
                            iterableApi.apiClient.track(event.getString(IterableConstants.KEY_EVENT_NAME), 0, 0, dataFields, createdAt);
                            break;
                        }
                        case IterableConstants.TRACK_PURCHASE: {
                            Gson gson = new GsonBuilder().create();
                            Type listType = new TypeToken<List<CommerceItem>>() {
                            }.getType();
                            List<CommerceItem> list = gson.fromJson(event.getString(IterableConstants.KEY_ITEMS), listType);

                            long createdAt = 0;
                            if (event.has(IterableConstants.KEY_CREATED_AT)) {
                                createdAt = Long.parseLong(event.getString(IterableConstants.KEY_CREATED_AT));
                            }
                            JSONObject dataFields = null;
                            if (event.has(IterableConstants.KEY_DATA_FIELDS)) {
                                dataFields = new JSONObject(event.getString(IterableConstants.KEY_DATA_FIELDS));
                            }
                            iterableApi.apiClient.trackPurchase(event.getDouble(IterableConstants.KEY_TOTAL), list, dataFields, createdAt);
                            break;
                        }
                        case IterableConstants.TRACK_UPDATE_CART: {
                            Gson gson = new GsonBuilder().create();
                            Type listType = new TypeToken<List<CommerceItem>>() {
                            }.getType();
                            List<CommerceItem> list = gson.fromJson(event.getString(IterableConstants.KEY_ITEMS), listType);
                            long createdAt = 0;
                            if (event.has(IterableConstants.KEY_CREATED_AT)) {
                                createdAt = Long.parseLong(event.getString(IterableConstants.KEY_CREATED_AT));
                            }
                            iterableApi.apiClient.updateCart(list, createdAt);
                            break;
                        }
                        case IterableConstants.UPDATE_USER: {
                            iterableApi.updateUser(event.getJSONObject(IterableConstants.KEY_DATA_FIELDS));
                            break;
                        }
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        clearAnonEventsData();
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

    private void storeEventListToLocalStorage(JSONArray newArrayObject) {
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, newArrayObject.toString());
        editor.apply();
    }

    private void storeEventListToLocalStorage(JSONObject newDataObject, boolean shouldOverWrite) {
        JSONArray previousDataArray = getEventListFromLocalStorage();
        ArrayList<JSONObject> eventListData = new ArrayList<>();
        try {
            if (shouldOverWrite) {
                int indexToRemove = -1;
                String trackingType = newDataObject.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE);
                for (int i = 0; i < previousDataArray.length(); i++) {
                    JSONObject jsonObject = previousDataArray.getJSONObject(i);
                    if (jsonObject.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(trackingType)) {
                        indexToRemove = i;
                        break;
                    }
                }

                if (indexToRemove >= 0) {
                    JSONArray newDataArray = new JSONArray();
                    for (int j = 0; j < previousDataArray.length(); j++) {
                        if (j != indexToRemove) {
                            newDataArray.put(previousDataArray.get(j));
                        }
                    }

                    previousDataArray = newDataArray;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        previousDataArray.put(newDataObject);
        int lengthOfData = previousDataArray.length();
        int eventThresholdLimit = iterableApi.config.eventThresholdLimit;
        if (lengthOfData > eventThresholdLimit) {
            int difference = lengthOfData - eventThresholdLimit;
            for (int i = difference; i < previousDataArray.length(); i++) {
                try {
                    eventListData.add(previousDataArray.getJSONObject(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            previousDataArray = new JSONArray(eventListData);
        }
        SharedPreferences sharedPref = IterableApi.getInstance().getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, previousDataArray.toString());
        editor.apply();

        String criteriaId = checkCriteriaCompletion();
        Log.i("TEST_USER", "criteriaId::" + String.valueOf(criteriaId));

        if (criteriaId != null) {
            createKnownUser(criteriaId);
        }
        Log.i("criteriaId::", String.valueOf(criteriaId != null));
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