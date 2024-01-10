package com.iterable.iterableapi.util;

import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CriteriaCompletionChecker {

    private JSONArray localStoredEventList;

    public Integer getMatchedCriteria(String criteriaData, JSONArray localStoredEventList) {
        this.localStoredEventList = localStoredEventList;
        Integer criteriaId = null;

        try {
            JSONObject json = new JSONObject(criteriaData);
            if (json.has("criterias")) {
                JSONArray criteriaList = json.getJSONArray("criterias");
                for (int i = 0; i < criteriaList.length(); i++) {
                    JSONObject criteria = criteriaList.getJSONObject(i);
                    if (criteria.has("searchQuery") && criteria.has("criteriaId")) {

                        JSONObject searchQuery = criteria.getJSONObject("searchQuery");
                        int currentCriteriaId = criteria.getInt("criteriaId");
                        JSONArray eventsToProcess = getEventsWithCartItems();
                        JSONArray nonPurchaseEvents = getNonCartEvents();
                        for (int j = 0; j < nonPurchaseEvents.length(); j++) {
                            eventsToProcess.put(nonPurchaseEvents.getJSONObject(j));
                        }

                        boolean result = evaluateTree(searchQuery, eventsToProcess);
                        if (result) {
                            criteriaId = currentCriteriaId;
                            break;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            handleJSONException(e);
        }
        return criteriaId;
    }

    private JSONArray getEventsWithCartItems() {
        JSONArray processedEvents = new JSONArray();
        try {
            for (int i = 0; i < localStoredEventList.length(); i++) {
                JSONObject localEventData = localStoredEventList.getJSONObject(i);
                if (localEventData.has(IterableConstants.SHARED_PREFS_EVENT_TYPE) && (
                        localEventData.get(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(IterableConstants.TRACK_PURCHASE)
                                || (localEventData.get(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(IterableConstants.TRACK_UPDATE_CART)))) {

                    JSONObject updatedItem = new JSONObject();
                    if (localEventData.has(IterableConstants.KEY_ITEMS)) {

                        JSONArray items = new JSONArray(localEventData.getString(IterableConstants.KEY_ITEMS));
                        for (int j = 0; j < items.length(); j++) {
                            JSONObject item = items.getJSONObject(j);
                            Iterator<String> itemKeys = item.keys();

                            while (itemKeys.hasNext()) {
                                String key = itemKeys.next();
                                updatedItem.put("shoppingCartItems." + key, item.get(key));
                            }
                        }
                    }

                    if (localEventData.has("dataFields")) {
                        JSONObject dataFields = localEventData.getJSONObject("dataFields");
                        Iterator<String> fieldKeys = dataFields.keys();
                        while (fieldKeys.hasNext()) {
                            String key = fieldKeys.next();
                            updatedItem.put(key, dataFields.get(key));
                        }
                    }

                    Iterator<String> keys = localEventData.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (!key.equals(IterableConstants.KEY_ITEMS) && !key.equals("dataFields")) {
                            updatedItem.put(key, localEventData.get(key));
                        }
                    }
                    processedEvents.put(updatedItem);
                }
            }
        } catch (JSONException e) {
            handleJSONException(e);
        }
        return processedEvents;
    }

    private JSONArray getNonCartEvents() {
        JSONArray nonPurchaseEvents = new JSONArray();
        try {
            for (int i = 0; i < localStoredEventList.length(); i++) {
                JSONObject localEventData = localStoredEventList.getJSONObject(i);
                if (localEventData.has(IterableConstants.SHARED_PREFS_EVENT_TYPE)
                        && !localEventData.get(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(IterableConstants.TRACK_PURCHASE)
                        && !localEventData.get(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(IterableConstants.TRACK_UPDATE_CART)) {
                    nonPurchaseEvents.put(localEventData);
                }
            }
        } catch (JSONException e) {
            handleJSONException(e);
        }
        return nonPurchaseEvents;
    }

    public boolean evaluateTree(JSONObject node, JSONArray localEventData) {
        try {
            if (node.has("searchQueries")) {
                String combinator = node.getString("combinator");
                JSONArray searchQueries = node.getJSONArray("searchQueries");
                if (combinator.equals("And")) {
                    for (int i = 0; i < searchQueries.length(); i++) {
                        if (!evaluateTree(searchQueries.getJSONObject(i), localEventData)) {
                            return false;
                        }
                    }
                    return true;
                } else if (combinator.equals("Or")) {
                    for (int i = 0; i < searchQueries.length(); i++) {
                        if (evaluateTree(searchQueries.getJSONObject(i), localEventData)) {
                            return true;
                        }
                    }
                    return false;
                }
            } else if (node.has("searchCombo")) {
                JSONObject searchCombo = node.getJSONObject("searchCombo");
                return evaluateTree(searchCombo, localEventData);
            } else if (node.has("field")) {
                return evaluateField(node, localEventData);
            }
        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }

    private boolean evaluateField(JSONObject node, JSONArray localEventData) {
        try {
            return evaluateFieldLogic(node, localEventData);
        } catch (JSONException e) {
            handleJSONException(e);
        }
        return false;
    }

    private boolean evaluateFieldLogic(JSONObject node, JSONArray localEventData) throws JSONException {

        for (int i = 0; i < localEventData.length(); i++) {
            JSONObject eventData = localEventData.getJSONObject(i);
            String trackingType = eventData.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE);
            String dataType = node.getString("dataType");
            if (!dataType.equals(trackingType)) {
                return false;
            }

            String field = node.getString("field");
            String comparatorType = node.getString("comparatorType");
            ArrayList<String> localDataKeys = extractKeys(eventData);

            for (String key : localDataKeys) {
                if (field.equals(key)) {
                    Object matchedCountObj = eventData.get(key);
                    if (evaluateComparison(comparatorType, matchedCountObj, node.getString("value"))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean evaluateComparison(String comparatorType, Object matchObj, String valueToCompare) {
        if (valueToCompare == null) {
            return false;
        }

        switch (comparatorType) {
            case "Equals":
                return compareValueEquality(matchObj, valueToCompare);
            case "DoesNotEquals":
                return !compareValueEquality(matchObj, valueToCompare);
            case "GreaterThan":
                return compareNumericValues(matchObj, valueToCompare, " > ");
            case "LessThan":
                return compareNumericValues(matchObj, valueToCompare, " < ");
            case "GreaterThanOrEqualTo":
                return compareNumericValues(matchObj, valueToCompare, " >= ");
            case "LessThanOrEqualTo":
                return compareNumericValues(matchObj, valueToCompare, " <= ");
            case "Contains":
                return compareStringContains(matchObj, valueToCompare);
            case "StartsWith":
                return compareStringStartsWith(matchObj, valueToCompare);
            case "MatchesRegex":
                return compareWithRegex(matchObj instanceof String ? (String) matchObj : "", valueToCompare);
            default:
                return false;
        }
    }

    private boolean compareValueEquality(Object sourceTo, String stringValue) {
        if (sourceTo instanceof Double && isDouble(stringValue)) {
            return ((Double) sourceTo).equals(Double.parseDouble(stringValue));
        } else if (sourceTo instanceof Integer && isInteger(stringValue)) {
            return ((Integer) sourceTo).equals(Integer.parseInt(stringValue));
        } else if (sourceTo instanceof Long && isLong(stringValue)) {
            return ((Long) sourceTo).equals(Long.parseLong(stringValue));
        } else if (sourceTo instanceof Boolean && isBoolean(stringValue)) {
            return ((Boolean) sourceTo).equals(Boolean.parseBoolean(stringValue));
        } else if (sourceTo instanceof String) {
            return ((String) sourceTo).equals(stringValue);
        }
        return false;
    }

    private boolean compareNumericValues(Object sourceTo, String stringValue, String compareOperator) {
        if (isDouble(stringValue)) {
            double sourceNumber = getDoubleValue(sourceTo);
            double numericValue = Double.parseDouble(stringValue);
            switch (compareOperator.trim()) {
                case ">":
                    return sourceNumber > numericValue;
                case "<":
                    return sourceNumber < numericValue;
                case ">=":
                    return sourceNumber >= numericValue;
                case "<=":
                    return sourceNumber <= numericValue;
                default:
                    return false;
            }
        }
        return false;
    }

    private boolean compareStringContains(Object sourceTo, String stringValue) {
        return sourceTo instanceof String && ((String) sourceTo).contains(stringValue);
    }

    private boolean compareStringStartsWith(Object sourceTo, String stringValue) {
        return sourceTo instanceof String && ((String) sourceTo).startsWith(stringValue);
    }

    private boolean compareWithRegex(String sourceTo, String pattern) {
        try {
            Pattern regexPattern = Pattern.compile(pattern);
            return regexPattern.matcher(sourceTo).matches();
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }

    private double getDoubleValue(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Integer) {
            return (double) (Integer) value;
        } else if (value instanceof Long) {
            return (double) (Long) value;
        } else if (value instanceof String && isDouble((String) value)) {
            return Double.parseDouble((String) value);
        }
        return 0.0;
    }

    private boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private ArrayList<String> extractKeys(JSONObject jsonObject) {
        ArrayList<String> keys = new ArrayList<>();
        Iterator<String> jsonKeys = jsonObject.keys();
        while (jsonKeys.hasNext()) {
            keys.add(jsonKeys.next());
        }
        return keys;
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