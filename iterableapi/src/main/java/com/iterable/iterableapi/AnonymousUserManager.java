package com.iterable.iterableapi;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.iterable.iterableapi.util.LogicalExpressionEvaluator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class AnonymousUserManager {

    private static final String TAG = "RNIterableAPIModule";

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
            newDataObject.put(IterableConstants.SHARED_PREFS_TRACKING_TYPE, IterableConstants.TRACK_EVENT);
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (checkCriteriaCompletion()) {
            createKnownUser();
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
            newDataObject.put(IterableConstants.SHARED_PREFS_TRACKING_TYPE, IterableConstants.TRACK_PURCHASE);
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (checkCriteriaCompletion()) {
            createKnownUser();
        }
    }

    void trackAnonUpdateCart(@NonNull List<CommerceItem> items) {

        IterableLogger.v(TAG, "trackAnonUpdateCart");

        try {
            Gson gson = new GsonBuilder().create();
            JSONObject newDataObject = new JSONObject();
            newDataObject.put(IterableConstants.KEY_ITEMS, gson.toJsonTree(items).getAsJsonArray().toString());
            newDataObject.put(IterableConstants.SHARED_PREFS_TRACKING_TYPE, IterableConstants.TRACK_UPDATE_CART);
            newDataObject.put(IterableConstants.KEY_CREATED_AT, getCurrentTime());
            storeEventListToLocalStorage(newDataObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (checkCriteriaCompletion()) {
            createKnownUser();
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
        boolean isCompleted = false;

        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String criteriaData = sharedPref.getString(IterableConstants.SHARED_PREFS_CRITERIA, "");
        JSONArray localStoredEventList = getEventListFromLocalStorage();

        try {
            if (!criteriaData.isEmpty() && localStoredEventList.length() > 0) {
                JSONArray criteriaJsonArray = new JSONArray(criteriaData);

                for (int j = 0; j < criteriaJsonArray.length(); j++) {
                    JSONObject criteriaJsonObject = criteriaJsonArray.getJSONObject(j);
                    JSONArray criteriaListJsonArray = criteriaJsonObject.getJSONArray(IterableConstants.SHARED_PREFS_CRITERIA_LIST);

                    for (int k = 0; k < criteriaListJsonArray.length(); k++) {
                        JSONObject criteriaToBeMatched = criteriaListJsonArray.getJSONObject(k);
                        int countToMatch = criteriaToBeMatched.has(IterableConstants.SHARED_PREFS_AGGREGATE_COUNT) ? criteriaToBeMatched.getInt(IterableConstants.SHARED_PREFS_AGGREGATE_COUNT) : 1;
                        int matchedCount = 0;

                        for (int i = 0; i < localStoredEventList.length(); i++) {
                            JSONObject localEventData = localStoredEventList.getJSONObject(i);
                            if (criteriaToBeMatched.getString(IterableConstants.SHARED_PREFS_CRITERIA_TYPE).equals(localEventData.optString(IterableConstants.SHARED_PREFS_TRACKING_TYPE))) {
                                matchedCount++;
                            }

                            if (matchedCount >= countToMatch) {
                                isCompleted = true;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return isCompleted;
    }

    private boolean checkCriteriaCompletion1() {
        boolean isCompleted = false;
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String criteriaData = sharedPref.getString(IterableConstants.SHARED_PREFS_CRITERIA, "");
        JSONArray localStoredEventList = getEventListFromLocalStorage();

        try {
            if (!criteriaData.isEmpty() && localStoredEventList.length() > 0) {
                JSONObject criteriaJsonObject = new JSONObject(criteriaData);
                JSONArray criteriaJsonArray = criteriaJsonObject.getJSONArray("criteriaList");

                for (int i = 0; i < criteriaJsonArray.length(); i++) {
                    JSONObject searchQuery1 = criteriaJsonArray.getJSONObject(i).getJSONObject("searchQuery");
                    JSONArray searchQueries1 = searchQuery1.getJSONArray("searchQueries");

                    for (int j = 0; j < searchQueries1.length(); j++) {
                        JSONObject searchQuery2 = searchQueries1.getJSONObject(j);
                        JSONArray searchQueries2 = searchQuery2.getJSONArray("searchQueries");

                        for (int k = 0; k < searchQueries2.length(); k++) {
                            JSONObject searchQuery3 = searchQueries2.getJSONObject(k);
                            JSONObject searchCombo = searchQuery3.getJSONObject("searchCombo");
                            String combinator = searchCombo.getString("combinator");
                            JSONArray searchQueriesFinal = searchCombo.getJSONArray("searchQueries");
                            ArrayList<Boolean> basicQueriesResult = new ArrayList<>();

                            for (int l = 0; l < searchQueriesFinal.length(); l++) {
                                JSONObject searchQueryData = searchQueriesFinal.getJSONObject(l);
                                String comparatorType = searchQueryData.getString("comparatorType");
                                String fieldType = searchQueryData.getString("fieldType");
                                double valueToMatch = searchQueryData.getDouble("value");
                                double matchedCount = 0;
                                boolean isCriteriaMatch = false;

                                for (int m = 0; m < localStoredEventList.length(); m++) {
                                    JSONObject localEventData = localStoredEventList.getJSONObject(m);
                                    if (searchQueryData.getString(IterableConstants.SHARED_PREFS_CRITERIA_TYPE).equals(localEventData.optString(IterableConstants.SHARED_PREFS_TRACKING_TYPE))) {
                                        matchedCount++;
                                    }

                                    if (comparatorType.equals(ComparatorType1.Equals.toString())) {
                                        if (fieldType.equals("string")) {

                                        } else {
                                            if (matchedCount == valueToMatch) {
                                                isCriteriaMatch = true;
                                                break;
                                            }
                                        }

                                    } else if (comparatorType.equals(ComparatorType1.GreaterThan.toString())) {
                                        if (matchedCount > valueToMatch) {
                                            isCriteriaMatch = true;
                                            break;
                                        }
                                    } else if (comparatorType.equals(ComparatorType1.LessThan.toString())) {
                                        if (matchedCount < valueToMatch) {
                                            isCriteriaMatch = true;
                                            break;
                                        }
                                    } else if (comparatorType.equals(ComparatorType1.GreaterThanOrEqualTo.toString())) {
                                        if (matchedCount >= valueToMatch) {
                                            isCriteriaMatch = true;
                                            break;
                                        }
                                    } else if (comparatorType.equals(ComparatorType1.LessThanOrEqualTo.toString())) {
                                        if (matchedCount <= valueToMatch) {
                                            isCriteriaMatch = true;
                                            break;
                                        }
                                    }
                                }
                                basicQueriesResult.add(isCriteriaMatch);
                            }

                            if (combinator.equals(CombinatorType.And.toString())) {
                                boolean allTrue = true;
                                for (Boolean value : basicQueriesResult) {
                                    if (!value) {
                                        allTrue = false;
                                        break;
                                    }
                                }
                                isCompleted = allTrue;
                            } else {
                                boolean isAnyTrue = false;
                                for (boolean value : basicQueriesResult) {
                                    if (value) {
                                        isAnyTrue = true;
                                        break;
                                    }
                                }
                                isCompleted = isAnyTrue;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isCompleted;
    }

//    private boolean checkCriteriaCompletion2() {
//        boolean isCompleted = false;
//        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
//        String criteriaData = sharedPref.getString(IterableConstants.SHARED_PREFS_CRITERIA, "");
//        JSONArray localStoredEventList = getEventListFromLocalStorage();
//
//        try {
//            if (!criteriaData.isEmpty() && localStoredEventList.length() > 0) {
//                JSONObject criteriaJsonObject = new JSONObject(criteriaData);
//                JSONArray criteriaList = criteriaJsonObject.getJSONArray("criteriaList");
//                LogicalExpressionEvaluator evaluator = new LogicalExpressionEvaluator();
//
//                for (int i = 0; i < criteriaList.length(); i++) {
//                    boolean result = evaluator.evaluateTree(criteriaList.getJSONObject(i), localStoredEventList);
//                    System.out.println("Result for criteria " + i + ": " + result);
//                    IterableLogger.e("Result for criteria ", i + ": " + result);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return isCompleted;
//    }

    private boolean checkCriteriaCompletion3() {
        boolean isCompleted = false;
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String criteriaData = sharedPref.getString(IterableConstants.SHARED_PREFS_CRITERIA, "");
        JSONArray localStoredEventList = getEventListFromLocalStorage();

        try {
            if (!criteriaData.isEmpty() && localStoredEventList.length() > 0) {
                JSONObject criteriaJsonObject = new JSONObject(criteriaData);
                JSONArray criteriaList = criteriaJsonObject.getJSONArray("criteriaList");
                LogicalExpressionEvaluator evaluator = new LogicalExpressionEvaluator();

                for (int i = 0; i < localStoredEventList.length(); i++) {
                    JSONObject localEventData = localStoredEventList.getJSONObject(i);
                    Gson gson = new GsonBuilder().create();
                    Type listType = new TypeToken<List<CommerceItem>>() {
                    }.getType();
                    List<CommerceItem> itemList = gson.fromJson(localEventData.getString(IterableConstants.KEY_ITEMS), listType);
                    for (int j = 0; j < itemList.size(); j++) {
                        for (int k = 0; k < criteriaList.length(); k++) {
                            boolean result = evaluator.evaluateTree(criteriaList.getJSONObject(k), itemList.get(j));
                            IterableLogger.e(TAG, "Result for criteria " + result);
                            if (result) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isCompleted;
    }

    private void createKnownUser() {
        IterableApi.getInstance().setUserId(UUID.randomUUID().toString());
        SharedPreferences sharedPref = IterableApi.sharedInstance.getMainActivityContext().getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String userData = sharedPref.getString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, "");

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

    private void syncEvents() {

        JSONArray trackEventList = getEventListFromLocalStorage();
        if (trackEventList.length() > 0) {
            for (int i = 0; i < trackEventList.length(); i++) {
                try {
                    JSONObject event = trackEventList.getJSONObject(i);
                    String eventType = event.getString(IterableConstants.SHARED_PREFS_TRACKING_TYPE);

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

enum CombinatorType {
    And {
        @NonNull
        @Override
        public String toString() {
            return "And";
        }
    },
    Or {
        @NonNull
        @Override
        public String toString() {
            return "Or";
        }
    }
}

enum ComparatorType1 {
    Equals {
        @NonNull
        @Override
        public String toString() {
            return "Equals";
        }
    },
    GreaterThanOrEqualTo {
        @NonNull
        @Override
        public String toString() {
            return "GreaterThanOrEqualTo";
        }
    },
    LessThanOrEqualTo {
        @NonNull
        @Override
        public String toString() {
            return "LessThanOrEqualTo";
        }
    },
    GreaterThan {
        @NonNull
        @Override
        public String toString() {
            return "GreaterThan";
        }
    },
    LessThan {
        @NonNull
        @Override
        public String toString() {
            return "LessThan";
        }
    }
}
