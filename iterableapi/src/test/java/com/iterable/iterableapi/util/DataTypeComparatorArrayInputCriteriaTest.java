package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DataTypeComparatorArrayInputCriteriaTest {

    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    static final String mockDataArrayDataTypeWithEquals = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_Equals\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"milestoneYears\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"1997\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"score\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": 11.5,\n" +
            "                                            \"fieldType\": \"double\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"timestamp\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": 1722500215276,\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testEqualsArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1996, 1997, 2002, 2020, 2024],\n" +
                "            \"score\": [10.5, 11.5, 12.5, 13.5, 14.5],\n" +
                "            \"timestamp\": [1722497422151, 1722500235276, 1722500215276, 1722500225276,\n" +
                "                1722500245276]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithEquals, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testEqualsArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1996, 1998, 2002, 2020, 2024],\n" +
                "            \"score\": [10.5, 11.5, 12.5, 13.5, 14.5],\n" +
                "            \"timestamp\": [1722497422151, 1722500235276, 1722500215276, 1722500225276,\n" +
                "                1722500245276]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithEquals, jsonArray);
        assertNull(result);
    }

    static final String mockDataArrayDataTypeWithDoesNotEqual = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_DoesNotEqual\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"milestoneYears\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"value\": \"1997\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"score\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"value\": 11.5,\n" +
            "                                            \"fieldType\": \"double\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"timestamp\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"value\": 1722500215276,\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testDoesNotEqualArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1996, 1998, 2002, 2020, 2024],\n" +
                "            \"score\": [10.5, 8.5, 12.5, 13.5, 14.5],\n" +
                "            \"timestamp\": [1722497422151, 1722500235276, 1722500215275, 1722500225276,\n" +
                "                1722500245276]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithDoesNotEqual, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testDoesNotEqualArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1996, 1998, 2002, 2020, 2024],\n" +
                "            \"score\": [10.5, 11.5, 12.5, 13.5, 14.5],\n" +
                "            \"timestamp\": [1722497422151, 1722500235276, 1722500215276, 1722500225276,\n" +
                "                1722500245276]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithDoesNotEqual, jsonArray);
        assertNull(result);
    }

    static final String mockDataArrayDataTypeWithGreaterThan = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_GreaterThan\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"milestoneYears\",\n" +
            "                                            \"comparatorType\": \"GreaterThan\",\n" +
            "                                            \"value\": 1997,\n" +
            "                                            \"fieldType\": \"double\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testGreaterThanArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1996, 1998, 2002, 2020, 2024]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithGreaterThan, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testGreaterThanArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1990, 1992, 1994, 1997]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithGreaterThan, jsonArray);
        assertNull(result);
    }

    static final String mockDataArrayDataTypeWithGreaterThanOrEqualTo = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_GreaterThanOrEqualTo\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"milestoneYears\",\n" +
            "                                            \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                                            \"value\": 1997,\n" +
            "                                            \"fieldType\": \"double\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testGreaterThanOrEqualArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1997, 1998, 2002, 2020, 2024]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithGreaterThanOrEqualTo, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testGreaterThanOrEqualArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1990, 1992, 1994, 1996]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithGreaterThanOrEqualTo, jsonArray);
        assertNull(result);
    }

    static final String mockDataArrayDataTypeWithLessThan = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_LessThan\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"milestoneYears\",\n" +
            "                                            \"comparatorType\": \"LessThan\",\n" +
            "                                            \"value\": 1997,\n" +
            "                                            \"fieldType\": \"double\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testLessThanArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1990, 1992, 1994, 1996, 1998]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithLessThan, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testLessThanArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1997, 1999, 2002, 2004]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithLessThan, jsonArray);
        assertNull(result);
    }

    static final String mockDataArrayDataTypeWithLessThanOrEqualTo = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_LessThanOrEqualTo\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"milestoneYears\",\n" +
            "                                            \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                                            \"value\": 1997,\n" +
            "                                            \"fieldType\": \"double\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testLessThanOrEqualsToArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1990, 1992, 1994, 1996, 1998]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithLessThanOrEqualTo, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testLessThanOrEqualsToArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"milestoneYears\": [1998, 1999, 2002, 2004]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithLessThanOrEqualTo, jsonArray);
        assertNull(result);
    }

    static final String mockDataArrayDataTypeWithContains = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_Contains\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"country\",\n" +
            "                                            \"comparatorType\": \"Contains\",\n" +
            "                                            \"value\": \"UK\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testContainArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": [\"US\", \"UK\", \"China\", \"Europe\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithContains, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testContainArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": [\"US\", \"Canada\", \"China\", \"Europe\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithContains, jsonArray);
        assertNull(result);
    }

    static final String mockDataArrayDataTypeWithMatchesRegex = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_MatchesRegex\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"country\",\n" +
            "                                            \"comparatorType\": \"MatchesRegex\",\n" +
            "                                            \"value\": \"^T.*iwa.*n$\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testMatchesRegexArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": [\"US\", \"Taiwan\", \"China\", \"Europe\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithMatchesRegex, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testMatchesRegexArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": [\"US\", \"Thailand\", \"China\", \"Europe\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithMatchesRegex, jsonArray);
        assertNull(result);
    }


    static final String mockDataArrayDataTypeWithStartsWith = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_StartsWith\",\n" +
            "            \"createdAt\": 1722497422151,\n" +
            "            \"updatedAt\": 1722500235276,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"country\",\n" +
            "                                            \"comparatorType\": \"StartsWith\",\n" +
            "                                            \"value\": \"T\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @Test
    public void testStartsWithArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": [\"US\", \"Taiwan\", \"China\", \"Europe\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithStartsWith, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testStartsWithArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": [\"US\", \"Canada\", \"China\", \"Europe\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayDataTypeWithStartsWith, jsonArray);
        assertNull(result);
    }

    static final String mockDataStringArrayMixCriteriaArea = " {\n" +
            "         \"count\": 1,\n" +
            "         \"criteriaSets\": [\n" +
            "           {\n" +
            "               \"criteriaId\": \"382\",\n" +
            "               \"name\": \"comparison_for_Array_data_types_or\",\n" +
            "               \"createdAt\": 1724315593795,\n" +
            "               \"updatedAt\": 1724315593795,\n" +
            "               \"searchQuery\": {\n" +
            "                   \"combinator\": \"And\",\n" +
            "                   \"searchQueries\": [\n" +
            "                       {\n" +
            "                           \"combinator\": \"Or\",\n" +
            "                           \"searchQueries\": [\n" +
            "                               {\n" +
            "                                   \"dataType\": \"user\",\n" +
            "                                   \"searchCombo\": {\n" +
            "                                       \"combinator\": \"And\",\n" +
            "                                       \"searchQueries\": [\n" +
            "                                           {\n" +
            "                                               \"dataType\": \"user\",\n" +
            "                                               \"field\": \"milestoneYears\",\n" +
            "                                               \"comparatorType\": \"GreaterThan\",\n" +
            "                                               \"value\": \"1997\",\n" +
            "                                               \"fieldType\": \"long\"\n" +
            "                                           }\n" +
            "                                       ]\n" +
            "                                   }\n" +
            "                               },\n" +
            "                               {\n" +
            "                                   \"dataType\": \"customEvent\",\n" +
            "                                   \"searchCombo\": {\n" +
            "                                       \"combinator\": \"And\",\n" +
            "                                       \"searchQueries\": [\n" +
            "                                           {\n" +
            "                                               \"dataType\": \"customEvent\",\n" +
            "                                               \"field\": \"button-clicked.animal\",\n" +
            "                                               \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                               \"value\": \"giraffe\",\n" +
            "                                               \"fieldType\": \"string\"\n" +
            "                                           }\n" +
            "                                       ]\n" +
            "                                   }\n" +
            "                               },\n" +
            "                               {\n" +
            "                                   \"dataType\": \"purchase\",\n" +
            "                                   \"searchCombo\": {\n" +
            "                                       \"combinator\": \"And\",\n" +
            "                                       \"searchQueries\": [\n" +
            "                                           {\n" +
            "                                               \"dataType\": \"purchase\",\n" +
            "                                               \"field\": \"total\",\n" +
            "                                               \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                                               \"value\": \"200\",\n" +
            "                                               \"fieldType\": \"double\"\n" +
            "                                           }\n" +
            "                                       ]\n" +
            "                                   }\n" +
            "                               }\n" +
            "                           ]\n" +
            "                       }\n" +
            "                   ]\n" +
            "               }\n" +
            "           }\n" +
            "         ]\n" +
            "       }\n";

    @Test
    public void testMixCriteriaAreaArrayMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "       {\n" +
                "        \"dataFields\": {     \n" +
                "                \"createdAt\": 1699246745093,\n" +
                "                \"milestoneYears\": [1998, 1999, 2002, 2004]\n" +
                "            },\n" +
                "         \"eventType\": \"user\"\n" +
                "        },\n" +
                "        {\n" +
                "        \"dataFields\": {\n" +
                "                \"button-clicked.animal\": [\"cow\", \"horse\"]\n" +
                "            },        \n" +
                "        \"eventType\": \"customEvent\"\n" +
                "        },\n" +
                "        {\n" +
                "        \"dataFields\": {\n" +
                "                \"total\": [199.99, 210.0, 220.20, 250.10]\n" +
                "            },\n" +
                "        \"eventType\": \"purchase\"\n" +
                "        }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataStringArrayMixCriteriaArea, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testMixCriteriaAreaArrayMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "       {\n" +
                "        \"dataFields\": {     \n" +
                "                \"createdAt\": 1699246745093,\n" +
                "                \"milestoneYears\": [1990, 1992, 1996, 1997]\n" +
                "            },\n" +
                "         \"eventType\": \"user\"\n" +
                "        },\n" +
                "        {\n" +
                "        \"dataFields\": {\n" +
                "                \"button-clicked.animal\": [\"cow\", \"horse\", \"giraffe\"]\n" +
                "            },        \n" +
                "        \"eventType\": \"customEvent\"\n" +
                "        },\n" +
                "        {\n" +
                "        \"dataFields\": {\n" +
                "                \"total\": [210.0, 220.20, 250.10]\n" +
                "            },\n" +
                "        \"eventType\": \"purchase\"\n" +
                "        }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataStringArrayMixCriteriaArea, jsonArray);
        assertNull(result);
    }
}
