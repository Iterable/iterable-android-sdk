package com.iterable.iterableapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CombinationComplexCriteriaCheckerTest {

    private CriteriaCompletionChecker evaluator;

    @Before
    public void setUp() {
        evaluator = new CriteriaCompletionChecker();
    }

    private final String complexCriteria1 = "{\n" +
            "         \"count\": 1,\n" +
            "         \"criteriaSets\": [\n" +
            "           {\n" +
            "             \"criteriaId\": \"290\",\n" +
            "             \"name\": \"Complex Criteria Unit Test #1\",\n" +
            "             \"createdAt\": 1722532861551,\n" +
            "             \"updatedAt\": 1722532861551,\n" +
            "             \"searchQuery\": {\n" +
            "               \"combinator\": \"And\",\n" +
            "               \"searchQueries\": [\n" +
            "                 {\n" +
            "                   \"combinator\": \"Or\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"firstName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"A\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"firstName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"B\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"firstName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"C\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 },\n" +
            "                 {\n" +
            "                   \"combinator\": \"And\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"customEvent\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"eventName\",\n" +
            "                             \"comparatorType\": \"IsSet\",\n" +
            "                             \"value\": \"\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           },\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"saved_cars.color\",\n" +
            "                             \"comparatorType\": \"IsSet\",\n" +
            "                             \"value\": \"\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"customEvent\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"eventName\",\n" +
            "                             \"comparatorType\": \"IsSet\",\n" +
            "                             \"value\": \"\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           },\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"animal-found.vaccinated\",\n" +
            "                             \"comparatorType\": \"Equals\",\n" +
            "                             \"value\": \"true\",\n" +
            "                             \"fieldType\": \"boolean\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 },\n" +
            "                 {\n" +
            "                   \"combinator\": \"Not\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"purchase\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"purchase\",\n" +
            "                             \"field\": \"total\",\n" +
            "                             \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                             \"value\": \"100\",\n" +
            "                             \"fieldType\": \"double\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"purchase\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"purchase\",\n" +
            "                             \"field\": \"reason\",\n" +
            "                             \"comparatorType\": \"Equals\",\n" +
            "                             \"value\": \"testing\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 }\n" +
            "               ]\n" +
            "             }\n" +
            "           }\n" +
            "         ]\n" +
            "       }";

    @Test
    public void complexCriteria1TestPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"reason\": \"new\",\n" +
                "      \"total\": 10\n" +
                "    },\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"vaccinated\": true\n" +
                "    },\n" +
                "    \"eventName\": \"animal-found\",\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"color\": \"black\"\n" +
                "    },\n" +
                "    \"eventName\": \"saved_cars\",\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"Adam\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria1, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void complexCriteria1TestFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"reason\": \"new\",\n" +
                "      \"total\": 10\n" +
                "    },\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"vaccinated\": true\n" +
                "    },\n" +
                "    \"eventName\": \"animal-found\",\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"color\": \"\"\n" +
                "    },\n" +
                "    \"eventName\": \"saved_cars\",\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"Adam\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria1, jsonArray);
        assertNull(result);
    }

    private final String complexCriteria2 = "{\n" +
            "         \"count\": 1,\n" +
            "         \"criteriaSets\": [\n" +
            "           {\n" +
            "             \"criteriaId\": \"291\",\n" +
            "             \"name\": \"Complex Criteria Unit Test #2\",\n" +
            "             \"createdAt\": 1722533473263,\n" +
            "             \"updatedAt\": 1722533473263,\n" +
            "             \"searchQuery\": {\n" +
            "               \"combinator\": \"Or\",\n" +
            "               \"searchQueries\": [\n" +
            "                 {\n" +
            "                   \"combinator\": \"Not\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"firstName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"A\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"firstName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"B\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"firstName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"C\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 },\n" +
            "                 {\n" +
            "                   \"combinator\": \"And\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"customEvent\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"eventName\",\n" +
            "                             \"comparatorType\": \"IsSet\",\n" +
            "                             \"value\": \"\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           },\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"saved_cars.color\",\n" +
            "                             \"comparatorType\": \"IsSet\",\n" +
            "                             \"value\": \"\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"customEvent\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"animal-found.vaccinated\",\n" +
            "                             \"comparatorType\": \"Equals\",\n" +
            "                             \"value\": \"true\",\n" +
            "                             \"fieldType\": \"boolean\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 },\n" +
            "                 {\n" +
            "                   \"combinator\": \"Or\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"purchase\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"purchase\",\n" +
            "                             \"field\": \"total\",\n" +
            "                             \"comparatorType\": \"GreaterThanOrEqualTo\",\n" +
            "                             \"value\": \"100\",\n" +
            "                             \"fieldType\": \"double\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"purchase\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"purchase\",\n" +
            "                             \"field\": \"reason\",\n" +
            "                             \"comparatorType\": \"DoesNotEqual\",\n" +
            "                             \"value\": \"gift\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 }\n" +
            "               ]\n" +
            "             }\n" +
            "           }\n" +
            "         ]\n" +
            "       }";

    @Test
    public void complexCriteria2TestPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"reason\": \"new\",\n" +
                "      \"total\": 110\n" +
                "    },\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"vaccinated\": true\n" +
                "    },\n" +
                "    \"eventName\": \"animal-found\",\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"color\": \"black\"\n" +
                "    },\n" +
                "    \"eventName\": \"saved_cars\",\n" +
                "    \"eventType\": \"customEvent\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"Xcode\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria2, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void complexCriteria2TestFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"reason\": \"gift\",\n" +
                "      \"total\": 10\n" +
                "    },\n" +
                "    \"eventType\": \"purchase\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"Alex\"\n" +
                "    },\n" +
                "    \"eventType\": \"user\"\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria2, jsonArray);
        assertNull(result);
    }

    private final String complexCriteria3 = "{\n" +
            "         \"count\": 1,\n" +
            "         \"criteriaSets\": [\n" +
            "           {\n" +
            "             \"criteriaId\": \"292\",\n" +
            "             \"name\": \"Complex Criteria Unit Test #3\",\n" +
            "             \"createdAt\": 1722533789589,\n" +
            "             \"updatedAt\": 1722533838989,\n" +
            "             \"searchQuery\": {\n" +
            "               \"combinator\": \"Not\",\n" +
            "               \"searchQueries\": [\n" +
            "                 {\n" +
            "                   \"combinator\": \"And\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"firstName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"A\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"lastName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"A\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 },\n" +
            "                 {\n" +
            "                   \"combinator\": \"Or\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"user\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"user\",\n" +
            "                             \"field\": \"firstName\",\n" +
            "                             \"comparatorType\": \"StartsWith\",\n" +
            "                             \"value\": \"C\",\n" +
            "                             \"fieldType\": \"string\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"customEvent\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"animal-found.vaccinated\",\n" +
            "                             \"comparatorType\": \"Equals\",\n" +
            "                             \"value\": \"false\",\n" +
            "                             \"fieldType\": \"boolean\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"customEvent\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"customEvent\",\n" +
            "                             \"field\": \"animal-found.count\",\n" +
            "                             \"comparatorType\": \"LessThan\",\n" +
            "                             \"value\": \"5\",\n" +
            "                             \"fieldType\": \"long\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 },\n" +
            "                 {\n" +
            "                   \"combinator\": \"Not\",\n" +
            "                   \"searchQueries\": [\n" +
            "                     {\n" +
            "                       \"dataType\": \"purchase\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"purchase\",\n" +
            "                             \"field\": \"total\",\n" +
            "                             \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                             \"value\": \"10\",\n" +
            "                             \"fieldType\": \"double\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     },\n" +
            "                     {\n" +
            "                       \"dataType\": \"purchase\",\n" +
            "                       \"searchCombo\": {\n" +
            "                         \"combinator\": \"And\",\n" +
            "                         \"searchQueries\": [\n" +
            "                           {\n" +
            "                             \"dataType\": \"purchase\",\n" +
            "                             \"field\": \"shoppingCartItems.quantity\",\n" +
            "                             \"comparatorType\": \"LessThanOrEqualTo\",\n" +
            "                             \"value\": \"34\",\n" +
            "                             \"fieldType\": \"long\"\n" +
            "                           }\n" +
            "                         ]\n" +
            "                       }\n" +
            "                     }\n" +
            "                   ]\n" +
            "                 }\n" +
            "               ]\n" +
            "             }\n" +
            "           }\n" +
            "         ]\n" +
            "       }";


    @Test
    public void complexCriteria3TestPass() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataType\": \"purchase\",\n" +
                "    \"createdAt\": 1699246745093,\n" +
                "    \"items\": [{\n" +
                "      \"id\": \"12\",\n" +
                "      \"name\": \"coffee\",\n" +
                "      \"price\": \"100\",\n" +
                "      \"quantity\": \"2\",\n" +
                "    }]\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria3, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void complexCriteria3TestPass2() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataType\": \"purchase\",\n" +
                "    \"createdAt\": 1699246745067,\n" +
                "    \"items\": [{\n" +
                "      \"id\": \"13\",\n" +
                "      \"name\": \"kittens\",\n" +
                "      \"price\": \"2\",\n" +
                "      \"quantity\": \"2\",\n" +
                "    }]\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria3, jsonArray);
        assertNotNull(result);
    }

    @Test
    public void complexCriteria3TestFail() throws Exception {
        String jsonString = "[\n" +
                "  {\n" +
                "    \"dataType\": \"purchase\",\n" +
                "    \"createdAt\": 1699246745093,\n" +
                "    \"items\": [{\n" +
                "      \"id\": \"12\",\n" +
                "      \"name\": \"coffee\",\n" +
                "      \"price\": \"100\",\n" +
                "      \"quantity\": \"2\",\n" +
                "    }]\n" +
                "  }\n" +
                "  {\n" +
                "    \"dataType\": \"user\",\n" +
                "    \"dataFields\": {\n" +
                "      \"firstName\": \"Alex\",\n" +
                "      \"lastName\": \"Aris\",\n" +
                "    }\n" +
                "  }\n" +
                "]";

        JSONArray jsonArray = new JSONArray(jsonString);
        String result = evaluator.getMatchedCriteria(complexCriteria3, jsonArray);
        assertNull(result);
    }
}
