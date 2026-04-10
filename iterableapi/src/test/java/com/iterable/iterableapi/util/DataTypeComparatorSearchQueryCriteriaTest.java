package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DataTypeComparatorSearchQueryCriteriaTest {

    static final String mockDataEquals = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"285\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_Equals\",\n" +
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
            "                                            \"field\": \"eventTimeStamp\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"3\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"savings\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"19.99\",\n" +
            "                                            \"fieldType\": \"double\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"likes_boba\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"true\",\n" +
            "                                            \"fieldType\": \"boolean\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"country\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"China\",\n" +
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

    static final String mockDataDoesNotEqual = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"286\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_DoesNotEqual\",\n" +
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
            "                                            \"field\": \"eventTimeStamp\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"value\": \"101\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"savings\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"value\": \"101\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"likes_boba\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"value\": \"false\",\n" +
            "                                            \"fieldType\": \"boolean\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"country\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"value\": \"China\",\n" +
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

    static final String mockDataGreaterThan = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"287\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_GreaterThan\",\n" +
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
            "                                            \"field\": \"eventTimeStamp\",\n" +
            "                                            \"comparatorType\": \"GreaterThan\",\n" +
            "                                            \"value\": \"7\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"savings\",\n" +
            "                                            \"comparatorType\": \"GreaterThan\",\n" +
            "                                            \"value\": \"10\",\n" +
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

    static final String mockDataGreaterThanOrEqualTo = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"288\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_GreaterThanOrEqualTo\",\n" +
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
            "                                            \"field\": \"eventTimeStamp\",\n" +
            "                                            \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                                            \"value\": \"12\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"savings\",\n" +
            "                                            \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                                            \"value\": \"20\",\n" +
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

    static final String mockDataLessThan = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"289\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_LessThan\",\n" +
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
            "                                            \"field\": \"eventTimeStamp\",\n" +
            "                                            \"comparatorType\": \"LessThan\",\n" +
            "                                            \"value\": \"6\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"savings\",\n" +
            "                                            \"comparatorType\": \"LessThan\",\n" +
            "                                            \"value\": \"10\",\n" +
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

    static final String mockDataLessThanOrEqualTo = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"290\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_LessThanOrEqualTo\",\n" +
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
            "                                            \"field\": \"eventTimeStamp\",\n" +
            "                                            \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                                            \"value\": \"56\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"savings\",\n" +
            "                                            \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                                            \"value\": \"60\",\n" +
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


    static final String mockDataIsSet = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"291\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_IsSet\",\n" +
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
            "                                            \"field\": \"eventTimeStamp\",\n" +
            "                                            \"comparatorType\": \"IsSet\",\n" +
            "                                            \"value\": \"\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"savings\",\n" +
            "                                            \"comparatorType\": \"IsSet\",\n" +
            "                                            \"value\": \"\",\n" +
            "                                            \"fieldType\": \"long\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"country\",\n" +
            "                                            \"comparatorType\": \"IsSet\",\n" +
            "                                            \"value\": \"\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"saved_cars\",\n" +
            "                                            \"comparatorType\": \"IsSet\",\n" +
            "                                            \"value\": \"\",\n" +
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

    static final String mockDataContains = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"291\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_IsSet\",\n" +
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
            "                                            \"value\": \"Taiwan\",\n" +
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

    static final String mockDataMatchesRegex = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"291\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_IsSet\",\n" +
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

    static final String mockDataStartsWith = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"291\",\n" +
            "            \"name\": \"Criteria_EventTimeStamp_IsSet\",\n" +
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


    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testEqualsMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 3\n," +
                "            \"savings\": 19.99\n," +
                "            \"likes_boba\": true\n," +
                "            \"country\": China\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataEquals, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testEqualsMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 4\n," +
                "            \"savings\": 20.99\n," +
                "            \"likes_boba\": true\n," +
                "            \"country\": China1\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataEquals, jsonArray);
        assertNull(result);
    }

    @Test
    public void testDoesNotEqualMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 105\n," +
                "            \"savings\": 105\n," +
                "            \"likes_boba\": true\n," +
                "            \"country\": China1\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataDoesNotEqual, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testDoesNotEqualMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 101\n," +
                "            \"savings\": 101\n," +
                "            \"likes_boba\": false\n," +
                "            \"country\": China\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataDoesNotEqual, jsonArray);
        assertNull(result);
    }

    @Test
    public void testGreaterThanMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 8\n," +
                "            \"savings\": 11\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataGreaterThan, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testGreaterThanMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 6\n," +
                "            \"savings\": 9\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataGreaterThan, jsonArray);
        assertNull(result);
    }

    @Test
    public void testGreaterThanOrEqualMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 13\n," +
                "            \"savings\": 21\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataGreaterThanOrEqualTo, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testGreaterThanOrEqualMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 11\n," +
                "            \"savings\": 19\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataGreaterThanOrEqualTo, jsonArray);
        assertNull(result);
    }

    @Test
    public void testLessThanMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 5\n," +
                "            \"savings\": 9\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataLessThan, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testLessThanMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 7\n," +
                "            \"savings\": 11\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataLessThan, jsonArray);
        assertNull(result);
    }

    @Test
    public void testLessThanOrEqualsToMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 56\n," +
                "            \"savings\": 60\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataLessThanOrEqualTo, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testLessThanOrEqualsToMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 57\n," +
                "            \"savings\": 61\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataLessThanOrEqualTo, jsonArray);
        assertNull(result);
    }

    @Test
    public void testIsSetMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "   {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\":\"10\"\n," +
                "            \"savings\":\"20\"\n," +
                "            \"country\":\"fwef\"\n," +
                "            \"saved_cars\":\"zdf\"\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "   }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataIsSet, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testIsSetMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "   {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\":\"12\"\n," +
                "            \"savings\":\"321\"\n," +
                "            \"country\":\"\"\n," +
                "            \"saved_cars\":\"\"\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "   }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataIsSet, jsonArray);
        assertNull(result);
    }

    @Test
    public void testContainsMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "           \"country\":\"Taiwan\"\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataContains, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testContainsMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\":\"Canada\"\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataContains, jsonArray);
        assertNull(result);
    }

    @Test
    public void testMatchesRegexMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\":\"Taiwan\"\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMatchesRegex, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testMatchesRegexMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\":\"Canada\"\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMatchesRegex, jsonArray);
        assertNull(result);
    }

    @Test
    public void testStartsWithMockDataPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "             \"country\":\"Taiwan\"\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataStartsWith, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testStartsWithMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "             \"country\":\"Canada\"\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataStartsWith, jsonArray);
        assertNull(result);
    }
}
