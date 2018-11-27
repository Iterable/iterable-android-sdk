package com.iterable.iterableapi;

import android.app.Application;
import android.test.ApplicationTestCase;

import org.json.JSONObject;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class IterableInAppManagerUnitTest extends ApplicationTestCase<Application> {
    public IterableInAppManagerUnitTest() {
        super(Application.class);
    }

    @Override
    public void setUp() {
        createApplication();
    }

    @Override
    public void tearDown() throws Exception {

    }

    public void testGetNextMessageNull() throws Exception {
        JSONObject message = IterableInAppMessage.getNextMessageFromPayload(null);
        assertEquals(null, message);
    }

    public void testGetNextMessageEmptyString() throws Exception {
        String payload = "";
        JSONObject message = IterableInAppMessage.getNextMessageFromPayload("");
        assertEquals(null, message);
    }

    public void testTestGetNextMessageEmptyPayload() throws Exception {
        String payload =
                "{ \"inAppMessages\": [] }";
        String p = "{\"inAppMessages\":[]}";
        JSONObject message = IterableInAppMessage.getNextMessageFromPayload(p);
        assertEquals(null, message);
    }

    public void testGetNextMessageEmptyPayload() throws Exception {
        String payload =
                "{ \"inAppMessages\": [] }";
        JSONObject message = IterableInAppMessage.getNextMessageFromPayload(payload);
        assertEquals(null, message);
    }

    public void testGetNextMessage() throws Exception {
        String payload =
                "{\n" +
                        "  \"inAppMessages\": [\n" +
                        "    {\n" +
                        "      \"messageId\": \"a6ea265965154f2cadb4774cb68a40ad\",\n" +
                        "      \"campaignId\": 10,\n" +
                        "      \"content\": {\n" +
                        "        \"id\": 15,\n" +
                        "        \"createdAt\": 1481820621154,\n" +
                        "        \"templateId\": 18,\n" +
                        "        \"creatorUserId\": \"dt@iterable.com\",\n" +
                        "        \"displayType\": \"Full\",\n" +
                        "        \"title\": {\n" +
                        "          \"text\": \"SPOTIFY\",\n" +
                        "          \"color\": \"#ffffff\"\n" +
                        "        },\n" +
                        "        \"body\": {\n" +
                        "          \"text\": \"NEW IN-APP NOTIFICATIONS\",\n" +
                        "          \"color\": \"#e3e3e3\"\n" +
                        "        },\n" +
                        "        \"backgroundColor\": \"#000000\",\n" +
                        "        \"mainImage\": \"http://www.freeiconspng.com/uploads/spotify-icon-3.png\",\n" +
                        "        \"buttons\": [\n" +
                        "          {\n" +
                        "            \"content\": {\n" +
                        "              \"text\": \"OK\",\n" +
                        "              \"font\": \"Inconsolata\",\n" +
                        "              \"color\": \"#ffffff\"\n" +
                        "            },\n" +
                        "            \"backgroundColor\": \"#333333\",\n" +
                        "            \"borderRadius\": 0,\n" +
                        "            \"action\": \"spotify://newFeature\"\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"content\": {\n" +
                        "              \"text\": \"Cancel\",\n" +
                        "              \"font\": \"Inconsolata\",\n" +
                        "              \"color\": \"#ffffff\"\n" +
                        "            },\n" +
                        "            \"backgroundColor\": \"#333333\",\n" +
                        "            \"borderRadius\": 0,\n" +
                        "            \"action\": \"spotify://...\"\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"messageId\": \"26ea265965154f2cadb4774cb68a40ad\",\n" +
                        "      \"campaignId\": 111,\n" +
                        "      \"content\": {\n" +
                        "        \"id\": 16,\n" +
                        "        \"createdAt\": 1481820621154,\n" +
                        "        \"templateId\": 18,\n" +
                        "        \"creatorUserId\": \"dt@iterable.com\",\n" +
                        "        \"displayType\": \"Full\",\n" +
                        "        \"title\": {\n" +
                        "          \"text\": \"SPOTIFY\",\n" +
                        "          \"color\": \"#ffffff\"\n" +
                        "        },\n" +
                        "        \"body\": {\n" +
                        "          \"text\": \"NEW IN-APP NOTIFICATIONS\",\n" +
                        "          \"color\": \"#e3e3e3\"\n" +
                        "        },\n" +
                        "        \"backgroundColor\": \"#000000\",\n" +
                        "        \"mainImage\": \"http://www.freeiconspng.com/uploads/spotify-icon-3.png\",\n" +
                        "        \"buttons\": [\n" +
                        "          {\n" +
                        "            \"content\": {\n" +
                        "              \"text\": \"OK\",\n" +
                        "              \"font\": \"Inconsolata\",\n" +
                        "              \"color\": \"#ffffff\"\n" +
                        "            },\n" +
                        "            \"backgroundColor\": \"#333333\",\n" +
                        "            \"borderRadius\": 0,\n" +
                        "            \"action\": \"spotify://newFeature\"\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"content\": {\n" +
                        "              \"text\": \"Cancel\",\n" +
                        "              \"font\": \"Inconsolata\",\n" +
                        "              \"color\": \"#ffffff\"\n" +
                        "            },\n" +
                        "            \"backgroundColor\": \"#333333\",\n" +
                        "            \"borderRadius\": 0,\n" +
                        "            \"action\": \"spotify://...\"\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        JSONObject result = IterableInAppMessage.getNextMessageFromPayload(payload);

        String expectedPayloadString =
                    "{\n" +
                        "      \"messageId\": \"a6ea265965154f2cadb4774cb68a40ad\",\n" +
                        "      \"campaignId\": 10,\n" +
                        "      \"content\": {\n" +
                        "        \"id\": 15,\n" +
                        "        \"createdAt\": 1481820621154,\n" +
                        "        \"templateId\": 18,\n" +
                        "        \"creatorUserId\": \"dt@iterable.com\",\n" +
                        "        \"displayType\": \"Full\",\n" +
                        "        \"title\": {\n" +
                        "          \"text\": \"SPOTIFY\",\n" +
                        "          \"color\": \"#ffffff\"\n" +
                        "        },\n" +
                        "        \"body\": {\n" +
                        "          \"text\": \"NEW IN-APP NOTIFICATIONS\",\n" +
                        "          \"color\": \"#e3e3e3\"\n" +
                        "        },\n" +
                        "        \"backgroundColor\": \"#000000\",\n" +
                        "        \"mainImage\": \"http://www.freeiconspng.com/uploads/spotify-icon-3.png\",\n" +
                        "        \"buttons\": [\n" +
                        "          {\n" +
                        "            \"content\": {\n" +
                        "              \"text\": \"OK\",\n" +
                        "              \"font\": \"Inconsolata\",\n" +
                        "              \"color\": \"#ffffff\"\n" +
                        "            },\n" +
                        "            \"backgroundColor\": \"#333333\",\n" +
                        "            \"borderRadius\": 0,\n" +
                        "            \"action\": \"spotify://newFeature\"\n" +
                        "          },\n" +
                        "          {\n" +
                        "            \"content\": {\n" +
                        "              \"text\": \"Cancel\",\n" +
                        "              \"font\": \"Inconsolata\",\n" +
                        "              \"color\": \"#ffffff\"\n" +
                        "            },\n" +
                        "            \"backgroundColor\": \"#333333\",\n" +
                        "            \"borderRadius\": 0,\n" +
                        "            \"action\": \"spotify://...\"\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    }";
        JSONObject expectedResult = new JSONObject(expectedPayloadString);

        assertEquals(expectedResult.toString(), result.toString());
    }
}

