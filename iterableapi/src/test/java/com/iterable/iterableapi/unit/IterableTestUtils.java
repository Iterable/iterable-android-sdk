package com.iterable.iterableapi.unit;

import com.iterable.iterableapi.IterableApi;

import org.robolectric.RuntimeEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IterableTestUtils {
    public static void createIterableApi() {
        IterableApi.sharedInstanceWithApiKey(RuntimeEnvironment.application, "fake_key", "test_email");
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
}
