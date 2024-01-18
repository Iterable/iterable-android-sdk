package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.iterable.iterableapi.util.CriteriaCompletionChecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class AnonymousUserManager {

    private static final String TAG = "AnonymousUserManager";

    void updateAnonSession() {
        IterableLogger.v(TAG, "updateAnonSession");
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
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
            newDataObject.put(IterableConstants.SHARED_PREFS_LAST_SESSION, getCurrentDateTime());

            if (firstSessionDate.isEmpty()) {
                newDataObject.put(IterableConstants.SHARED_PREFS_FIRST_SESSION, getCurrentDateTime());
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
        try {
            // call API when it is available and save data in SharedPreferences, until then just save the data using static data
            String mockData = "{" +
                    "    \"count\":2," +
                    "    \"criteriaList\":[" +
                    "        {" +
                    "            \"criteriaId\":12345," +
                    "            \"searchQuery\":{" +
                    "                \"combinator\":\"Or\"," +
                    "                \"searchQueries\":[" +
                    "                    {" +
                    "                        \"combinator\":\"And\"," +
                    "                        \"searchQueries\":[" +
                    "                            {" +
                    "                                \"dataType\":\"purchase\"," +
                    "                                \"searchCombo\":{" +
                    "                                    \"combinator\":\"And\"," +
                    "                                    \"searchQueries\":[" +
                    "                                        {" +
                    "                                            \"field\":\"shoppingCartItems.price\"," +
                    "                                            \"fieldType\":\"double\"," +
                    "                                            \"comparatorType\":\"Equals\"," +
                    "                                            \"dataType\":\"purchase\"," +
                    "                                            \"id\":2," +
                    "                                            \"value\":\"4.67\"" +
                    "                                        }," +
                    "                                        {" +
                    "                                            \"field\":\"shoppingCartItems.quantity\"," +
                    "                                            \"fieldType\":\"long\"," +
                    "                                            \"comparatorType\":\"GreaterThanOrEqualTo\"," +
                    "                                            \"dataType\":\"purchase\"," +
                    "                                            \"id\":3," +
                    "                                            \"valueLong\":2," +
                    "                                            \"value\":\"2\"" +
                    "                                        }" +
                    "                                    ]" +
                    "                                }" +
                    "                            }" +
                    "                        ]" +
                    "                    }" +
                    "                ]" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"criteriaId\":5678," +
                    "            \"searchQuery\":{" +
                    "                \"combinator\":\"Or\"," +
                    "                \"searchQueries\":[" +
                    "                    {" +
                    "                        \"combinator\":\"Or\"," +
                    "                        \"searchQueries\":[" +
                    "                            {" +
                    "                                \"dataType\":\"user\"," +
                    "                                \"searchCombo\":{" +
                    "                                    \"combinator\":\"And\"," +
                    "                                    \"searchQueries\":[" +
                    "                                        {" +
                    "                                            \"field\":\"itblInternal.emailDomain\"," +
                    "                                            \"fieldType\":\"string\"," +
                    "                                            \"comparatorType\":\"Equals\"," +
                    "                                            \"dataType\":\"user\"," +
                    "                                            \"id\":6," +
                    "                                            \"value\":\"gmail.com\"" +
                    "                                        }" +
                    "                                    ]" +
                    "                                }" +
                    "                            }," +
                    "                            {" +
                    "                                \"dataType\":\"customEvent\"," +
                    "                                \"searchCombo\":{" +
                    "                                    \"combinator\":\"And\"," +
                    "                                    \"searchQueries\":[" +
                    "                                        {" +
                    "                                            \"field\":\"eventName\"," +
                    "                                            \"fieldType\":\"string\"," +
                    "                                            \"comparatorType\":\"Equals\"," +
                    "                                            \"dataType\":\"customEvent\"," +
                    "                                            \"id\":9," +
                    "                                            \"value\":\"processing_cancelled\"" +
                    "                                        }," +
                    "                                        {" +
                    "                                            \"field\":\"createdAt\"," +
                    "                                            \"fieldType\":\"date\"," +
                    "                                            \"comparatorType\":\"GreaterThan\"," +
                    "                                            \"dataType\":\"customEvent\"," +
                    "                                            \"id\":10," +
                    "                                            \"dateRange\":{" +
                    "                                            }," +
                    "                                            \"isRelativeDate\":false," +
                    "                                            \"value\":\"1688194800000\"" +
                    "                                        }" +
                    "                                    ]" +
                    "                                }" +
                    "                            }" +
                    "                        ]" +
                    "                    }" +
                    "                ]" +
                    "            }" +
                    "        }" +
                    "    ]" +
                    "}";

            JSONObject mockDataObject = new JSONObject(mockData);
            SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(IterableConstants.SHARED_PREFS_CRITERIA, mockDataObject.toString());
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean checkCriteriaCompletion() {
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String criteriaData = sharedPref.getString(IterableConstants.SHARED_PREFS_CRITERIA, "");
        JSONArray localStoredEventList = getEventListFromLocalStorage();

        try {
            if (!criteriaData.isEmpty() && localStoredEventList.length() > 0) {
                CriteriaCompletionChecker checker = new CriteriaCompletionChecker();
                return checker.getMatchedCriteria(criteriaData, localStoredEventList) != null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void createKnownUser() {
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String userData = sharedPref.getString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, "");
        IterableApi.getInstance().setUserId(UUID.randomUUID().toString());

        try {
            if (!userData.isEmpty()) {
                JSONObject userDataJson = new JSONObject(userData);
                IterableApi.getInstance().updateUser(userDataJson);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        syncEvents();
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
                            if (event.has(IterableConstants.KEY_DATA_FIELDS)) {
                                JSONObject dataFields = new JSONObject(event.getString(IterableConstants.KEY_DATA_FIELDS));
                                IterableApi.getInstance().track(event.getString(IterableConstants.KEY_EVENT_NAME), 0, 0, dataFields, createdAt);
                            } else {
                                IterableApi.getInstance().track(event.getString(IterableConstants.KEY_EVENT_NAME), 0, 0, null, createdAt);
                            }
                            break;
                        }
                        case IterableConstants.TRACK_PURCHASE: {
                            Gson gson = new GsonBuilder().create();
                            Type listType = new TypeToken<List<CommerceItem>>() {
                            }.getType();
                            List<CommerceItem> list = gson.fromJson(event.getString(IterableConstants.KEY_ITEMS), listType);
                            JSONObject userObject = new JSONObject();
                            userObject.put(IterableConstants.KEY_CREATE_NEW_FIELDS, true);

                            String createdAt = "";
                            if (event.has(IterableConstants.KEY_CREATED_AT)) {
                                createdAt = event.getString(IterableConstants.KEY_CREATED_AT);
                            }
                            if (event.has(IterableConstants.KEY_DATA_FIELDS)) {
                                JSONObject dataFields = new JSONObject(event.getString(IterableConstants.KEY_DATA_FIELDS));
                                IterableApi.getInstance().trackPurchase(event.getDouble(IterableConstants.KEY_TOTAL), list, dataFields, userObject, createdAt);
                            } else {
                                IterableApi.getInstance().trackPurchase(event.getDouble(IterableConstants.KEY_TOTAL), list, null, userObject, createdAt);
                            }
                            break;
                        }
                        case IterableConstants.TRACK_UPDATE_CART: {
                            Gson gson = new GsonBuilder().create();
                            Type listType = new TypeToken<List<CommerceItem>>() {
                            }.getType();
                            List<CommerceItem> list = gson.fromJson(event.getString(IterableConstants.KEY_ITEMS), listType);
                            JSONObject userObject = new JSONObject();
                            userObject.put(IterableConstants.KEY_PREFER_USER_ID, true);
                            userObject.put(IterableConstants.KEY_MERGE_NESTED_OBJECTS, true);
                            userObject.put(IterableConstants.KEY_CREATE_NEW_FIELDS, true);
                            String createdAt = "";
                            if (event.has(IterableConstants.KEY_CREATED_AT)) {
                                createdAt = event.getString(IterableConstants.KEY_CREATED_AT);
                            }
                            IterableApi.getInstance().updateCart(list, userObject, createdAt);
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

        clearLocallyStoredData();
    }

    private void clearLocallyStoredData() {
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, "");
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
        editor.apply();
    }

    private void storeEventListToLocalStorage(JSONObject newDataObject) {
        JSONArray previousDataArray = getEventListFromLocalStorage();
        previousDataArray.put(newDataObject);
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, previousDataArray.toString());
        editor.apply();

        if (checkCriteriaCompletion()) {
            createKnownUser();
        }
    }

    private JSONArray getEventListFromLocalStorage() {
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
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
        return Calendar.getInstance().getTimeInMillis();
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }
}
