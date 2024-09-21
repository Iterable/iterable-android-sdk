package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IsOneOfAndIsNotOneOfCriteriaMatchTest {

    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    static final String mockDataCriteriaIsOneOf = "{\n" +
            "    \"count\": 5,\n" +
            "    \"criteriaSets\": [\n" +
            "      {\n" +
            "            \"criteriaId\": \"299\",\n" +
            "            \"name\": \"Criteria_Is_One_of\",\n" +
            "            \"createdAt\": 1722851586508,\n" +
            "            \"updatedAt\": 1724404229481,\n" +
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
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"values\": [\n" +
            "                                                \"China\",\n" +
            "                                                \"Japan\",\n" +
            "                                                \"Kenya\"\n" +
            "                                            ]\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"addresses\",\n" +
            "                                            \"comparatorType\": \"Equals\",\n" +
            "                                            \"values\": [\n" +
            "                                                \"JP\",\n" +
            "                                                \"DE\",\n" +
            "                                                \"GB\"\n" +
            "                                            ]\n" +
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
    public void testIsOneOfPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": \"China\",\n" +
                "            \"addresses\": [\"US\",\"UK\",\"JP\",\"GB\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCriteriaIsOneOf, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testIsOneOfFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": \"Korea\",\n" +
                "            \"addresses\": [\"US\", \"UK\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCriteriaIsOneOf, jsonArray);
        assertNull(result);
    }

    static final String mockDataCriteriaIsNotOneOf = "{\n" +
            "    \"count\": 5,\n" +
            "    \"criteriaSets\": [\n" +
            "      {\n" +
            "            \"criteriaId\": \"299\",\n" +
            "            \"name\": \"Criteria_IsNonOf\",\n" +
            "            \"createdAt\": 1722851586508,\n" +
            "            \"updatedAt\": 1724404229481,\n" +
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
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"values\": [\n" +
            "                                                \"China\",\n" +
            "                                                \"Japan\",\n" +
            "                                                \"Kenya\"\n" +
            "                                            ]\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"user\",\n" +
            "                                            \"field\": \"addresses\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"values\": [\n" +
            "                                                \"JP\",\n" +
            "                                                \"DE\",\n" +
            "                                                \"GB\"\n" +
            "                                            ]\n" +
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
    public void testIsNotOneOfPass() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": \"Korea\",\n" +
                "            \"addresses\": [\"US\", \"UK\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCriteriaIsNotOneOf, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testIsNotOneOfFail() throws Exception {
        String jsonString = "[\n" +
                "    {\n" +
                "        \"dataFields\": {\n" +
                "            \"country\": \"China\",\n" +
                "            \"addresses\": [\"US\",\"UK\",\"JP\",\"GB\"]\n" +
                "        },\n" +
                "        \"eventType\": \"user\"\n" +
                "    }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataCriteriaIsNotOneOf, jsonArray);
        assertNull(result);
    }
}
