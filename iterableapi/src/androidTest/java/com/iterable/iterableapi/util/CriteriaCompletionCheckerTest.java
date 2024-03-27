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
            "   \"criteriaList\":[\n" +
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
            "   \"criteriaList\":[\n" +
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

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testCompareDataWithANDCombinatorFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":3.5,\\\"quantity\\\":6}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"purchase\"}]");
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
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":3.5,\\\"quantity\\\":3}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"purchase\"}]");
        boolean result = evaluator.getMatchedCriteria(mockDataWithOr, jsonArray) != null;
        assertTrue(result);
    }

    @Test
    public void testCompareDataWithORCombinatorFail() throws Exception {
        JSONArray jsonArray = new JSONArray("[{\"items\":\"[{\\\"id\\\":\\\"12\\\",\\\"name\\\":\\\"Mocha\\\",\\\"price\\\":3.5,\\\"quantity\\\":1}]\",\"createdAt\":1700071052507,\"total\":4.67,\"eventType\":\"purchase\"}]");
        boolean result = evaluator.getMatchedCriteria(mockDataWithOr, jsonArray) != null;
        assertFalse(result);
    }
}