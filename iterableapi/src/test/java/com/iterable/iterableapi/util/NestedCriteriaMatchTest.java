package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NestedCriteriaMatchTest {

    static final String mockDataNested = " {\n" +
            "         \"count\": 1,\n" +
            "         \"criteriaSets\": [\n" +
            "           {\n" +
            "               \"criteriaId\": \"168\",\n" +
            "               \"name\": \"nested testing\",\n" +
            "               \"createdAt\": 1721251169153,\n" +
            "               \"updatedAt\": 1723488175352,\n" +
            "               \"searchQuery\": {\n" +
            "                   \"combinator\": \"And\",\n" +
            "                   \"searchQueries\": [\n" +
            "                       {\n" +
            "                           \"combinator\": \"And\",\n" +
            "                           \"searchQueries\": [\n" +
            "                               {\n" +
            "                                   \"dataType\": \"user\",\n" +
            "                                   \"searchCombo\": {\n" +
            "                                       \"combinator\": \"And\",\n" +
            "                                       \"searchQueries\": [\n" +
            "                                           {\n" +
            "                                               \"dataType\": \"user\",\n" +
            "                                               \"field\": \"furniture\",\n" +
            "                                               \"comparatorType\": \"IsSet\",\n" +
            "                                               \"value\": \"\",\n" +
            "                                               \"fieldType\": \"nested\"\n" +
            "                                           },\n" +
            "                                           {\n" +
            "                                               \"dataType\": \"user\",\n" +
            "                                               \"field\": \"furniture.furnitureColor\",\n" +
            "                                               \"comparatorType\": \"Equals\",\n" +
            "                                               \"value\": \"White\",\n" +
            "                                               \"fieldType\": \"string\"\n" +
            "                                           },\n" +
            "                                           {\n" +
            "                                               \"dataType\": \"user\",\n" +
            "                                               \"field\": \"furniture.furnitureType\",\n" +
            "                                               \"comparatorType\": \"Equals\",\n" +
            "                                               \"value\": \"Sofa\",\n" +
            "                                               \"fieldType\": \"string\"\n" +
            "                                           }\n" +
            "                                       ]\n" +
            "                                   }\n" +
            "                               }\n" +
            "                           ]\n" +
            "                       }\n" +
            "                   ]\n" +
            "               }\n" +
            "           }\n" +
            "         ]\n" +
            "      }";


    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    @Test
    public void testNestedPass() throws Exception {
        String jsonString = "[\n" +
                "          {\n" +
                "            \"dataFields\": {\n" +
                "              \"furniture\": [\n" +
                "                {\n" +
                "                  \"furnitureType\": \"Sofa\",\n" +
                "                  \"furnitureColor\": \"White\",\n" +
                "                  \"lengthInches\": 40,\n" +
                "                  \"widthInches\": 60\n" +
                "                },\n" +
                "                {\n" +
                "                  \"furnitureType\": \"Table\",\n" +
                "                  \"furnitureColor\": \"Gray\",\n" +
                "                  \"lengthInches\": 20,\n" +
                "                  \"widthInches\": 30\n" +
                "                }\n" +
                "              ]\n" +
                "            },\n" +
                "            \"eventType\": \"user\"\n" +
                "          }\n" +
                "        ]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataNested, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testNestedFail() throws Exception {
        String jsonString = "[\n" +
                "          {\n" +
                "            \"dataFields\": {\n" +
                "              \"furniture\": [\n" +
                "                {\n" +
                "                  \"furnitureType\": \"Sofa\",\n" +
                "                  \"furnitureColor\": \"Gray\",\n" +
                "                  \"lengthInches\": 40,\n" +
                "                  \"widthInches\": 60\n" +
                "                },\n" +
                "                {\n" +
                "                  \"furnitureType\": \"Table\",\n" +
                "                  \"furnitureColor\": \"White\",\n" +
                "                  \"lengthInches\": 20,\n" +
                "                  \"widthInches\": 30\n" +
                "                }\n" +
                "              ]\n" +
                "            },\n" +
                "            \"eventType\": \"user\"\n" +
                "          }\n" +
                "        ]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataNested, jsonArray);
        assertNull(result);
    }
}
