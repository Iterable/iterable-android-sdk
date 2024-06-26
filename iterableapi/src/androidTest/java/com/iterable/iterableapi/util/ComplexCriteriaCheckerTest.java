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
            + "\"criterias\": ["
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
            "    \"criterias\": [\n" +
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
        System.out.println("TEST_USER: " + String.valueOf(result));
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
        System.out.println("TEST_USER: " + String.valueOf(result));
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
        System.out.println("TEST_USER: " + String.valueOf(result));
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
        System.out.println("TEST_USER: " + String.valueOf(result));
        assertFalse(result != null);
    }

}
