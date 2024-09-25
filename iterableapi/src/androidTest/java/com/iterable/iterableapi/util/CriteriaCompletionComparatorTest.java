package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CriteriaCompletionComparatorTest {
    private CriteriaCompletionChecker evaluator;
    private String isSetMockData = "{"
            + "\"count\": 4,"
            + "\"criteriaSets\": ["
            + "    {"
            + "        \"criteriaId\": \"1\","
            + "        \"name\": \"CustomEvent\","
            + "        \"createdAt\": 1716560453973,"
            + "        \"updatedAt\": 1716560453973,"
            + "        \"searchQuery\": {"
            + "            \"combinator\": \"And\","
            + "            \"searchQueries\": ["
            + "                {"
            + "                    \"combinator\": \"And\","
            + "                    \"searchQueries\": ["
            + "                        {"
            + "                            \"dataType\": \"customEvent\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"button-clicked\","
            + "                                        \"fieldType\": \"object\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 2,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"button-clicked.animal\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 4,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"button-clicked.clickCount\","
            + "                                        \"fieldType\": \"long\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 5,"
            + "                                        \"valueLong\": null,"
            + "                                        \"value\": \"\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        }"
            + "                    ]"
            + "                }"
            + "            ]"
            + "        }"
            + "    },"
            + "    {"
            + "        \"criteriaId\": \"2\","
            + "        \"name\": \"UpdateCart\","
            + "        \"createdAt\": 1716560453973,"
            + "        \"updatedAt\": 1716560453973,"
            + "        \"searchQuery\": {"
            + "            \"combinator\": \"And\","
            + "            \"searchQueries\": ["
            + "                {"
            + "                    \"combinator\": \"And\","
            + "                    \"searchQueries\": ["
            + "                        {"
            + "                            \"dataType\": \"customEvent\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"updateCart\","
            + "                                        \"fieldType\": \"object\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 9,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"updateCart.updatedShoppingCartItems.name\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 13,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"updateCart.updatedShoppingCartItems.price\","
            + "                                        \"fieldType\": \"double\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 15,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"updateCart.updatedShoppingCartItems.quantity\","
            + "                                        \"fieldType\": \"long\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 16,"
            + "                                        \"valueLong\": null,"
            + "                                        \"value\": \"\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        }"
            + "                    ]"
            + "                }"
            + "            ]"
            + "        }"
            + "    },"
            + "    {"
            + "        \"criteriaId\": \"3\","
            + "        \"name\": \"Purchase\","
            + "        \"createdAt\": 1716560453973,"
            + "        \"updatedAt\": 1716560453973,"
            + "        \"searchQuery\": {"
            + "            \"combinator\": \"And\","
            + "            \"searchQueries\": ["
            + "                {"
            + "                    \"combinator\": \"And\","
            + "                    \"searchQueries\": ["
            + "                        {"
            + "                            \"dataType\": \"purchase\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"shoppingCartItems\","
            + "                                        \"fieldType\": \"object\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"purchase\","
            + "                                        \"id\": 1,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"shoppingCartItems.price\","
            + "                                        \"fieldType\": \"double\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"purchase\","
            + "                                        \"id\": 3,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"shoppingCartItems.name\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"purchase\","
            + "                                        \"id\": 5,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"total\","
            + "                                        \"fieldType\": \"double\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"purchase\","
            + "                                        \"id\": 7,"
            + "                                        \"value\": \"\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        }"
            + "                    ]"
            + "                }"
            + "            ]"
            + "        }"
            + "    },"
            + "    {"
            + "        \"criteriaId\": \"4\","
            + "        \"name\": \"User\","
            + "        \"createdAt\": 1716560453973,"
            + "        \"updatedAt\": 1716560453973,"
            + "        \"searchQuery\": {"
            + "            \"combinator\": \"And\","
            + "            \"searchQueries\": ["
            + "                {"
            + "                    \"combinator\": \"And\","
            + "                    \"searchQueries\": ["
            + "                        {"
            + "                            \"dataType\": \"user\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"country\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 25,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"eventTimeStamp\","
            + "                                        \"fieldType\": \"long\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 26,"
            + "                                        \"valueLong\": null,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"phoneNumberDetails\","
            + "                                        \"fieldType\": \"object\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 28,"
            + "                                        \"value\": \"\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"shoppingCartItems.price\","
            + "                                        \"fieldType\": \"double\","
            + "                                        \"comparatorType\": \"IsSet\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 30,"
            + "                                        \"value\": \"\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        }"
            + "                    ]"
            + "                }"
            + "            ]"
            + "        }"
            + "    }"
            + "]"
            + "}";

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testUserWithIsSetMockData() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": \"UK\",\n" +
                "            \"eventTimeStamp\": 10,\n" +
                "            \"phoneNumberDetails\": \"99999999\",\n" +
                "            \"shoppingCartItems.price\": 50.5\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(isSetMockData, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testUserWithIsSetMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"eventTimeStamp\": 10,\n" +
                "            \"phoneNumberDetails\": \"99999999\",\n" +
                "            \"shoppingCartItems.price\": 50.5\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(isSetMockData, jsonArray);
        assertNull(result);
    }

    @Test
    public void testPurchaseWithIsSetMockData() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"id\": \"12\",\n" +
                "                \"name\": \"keyboard\",\n" +
                "                \"price\": 10,\n" +
                "                \"quantity\": 2\n" +
                "            }\n" +
                "        ],\n" +
                "        \"createdAt\": 1700071052507,\n" +
                "        \"total\": 2,\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(isSetMockData, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testPurchaseWithIsSetMockDataFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"id\": \"12\",\n" +
                "                \"name\": \"keyboard\"," +
                "                \"quantity\": 5\n" +
                "            }\n" +
                "        ],\n" +
                "        \"createdAt\": 1700071052507,\n" +
                "        \"total\": 2,\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(isSetMockData, jsonArray);
        assertNull(result);
    }

    @Test
    public void testUpdateCartWithIsSetMockData() throws Exception {
        String jsonString = "["
                + "    {"
                + "        \"items\": ["
                + "            {"
                + "                \"id\": \"12\","
                + "                \"name\": \"Mocha\","
                + "                \"price\": 9,"
                + "                \"quantity\": 52"
                + "            }"
                + "        ],"
                + "        \"createdAt\": 1700071052507,"
                + "        \"eventType\": \"cartUpdate\""
                + "    }"
                + "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(isSetMockData, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testUpdateCartWithIsSetMockDataFail() throws Exception {
        String jsonString = "["
                + "    {"
                + "        \"items\": ["
                + "            {"
                + "                \"id\": \"12\","
                + "                \"name\": \"Mocha\","
                + "                \"quantity\": 5"
                + "            }"
                + "        ],"
                + "        \"createdAt\": 1700071052507,"
                + "        \"eventType\": \"cartUpdate\""
                + "    }"
                + "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(isSetMockData, jsonArray);
        assertNull(result);
    }

    @Test
    public void testCustomEventWithIsSetMockData() throws Exception {
        String jsonString = "["
                + "    {"
                + "        \"eventName\": \"button-clicked\","
                + "        \"dataFields\": {"
                + "            \"animal\": \"test page\","
                + "            \"clickCount\": \"2\""
                + "        },"
                + "        \"createdAt\": 1700071052507,"
                + "        \"eventType\": \"customEvent\""
                + "    }"
                + "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(isSetMockData, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testCustomEventWithIsSetMockDataFail() throws Exception {
        String jsonString = "["
                + "    {"
                + "        \"eventName\": \"button-clicked\","
                + "        \"dataFields\": {"
                + "            \"animal\": \"\""
                + "        },"
                + "        \"total\": 3,"
                + "        \"createdAt\": 1700071052507,"
                + "        \"eventType\": \"customEvent\""
                + "    }"
                + "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(isSetMockData, jsonArray);
        assertNull(result);
    }
}
