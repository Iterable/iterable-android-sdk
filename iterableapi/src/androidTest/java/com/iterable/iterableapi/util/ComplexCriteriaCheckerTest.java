package com.iterable.iterableapi.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ComplexCriteriaCheckerTest {
    private CriteriaCompletionChecker evaluator;

    private String criteriaMinMax = "{"
            + "\"count\": 1,"
            + "\"criteriaSets\": ["
            + "    {"
            + "        \"criteriaId\": \"1\","
            + "        \"name\": \"Custom Event\","
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
            + "                            \"minMatch\": 2,"
            + "                            \"maxMatch\": 3,"
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"updateCart.updatedShoppingCartItems.price\","
            + "                                        \"fieldType\": \"double\","
            + "                                        \"comparatorType\": \"GreaterThanOrEqualTo\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 2,"
            + "                                        \"value\": \"50.0\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        },"
            + "                        {"
            + "                            \"dataType\": \"user\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"preferred_car_models\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Contains\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 4,"
            + "                                        \"value\": \"Honda\""
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

    private String complexCriteria1 = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"1\",\n" +
            "            \"name\": \"Custom Event\",\n" +
            "            \"createdAt\": 1716560453973,\n" +
            "            \"updatedAt\": 1716560453973,\n" +
            "            \"searchQuery\": {\n" +
            "                \"combinator\": \"Or\",\n" +
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
            "                                            \"field\": \"eventName\",\n" +
            "                                            \"fieldType\": \"string\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"id\": 23,\n" +
            "                                            \"value\": \"button.clicked\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"field\": \"button-clicked.animal\",\n" +
            "                                            \"fieldType\": \"string\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"id\": 25,\n" +
            "                                            \"value\": \"giraffe\"\n" +
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
            "                                            \"field\": \"updateCart.updatedShoppingCartItems.price\",\n" +
            "                                            \"fieldType\": \"double\",\n" +
            "                                            \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"id\": 28,\n" +
            "                                            \"value\": \"120\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"field\": \"updateCart.updatedShoppingCartItems.quantity\",\n" +
            "                                            \"fieldType\": \"long\",\n" +
            "                                            \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"id\": 29,\n" +
            "                                            \"valueLong\": 100,\n" +
            "                                            \"value\": \"100\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"combinator\": \"And\",\n" +
            "                        \"searchQueries\": [\n" +
            "                            {\n" +
            "                                \"dataType\": \"purchase\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"field\": \"shoppingCartItems.name\",\n" +
            "                                            \"fieldType\": \"string\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"dataType\": \"purchase\",\n" +
            "                                            \"id\": 31,\n" +
            "                                            \"value\": \"monitor\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"field\": \"shoppingCartItems.quantity\",\n" +
            "                                            \"fieldType\": \"long\",\n" +
            "                                            \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                                            \"dataType\": \"purchase\",\n" +
            "                                            \"id\": 32,\n" +
            "                                            \"valueLong\": 5,\n" +
            "                                            \"value\": \"5\"\n" +
            "                                        }\n" +
            "                                    ]\n" +
            "                                }\n" +
            "                            },\n" +
            "                            {\n" +
            "                                \"dataType\": \"user\",\n" +
            "                                \"searchCombo\": {\n" +
            "                                    \"combinator\": \"And\",\n" +
            "                                    \"searchQueries\": [\n" +
            "                                        {\n" +
            "                                            \"field\": \"country\",\n" +
            "                                            \"fieldType\": \"string\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"id\": 34,\n" +
            "                                            \"value\": \"Japan\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"field\": \"preferred_car_models\",\n" +
            "                                            \"fieldType\": \"string\",\n" +
            "                                            \"comparatorType\": \"Contains\",\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"id\": 36,\n" +
            "                                            \"value\": \"Honda\"\n" +
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

    private String complexCriteria2 = "{"
            + "\"count\": 1,"
            + "\"criteriaSets\": ["
            + "    {"
            + "        \"criteriaId\": \"1\","
            + "        \"name\": \"Custom Event\","
            + "        \"createdAt\": 1716560453973,"
            + "        \"updatedAt\": 1716560453973,"
            + "        \"searchQuery\": {"
            + "            \"combinator\": \"And\","
            + "            \"searchQueries\": ["
            + "                {"
            + "                    \"combinator\": \"Or\","
            + "                    \"searchQueries\": ["
            + "                        {"
            + "                            \"dataType\": \"customEvent\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"eventName\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Equals\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 2,"
            + "                                        \"value\": \"button-clicked\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"button-clicked.lastPageViewed\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Equals\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 4,"
            + "                                        \"value\": \"welcome page\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        },"
            + "                        {"
            + "                            \"dataType\": \"customEvent\","
            + "                            \"minMatch\": 2,"
            + "                            \"maxMatch\": 3,"
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"updateCart.updatedShoppingCartItems.price\","
            + "                                        \"fieldType\": \"double\","
            + "                                        \"comparatorType\": \"GreaterThanOrEqualTo\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 6,"
            + "                                        \"value\": \"85\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"updateCart.updatedShoppingCartItems.quantity\","
            + "                                        \"fieldType\": \"long\","
            + "                                        \"comparatorType\": \"GreaterThanOrEqualTo\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 7,"
            + "                                        \"valueLong\": 50,"
            + "                                        \"value\": \"50\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        }"
            + "                    ]"
            + "                },"
            + "                {"
            + "                    \"combinator\": \"Or\","
            + "                    \"searchQueries\": ["
            + "                        {"
            + "                            \"dataType\": \"purchase\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"shoppingCartItems.name\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Equals\","
            + "                                        \"dataType\": \"purchase\","
            + "                                        \"id\": 16,"
            + "                                        \"isFiltering\": false,"
            + "                                        \"value\": \"coffee\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"shoppingCartItems.quantity\","
            + "                                        \"fieldType\": \"long\","
            + "                                        \"comparatorType\": \"GreaterThanOrEqualTo\","
            + "                                        \"dataType\": \"purchase\","
            + "                                        \"id\": 17,"
            + "                                        \"valueLong\": 2,"
            + "                                        \"value\": \"2\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        },"
            + "                        {"
            + "                            \"dataType\": \"user\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"country\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Equals\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 19,"
            + "                                        \"value\": \"USA\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"preferred_car_models\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Contains\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 21,"
            + "                                        \"value\": \"Subaru\""
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

    private String complexCriteria3 = "{"
            + "\"count\": 1,"
            + "\"criteriaSets\": ["
            + "    {"
            + "        \"criteriaId\": \"1\","
            + "        \"name\": \"Custom Event\","
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
            + "                                        \"field\": \"eventName\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Equals\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 2,"
            + "                                        \"value\": \"button-clicked\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"button-clicked.lastPageViewed\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Equals\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 4,"
            + "                                        \"value\": \"welcome page\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        },"
            + "                        {"
            + "                            \"dataType\": \"customEvent\","
            + "                            \"minMatch\": 2,"
            + "                            \"maxMatch\": 3,"
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"updateCart.updatedShoppingCartItems.price\","
            + "                                        \"fieldType\": \"double\","
            + "                                        \"comparatorType\": \"GreaterThanOrEqualTo\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 6,"
            + "                                        \"value\": \"85\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"updateCart.updatedShoppingCartItems.quantity\","
            + "                                        \"fieldType\": \"long\","
            + "                                        \"comparatorType\": \"GreaterThanOrEqualTo\","
            + "                                        \"dataType\": \"customEvent\","
            + "                                        \"id\": 7,"
            + "                                        \"valueLong\": 50,"
            + "                                        \"value\": \"50\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        },"
            + "                        {"
            + "                            \"dataType\": \"purchase\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"shoppingCartItems.name\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Equals\","
            + "                                        \"dataType\": \"purchase\","
            + "                                        \"id\": 9,"
            + "                                        \"value\": \"coffee\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"shoppingCartItems.quantity\","
            + "                                        \"fieldType\": \"long\","
            + "                                        \"comparatorType\": \"GreaterThanOrEqualTo\","
            + "                                        \"dataType\": \"purchase\","
            + "                                        \"id\": 10,"
            + "                                        \"valueLong\": 2,"
            + "                                        \"value\": \"2\""
            + "                                    }"
            + "                                ]"
            + "                            }"
            + "                        },"
            + "                        {"
            + "                            \"dataType\": \"user\","
            + "                            \"searchCombo\": {"
            + "                                \"combinator\": \"And\","
            + "                                \"searchQueries\": ["
            + "                                    {"
            + "                                        \"field\": \"country\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Equals\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 12,"
            + "                                        \"value\": \"USA\""
            + "                                    },"
            + "                                    {"
            + "                                        \"field\": \"preferred_car_models\","
            + "                                        \"fieldType\": \"string\","
            + "                                        \"comparatorType\": \"Contains\","
            + "                                        \"dataType\": \"user\","
            + "                                        \"id\": 14,"
            + "                                        \"value\": \"Subaru\""
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

    private String complexCriteria4 = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"1\",\n" +
            "      \"name\": \"Custom Event\",\n" +
            "      \"createdAt\": 1716560453973,\n" +
            "      \"updatedAt\": 1716560453973,\n" +
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
            "                      \"field\": \"shoppingCartItems.name\",\n" +
            "                      \"fieldType\": \"string\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"id\": 1,\n" +
            "                      \"value\": \"sneakers\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                      \"field\": \"shoppingCartItems.quantity\",\n" +
            "                      \"fieldType\": \"long\",\n" +
            "                      \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"id\": 2,\n" +
            "                      \"valueLong\": 3,\n" +
            "                      \"value\": \"3\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          },\n" +
            "          {\n" +
            "            \"combinator\": \"And\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"field\": \"shoppingCartItems.name\",\n" +
            "                      \"fieldType\": \"string\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"id\": 4,\n" +
            "                      \"value\": \"slippers\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                      \"field\": \"shoppingCartItems.quantity\",\n" +
            "                      \"fieldType\": \"long\",\n" +
            "                      \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"id\": 5,\n" +
            "                      \"valueLong\": 3,\n" +
            "                      \"value\": \"3\"\n" +
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

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testMinMatchWithCriteriaMinMaxData() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":50,\\\"quantity\\\":40}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":50,\\\"quantity\\\":40}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"preferred_car_models\": \"Honda\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(criteriaMinMax, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testMinMatchWithCriteriaMinMaxDataFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":50,\\\"quantity\\\":40}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"preferred_car_models\": \"Honda\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(criteriaMinMax, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testComplexCriteria1() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"monitor\\\",\\\"price\\\":50,\\\"quantity\\\":10}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"preferred_car_models\": \"Honda\",\n" +
                "      \"country\": \"Japan\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria1, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testComplexCriteria1Fail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"monitor\\\",\\\"price\\\":50,\\\"quantity\\\":10}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"preferred_car_models\": \"Honda\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria1, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testComplexCriteria2() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":90,\\\"quantity\\\":50}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":90,\\\"quantity\\\":50}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"preferred_car_models\": \"Subaru\",\n" +
                "      \"country\": \"USA\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria2, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testComplexCriteria2Fail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":90,\\\"quantity\\\":50}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"preferred_car_models\": \"Subaru\",\n" +
                "      \"country\": \"USA\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria2, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testComplexCriteria3() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":90,\\\"quantity\\\":50}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":90,\\\"quantity\\\":50}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"preferred_car_models\": \"Subaru\",\n" +
                "      \"country\": \"USA\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"eventName\": \"button-clicked\",\n" +
                "    \"dataFields\": {\n" +
                "      \"button-clicked.lastPageViewed\": \"welcome page\"\n" +
                "    },\n" +
                "    \"total\": 3,\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"items\": [\n" +
                "      {\n" +
                "        \"id\": \"12\",\n" +
                "        \"name\": \"coffee\",\n" +
                "        \"price\": 10,\n" +
                "        \"quantity\": 5\n" +
                "      }\n" +
                "    ],\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 2,\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"eventName\": \"button-clicked\",\n" +
                "    \"dataFields\": {\n" +
                "      \"button-clicked.lastPageViewed\": \"welcome page\"\n" +
                "    },\n" +
                "    \"total\": 3,\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria3, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testComplexCriteria3Fail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":90,\\\"quantity\\\":50}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"items\": \"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":90,\\\"quantity\\\":50}]\",\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 50,\n" +
                "    \"eventType\": \"cartUpdate\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"preferred_car_models\": \"Subaru\",\n" +
                "      \"country\": \"USA\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"eventName\": \"button-clicked\",\n" +
                "    \"dataFields\": {\n" +
                "      \"button-clicked.lastPageViewed\": \"welcome page\"\n" +
                "    },\n" +
                "    \"total\": 3,\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";


        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria3, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testComplexCriteria4() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": [\n" +
                "      {\n" +
                "        \"id\": \"12\",\n" +
                "        \"name\": \"sneakers\",\n" +
                "        \"price\": 10,\n" +
                "        \"quantity\": 5\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"12\",\n" +
                "        \"name\": \"slippers\",\n" +
                "        \"price\": 10,\n" +
                "        \"quantity\": 5\n" +
                "      }\n" +
                "    ],\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 2,\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria4, jsonArray);
        System.out.println("TEST_USER: " + String.valueOf(result));
        assertTrue(result != null);
    }

    @Test
    public void testComplexCriteria4Fail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"items\": [\n" +
                "      {\n" +
                "        \"id\": \"12\",\n" +
                "        \"name\": \"sneakers\",\n" +
                "        \"price\": 10,\n" +
                "        \"quantity\": 3\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": \"12\",\n" +
                "        \"name\": \"slippers\",\n" +
                "        \"price\": 10,\n" +
                "        \"quantity\": 5\n" +
                "      }\n" +
                "    ],\n" +
                "    \"createdAt\": 1700071052507,\n" +
                "    \"total\": 2,\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria4, jsonArray);
        System.out.println("TEST_USER: " + String.valueOf(result));
        assertFalse(result != null);
    }
}
