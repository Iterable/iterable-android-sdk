package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MultiLevelNestedCriteriaMatchTest {

    static final String mockDataMultiLevelNested = "{\n" +
            "  \"count\": 3,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"425\",\n" +
            "      \"name\": \"Multi level Nested field criteria\",\n" +
            "      \"createdAt\": 1726811375306,\n" +
            "      \"updatedAt\": 1726811375306,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"And\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"customEvent\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"button-clicked.updateCart.updatedShoppingCartItems.quantity\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"10\",\n" +
            "                      \"fieldType\": \"long\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"button-clicked.browserVisit.website.domain\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"https://mybrand.com/socks\",\n" +
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


    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testMultiLevelNestedPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"updateCart\": {\n" +
                "        \"updatedShoppingCartItems\": {\n" +
                "          \"quantity\": 10\n" +
                "        }\n" +
                "      },\n" +
                "      \"browserVisit\": {\n" +
                "        \"website\": {\n" +
                "          \"domain\": \"https://mybrand.com/socks\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\",\n" +
                "    \"eventName\": \"button-clicked\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMultiLevelNested, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testMultiLevelNestedFail1() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"updateCart\": {\n" +
                "        \"updatedShoppingCartItems\": {\n" +
                "          \"quantity\": 10\n" +
                "        }\n" +
                "      },\n" +
                "      \"browserVisit\": {\n" +
                "        \"website.domain\": \"https://mybrand.com/socks\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\",\n" +
                "    \"eventName\": \"button-clicked\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMultiLevelNested, jsonArray);
        assertNull(result);
    }

    @Test
    public void testMultiLevelNestedFail2() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"updateCart\": {\n" +
                "        \"updatedShoppingCartItems.quantity\": 10\n" +
                "      },\n" +
                "      \"browserVisit\": {\n" +
                "        \"website.domain\": \"https://mybrand.com/socks\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\",\n" +
                "    \"eventName\": \"button-clicked\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMultiLevelNested, jsonArray);
        assertNull(result);
    }

    @Test
    public void testMultiLevelNestedFail3() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"button-clicked\": {\n" +
                "        \"updateCart\": {\n" +
                "          \"updatedShoppingCartItems\": {\n" +
                "            \"quantity\": 10\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"browserVisit\": {\n" +
                "        \"website\": {\n" +
                "          \"domain\": \"https://mybrand.com/socks\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\",\n" +
                "    \"eventName\": \"button-clicked\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMultiLevelNested, jsonArray);
        assertNull(result);
    }

    @Test
    public void testMultiLevelNestedFail4() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"quantity\": 10,\n" +
                "      \"domain\": \"https://mybrand.com/socks\"\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\",\n" +
                "    \"eventName\": \"button-clicked\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMultiLevelNested, jsonArray);
        assertNull(result);
    }
}
