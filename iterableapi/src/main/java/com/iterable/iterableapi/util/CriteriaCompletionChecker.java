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
import java.util.Map;

public class CriteriaCompletionChecker {

    private JSONArray localStoredEventList;

    public String getMatchedCriteria(String criteriaData, JSONArray localStoredEventList) {
        //store locally stored event list
        this.localStoredEventList = localStoredEventList;
        String criteriaId = null;

        try {
            JSONObject json = new JSONObject(criteriaData);
            if (json.has(IterableConstants.CRITERIAS)) {
                // pull criteria list from json
                JSONArray criteriaList = json.getJSONArray(IterableConstants.CRITERIAS);

                // find matched criteria
                criteriaId = findMatchedCriteria(criteriaList);
            }
        } catch (JSONException e) {
            handleJSONException(e);
        }

        return criteriaId;
    }

    private String findMatchedCriteria(JSONArray criteriaList) {
        JSONArray eventsToProcess = prepareEventsToProcess();

        //for each criteria
        for (int i = 0; i < criteriaList.length(); i++) {
            try {
                JSONObject criteria = criteriaList.getJSONObject(i);

                // check if criteria is valid
                if (isCriteriaValid(criteria)) {
                    JSONObject searchQuery = criteria.getJSONObject(IterableConstants.SEARCH_QUERY);
                    String currentCriteriaId = criteria.getString(IterableConstants.CRITERIA_ID);

                    // check if criteria matches
                    if (isCriteriaMatched(searchQuery, eventsToProcess)) {
                        return currentCriteriaId;
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException("Error processing criteria: " + e.getMessage(), e);
            }
        }

        return null;
    }

    private boolean isCriteriaValid(JSONObject criteria) {
        return criteria.has(IterableConstants.SEARCH_QUERY) && criteria.has(IterableConstants.CRITERIA_ID);
    }

    private boolean isCriteriaMatched(JSONObject searchQuery, JSONArray eventsToProcess) {
        return evaluateTree(searchQuery, eventsToProcess);
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
                        updatedItem.put(IterableConstants.PURCHASE_ITEM, processedItems);
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
        if (combinator.equals("And")) {
            if (!evaluateFieldLogic(searchQueries, eventData)) {
                return false;
            }
            return true;
        } else if (combinator.equals("Or")) {
            if (evaluateFieldLogic(searchQueries, eventData)) {
                return true;
            }
        } else if (combinator.equals("Not")) {
            return !evaluateFieldLogic(searchQueries, eventData);
        }

        return false;
    }

    private boolean evaluateFieldLogic(JSONArray searchQueries, JSONObject eventData) throws JSONException {
        // evaluate item-related queries
        String itemKey = getItemKey(eventData);

        boolean itemMatchResult = false;

        if (itemKey != null) {
            if (!evaluateItemQueries(searchQueries, eventData, itemKey) && doesItemCriteriaExists(searchQueries)) {
                return false;
            }
        }

        //filter event data keys
        ArrayList<String> filteredDataKeys = filterEventDataKeys(eventData);

        // if there are no filtered data keys, return the result of item queries
        if (filteredDataKeys.isEmpty()) {
            return itemKey != null;
        }

        // filter search queries that are not item-related
        JSONArray nonItemSearchQueries = getNonItemSearchQueries(searchQueries);
        if (nonItemSearchQueries.length() == 0) {
            return itemKey != null;
        }

        // evaluate non-item related search queries
        return evaluateNonItemQueries(nonItemSearchQueries, eventData, filteredDataKeys);
    }

    private String getItemKey(JSONObject eventData) throws JSONException {
        if (eventData.has(IterableConstants.KEY_ITEMS)) {
            return IterableConstants.KEY_ITEMS;
        } else if (eventData.has(IterableConstants.PURCHASE_ITEM)) {
            return IterableConstants.PURCHASE_ITEM;
        }
        return null;
    }

    private boolean evaluateItemQueries(JSONArray searchQueries, JSONObject eventData, String itemKey) throws JSONException {
        JSONArray items = new JSONArray(eventData.getString(itemKey));
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (doesItemMatchQueries(searchQueries, item)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<String> filterEventDataKeys(JSONObject eventData) {
        ArrayList<String> filteredDataKeys = new ArrayList<>();
        Iterator<String> keys = eventData.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            if (!key.equals(IterableConstants.KEY_ITEMS)) {
                filteredDataKeys.add(key);
            }
        }

        return filteredDataKeys;
    }

    private JSONArray getNonItemSearchQueries(JSONArray searchQueries) throws JSONException {
        JSONArray filteredSearchQueries = new JSONArray();
        for (int i = 0; i < searchQueries.length(); i++) {
            JSONObject searchQuery = searchQueries.getJSONObject(i);
            String field = searchQuery.getString(IterableConstants.FIELD);
            if (!field.startsWith(IterableConstants.PURCHASE_ITEM_PREFIX) && !field.startsWith(IterableConstants.UPDATECART_ITEM_PREFIX)) {
                filteredSearchQueries.put(searchQuery);
            }
        }
        return filteredSearchQueries;
    }

    private boolean evaluateNonItemQueries(JSONArray searchQueries, JSONObject eventData, ArrayList<String> filteredDataKeys) throws JSONException {
        boolean matchResult = false;
        for (int k = 0; k < searchQueries.length(); k++) {
            JSONObject searchQuery = searchQueries.getJSONObject(k);
            String field = searchQuery.getString(IterableConstants.FIELD);

            boolean isKeyExists = false;
            if (searchQuery.getString(IterableConstants.DATA_TYPE).equals(IterableConstants.TRACK_EVENT) && searchQuery.getString("fieldType").equals("object") && searchQuery.getString(IterableConstants.COMPARATOR_TYPE).equals(MatchComparator.IS_SET)) {
                final String eventName = eventData.getString(IterableConstants.KEY_EVENT_NAME);
                if ((eventName.equals(IterableConstants.UPDATE_CART) && field.equals(eventName)) || field.equals(eventName)) {
                    matchResult = true;
                    continue;
                }
            } else {
                for (String filteredDataKey : filteredDataKeys) {
                    if (field.equals(filteredDataKey)) {
                        isKeyExists = true;
                    }
                }
            }
            if (field.contains(".")) {
                String[] splitString = field.split("\\.");
                String firstElement = splitString[0];
                Object eventDataFirstElement = eventData.has(firstElement) ? eventData.get(firstElement) : null;
                if (eventDataFirstElement instanceof JSONArray) {
                    JSONArray jsonArraySourceTo = (JSONArray) eventDataFirstElement;
                    for (int i = 0; i < jsonArraySourceTo.length(); i++) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(firstElement, jsonArraySourceTo.get(i));
                        jsonObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, eventData.get(IterableConstants.SHARED_PREFS_EVENT_TYPE));
                        matchResult = evaluateFieldLogic(searchQueries, jsonObject);
                        if (matchResult) {
                            break;
                        }
                    }
                    if (matchResult) {
                        break;
                    }
                } else {
                    Object valueFromObj = getFieldValue(eventData, field);
                    if (valueFromObj != null) {
                        matchResult = evaluateComparison(
                                searchQuery.getString(IterableConstants.COMPARATOR_TYPE),
                                valueFromObj,
                                searchQuery.has(IterableConstants.VALUES) ?
                                        searchQuery.getJSONArray(IterableConstants.VALUES) :
                                        searchQuery.getString(IterableConstants.VALUE)
                        );
                        if (matchResult) {
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                return matchResult;
            }
            if (isKeyExists) {
                if (evaluateComparison(searchQuery.getString(IterableConstants.COMPARATOR_TYPE),
                        eventData.get(field),
                        searchQuery.has(IterableConstants.VALUES) ?
                                searchQuery.getJSONArray(IterableConstants.VALUES) :
                                searchQuery.getString(IterableConstants.VALUE))) {
                    matchResult = true;
                    continue;
                }
            }
            matchResult = false;
            break;
        }
        return matchResult;
    }

    private Object getFieldValue(JSONObject data, String field) {
        String[] fields = field.split("\\.");
        try {
            String eventType = data.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE);

            if (eventType.equals(IterableConstants.TRACK_EVENT)) {
                String eventName = data.getString(IterableConstants.KEY_EVENT_NAME);
                if (fields[0].equals(eventName)) {
                    fields = new String[]{fields[fields.length - 1]};
                }
            }

            JSONObject value = data;
            Object fieldValue = null;

            for (String currentField : fields) {
                if (value.has(currentField)) {
                    Object dataValue = value.get(currentField);
                    if (dataValue instanceof JSONObject) {
                        value = value.getJSONObject(currentField);
                    } else  {
                        fieldValue = value.get(currentField);
                    }
                }
            }
            return fieldValue;
        } catch (JSONException e) {
            return null;
        }
    }

    private boolean doesItemCriteriaExists(JSONArray searchQueries) throws JSONException {
        for (int i = 0; i < searchQueries.length(); i++) {
            String field = searchQueries.getJSONObject(i).getString(IterableConstants.FIELD);
            if (isRelevantField(field)) {
                return true;
            }
        }
        return false;
    }
    private boolean doesItemMatchQueries(JSONArray searchQueries, JSONObject item) throws JSONException {
        JSONArray filterSearchQueries = getFilteredSearchQueries(searchQueries, item);

        //if there are no valid queries found after filtering
        if (filterSearchQueries.length() == 0) {
            return false;
        }

        //evaluate all relevant queries
        return evaluateRelevantQueries(filterSearchQueries, item);
    }

    private JSONArray getFilteredSearchQueries(JSONArray searchQueries, JSONObject item) throws JSONException {
        JSONArray filteredQueries = new JSONArray();

        for (int i = 0; i < searchQueries.length(); i++) {
            JSONObject searchQuery = searchQueries.getJSONObject(i);
            String field = searchQuery.getString(IterableConstants.FIELD);

            if (isRelevantField(field)) {
                if (!item.has(field)) {
                    return new JSONArray();
                }
                filteredQueries.put(searchQuery);
            }
        }

        return filteredQueries;
    }

    private boolean isRelevantField(String field) {
        return field.startsWith(IterableConstants.UPDATECART_ITEM_PREFIX) ||
                field.startsWith(IterableConstants.PURCHASE_ITEM_PREFIX);
    }

    private boolean evaluateRelevantQueries(JSONArray relevantSearchQueries, JSONObject item) throws JSONException {
        for (int j = 0; j < relevantSearchQueries.length(); j++) {
            JSONObject query = relevantSearchQueries.getJSONObject(j);
            String field = query.getString(IterableConstants.FIELD);

            if (item.has(field) && !evaluateSingleQuery(query, item.get(field))) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluateSingleQuery(JSONObject query, Object itemValue) throws JSONException {
        String comparatorType = query.getString(IterableConstants.COMPARATOR_TYPE);
        Object comparisonValue = query.has(IterableConstants.VALUES) ? query.getJSONArray(IterableConstants.VALUES) : query.getString(IterableConstants.VALUE);

        return evaluateComparison(comparatorType, itemValue, comparisonValue);
    }

    public static String formattedDoubleValue(double d) {
        if (d == (long) d)
            return String.format("%d", (long) d);
        else
            return String.format("%s", d);
    }

    private boolean evaluateComparison(String comparatorType, Object matchObj, Object valueToCompare) throws JSONException {
        if (valueToCompare == null && !comparatorType.equals(MatchComparator.IS_SET)) {
            return false;
        }

        if (valueToCompare instanceof String && isDouble((String) valueToCompare)) {
            // here do the conversion of this number to formatted double value by removing trailing zeros
            // because when jsonstring to jsonarray happens for items object it removes trailing zeros
            valueToCompare = formattedDoubleValue(Double.parseDouble((String) valueToCompare));
        }

        switch (comparatorType) {
            case MatchComparator.EQUALS:
                return compareValueEquality(matchObj, valueToCompare);
            case MatchComparator.DOES_NOT_EQUALS:
                return !compareValueEquality(matchObj, valueToCompare);
            case MatchComparator.IS_SET:
                return issetCheck(matchObj);
            case MatchComparator.GREATER_THAN:
                return compareNumericValues(matchObj, String.valueOf(valueToCompare), " > ");
            case MatchComparator.LESS_THAN:
                return compareNumericValues(matchObj, String.valueOf(valueToCompare), " < ");
            case MatchComparator.GREATER_THAN_OR_EQUAL_TO:
                return compareNumericValues(matchObj, String.valueOf(valueToCompare), " >= ");
            case MatchComparator.LESS_THAN_OR_EQUAL_TO:
                return compareNumericValues(matchObj, String.valueOf(valueToCompare), " <= ");
            case MatchComparator.CONTAINS:
                return compareContains(matchObj, String.valueOf(valueToCompare));
            case MatchComparator.STARTS_WITH:
                return compareStartsWith(matchObj, String.valueOf(valueToCompare));
            case MatchComparator.MATCHES_REGEX:
                return compareWithRegex(matchObj, String.valueOf(valueToCompare));
            default:
                return false;
        }
    }

    private boolean issetCheck(Object matchObj) {
        if (matchObj instanceof Object[]) {
            return ((Object[]) matchObj).length > 0;
        } else if (matchObj instanceof Map) {
            return !((Map<?, ?>) matchObj).isEmpty();
        } else {
            return matchObj != null && !matchObj.equals("");
        }
    }

    private boolean compareValueEquality(Object sourceTo, Object stringValue) throws JSONException {
        if (sourceTo instanceof JSONArray) {
            JSONArray jsonArraySourceTo = (JSONArray) sourceTo;
            boolean matchResult = false;
            for (int i = 0; i < jsonArraySourceTo.length(); i++) {
                if (compareValueEquality(jsonArraySourceTo.get(i), stringValue)) {
                    matchResult = true;
                    break;
                }
            }
            return matchResult;
        } else if (stringValue instanceof JSONArray) {
            JSONArray jsonArrayStringValue = (JSONArray) stringValue;
            boolean matchResult = false;
            for (int i = 0; i < jsonArrayStringValue.length(); i++) {
                if (compareValueEquality(sourceTo, jsonArrayStringValue.get(i))) {
                    matchResult = true;
                    break;
                }
            }
            return matchResult;
        } else if (sourceTo instanceof Double && isDouble((String) stringValue)) {
            return sourceTo.equals(Double.parseDouble((String) stringValue));
        } else if (sourceTo instanceof Integer && isInteger((String) stringValue)) {
            return sourceTo.equals(Integer.parseInt((String) stringValue));
        } else if (sourceTo instanceof Long && isLong((String) stringValue)) {
            return sourceTo.equals(Long.parseLong((String) stringValue));
        } else if (sourceTo instanceof Boolean && isBoolean((String) stringValue)) {
            return sourceTo.equals(Boolean.parseBoolean((String) stringValue));
        } else if (sourceTo instanceof String) {
            return sourceTo.equals(stringValue);
        }
        return false;
    }

    private boolean compareArrayNumericValue(Object sourceTo, String stringValue, String compareOperator) throws JSONException {
        JSONArray jsonArraySourceTo = (JSONArray) sourceTo;
        boolean isMatched = false;
        for (int i = 0; i < jsonArraySourceTo.length(); i++) {
            if (compareNumericValues(jsonArraySourceTo.get(i), stringValue, compareOperator)) {
                isMatched = true;
            }
        }
        return isMatched;
    }

    private boolean compareNumericValues(Object sourceTo, String stringValue, String compareOperator) throws JSONException {
        if (sourceTo instanceof JSONArray) {
            return compareArrayNumericValue(sourceTo, stringValue, compareOperator);
        } else if (isDouble(stringValue)) {
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

    private boolean compareContains(Object sourceTo, String stringValue) throws JSONException {
        if (sourceTo instanceof JSONArray) {
            JSONArray jsonArraySourceTo = (JSONArray) sourceTo;
            Object element = jsonArraySourceTo.get(0);
            if (element instanceof String) {
                ArrayList<String> stringArrayList = new ArrayList<>();
                for (int i = 0; i < jsonArraySourceTo.length(); i++) {
                    stringArrayList.add(jsonArraySourceTo.getString(i));
                }
                return stringArrayList.contains(stringValue);
            }
        } else if (sourceTo instanceof String) {
            String stringSourceTo = (String) sourceTo;
            return stringSourceTo.contains(stringValue);
        }
        return false;
    }

    private boolean compareStartsWith(Object sourceTo, String stringValue) throws JSONException {
        if (sourceTo instanceof JSONArray) {
            JSONArray jsonArraySourceTo = (JSONArray) sourceTo;
            Object element = jsonArraySourceTo.get(0);
            if (element instanceof String) {
                boolean isMatched = false;
                for (int i = 0; i < jsonArraySourceTo.length(); i++) {
                    if (jsonArraySourceTo.getString(i).startsWith(stringValue)) {
                        isMatched = true;
                    }
                }
                return isMatched;
            }
        } else if (sourceTo instanceof String) {
            return ((String) sourceTo).startsWith(stringValue);
        }
        return false;
    }

    private boolean compareWithRegex(Object sourceTo, String pattern) throws JSONException {
        if (sourceTo instanceof JSONArray) {
            JSONArray jsonArraySourceTo = (JSONArray) sourceTo;
            Object element = jsonArraySourceTo.get(0);
            if (element instanceof String) {
                boolean isMatched = false;
                Pattern regexPattern = Pattern.compile(pattern);
                for (int i = 0; i < jsonArraySourceTo.length(); i++) {
                    if (regexPattern.matcher(jsonArraySourceTo.getString(i)).matches()) {
                        isMatched = true;
                    }
                }
                return isMatched;
            }
        } else if (sourceTo instanceof String) {
            try {
                Pattern regexPattern = Pattern.compile(pattern);
                return regexPattern.matcher((String) sourceTo).matches();
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
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