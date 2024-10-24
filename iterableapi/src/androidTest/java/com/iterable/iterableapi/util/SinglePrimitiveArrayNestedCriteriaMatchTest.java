package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SinglePrimitiveArrayNestedCriteriaMatchTest {

    static final String mockDataArrayNested = "{\n" +
            "    \"count\": 1,\n" +
            "    \"criteriaSets\": [\n" +
            "        {\n" +
            "            \"criteriaId\": \"467\",\n" +
            "            \"name\": \"Custom event - single primitive\",\n" +
            "            \"createdAt\": 1728166585122,\n" +
            "            \"updatedAt\": 1729581351423,\n" +
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
            "                                            \"value\": \"animal_found\",\n" +
            "                                            \"fieldType\": \"string\"\n" +
            "                                        },\n" +
            "                                        {\n" +
            "                                            \"dataType\": \"customEvent\",\n" +
            "                                            \"field\": \"animal_found.count\",\n" +
            "                                            \"comparatorType\": \"DoesNotEqual\",\n" +
            "                                            \"value\": \"4\",\n" +
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


    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testSinglePrimitiveArrayNestedCriteriaMatchPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "          \"count\": [5,8,9]\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\",\n" +
                "    \"eventName\": \"animal_found\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayNested, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testSinglePrimitiveArrayNestedCriteriaMatchFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "          \"count\": [4, 8, 9]\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\",\n" +
                "    \"eventName\": \"animal_found\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataArrayNested, jsonArray);
        assertNull(result);
    }
}
