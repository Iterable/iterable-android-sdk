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

    //
    //event processing functions
    //
    private JSONArray prepareEventsToProcess() {
        //store purchase events in events list
        JSONArray eventsToProcess = getEventsWithCartItems();
        JSONArray nonPurchaseEvents = getNonCartEvents();

        //store non-purchase events in event list
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
                //get event data and event type
                JSONObject localEventData = localStoredEventList.getJSONObject(i);
                String eventType = localEventData.optString(IterableConstants.SHARED_PREFS_EVENT_TYPE);

                //process event based on if its a purchase or a cart update
                if (eventType.equals(IterableConstants.TRACK_PURCHASE)) {
                    JSONObject updatedItem = processPurchaseEvent(localEventData);
                    processedEvents.put(updatedItem);
                } else if (eventType.equals(IterableConstants.TRACK_UPDATE_CART)) {
                    JSONObject updatedItem = processUpdateCartEvent(localEventData);
                    processedEvents.put(updatedItem);
                }
            }
        } catch (JSONException e) {
            handleJSONException(e);
        }
        return processedEvents;
    }

    private JSONObject processPurchaseEvent(JSONObject localEventData) throws JSONException {
        JSONObject updatedItem = new JSONObject();

        //process shopping cart items
        if (localEventData.has(IterableConstants.KEY_ITEMS)) {
            JSONArray items = new JSONArray(localEventData.getString(IterableConstants.KEY_ITEMS));
            JSONArray processedItems = processItems(items, IterableConstants.PURCHASE_ITEM_PREFIX);
            updatedItem.put(IterableConstants.PURCHASE_ITEM, processedItems);
        }

        //process data fields
        addDataFields(localEventData, updatedItem);

        //process remaining fields
        addRemainingFields(localEventData, updatedItem);

        return updatedItem;
    }

    private JSONObject processUpdateCartEvent(JSONObject localEventData) throws JSONException {
        JSONObject updatedItem = new JSONObject();
        updatedItem.put(IterableConstants.KEY_EVENT_NAME, IterableConstants.UPDATE_CART);

        //process shopping cart items
        if (localEventData.has(IterableConstants.KEY_ITEMS)) {
            JSONArray items = new JSONArray(localEventData.getString(IterableConstants.KEY_ITEMS));
            JSONArray processedItems = processItems(items, IterableConstants.UPDATECART_ITEM_PREFIX);
            updatedItem.put(IterableConstants.KEY_ITEMS, processedItems);
        }

        //process data fields
        addDataFields(localEventData, updatedItem);

        //process remaining fields
        addRemainingFields(localEventData, updatedItem);

        //add event type
        updatedItem.put(IterableConstants.SHARED_PREFS_EVENT_TYPE, IterableConstants.TRACK_EVENT);

        return updatedItem;
    }

    private JSONArray processItems(JSONArray items, String prefix) throws JSONException {
        final JSONArray processedItems = new JSONArray();

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONObject processedItem = new JSONObject();
            Iterator<String> keys = item.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                processedItem.put(prefix + key, item.get(key));
            }
            processedItems.put(processedItem);
        }
        return processedItems;
    }

    private void addDataFields(JSONObject localEventData, JSONObject updatedItem) throws JSONException {
        if (localEventData.has(IterableConstants.KEY_DATA_FIELDS)) {
            JSONObject dataFields = localEventData.getJSONObject(IterableConstants.KEY_DATA_FIELDS);
            Iterator<String> fieldKeys = dataFields.keys();
            while (fieldKeys.hasNext()) {
                String key = fieldKeys.next();
                updatedItem.put(key, dataFields.get(key));
            }
        }
    }

    private void addRemainingFields(JSONObject localEventData, JSONObject updatedItem) throws JSONException {
        Iterator<String> keys = localEventData.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!key.equals(IterableConstants.KEY_ITEMS) && !key.equals(IterableConstants.KEY_DATA_FIELDS)) {
                updatedItem.put(key, localEventData.get(key));
            }
        }
    }

    private JSONArray getNonCartEvents() {
        JSONArray nonPurchaseEvents = new JSONArray();
        try {
            for (int i = 0; i < localStoredEventList.length(); i++) {
                JSONObject localEventData = localStoredEventList.getJSONObject(i);
                if (isNonCartEvent(localEventData)) {
                    JSONObject updatedItem = createUpdatedItem(localEventData);
                    nonPurchaseEvents.put(updatedItem);
                }
            }
        } catch (JSONException e) {
            handleJSONException(e);
        }
        return nonPurchaseEvents;
    }

    private boolean isNonCartEvent(JSONObject localEventData) throws JSONException {
        String eventType = localEventData.optString(IterableConstants.SHARED_PREFS_EVENT_TYPE);
        return !eventType.equals(IterableConstants.TRACK_PURCHASE) && !eventType.equals(IterableConstants.TRACK_UPDATE_CART);
    }

    private JSONObject createUpdatedItem(JSONObject localEventData) throws JSONException {
        JSONObject updatedItem = new JSONObject(localEventData.toString());
        if (localEventData.has(IterableConstants.KEY_DATA_FIELDS)) {
            JSONObject dataFields = localEventData.getJSONObject(IterableConstants.KEY_DATA_FIELDS);
            mergeDataFields(updatedItem, dataFields);
        }
        return updatedItem;
    }

    private void mergeDataFields(JSONObject updatedItem, JSONObject dataFields) throws JSONException {
        Iterator<String> fieldKeys = dataFields.keys();
        while (fieldKeys.hasNext()) {
            String key = fieldKeys.next();
            updatedItem.put(key, dataFields.get(key));
        }
    }

    //
    // evaluate tree functions
    //

    public boolean evaluateTree(JSONObject node, JSONArray localEventData) {
        try {
            if (node.has(IterableConstants.SEARCH_QUERIES)) {
                String combinator = node.getString(IterableConstants.COMBINATOR);
                JSONArray searchQueries = node.getJSONArray(IterableConstants.SEARCH_QUERIES);

                switch (combinator) {
                    case "And":
                        return evaluateAnd(searchQueries, localEventData);
                    case "Or":
                        return evaluateOr(searchQueries, localEventData);
                    case "Not":
                        return evaluateNot(searchQueries, localEventData);
                    default:
                        throw new IllegalArgumentException("Unknown combinator: " + combinator);
                }
            } else if (node.has(IterableConstants.SEARCH_COMBO)) {
                return evaluateSearchQueries(node, localEventData);
            }
        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }

    private boolean evaluateAnd(JSONArray searchQueries, JSONArray localEventData) throws JSONException {
        for (int i = 0; i < searchQueries.length(); i++) {
            if (!evaluateTree(searchQueries.getJSONObject(i), localEventData)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateOr(JSONArray searchQueries, JSONArray localEventData) throws JSONException {
        for (int i = 0; i < searchQueries.length(); i++) {
            if (evaluateTree(searchQueries.getJSONObject(i), localEventData)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateNot(JSONArray searchQueries, JSONArray localEventData) throws JSONException {
        for (int i = 0; i < searchQueries.length(); i++) {
            searchQueries.getJSONObject(i).put("isNot", true);
            if (evaluateTree(searchQueries.getJSONObject(i), localEventData)) {
                return false;
            }
        }
        return true;
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
        boolean result = evaluateFieldLogic(searchQueries, eventData);

        switch (combinator) {
            case "And":
            case "Or":
                return result;
            case "Not":
                return !result;
            default:
                return false;
        }
    }

    private boolean evaluateFieldLogic(JSONArray searchQueries, JSONObject eventData) throws JSONException {
        boolean itemMatchResult = false;
        String itemKey = null;
        if (eventData.has(IterableConstants.KEY_ITEMS)) {
            itemKey = IterableConstants.KEY_ITEMS;
        } else if (eventData.has(IterableConstants.PURCHASE_ITEM)) {
            itemKey = IterableConstants.PURCHASE_ITEM;
        }

        if (itemKey != null) {
            boolean result = false;
            JSONArray items = new JSONArray(eventData.getString(itemKey));
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
            if (isTrackEvent(data)) {
                fields = adjustFieldsForTrackEvent(data, fields);
            }
            return extractFieldValue(data, fields);
        } catch (JSONException e) {
            return null;
        }
    }

    private boolean isTrackEvent(JSONObject data) throws JSONException {
        return data.has(IterableConstants.SHARED_PREFS_EVENT_TYPE) &&
                IterableConstants.TRACK_EVENT.equals(data.getString(IterableConstants.SHARED_PREFS_EVENT_TYPE));
    }

    private String[] adjustFieldsForTrackEvent(JSONObject data, String[] fields) throws JSONException {
        String eventName = data.getString(IterableConstants.KEY_EVENT_NAME);
        if (fields.length > 0 && fields[0].equals(eventName)) {
            return new String[]{fields[fields.length - 1]};
        }
        return fields;
    }

    private Object extractFieldValue(JSONObject data, String[] fields) throws JSONException {
        JSONObject value = data;
        Object fieldValue = null;

        for (String currentField : fields) {
            if (value.has(currentField)) {
                Object dataValue = value.get(currentField);

                if (dataValue instanceof JSONObject) {
                    value = value.getJSONObject(currentField);
                } else  {
                    fieldValue = value.get(currentField);
                    break;
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