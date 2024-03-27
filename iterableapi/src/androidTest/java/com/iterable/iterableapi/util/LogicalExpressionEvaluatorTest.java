package com.iterable.iterableapi.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LogicalExpressionEvaluatorTest {

    private LogicalExpressionEvaluator evaluator;

    private String localEvent = "{\n" +
            "  \"items\": [{\"id\":\"12\",\"name\":\"Mocha\",\"price\":3.5,\"quantity\":1}],\n" +
            "  \"createdAt\": 1699246745093,\n" +
            "  \"total\": 3.5,\n" +
            "  \"tracking_type\": \"purchase\"\n" +
            "}";

    private String mockDataWithAnd = "{\n" +
            "  \"combinator\": \"Or\",\n" +
            "  \"searchQueries\": [\n" +
            "    {\n" +
            "      \"combinator\": \"And\",\n" +
            "      \"searchQueries\": [\n" +
            "        {\n" +
            "          \"dataType\": \"purchase\",\n" +
            "          \"searchCombo\": {\n" +
            "            \"combinator\": \"And\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"field\": \"shoppingCartItems.price\",\n" +
            "                \"fieldType\": \"double\",\n" +
            "                \"comparatorType\": \"Equals\",\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"id\": 2,\n" +
            "                \"value\": \"4.67\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"field\": \"shoppingCartItems.quantity\",\n" +
            "                \"fieldType\": \"long\",\n" +
            "                \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"id\": 3,\n" +
            "                \"valueLong\": 2,\n" +
            "                \"value\": \"2\"\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private String mockDataWithOr = "{\n" +
            "  \"combinator\": \"Or\",\n" +
            "  \"searchQueries\": [\n" +
            "    {\n" +
            "      \"combinator\": \"And\",\n" +
            "      \"searchQueries\": [\n" +
            "        {\n" +
            "          \"dataType\": \"purchase\",\n" +
            "          \"searchCombo\": {\n" +
            "            \"combinator\": \"Or\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"field\": \"shoppingCartItems.price\",\n" +
            "                \"fieldType\": \"double\",\n" +
            "                \"comparatorType\": \"Equals\",\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"id\": 2,\n" +
            "                \"value\": \"4.67\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"field\": \"shoppingCartItems.quantity\",\n" +
            "                \"fieldType\": \"long\",\n" +
            "                \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"id\": 3,\n" +
            "                \"valueLong\": 2,\n" +
            "                \"value\": \"2\"\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Before
    public void setUp() {
        evaluator = new LogicalExpressionEvaluator();
    }

    @Test
    public void testCompareDataWithANDCombinatorFail() throws Exception {

        System.out.println("Initiate a test designed to fail for the AND combinator.");
        JSONObject node = new JSONObject(mockDataWithAnd);
        JSONObject localEventData = new JSONObject(localEvent);
        JSONObject eventItem = new JSONObject("{\"id\":\"12\",\"name\":\"Mocha\",\"price\":3.5,\"quantity\":1}");

        boolean result = evaluator.compareData(node, eventItem, localEventData);
        System.out.println("Result -> Criteria does not match.");
        assertFalse(result);
    }

    @Test
    public void testCompareDataWithANDCombinator() throws Exception {

        System.out.println("Initiate a test designed to success for the AND combinator.");
        JSONObject node = new JSONObject(mockDataWithAnd);
        JSONObject localEventData = new JSONObject(localEvent);
        JSONObject eventItem = new JSONObject("{\"id\":\"12\",\"name\":\"Mocha\",\"price\":4.67,\"quantity\":3}");

        boolean result = evaluator.compareData(node, eventItem, localEventData);
        System.out.println("Result -> Criteria matched.");
        assertTrue(result);
    }

    @Test
    public void testCompareDataWithORCombinator() throws Exception {

        System.out.println("Initiate a test designed to success for the OR combinator.");
        JSONObject node = new JSONObject(mockDataWithOr);
        JSONObject localEventData = new JSONObject(localEvent);
        JSONObject eventItem = new JSONObject("{\"id\":\"12\",\"name\":\"Mocha\",\"price\":3.5,\"quantity\":3}");

        boolean result = evaluator.compareData(node, eventItem, localEventData);
        System.out.println("Result -> Criteria matched.");
        assertTrue(result);
    }

    @Test
    public void testCompareDataWithORCombinatorFail() throws Exception {

        System.out.println("Initiate a test designed to fail for the OR combinator.");
        JSONObject node = new JSONObject(mockDataWithOr);
        JSONObject localEventData = new JSONObject(localEvent);
        JSONObject eventItem = new JSONObject("{\"id\":\"12\",\"name\":\"Mocha\",\"price\":3.5,\"quantity\":1}");

        boolean result = evaluator.compareData(node, eventItem, localEventData);
        System.out.println("Result -> Criteria does not match.");
        assertFalse(result);
    }

    @Test
    public void testEvaluateTree() throws Exception {
        JSONObject node = new JSONObject();
        JSONObject localEventData = new JSONObject();
        String trackingType = "sampleType";

        boolean result = evaluator.evaluateTree(node, localEventData, trackingType);
        System.out.println("Result -> Criteria does not match.");
        assertFalse(result);
    }

    @Test
    public void testEvaluateField() throws JSONException {
        JSONObject node = new JSONObject();
        JSONObject localEventData = new JSONObject();
        String trackingType = "sampleType";

        boolean result = evaluator.evaluateField(node, localEventData, trackingType);
        System.out.println("Result -> Criteria does not match.");
        assertFalse(result);
    }

    @Test
    public void testEvaluateComparison() throws JSONException {
        JSONObject node = new JSONObject();
        node.put("value", 10.0);
        node.put("comparatorType", "GreaterThan");
        node.put("fieldType", "number");

        boolean result = evaluator.evaluateComparison("GreaterThan", "number", 15.0, node);
        System.out.println("Result -> Criteria matched.");
        assertTrue(result);
    }
}