package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MultiLevelNestedCriteriaWithArrayMatchTest {

    static final String mockDataNestedMultiLevelArrayTrackEvent = "{\n" +
            "             \"count\": 1,\n" +
            "             \"criteriaSets\": [\n" +
            "                    {\n" +
            "                         \"criteriaId\": \"459\",\n" +
            "                         \"name\": \"event a.h.b=d && a.h.c=g\",\n" +
            "                         \"createdAt\": 1727717997842,\n" +
            "                         \"updatedAt\": 1728024187962,\n" +
            "                         \"searchQuery\": {\n" +
            "                             \"combinator\": \"And\",\n" +
            "                             \"searchQueries\": [\n" +
            "                                 {\n" +
            "                                     \"combinator\": \"And\",\n" +
            "                                     \"searchQueries\": [\n" +
            "                                         {\n" +
            "                                             \"dataType\": \"customEvent\",\n" +
            "                                             \"searchCombo\": {\n" +
            "                                                 \"combinator\": \"And\",\n" +
            "                                                 \"searchQueries\": [\n" +
            "                                                     {\n" +
            "                                                         \"dataType\": \"customEvent\",\n" +
            "                                                         \"field\": \"TopLevelArrayObject.a.h.b\",\n" +
            "                                                         \"comparatorType\": \"Equals\",\n" +
            "                                                         \"value\": \"d\",\n" +
            "                                                         \"fieldType\": \"string\"\n" +
            "                                                     },\n" +
            "                                                     {\n" +
            "                                                         \"dataType\": \"customEvent\",\n" +
            "                                                         \"field\": \"TopLevelArrayObject.a.h.c\",\n" +
            "                                                         \"comparatorType\": \"Equals\",\n" +
            "                                                         \"value\": \"g\",\n" +
            "                                                         \"fieldType\": \"string\"\n" +
            "                                                     }\n" +
            "                                                 ]\n" +
            "                                             }\n" +
            "                                         }\n" +
            "                                     ]\n" +
            "                                 }\n" +
            "                             ]\n" +
            "                         }\n" +
            "                     }\n" +
            "                 ]\n" +
            "           }";

    static final String mockDataMultiLevelNestedWithArray = "{\n" +
            "  \"count\": 1,\n" +
            "  \"criteriaSets\": [\n" +
            "    {\n" +
            "      \"criteriaId\": \"436\",\n" +
            "      \"name\": \"Criteria 2.1 - 09252024 Bug Bash\",\n" +
            "      \"createdAt\": 1727286807360,\n" +
            "      \"updatedAt\": 1727445082036,\n" +
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
            "                      \"field\": \"furniture.material.type\",\n" +
            "                      \"comparatorType\": \"Contains\",\n" +
            "                      \"value\": \"table\",\n" +
            "                      \"fieldType\": \"string\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                      \"dataType\": \"user\",\n" +
            "                      \"field\": \"furniture.material.color\",\n" +
            "                      \"comparatorType\": \"Equals\",\n" +
            "                      \"values\": [\"black\"]\n" +
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
            "}\n";


    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }


    @Test
    public void testMultiLevelNestedWithArrayPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"furniture\": {\n" +
                "        \"material\": [\n" +
                "          {\n" +
                "            \"type\": \"table\",\n" +
                "            \"color\": \"black\",\n" +
                "            \"lengthInches\": 40,\n" +
                "            \"widthInches\": 60\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"Sofa\",\n" +
                "            \"color\": \"Gray\",\n" +
                "            \"lengthInches\": 20,\n" +
                "            \"widthInches\": 30\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]\n";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMultiLevelNestedWithArray, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testMultiLevelNestedWithArrayFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"furniture\": {\n" +
                "        \"material\": [\n" +
                "          {\n" +
                "            \"type\": \"table\",\n" +
                "            \"color\": \"gray\",\n" +
                "            \"lengthInches\": 40,\n" +
                "            \"widthInches\": 60\n" +
                "          },\n" +
                "          {\n" +
                "            \"type\": \"Sofa\",\n" +
                "            \"color\": \"black\",\n" +
                "            \"lengthInches\": 20,\n" +
                "            \"widthInches\": 30\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]\n";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataMultiLevelNestedWithArray, jsonArray);
        assertNull(result);
    }

    @Test
    public void testNestedMultiLevelArrayTrackEventPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"eventName\": \"TopLevelArrayObject\",\n" +
                "    \"dataFields\": {\n" +
                "      \"a\": {\n" +
                "        \"h\": [\n" +
                "          {\n" +
                "            \"b\": \"e\",\n" +
                "            \"c\": \"h\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"b\": \"d\",\n" +
                "            \"c\": \"g\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataNestedMultiLevelArrayTrackEvent, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void testNestedMultiLevelArrayTrackEventFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"eventName\": \"TopLevelArrayObject\",\n" +
                "    \"dataFields\": {\n" +
                "      \"a\": {\n" +
                "        \"h\": [\n" +
                "          {\n" +
                "            \"b\": \"d\",\n" +
                "            \"c\": \"h\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"b\": \"e\",\n" +
                "            \"c\": \"g\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  }\n" +
                "]";
        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(mockDataNestedMultiLevelArrayTrackEvent, jsonArray);
        assertNull(result);
    }
}
