package com.iterable.iterableapi.util;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class LogicalExpressionEvaluator {

    private ArrayList<String> localDataKeys;

    public boolean compareData(JSONObject node, JSONObject eventItem, JSONObject localEventData) {
        try {
            localDataKeys = new ArrayList<>();

            Iterator<String> keys = localEventData.keys();
            while (keys.hasNext()) {
                localDataKeys.add(keys.next());
            }
            keys = eventItem.keys();
            while (keys.hasNext()) {
                localDataKeys.add(keys.next());
            }
            return evaluateTree(node, eventItem, localEventData.getString(IterableConstants.SHARED_PREFS_TRACKING_TYPE));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
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
                String dataType = node.getString("dataType");
                if (!dataType.equals(trackingType)) {
                    return false;
                }
                String comparatorType = node.getString("comparatorType");
                String field = node.getString("field");
                String fieldType = node.getString("fieldType");
                Object matchedCountObj = null;
                boolean isCriteriaMatch = false;

                for (int i = 0; i < localDataKeys.size(); i++) {
                    if (field.endsWith(localDataKeys.get(i))) {
                        matchedCountObj = localEventData.get(localDataKeys.get(i));
                    }
                }

                if (matchedCountObj == null) {
                    return false;
                }

                double matchedCount = 0.0;

                if (fieldType.equals("string") && matchedCountObj instanceof String) {
                    isCriteriaMatch = matchedCountObj.equals(node.getString("value"));
                } else if (matchedCountObj instanceof Integer) {
                    matchedCount = (double) (int) matchedCountObj;
                } else if (matchedCountObj instanceof Long) {
                    matchedCount = (double) (long) matchedCountObj;
                } else if (matchedCountObj instanceof Double) {
                    matchedCount = (double) matchedCountObj;
                }

                if (comparatorType.equals(ComparatorType.Equals.toString())) {
                    double valueToMatch = node.getDouble("value");
                    if (matchedCount == valueToMatch) {
                        isCriteriaMatch = true;
                    }
                } else if (comparatorType.equals(ComparatorType.GreaterThan.toString())) {
                    double valueToMatch = node.getDouble("value");
                    if (matchedCount > valueToMatch) {
                        isCriteriaMatch = true;
                    }
                } else if (comparatorType.equals(ComparatorType.LessThan.toString())) {
                    double valueToMatch = node.getDouble("value");
                    if (matchedCount < valueToMatch) {
                        isCriteriaMatch = true;
                    }
                } else if (comparatorType.equals(ComparatorType.GreaterThanOrEqualTo.toString())) {
                    double valueToMatch = node.getDouble("value");
                    if (matchedCount >= valueToMatch) {
                        isCriteriaMatch = true;
                    }
                } else if (comparatorType.equals(ComparatorType.LessThanOrEqualTo.toString())) {
                    double valueToMatch = node.getDouble("value");
                    if (matchedCount <= valueToMatch) {
                        isCriteriaMatch = true;
                    }
                }

                return isCriteriaMatch;
            }
        } catch (Exception e) {
            IterableLogger.e("Exception", e.toString());
            e.printStackTrace();
        }
        return false;
    }
}

enum ComparatorType {
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
