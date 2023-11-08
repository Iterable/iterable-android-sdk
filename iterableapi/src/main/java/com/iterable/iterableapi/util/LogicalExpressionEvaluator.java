package com.iterable.iterableapi.util;

import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class LogicalExpressionEvaluator {

    private ArrayList<String> localDataKeys;

    public boolean compareData(JSONObject node, JSONObject eventItem, JSONObject localEventData) {
        try {
            localDataKeys = extractKeys(localEventData);
            localDataKeys.addAll(extractKeys(eventItem));

            String trackingType = localEventData.getString(IterableConstants.SHARED_PREFS_TRACKING_TYPE);
            return evaluateTree(node, eventItem, trackingType);

        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }

    private ArrayList<String> extractKeys(JSONObject jsonObject) {
        ArrayList<String> keys = new ArrayList<>();
        Iterator<String> jsonKeys = jsonObject.keys();
        while (jsonKeys.hasNext()) {
            keys.add(jsonKeys.next());
        }
        return keys;
    }

    public boolean evaluateTree(JSONObject node, JSONObject localEventData, String trackingType) {
        try {
            if (node.has("searchQueries")) {
                String combinator = node.getString("combinator");
                JSONArray searchQueries = node.getJSONArray("searchQueries");
                if (combinator.equals("And")) {
                    for (int i = 0; i < searchQueries.length(); i++) {
                        if (!evaluateTree(searchQueries.getJSONObject(i), localEventData, trackingType)) {
                            return false;
                        }
                    }
                    return true;
                } else if (combinator.equals("Or")) {
                    for (int i = 0; i < searchQueries.length(); i++) {
                        if (evaluateTree(searchQueries.getJSONObject(i), localEventData, trackingType)) {
                            return true;
                        }
                    }
                    return false;
                }
            } else if (node.has("searchCombo")) {
                JSONObject searchCombo = node.getJSONObject("searchCombo");
                return evaluateTree(searchCombo, localEventData, trackingType);
            } else if (node.has("field")) {
                return evaluateField(node, localEventData, trackingType);
            }
        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }

    private boolean evaluateField(JSONObject node, JSONObject localEventData, String trackingType) {
        try {
            return evaluateFieldLogic(node, localEventData, trackingType);
        } catch (JSONException e) {
            handleJSONException(e);
        }
        return false;
    }

    private boolean evaluateFieldLogic(JSONObject node, JSONObject localEventData, String trackingType) throws JSONException {
        String dataType = node.getString("dataType");
        if (!dataType.equals(trackingType)) {
            return false;
        }

        String field = node.getString("field");
        String comparatorType = node.getString("comparatorType");
        String fieldType = node.getString("fieldType");

        for (String key : localDataKeys) {
            if (field.endsWith(key)) {
                Object matchedCountObj = localEventData.get(key);
                if (evaluateComparison(comparatorType, fieldType, matchedCountObj, node)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean evaluateComparison(String comparatorType, String fieldType, Object matchedCountObj, JSONObject node) {
        try {
            double matchedCount = 0.0;

            if (fieldType.equals("string") && matchedCountObj instanceof String) {
                return matchedCountObj.equals(node.getString("value"));
            } else if (matchedCountObj instanceof Number) {
                matchedCount = ((Number) matchedCountObj).doubleValue();
            }

            double valueToMatch = node.getDouble("value");

            switch (comparatorType) {
                case "Equals":
                    return matchedCount == valueToMatch;
                case "GreaterThan":
                    return matchedCount > valueToMatch;
                case "LessThan":
                    return matchedCount < valueToMatch;
                case "GreaterThanOrEqualTo":
                    return matchedCount >= valueToMatch;
                case "LessThanOrEqualTo":
                    return matchedCount <= valueToMatch;
                default:
                    return false;
            }
        } catch (JSONException e) {
            handleJSONException(e);
            return false;
        }
    }

    private void handleException(Exception e) {
        IterableLogger.e("Exception occurred", e.toString());
        e.printStackTrace();
    }

    private void handleJSONException(JSONException e) {
        IterableLogger.e("JSONException occurred", e.toString());
        e.printStackTrace();
    }
}