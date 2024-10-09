package com.iterable.iterableapi.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DoesNotEqualCriteriaMatchTest {

    static final String mockDataLongDoesNotEqual = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"194\",\n" +
            "      \"name\": \"Contact: Phone Number != 57688559\",\n" +
            "      \"createdAt\": 1721337331194,\n" +
            "      \"updatedAt\": 1722338525737,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"And\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"user\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"field\": \"eventTimeStamp\",\n" +
            "                      \"comparatorType\": \"DoesNotEqual\",\n" +
            "                      \"value\": \"15\",\n" +
            "                      \"fieldType\": \"long\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    static final String mockDataStringDoesNotEqual = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"194\",\n" +
            "      \"name\": \"Contact: Phone Number != 57688559\",\n" +
            "      \"createdAt\": 1721337331194,\n" +
            "      \"updatedAt\": 1722338525737,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"And\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"user\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"field\": \"phoneNumber\",\n" +
            "                      \"comparatorType\": \"DoesNotEqual\",\n" +
            "                      \"value\": \"57688559\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    static final String mockDataBooleanDoesNotEqual = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"194\",\n" +
            "      \"name\": \"Contact: Phone Number != 57688559\",\n" +
            "      \"createdAt\": 1721337331194,\n" +
            "      \"updatedAt\": 1722338525737,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"And\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"user\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"field\": \"subscribed\",\n" +
            "                      \"fieldType\": \"boolean\",\n" +
            "                      \"comparatorType\": \"DoesNotEqual\",\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"id\": 25,\n" +
            "                      \"value\": \"true\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    static final String mockDataDoubleDoesNotEqual = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"194\",\n" +
            "      \"name\": \"Contact: Phone Number != 57688559\",\n" +
            "      \"createdAt\": 1721337331194,\n" +
            "      \"updatedAt\": 1722338525737,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"And\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"user\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"field\": \"savings\",\n" +
            "                      \"comparatorType\": \"DoesNotEqual\",\n" +
            "                      \"value\": \"19.99\",\n" +
            "                      \"fieldType\": \"double\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testCompareDataLongDoesNotEqualPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 17\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataLongDoesNotEqual, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataLongDoesNotEqualFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 15\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataLongDoesNotEqual, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testCompareDataStringDoesNotEqualPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"phoneNumber\": \"5768855923\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataStringDoesNotEqual, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataStringDoesNotEqualFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"phoneNumber\": \"57688559\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataStringDoesNotEqual, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testCompareDataBooleanDoesNotEqualPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"subscribed\": false\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataBooleanDoesNotEqual, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataBooleanDoesNotEqualFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"subscribed\": true\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataBooleanDoesNotEqual, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testCompareDataDoubleDoesNotEqualPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"savings\": 20.99\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataDoubleDoesNotEqual, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataDoubleDoesNotEqualFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"savings\": 19.99\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataDoubleDoesNotEqual, jsonArray);
        assertFalse(result != null);
    }
}
