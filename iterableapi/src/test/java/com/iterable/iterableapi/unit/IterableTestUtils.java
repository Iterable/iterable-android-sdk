package com.iterable.iterableapi.unit;

import android.os.Bundle;
import android.util.Log;

import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.robolectric.RuntimeEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IterableTestUtils {
    public static final String apiKey = "fake_key";
    public static final String userEmail = "test_email";

    public interface ConfigBuilderExtender {
        IterableConfig.Builder run(IterableConfig.Builder builder);
    }


    public static void createIterableApi() {
        IterableApi.sharedInstanceWithApiKey(RuntimeEnvironment.application, apiKey, userEmail);
    }

    public static void createIterableApiNew() {
        createIterableApiNew(null);
    }

    public static void createIterableApiNew(ConfigBuilderExtender extender) {
        IterableConfig.Builder builder = new IterableConfig.Builder()
                .setAutoPushRegistration(false);

        if (extender != null) {
            builder = extender.run(builder);
        }

        IterableApi.initialize(RuntimeEnvironment.application, apiKey, builder.build());
        IterableApi.getInstance().setEmail(userEmail);
    }

    public static String getResourceString(String fileName) throws IOException {
        InputStream inputStream = IterableTestUtils.class.getClassLoader().getResourceAsStream(fileName);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String receiveString = "";
        StringBuilder stringBuilder = new StringBuilder();

        while ( (receiveString = bufferedReader.readLine()) != null ) {
            stringBuilder.append(receiveString);
        }

        inputStream.close();
        return stringBuilder.toString();
    }

    public static Bundle getBundleFromJsonResource(String fileName) throws IOException, JSONException {
        String jsonString = getResourceString(fileName);
        return jsonToBundle(new JSONObject(jsonString));
    }

    public static Map<String, String> getMapFromJsonResource(String fileName) throws IOException, JSONException {
        Bundle bundle = getBundleFromJsonResource(fileName);
        return bundleToMap(bundle);
    }

    public static Bundle jsonToBundle(JSONObject jsonObject) throws JSONException {
        Bundle bundle = new Bundle();
        for(Iterator<String> iterator = jsonObject.keys(); iterator.hasNext(); ){
            String key = iterator.next();
            Object value = jsonObject.get(key);
            bundle.putString(key, value.toString());
        }
        return bundle;
    }

    public static Map<String, String> bundleToMap(Bundle bundle) {
        Map<String, String> map = new HashMap<>();
        for (String key : bundle.keySet()) {
            map.put(key, bundle.get(key).toString());
        }
        return map;
    }
}
