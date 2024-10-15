package com.iterable.iterableapi.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CriteriaCompletionCheckerTest {

    private CriteriaCompletionChecker evaluator;

    private final String mockDataWithAnd = "{\n" +
            "   \"count\":2,\n" +
            "   \"criteriaSets\":[\n" +
            "      {\n" +
            "         \"criteriaId\":12345,\n" +
            "         \"searchQuery\":{\n" +
            "            \"combinator\":\"Or\",\n" +
            "            \"searchQueries\":[\n" +
            "               {\n" +
            "                  \"combinator\":\"And\",\n" +
            "                  \"searchQueries\":[\n" +
            "                     {\n" +
            "                        \"dataType\":\"purchase\",\n" +
            "                        \"searchCombo\":{\n" +
            "                           \"combinator\":\"And\",\n" +
            "                           \"searchQueries\":[\n" +
            "                              {\n" +
            "                                 \"field\":\"shoppingCartItems.price\",\n" +
            "                                 \"fieldType\":\"double\",\n" +
            "                                 \"comparatorType\":\"Equals\",\n" +
            "                                 \"dataType\":\"purchase\",\n" +
            "                                 \"id\":2,\n" +
            "                                 \"value\":\"4.67\"\n" +
            "                              },\n" +
            "                              {\n" +
            "                                 \"field\":\"shoppingCartItems.quantity\",\n" +
            "                                 \"fieldType\":\"long\",\n" +
            "                                 \"comparatorType\":\"GreaterThanOrEqualTo\",\n" +
            "                                 \"dataType\":\"purchase\",\n" +
            "                                 \"id\":3,\n" +
            "                                 \"valueLong\":2,\n" +
            "                                 \"value\":\"2\"\n" +
            "                              }\n" +
            "                           ]\n" +
            "                        }\n" +
            "                     }\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      },\n" +
            "      {\n" +
            "         \"criteriaId\":5678,\n" +
            "         \"searchQuery\":{\n" +
            "            \"combinator\":\"Or\",\n" +
            "            \"searchQueries\":[\n" +
            "               {\n" +
            "                  \"combinator\":\"Or\",\n" +
            "                  \"searchQueries\":[\n" +
            "                     {\n" +
            "                        \"dataType\":\"user\",\n" +
            "                        \"searchCombo\":{\n" +
            "                           \"combinator\":\"And\",\n" +
            "                           \"searchQueries\":[\n" +
            "                              {\n" +
            "                                 \"field\":\"itblInternal.emailDomain\",\n" +
            "                                 \"fieldType\":\"string\",\n" +
            "                                 \"comparatorType\":\"Equals\",\n" +
            "                                 \"dataType\":\"user\",\n" +
            "                                 \"id\":6,\n" +
            "                                 \"value\":\"gmail.com\"\n" +
            "                              }\n" +
            "                           ]\n" +
            "                        }\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"dataType\":\"customEvent\",\n" +
            "                        \"searchCombo\":{\n" +
            "                           \"combinator\":\"And\",\n" +
            "                           \"searchQueries\":[\n" +
            "                              {\n" +
            "                                 \"field\":\"eventName\",\n" +
            "                                 \"fieldType\":\"string\",\n" +
            "                                 \"comparatorType\":\"Equals\",\n" +
            "                                 \"dataType\":\"customEvent\",\n" +
            "                                 \"id\":9,\n" +
            "                                 \"value\":\"processing_cancelled\"\n" +
            "                              },\n" +
            "                              {\n" +
            "                                 \"field\":\"createdAt\",\n" +
            "                                 \"fieldType\":\"date\",\n" +
            "                                 \"comparatorType\":\"GreaterThan\",\n" +
            "                                 \"dataType\":\"customEvent\",\n" +
            "                                 \"id\":10,\n" +
            "                                 \"dateRange\":{\n" +
            "                                    \n" +
            "                                 },\n" +
            "                                 \"isRelativeDate\":false,\n" +
            "                                 \"value\":\"1688194800000\"\n" +
            "                              }\n" +
            "                           ]\n" +
            "                        }\n" +
            "                     }\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      }\n" +
            "   ]\n" +
            "}";

    private final String mockDataWithOr = "{\n" +
            "   \"count\":2,\n" +
            "   \"criteriaSets\":[\n" +
            "      {\n" +
            "         \"criteriaId\":12345,\n" +
            "         \"searchQuery\":{\n" +
            "            \"combinator\":\"Or\",\n" +
            "            \"searchQueries\":[\n" +
            "               {\n" +
            "                  \"combinator\":\"And\",\n" +
            "                  \"searchQueries\":[\n" +
            "                     {\n" +
            "                        \"dataType\":\"purchase\",\n" +
            "                        \"searchCombo\":{\n" +
            "                           \"combinator\":\"Or\",\n" +
            "                           \"searchQueries\":[\n" +
            "                              {\n" +
            "                                 \"field\":\"shoppingCartItems.price\",\n" +
            "                                 \"fieldType\":\"double\",\n" +
            "                                 \"comparatorType\":\"Equals\",\n" +
            "                                 \"dataType\":\"purchase\",\n" +
            "                                 \"id\":2,\n" +
            "                                 \"value\":\"4.67\"\n" +
            "                              },\n" +
            "                              {\n" +
            "                                 \"field\":\"shoppingCartItems.quantity\",\n" +
            "                                 \"fieldType\":\"long\",\n" +
            "                                 \"comparatorType\":\"GreaterThanOrEqualTo\",\n" +
            "                                 \"dataType\":\"purchase\",\n" +
            "                                 \"id\":3,\n" +
            "                                 \"valueLong\":2,\n" +
            "                                 \"value\":\"2\"\n" +
            "                              }\n" +
            "                           ]\n" +
            "                        }\n" +
            "                     }\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      },\n" +
            "      {\n" +
            "         \"criteriaId\":5678,\n" +
            "         \"searchQuery\":{\n" +
            "            \"combinator\":\"Or\",\n" +
            "            \"searchQueries\":[\n" +
            "               {\n" +
            "                  \"combinator\":\"Or\",\n" +
            "                  \"searchQueries\":[\n" +
            "                     {\n" +
            "                        \"dataType\":\"user\",\n" +
            "                        \"searchCombo\":{\n" +
            "                           \"combinator\":\"And\",\n" +
            "                           \"searchQueries\":[\n" +
            "                              {\n" +
            "                                 \"field\":\"itblInternal.emailDomain\",\n" +
            "                                 \"fieldType\":\"string\",\n" +
            "                                 \"comparatorType\":\"Equals\",\n" +
            "                                 \"dataType\":\"user\",\n" +
            "                                 \"id\":6,\n" +
            "                                 \"value\":\"gmail.com\"\n" +
            "                              }\n" +
            "                           ]\n" +
            "                        }\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"dataType\":\"customEvent\",\n" +
            "                        \"searchCombo\":{\n" +
            "                           \"combinator\":\"And\",\n" +
            "                           \"searchQueries\":[\n" +
            "                              {\n" +
            "                                 \"field\":\"eventName\",\n" +
            "                                 \"fieldType\":\"string\",\n" +
            "                                 \"comparatorType\":\"Equals\",\n" +
            "                                 \"dataType\":\"customEvent\",\n" +
            "                                 \"id\":9,\n" +
            "                                 \"value\":\"processing_cancelled\"\n" +
            "                              },\n" +
            "                              {\n" +
            "                                 \"field\":\"createdAt\",\n" +
            "                                 \"fieldType\":\"date\",\n" +
            "                                 \"comparatorType\":\"GreaterThan\",\n" +
            "                                 \"dataType\":\"customEvent\",\n" +
            "                                 \"id\":10,\n" +
            "                                 \"dateRange\":{\n" +
            "                                    \n" +
            "                                 },\n" +
            "                                 \"isRelativeDate\":false,\n" +
            "                                 \"value\":\"1688194800000\"\n" +
            "                              }\n" +
            "                           ]\n" +
            "                        }\n" +
            "                     }\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      }\n" +
            "   ]\n" +
            "}";

    private String mockCriteria = "{\n" +
            "  \"count\": 4,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"43\",\n" +
            "      \"name\": \"ContactProperty\",\n" +
            "      \"createdAt\": 1716560453973,\n" +
            "      \"updatedAt\": 1716560453973,\n" +
            "      \"searchQuery\": {\n" +
            "        \"combinator\": \"And\",\n" +
            "        \"searchQueries\": [\n" +
            "          {\n" +
            "            \"combinator\": \"Or\",\n" +
            "            \"searchQueries\": [\n" +
            "              {\n" +
            "                \"dataType\": \"user\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"field\": \"country\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"UK\",\n" +
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
            "                      \"field\": \"preferred_car_models\",\n" +
            "                      \"comparatorType\": \"Contains\",\n" +
            "                      \"value\": \"Mazda\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"criteriaId\": \"42\",\n" +
            "      \"name\": \"purchase\",\n" +
            "      \"createdAt\": 1716560403912,\n" +
            "      \"updatedAt\": 1716560403912,\n" +
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
            "                      \"value\": \"keyboard\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"purchase\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"field\": \"shoppingCartItems.quantity\",\n" +
            "                      \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                      \"value\": \"3\",\n" +
            "                      \"fieldType\": \"long\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                      \"dataType\": \"purchase\",\n" +
            "                      \"field\": \"shoppingCartItems.price\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"10\",\n" +
            "                      \"fieldType\": \"long\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"criteriaId\": \"41\",\n" +
            "      \"name\": \"updateCart\",\n" +
            "      \"createdAt\": 1716560369947,\n" +
            "      \"updatedAt\": 1716560369947,\n" +
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
            "                      \"field\": \"eventName\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"updateCart\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"updateCart.updatedShoppingCartItems.price\",\n" +
            "                      \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                      \"value\": \"10\",\n" +
            "                      \"fieldType\": \"double\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                },\n" +
            "                \"minMatch\": 3,\n" +
            "                \"maxMatch\": 2\n" +
            "              },\n" +
            "              {\n" +
            "                \"dataType\": \"customEvent\",\n" +
            "                \"searchCombo\": {\n" +
            "                  \"combinator\": \"And\",\n" +
            "                  \"searchQueries\": [\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"eventName\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"updateCart\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"updateCart.updatedShoppingCartItems.quantity\",\n" +
            "                      \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                      \"value\": \"50\",\n" +
            "                      \"fieldType\": \"long\"\n" +
            "                    }\n" +
            "                  ]\n" +
            "                }\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"criteriaId\": \"40\",\n" +
            "      \"name\": \"Customevent\",\n" +
            "      \"createdAt\": 1716560323583,\n" +
            "      \"updatedAt\": 1716560323583,\n" +
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
            "                      \"field\": \"eventName\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"button-clicked\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                      \"dataType\": \"customEvent\",\n" +
            "                      \"field\": \"button-clicked.lastPageViewed\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"value\": \"signup page\",\n" +
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

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testCompareDataWithANDCombinatorFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":3.5,\\\"quantity\\\":6}]\",\"createdAt\":1700071052507,\"total\":4.67,\"dataType\":\"purchase\"}]");
        boolean result = evaluator.getMatchedCriteria(mockDataWithAnd, jsonArray) != null;
        assertFalse(result);
    }

    @Test
    public void testCompareDataWithANDCombinator() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":4.67,\\\"quantity\\\":3}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"purchase\"}]");
        boolean result = evaluator.getMatchedCriteria(mockDataWithAnd, jsonArray) != null;
        assertTrue(result);
    }

    @Test
    public void testCompareDataWithORCombinator() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":4.67,\\\"quantity\\\":3}]\",\"createdAt\":1700071052507,\"total\":2,\"eventType\":\"purchase\"}]");
        boolean result = evaluator.getMatchedCriteria(mockDataWithOr, jsonArray) != null;
        assertTrue(result);
    }

    @Test
    public void testCompareDataWithORCombinatorFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":3.5,\\\"quantity\\\":1}]\",\"createdAt\":1700071052507,\"total\":4.67,\"dataType\":\"purchase\"}]");
        boolean result = evaluator.getMatchedCriteria(mockDataWithOr, jsonArray) != null;
        assertFalse(result);
    }

    @Test
    public void testUserWithMockData() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"dataFields\":{\"country\":\"UK\"},\"eventType\":\"user\"}]");
        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testUserWithMockDataFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"dataFields\":{\"country\":\"US\"},\"eventType\":\"user\"}]");
        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testPurchaseWithMockData() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"keyboard\\\",\\\"price\\\":10,\\\"quantity\\\":2}]\",\"createdAt\":1700071052507,\"total\":2,\"eventType\":\"purchase\"}]");
        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testPurchaseWithMockDataFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"guitar\\\",\\\"price\\\":15,\\\"quantity\\\":2}]\",\"createdAt\":1700071052507,\"total\":2,\"eventType\":\"purchase\"}]");
        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testUpdateCartWithMockData() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":9,\\\"quantity\\\":52}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"cartUpdate\"}]");
        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testUpdateCartWithMockDataFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":9,\\\"quantity\\\":40}]\",\"createdAt\":1700071052507,\"total\":9,\"eventType\":\"cartUpdate\"}]");
        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testCustomEventWithMockData() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"eventName\":\"button-clicked\",\"dataFields\":{\"lastPageViewed\":\"signup page\"},\"createdAt\":1700071052507,\"eventType\":\"customEvent\"}]");

        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testCustomEventWithMockDataFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"eventName\":\"button-clicked\",\"dataFields\":{\"lastPageViewed\":\"login page\"},\"createdAt\":1700071052507,\"eventType\":\"customEvent\"}]");

        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testSingleItemMatchWithMockDataFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"piano\\\",\\\"price\\\":10,\\\"quantity\\\":2},{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"piano\\\",\\\"price\\\":5,\\\"quantity\\\":3}]\",\"createdAt\":1700071052507,\"total\":2,\"eventType\":\"purchase\"}]");

        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertFalse(result != null);
    }

    @Test
    public void testMinMatchWithMockData() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":10,\\\"quantity\\\":40}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"cartUpdate\"},{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":10,\\\"quantity\\\":40}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"cartUpdate\"},{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":10,\\\"quantity\\\":40}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"cartUpdate\"}]");

        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertTrue(result != null);
    }

    @Test
    public void testMinMatchWithMockDataFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":10,\\\"quantity\\\":40}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"cartUpdate\"},{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":10,\\\"quantity\\\":40}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"cartUpdate\"},{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":15,\\\"quantity\\\":40}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"cartUpdate\"}]");

        String result = evaluator.getMatchedCriteria(mockCriteria, jsonArray);
        assertFalse(result != null);
    }

}