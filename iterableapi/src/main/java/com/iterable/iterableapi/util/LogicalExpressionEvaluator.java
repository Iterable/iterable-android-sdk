package com.iterable.iterableapi.util;

import androidx.annotation.NonNull;

import com.iterable.iterableapi.CommerceItem;
import com.iterable.iterableapi.IterableLogger;

import org.json.JSONArray;
import org.json.JSONObject;

public class LogicalExpressionEvaluator {

    public boolean evaluateTree(JSONObject node, CommerceItem localEventData) {
        try {
            if (node.has("searchQuery")) {
                JSONObject searchQuery = node.getJSONObject("searchQuery");
                String combinator = searchQuery.getString("combinator");

                if (searchQuery.has("searchQueries")) {
                    JSONArray searchQueries = searchQuery.getJSONArray("searchQueries");
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
                }
            } else if (node.has("searchCombo")) {
                JSONObject searchCombo = node.getJSONObject("searchCombo");
                return evaluateTree(searchCombo, localEventData);
            } else if (node.has("field")) {

                String comparatorType = node.getString("comparatorType");
                String field = node.getString("field");
                String fieldType = node.getString("fieldType");
                double valueToMatch = node.getDouble("value");
                double matchedCount = 0;
                boolean isCriteriaMatch = false;

                if (field.endsWith("price")) {
                    matchedCount = localEventData.price;
                } else if (field.endsWith("quantity")) {
                    matchedCount = localEventData.quantity;
                }

                if (comparatorType.equals(ComparatorType.Equals.toString())) {
                    if (fieldType.equals("string")) {

                    } else {
                        if (matchedCount == valueToMatch) {
                            isCriteriaMatch = true;
                        }
                    }

                } else if (comparatorType.equals(ComparatorType.GreaterThan.toString())) {
                    if (matchedCount > valueToMatch) {
                        isCriteriaMatch = true;
                    }
                } else if (comparatorType.equals(ComparatorType.LessThan.toString())) {
                    if (matchedCount < valueToMatch) {
                        isCriteriaMatch = true;
                    }
                } else if (comparatorType.equals(ComparatorType.GreaterThanOrEqualTo.toString())) {
                    if (matchedCount >= valueToMatch) {
                        isCriteriaMatch = true;
                    }
                } else if (comparatorType.equals(ComparatorType.LessThanOrEqualTo.toString())) {
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
        return true;
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
