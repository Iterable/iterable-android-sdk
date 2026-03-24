package com.iterable.iterableapi.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CombinationLogicEventTypeCriteriaTest {

    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    static final String mockDataCombinatorContactPropertyANDCustomEvent = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"310\",\n" +
            "      \"name\": \"Contact_Property_AND_Custom_Event\",\n" +
            "      \"createdAt\": 1723113771608,\n" +
            "      \"updatedAt\": 1723113771608,\n" +
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
            "                      \"field\": \"firstName\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"David\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"customEvent\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"total\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"10\",\n" +
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


    @Test
    public void testCompareDataContactPropertyANDCustomEventPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"eventType\": \"user\",\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"David\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"total\": \"10\"\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorContactPropertyANDCustomEvent, jsonArray);
        assertTrue(result != null);
    }
    @Test
    public void testCompareDataContactPropertyANDCustomEventFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"eventType\": \"user\",\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"DavidJohn\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"total\": \"11\"\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorContactPropertyANDCustomEvent, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorContactPropertyORCustomEvent = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "         {\n" +
            "            \"criteriaId\": \"312\",\n" +
            "            \"name\": \"Contact_Property_OR_Custom_Event\",\n" +
            "            \"createdAt\": 1723115120517,\n" +
            "            \"updatedAt\": 1723115120517,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"Or\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"firstName\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"David\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            },\n" +
            "                            {\n" +
            "                                \"dataType\": \"customEvent\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"field\": \"total\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"10\",\n" +
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
    public void testCompareDataContactPropertyORCustomEventPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"eventType\": \"user\",\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"David\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"total\": \"10\"\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorContactPropertyORCustomEvent, jsonArray);
        assertTrue(result != null);
    }
    @Test
    public void testCompareDataContactPropertyORCustomEventFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"eventType\": \"user\",\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"DavidAs\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"total\": \"101\"\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorContactPropertyORCustomEvent, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorContactPropertyNOTCustomEvent = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "         {\n" +
            "            \"criteriaId\": \"312\",\n" +
            "            \"name\": \"Contact_Property_NOT_Custom_Event\",\n" +
            "            \"createdAt\": 1723115120517,\n" +
            "            \"updatedAt\": 1723115120517,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"Not\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"firstName\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"David\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            },\n" +
            "                            {\n" +
            "                                \"dataType\": \"customEvent\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"field\": \"total\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"10\",\n" +
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
    public void testCompareDataContactPropertyNOTCustomEventPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"eventType\": \"user\",\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"Davidson\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"total\": \"1\"\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorContactPropertyNOTCustomEvent, jsonArray);
        assertTrue(result != null);
    }
    @Test
    public void testCompareDataContactPropertyNOTCustomEventFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"eventType\": \"user\",\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"David\"\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"total\": \"10\"\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorContactPropertyNOTCustomEvent, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorUpdateCartANDContactProperty = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"315\",\n" +
            "      \"name\": \"UpdateCart_AND_ContactProperty\",\n" +
            "      \"createdAt\": 1723119153268,\n" +
            "      \"updatedAt\": 1723119153268,\n" +
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
            "                      \"field\": \"updateCart.updatedShoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"fried\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"user\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"field\": \"firstName\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"David\",\n" +
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

    @Test
    public void testCompareDataUpdateCartANDContactPropertyPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"fried\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "      \"eventType\": \"user\",\n" +
                "      \"dataFields\": {\n" +
                "        \"firstName\": \"David\"\n" +
                "      }\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorUpdateCartANDContactProperty, jsonArray);
        assertTrue(result != null);
    }
    @Test
    public void testCompareDataUpdateCartANDContactPropertyFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"boiled\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "      \"eventType\": \"user\",\n" +
                "      \"dataFields\": {\n" +
                "        \"firstName\": \"David\"\n" +
                "      }\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorUpdateCartANDContactProperty, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorUpdateCartORContactProperty = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"315\",\n" +
            "      \"name\": \"UpdateCart_OR_ContactProperty\",\n" +
            "      \"createdAt\": 1723119153268,\n" +
            "      \"updatedAt\": 1723119153268,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"Or\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"customEvent\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"updateCart.updatedShoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"fried\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"user\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"field\": \"firstName\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"David\",\n" +
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

    @Test
    public void testCompareDataUpdateCartORContactPropertyPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"fried\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "      \"eventType\": \"user\",\n" +
                "      \"dataFields\": {\n" +
                "        \"firstName\": \"David\"\n" +
                "      }\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorUpdateCartORContactProperty, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataUpdateCartORContactPropertyFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"boiled\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "      \"eventType\": \"user\",\n" +
                "      \"dataFields\": {\n" +
                "        \"firstName\": \"John\"\n" +
                "      }\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorUpdateCartORContactProperty, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorUpdateCartNOTContactProperty = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"315\",\n" +
            "      \"name\": \"UpdateCart_NOT_ContactProperty\",\n" +
            "      \"createdAt\": 1723119153268,\n" +
            "      \"updatedAt\": 1723119153268,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"Not\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"customEvent\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"updateCart.updatedShoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"fried\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"user\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"field\": \"firstName\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"David\",\n" +
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

    @Test
    public void testCompareDataUpdateCartNOTContactPropertyPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"boiled\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "      \"eventType\": \"user\",\n" +
                "      \"dataFields\": {\n" +
                "        \"firstName\": \"DavidJohn\"\n" +
                "      }\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorUpdateCartNOTContactProperty, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataUpdateCartNOTContactPropertyFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"fried\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "      \"eventType\": \"user\",\n" +
                "      \"dataFields\": {\n" +
                "        \"firstName\": \"David\"\n" +
                "      }\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorUpdateCartNOTContactProperty, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorPurchaseANDUpdateCart = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"316\",\n" +
            "      \"name\": \"Purchase_AND_UpdateCart\",\n" +
            "      \"createdAt\": 1723124161944,\n" +
            "      \"updatedAt\": 1723124205406,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"And\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"field\": \"shoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"chicken\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"customEvent\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"updateCart.updatedShoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"fried\",\n" +
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

    @Test
    public void testCompareDataPurchaseANDUpdateCartPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"fried\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"chicken\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorPurchaseANDUpdateCart, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataPurchaseANDUpdateCartFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"boiled\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"chicken\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorPurchaseANDUpdateCart, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorPurchaseORUpdateCart = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"316\",\n" +
            "      \"name\": \"Purchase_OR_UpdateCart\",\n" +
            "      \"createdAt\": 1723124161944,\n" +
            "      \"updatedAt\": 1723124205406,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"Or\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"field\": \"shoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"chicken\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"customEvent\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"updateCart.updatedShoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"fried\",\n" +
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

    @Test
    public void testCompareDataPurchaseORUpdateCartPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"fried\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"chicken\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorPurchaseORUpdateCart, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataPurchaseORUpdateCartFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"boiled\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"beef\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorPurchaseORUpdateCart, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorPurchaseNOTUpdateCart = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"316\",\n" +
            "      \"name\": \"Purchase_NOT_UpdateCart\",\n" +
            "      \"createdAt\": 1723124161944,\n" +
            "      \"updatedAt\": 1723124205406,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"Not\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"field\": \"shoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"chicken\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"customEvent\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"updateCart.updatedShoppingCartItems.name\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"fried\",\n" +
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

    @Test
    public void testCompareDataPurchaseNOTUpdateCartPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"boiled\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"beef\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorPurchaseNOTUpdateCart, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataPurchaseNOTUpdateCartFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"fried\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"cartUpdate\"\n" +
                "    }, \n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"chicken\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorPurchaseNOTUpdateCart, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorCustomEventANDPurchase = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"320\",\n" +
            "            \"name\": \"CustomEvent_AND_Purchase\",\n" +
            "            \"createdAt\": 1723184939510,\n" +
            "            \"updatedAt\": 1723184939510,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"customEvent\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"field\": \"eventName\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"birthday\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            },\n" +
            "                            {\n" +
            "                                \"dataType\": \"purchase\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"purchase\",\n" +
            "                                            \"field\": \"shoppingCartItems.name\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"chicken\",\n" +
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
    public void testCompareDataCustomEventANDPurchasePass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "      \"eventType\": \"customEvent\",\n" +
                "           \"dataFields\": {\n" +
                "             \"eventName\": \"birthday\"\n" +
                "           }\n" +
                "    },\n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"chicken\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorCustomEventANDPurchase, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataCustomEventANDPurchaseFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "      \"eventType\": \"customEvent\",\n" +
                "           \"dataFields\": {\n" +
                "             \"eventName\": \"birthday\"\n" +
                "           }\n" +
                "    },\n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"beef\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorCustomEventANDPurchase, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorCustomEventORPurchase = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"320\",\n" +
            "            \"name\": \"CustomEvent_OR_Purchase\",\n" +
            "            \"createdAt\": 1723184939510,\n" +
            "            \"updatedAt\": 1723184939510,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"Or\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"customEvent\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"field\": \"eventName\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"birthday\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            },\n" +
            "                            {\n" +
            "                                \"dataType\": \"purchase\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"purchase\",\n" +
            "                                            \"field\": \"shoppingCartItems.name\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"chicken\",\n" +
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
    public void testCompareDataCustomEventORPurchasePass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "      \"eventType\": \"customEvent\",\n" +
                "           \"dataFields\": {\n" +
                "             \"eventName\": \"birthday\"\n" +
                "           }\n" +
                "    },\n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"beef\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorCustomEventORPurchase, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataCustomEventORPurchaseFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "      \"eventType\": \"customEvent\",\n" +
                "           \"dataFields\": {\n" +
                "             \"eventName\": \"anniversary\"\n" +
                "           }\n" +
                "    },\n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"beef\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorCustomEventORPurchase, jsonArray);
        assertFalse(result != null);
    }

    static final String mockDataCombinatorCustomEventNOTPurchase = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"320\",\n" +
            "            \"name\": \"CustomEvent_NOT_Purchase\",\n" +
            "            \"createdAt\": 1723184939510,\n" +
            "            \"updatedAt\": 1723184939510,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"And\",\n" +
            "                \"searchQueries\": [\n" +
            "                    {\n" +
            "                        \"combinator\": \"Not\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"customEvent\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"field\": \"eventName\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"birthday\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            },\n" +
            "                            {\n" +
            "                                \"dataType\": \"purchase\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"purchase\",\n" +
            "                                            \"field\": \"shoppingCartItems.name\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"value\": \"chicken\",\n" +
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
    public void testCompareDataCustomEventNOTPurchasePass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "      \"eventType\": \"customEvent\",\n" +
                "           \"dataFields\": {\n" +
                "             \"eventName\": \"birthday1\"\n" +
                "           }\n" +
                "    },\n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"chicken\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorCustomEventNOTPurchase, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCompareDataCustomEventNOTPurchaseFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "      \"eventType\": \"customEvent\",\n" +
                "           \"dataFields\": {\n" +
                "             \"eventName\": \"birthday\"\n" +
                "           }\n" +
                "    },\n" +
                "    {\n" +
                "     \"items\": [\n" +
                "            {\n" +
                "                \"name\": \"chicken\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"eventType\": \"purchase\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCombinatorCustomEventNOTPurchase, jsonArray);
        assertFalse(result != null);
    }
}
