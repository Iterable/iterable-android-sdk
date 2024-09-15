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

    //
    // field logic evaluation
    //

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

            // check if the field is a track event object
            if (isTrackEventObject(searchQuery, eventData, field)) {
                continue;
            }

            //evaluate filtered keys first
            if (isFieldInFilteredKeys(field, filteredDataKeys)) {
                return evaluateComparisonForKey(searchQuery, eventData.get(field));
            }

            if (field.contains(".")) {
                return evaluateNestedField(eventData, field, searchQuery, searchQueries);
            }

            return false;
        }
        return false;
    }

    private boolean isTrackEventObject(JSONObject searchQuery, JSONObject eventData, String field) throws JSONException {
        if (searchQuery.getString(IterableConstants.DATA_TYPE).equals(IterableConstants.TRACK_EVENT) &&
                searchQuery.getString("fieldType").equals("object") &&
                searchQuery.getString(IterableConstants.COMPARATOR_TYPE).equals(MatchComparator.IS_SET)) {
            String eventName = eventData.getString(IterableConstants.KEY_EVENT_NAME);
            return field.equals(eventName);
        }
        return false;
    }

    private boolean isFieldInFilteredKeys(String field, ArrayList<String> filteredDataKeys) {
        return filteredDataKeys.contains(field);
    }

    private boolean evaluateComparisonForKey(JSONObject searchQuery, Object eventFieldValue) throws JSONException {
        return evaluateComparison(
                searchQuery.getString(IterableConstants.COMPARATOR_TYPE),
                eventFieldValue,
                searchQuery.has(IterableConstants.VALUES) ?
                        searchQuery.getJSONArray(IterableConstants.VALUES) :
                        searchQuery.getString(IterableConstants.VALUE)
        );
    }

    private boolean evaluateNestedField(JSONObject eventData, String field, JSONObject searchQuery, JSONArray searchQueries) throws JSONException {
        String[] splitString = field.split("\\.");
        String firstElement = splitString[0];
        Object firstElementValue = eventData.opt(firstElement);

        if (firstElementValue instanceof JSONArray) {
            return evaluateJsonArrayForNestedField((JSONArray) firstElementValue, firstElement, eventData, searchQueries);
        } else {
            Object fieldValue = getFieldValue(eventData, field);
            return fieldValue != null && evaluateComparisonForKey(searchQuery, fieldValue);
        }
    }

    private boolean evaluateJsonArrayForNestedField(JSONArray jsonArray, String firstElement, JSONObject eventData, JSONArray searchQueries) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = createJsonObjectForArrayEvaluation(jsonArray, i, firstElement, eventData);
            if (evaluateFieldLogic(searchQueries, jsonObject)) {
                return true;
            }
        }
        return false;
    }

    private JSONObject createJsonObjectForArrayEvaluation(JSONArray jsonArray, int index, String firstElement, JSONObject eventData) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(firstElement, jsonArray.get(index));
        jsonObject.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, eventData.get(IterableConstants.SHARED_PREFS_EVENT_TYPE));
        return jsonObject;
    }

    private Object getFieldValue(JSONObject data, String field) {
        try {
            String[] fields = getFieldsArray(data, field);
            return retrieveFieldValue(data, fields);
        } catch (JSONException e) {
            return null;
        }
    }

    private String[] getFieldsArray(JSONObject data, String field) throws JSONException {
        String[] fields = field.split("\\.");
        String eventType = data.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE);

        // If the event is a track event, and the first field is the event name, shorten the fields array
        if (eventType.equals(IterableConstants.TRACK_EVENT)) {
            String eventName = data.getString(IterableConstants.KEY_EVENT_NAME);
            if (fields[0].equals(eventName)) {
                fields = new String[]{fields[fields.length - 1]};
            }
        }

        return fields;
    }

    private Object retrieveFieldValue(JSONObject data, String[] fields) throws JSONException {
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

    //
    // comparison functions
    //

    private boolean evaluateComparison(String comparatorType, Object matchObj, Object valueToCompare) throws JSONException {
        if (valueToCompare == null && !comparatorType.equals(MatchComparator.IS_SET)) {
            return false;
        }

        valueToCompare = formatValueToCompare(valueToCompare);

        switch (comparatorType) {
            case MatchComparator.EQUALS:
                return compareValueEquality(matchObj, valueToCompare);
            case MatchComparator.DOES_NOT_EQUALS:
                return !compareValueEquality(matchObj, valueToCompare);
            case MatchComparator.IS_SET:
                return issetCheck(matchObj);
            case MatchComparator.GREATER_THAN:
            case MatchComparator.LESS_THAN:
            case MatchComparator.GREATER_THAN_OR_EQUAL_TO:
            case MatchComparator.LESS_THAN_OR_EQUAL_TO:
                return compareNumeric(matchObj, valueToCompare, comparatorType);
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

    private Object formatValueToCompare(Object valueToCompare) {
        if (valueToCompare instanceof String && isDouble((String) valueToCompare)) {
            return formattedDoubleValue(Double.parseDouble((String) valueToCompare));
        }
        return valueToCompare;
    }

    private boolean compareNumeric(Object matchObj, Object valueToCompare, String comparatorType) throws JSONException {
        String comparisonOperator = getComparisonOperator(comparatorType);
        return compareNumericValues(matchObj, String.valueOf(valueToCompare), comparisonOperator);
    }

    private String getComparisonOperator(String comparatorType) {
        switch (comparatorType) {
            case MatchComparator.GREATER_THAN:
                return " > ";
            case MatchComparator.LESS_THAN:
                return " < ";
            case MatchComparator.GREATER_THAN_OR_EQUAL_TO:
                return " >= ";
            case MatchComparator.LESS_THAN_OR_EQUAL_TO:
                return " <= ";
            default:
                throw new IllegalArgumentException("Invalid comparator type: " + comparatorType);
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
            return compareWithJSONArray((JSONArray) sourceTo, stringValue);
        } else if (stringValue instanceof JSONArray) {
            return compareWithJSONArray((JSONArray) stringValue, sourceTo);
        } else if (sourceTo instanceof String || stringValue instanceof String) {
            return compareWithParsedValues(sourceTo, stringValue);
        } else {
            return sourceTo.equals(stringValue);
        }
    }

    private boolean compareWithJSONArray(JSONArray jsonArray, Object valueToCompare) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (compareValueEquality(jsonArray.get(i), valueToCompare)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareWithParsedValues(Object sourceTo, Object stringValue) {
        String stringVal = stringValue.toString();

        if (sourceTo instanceof Double && isDouble(stringVal)) {
            return sourceTo.equals(Double.parseDouble(stringVal));
        } else if (sourceTo instanceof Integer && isInteger(stringVal)) {
            return sourceTo.equals(Integer.parseInt(stringVal));
        } else if (sourceTo instanceof Long && isLong(stringVal)) {
            return sourceTo.equals(Long.parseLong(stringVal));
        } else if (sourceTo instanceof Boolean && isBoolean(stringVal)) {
            return sourceTo.equals(Boolean.parseBoolean(stringVal));
        } else {
            return sourceTo.equals(stringValue);
        }
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

            if (jsonArraySourceTo.get(0) instanceof String) {
                // check if any string in the array contains the string
                return arrayContains(jsonArraySourceTo, stringValue);
            }
        } else if (sourceTo instanceof String) {
            return ((String) sourceTo).contains(stringValue);
        }
        return false;
    }

    private boolean arrayContains(JSONArray jsonArray, String stringValue) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getString(i).contains(stringValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareStartsWith(Object sourceTo, String stringValue) throws JSONException {
        if (sourceTo instanceof JSONArray) {
            JSONArray jsonArraySourceTo = (JSONArray) sourceTo;

            if (jsonArraySourceTo.get(0) instanceof String) {
                // check if any string in the array starts with string
                return anyStartsWith(jsonArraySourceTo, stringValue);
            }
        } else if (sourceTo instanceof String) {
            return ((String) sourceTo).startsWith(stringValue);
        }

        return false;
    }

    private boolean anyStartsWith(JSONArray jsonArray, String stringValue) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getString(i).startsWith(stringValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareWithRegex(Object sourceTo, String pattern) throws JSONException {
        try {
            Pattern regexPattern = Pattern.compile(pattern);

            // If the source is a JSONArray
            if (sourceTo instanceof JSONArray) {
                JSONArray jsonArraySourceTo = (JSONArray) sourceTo;

                if (jsonArraySourceTo.get(0) instanceof String) {
                    // check if any string in the array matches the regex
                    return anyMatchesRegex(jsonArraySourceTo, regexPattern);
                }
            } else if (sourceTo instanceof String) {
                return regexPattern.matcher((String) sourceTo).matches();
            }

        } catch (PatternSyntaxException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean anyMatchesRegex(JSONArray jsonArray, Pattern regexPattern) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (regexPattern.matcher(jsonArray.getString(i)).matches()) {
                return true;
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

    private void handleException(Exception e) {
        IterableLogger.e("Exception occurred", e.toString());
        e.printStackTrace();
    }

    private void handleJSONException(JSONException e) {
        IterableLogger.e("JSONException occurred", e.toString());
        e.printStackTrace();
    }
}