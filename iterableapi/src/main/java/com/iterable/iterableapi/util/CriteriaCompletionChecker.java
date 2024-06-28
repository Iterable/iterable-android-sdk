package com.iterable.iterableapi.util;

import androidx.annotation.NonNull;
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

    public String getMatchedCriteria(String criteriaData, JSONArray localStoredEventList) {
        this.localStoredEventList = localStoredEventList;
        String criteriaId = null;

        try {
            JSONObject json = new JSONObject(criteriaData);
            if (json.has(IterableConstants.CRITERIAS)) {
                JSONArray criteriaList = json.getJSONArray(IterableConstants.CRITERIAS);
                criteriaId = findMatchedCriteria(criteriaList);
            }
        } catch (JSONException e) {
            handleJSONException(e);
        }

        return criteriaId;
    }

    private String findMatchedCriteria(JSONArray criteriaList) {
        String criteriaId = null;
        JSONArray eventsToProcess = prepareEventsToProcess();

        for (int i = 0; i < criteriaList.length(); i++) {
            try {
                JSONObject criteria = criteriaList.getJSONObject(i);
                if (criteria.has(IterableConstants.SEARCH_QUERY) && criteria.has(IterableConstants.CRITERIA_ID)) {
                    JSONObject searchQuery = criteria.getJSONObject(IterableConstants.SEARCH_QUERY);
                    String currentCriteriaId = criteria.getString(IterableConstants.CRITERIA_ID);
                    boolean result = evaluateTree(searchQuery, eventsToProcess);
                    if (result) {
                        criteriaId = currentCriteriaId;
                        break;
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return criteriaId;
    }

    private JSONArray prepareEventsToProcess() {
        JSONArray eventsToProcess = getEventsWithCartItems();
        JSONArray nonPurchaseEvents = getNonCartEvents();

        for (int i = 0; i < nonPurchaseEvents.length(); i++) {
            try {
                eventsToProcess.put(nonPurchaseEvents.getJSONObject(i));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return eventsToProcess;
    }

    private JSONArray getEventsWithCartItems() {
        JSONArray processedEvents = new JSONArray();
        try {
            for (int i = 0; i < localStoredEventList.length(); i++) {
                JSONObject localEventData = localStoredEventList.getJSONObject(i);
                if (localEventData.has(IterableConstants.SHARED_PREFS_EVENT_TYPE) && (
                        localEventData.get(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(IterableConstants.TRACK_PURCHASE))) {
                    JSONObject updatedItem = new JSONObject();

                    if (localEventData.has(IterableConstants.KEY_ITEMS)) {
                        final JSONArray items = new JSONArray(localEventData.getString(IterableConstants.KEY_ITEMS));
                        final JSONArray processedItems = new JSONArray();
                        for (int j = 0; j < items.length(); j++) {
                            JSONObject processedItem = new JSONObject();
                            JSONObject item = items.getJSONObject(j);
                            Iterator<String> keys = item.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                processedItem.put(IterableConstants.PURCHASE_ITEM_PREFIX + key, item.get(key));
                            }
                            processedItems.put(processedItem);
                        }
                        updatedItem.put(IterableConstants.KEY_ITEMS, processedItems);
                    }

                    if (localEventData.has(IterableConstants.KEY_DATA_FIELDS)) {
                        JSONObject dataFields = localEventData.getJSONObject(IterableConstants.KEY_DATA_FIELDS);
                        Iterator<String> fieldKeys = dataFields.keys();
                        while (fieldKeys.hasNext()) {
                            String key = fieldKeys.next();
                            updatedItem.put(key, dataFields.get(key));
                        }
                    }

                    Iterator<String> keys = localEventData.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (!key.equals(IterableConstants.KEY_ITEMS) && !key.equals(IterableConstants.KEY_DATA_FIELDS)) {
                            updatedItem.put(key, localEventData.get(key));
                        }
                    }
                    processedEvents.put(updatedItem);
                } else if (localEventData.has(IterableConstants.SHARED_PREFS_EVENT_TYPE) && (
                        localEventData.get(IterableConstants.SHARED_PREFS_EVENT_TYPE).equals(IterableConstants.TRACK_UPDATE_CART))) {
                    JSONObject updatedItem = new JSONObject();
                    updatedItem.put(IterableConstants.KEY_EVENT_NAME, IterableConstants.UPDATE_CART);

                    if (localEventData.has(IterableConstants.KEY_ITEMS)) {
                        final JSONArray items = new JSONArray(localEventData.getString(IterableConstants.KEY_ITEMS));
                        final JSONArray processedItems = new JSONArray();
                        for (int j = 0; j < items.length(); j++) {
                            JSONObject processedItem = new JSONObject();
                            JSONObject item = items.getJSONObject(j);
                            Iterator<String> keys = item.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                processedItem.put(IterableConstants.UPDATECART_ITEM_PREFIX + key, item.get(key));
                            }
                            processedItems.put(processedItem);
                        }
                        updatedItem.put(IterableConstants.KEY_ITEMS, processedItems);
                    }

                    if (localEventData.has(IterableConstants.KEY_DATA_FIELDS)) {
                        JSONObject dataFields = localEventData.getJSONObject(IterableConstants.KEY_DATA_FIELDS);
                        Iterator<String> fieldKeys = dataFields.keys();
                        while (fieldKeys.hasNext()) {
                            String key = fieldKeys.next();
                            updatedItem.put(key, dataFields.get(key));
                        }
                    }

                    Iterator<String> localEventDataKeys = localEventData.keys();
                    while (localEventDataKeys.hasNext()) {
                        String key = localEventDataKeys.next();
                        if (!key.equals(IterableConstants.KEY_ITEMS) && !key.equals(IterableConstants.KEY_DATA_FIELDS)) {
                            if (key.equals(IterableConstants.SHARED_PREFS_EVENT_TYPE)) {
                                updatedItem.put(key, IterableConstants.TRACK_EVENT);
                            } else {
                                updatedItem.put(key, localEventData.get(key));
                            }
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

                    JSONObject updatedItem = new JSONObject(localEventData.toString());
                    if (localEventData.has(IterableConstants.KEY_DATA_FIELDS)) {
                        JSONObject dataFields = localEventData.getJSONObject(IterableConstants.KEY_DATA_FIELDS);
                        Iterator<String> fieldKeys = dataFields.keys();
                        while (fieldKeys.hasNext()) {
                            String key = fieldKeys.next();
                            updatedItem.put(key, dataFields.get(key));
                        }
                    }
                    nonPurchaseEvents.put(updatedItem);
                }
            }
        } catch (JSONException e) {
            handleJSONException(e);
        }
        return nonPurchaseEvents;
    }

    public boolean evaluateTree(JSONObject node, JSONArray localEventData) {
        try {
            if (node.has(IterableConstants.SEARCH_QUERIES)) {
                String combinator = node.getString(IterableConstants.COMBINATOR);
                JSONArray searchQueries = node.getJSONArray(IterableConstants.SEARCH_QUERIES);
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
                } else if (combinator.equals("Not")) {
                    for (int i = 0; i < searchQueries.length(); i++) {
                        searchQueries.getJSONObject(i).put("isNot", true);
                        if (evaluateTree(searchQueries.getJSONObject(i), localEventData)) {
                            return false;
                        }
                    }
                    return true;
                }
            } else if (node.has(IterableConstants.SEARCH_COMBO)) {
                return evaluateSearchQueries(node, localEventData);
            }
        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }

    private boolean evaluateSearchQueries(JSONObject node, @NonNull JSONArray localEventData) throws JSONException {
        for (int i = 0; i < localEventData.length(); i++) {
            JSONObject eventData = localEventData.getJSONObject(i);
            String trackingType = eventData.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE);
            String dataType = node.getString(IterableConstants.DATA_TYPE);
            if (dataType.equals(trackingType)) {
                JSONObject searchCombo = node.getJSONObject(IterableConstants.SEARCH_COMBO);
                JSONArray searchQueries = searchCombo.getJSONArray(IterableConstants.SEARCH_QUERIES);
                String combinator = searchCombo.getString(IterableConstants.COMBINATOR);
                boolean isNot = node.has("isNot");

                if (evaluateEvent(searchQueries, eventData, combinator)) {
                    if (node.has(IterableConstants.MIN_MATCH)) {
                        int minMatch = node.getInt(IterableConstants.MIN_MATCH) - 1;
                        node.put(IterableConstants.MIN_MATCH, minMatch);
                        if (minMatch > 0) {
                            continue;
                        }
                    }
                    if (isNot && !(i + 1 == localEventData.length())) {
                        continue;
                    }
                    return true;
                } else if (isNot) {
                    return false;
                }

            }
        }
        return false;
    }

    private boolean evaluateEvent(JSONArray searchQueries, JSONObject eventData, String combinator) throws JSONException {
        if (combinator.equals("And") || combinator.equals("Or")) {
            return evaluateFieldLogic(searchQueries, eventData);
        } else if (combinator.equals("Not")) {
            return !evaluateFieldLogic(searchQueries, eventData);
        }
        return false;
    }

    private boolean evaluateFieldLogic(JSONArray searchQueries, JSONObject eventData) throws JSONException {
                boolean itemMatchResult = false;
                if (eventData.has(IterableConstants.KEY_ITEMS)) {
                    boolean result = false;
                    JSONArray items = new JSONArray(eventData.getString(IterableConstants.KEY_ITEMS));
                    for (int j = 0; j < items.length(); j++) {
                        JSONObject item = items.getJSONObject(j);
                        if (doesItemMatchQueries(searchQueries, item)) {
                           result = true;
                           break;
                        }
                    }
                    if (!result && doesItemCriteriaExists(searchQueries)) {
                        return false;
                    }
                    itemMatchResult = result;
                }

                ArrayList<String> filteredDataKeys = new ArrayList<>();
                Iterator<String> localEventDataKeys = eventData.keys();
                while (localEventDataKeys.hasNext()) {
                    String localEventDataKey = localEventDataKeys.next();
                    if (!localEventDataKey.equals(IterableConstants.KEY_ITEMS)) {
                        filteredDataKeys.add(localEventDataKey);
                    }
                }

                if (filteredDataKeys.size() == 0) {
                    return itemMatchResult;
                }

                JSONArray filteredSearchQueries = new JSONArray();
                for (int i = 0; i < searchQueries.length(); i++) {
                    JSONObject searchQuery = searchQueries.getJSONObject(i);
                    String field = searchQuery.getString(IterableConstants.FIELD);
                    if (!field.startsWith(IterableConstants.PURCHASE_ITEM_PREFIX) && !field.startsWith(IterableConstants.UPDATECART_ITEM_PREFIX)) {
                        filteredSearchQueries.put(searchQuery);
                    }
                }
                if (filteredSearchQueries.length() == 0) {
                    return itemMatchResult;
                }
                boolean matchResult = false;
                for (int k = 0; k < filteredSearchQueries.length(); k++) {
                    JSONObject searchQuery = filteredSearchQueries.getJSONObject(k);
                    String field = searchQuery.getString(IterableConstants.FIELD);
                    boolean isKeyExists = false;
                    for (String filteredDataKey : filteredDataKeys) {
                        if (field.equals(filteredDataKey)) {
                            isKeyExists = true;
                        }
                    }

                    if (isKeyExists) {
                        if (evaluateComparison(searchQuery.getString(IterableConstants.COMPARATOR_TYPE), eventData.get(field), searchQuery.getString(IterableConstants.VALUE))) {
                            matchResult = true;
                            continue;
                        }
                    }
                    matchResult = false;
                    break;
                }
                return matchResult;
    }

    private boolean doesItemCriteriaExists(JSONArray searchQueries) throws JSONException {
        for (int i = 0; i < searchQueries.length(); i++) {
            String field = searchQueries.getJSONObject(i).getString(IterableConstants.FIELD);
            if (field.startsWith(IterableConstants.UPDATECART_ITEM_PREFIX) || field.startsWith(IterableConstants.PURCHASE_ITEM_PREFIX)) {
                return true;
            }
        }
        return false;
    }
    private boolean doesItemMatchQueries(JSONArray searchQueries, JSONObject item) throws JSONException {

        JSONArray filterSearchQueries = new JSONArray();

        for (int i = 0; i < searchQueries.length(); i++) {
            JSONObject searchQuery = searchQueries.getJSONObject(i);
            if (item.has(searchQuery.getString(IterableConstants.FIELD))) {
                filterSearchQueries.put(searchQuery);
            }
        }

        if (filterSearchQueries.length() == 0) {
            return  false;
        }

        for (int j = 0; j < filterSearchQueries.length(); j++) {
            JSONObject query = filterSearchQueries.getJSONObject(j);
            String field = query.getString(IterableConstants.FIELD);
            if (item.has(field)) {
                if(!evaluateComparison(query.getString(IterableConstants.COMPARATOR_TYPE), item.get(field), query.getString(IterableConstants.VALUE))) {
                    return false;
                }
            }

        }

        if (filterSearchQueries.length() > 0) {
            return true;
        }

        return false;
    }

    public static String formattedDoubleValue(double d) {
        if (d == (long) d)
            return String.format("%d", (long) d);
        else
            return String.format("%s", d);
    }

    private boolean evaluateComparison(String comparatorType, Object matchObj, String valueToCompare) {
        if (valueToCompare == null && !comparatorType.equals(MatchComparator.IS_SET)) {
            return false;
        }

        if (isDouble(valueToCompare)) {
            // here do the conversion of this number to formatted double value by removing trailing zeros
            // because when jsonstring to jsonarray happens for items object it removes trailing zeros
            valueToCompare = formattedDoubleValue(Double.parseDouble(valueToCompare));
        }

        switch (comparatorType) {
            case MatchComparator.EQUALS:
                return compareValueEquality(matchObj, valueToCompare);
            case MatchComparator.DOES_NOT_EQUALS:
                return !compareValueEquality(matchObj, valueToCompare);
            case MatchComparator.IS_SET:
                return !compareValueEquality(matchObj, "");
            case MatchComparator.GREATER_THAN:
                return compareNumericValues(matchObj, valueToCompare, " > ");
            case MatchComparator.LESS_THAN:
                return compareNumericValues(matchObj, valueToCompare, " < ");
            case MatchComparator.GREATER_THAN_OR_EQUAL_TO:
                return compareNumericValues(matchObj, valueToCompare, " >= ");
            case MatchComparator.LESS_THAN_OR_EQUAL_TO:
                return compareNumericValues(matchObj, valueToCompare, " <= ");
            case MatchComparator.CONTAINS:
                return compareStringContains(String.valueOf(matchObj), valueToCompare);
            case MatchComparator.STARTS_WITH:
                return compareStringStartsWith(matchObj, valueToCompare);
            case MatchComparator.MATCHES_REGEX:
                return compareWithRegex(matchObj instanceof String ? (String) matchObj : "", valueToCompare);
            default:
                return false;
        }
    }

    private boolean compareValueEquality(Object sourceTo, String stringValue) {
        if (sourceTo instanceof Double && isDouble(stringValue)) {
            return sourceTo.equals(Double.parseDouble(stringValue));
        } else if (sourceTo instanceof Integer && isInteger(stringValue)) {
            return sourceTo.equals(Integer.parseInt(stringValue));
        } else if (sourceTo instanceof Long && isLong(stringValue)) {
            return sourceTo.equals(Long.parseLong(stringValue));
        } else if (sourceTo instanceof Boolean && isBoolean(stringValue)) {
            return sourceTo.equals(Boolean.parseBoolean(stringValue));
        } else if (sourceTo instanceof String) {
            return sourceTo.equals(stringValue);
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

    private boolean compareStringContains(String sourceTo, String stringValue) {
        return sourceTo.contains(stringValue);
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