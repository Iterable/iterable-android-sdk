package com.iterable.iterableapi.unit;

import com.iterable.iterableapi.IterableHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * IterableConstants tests the functionality in IterableHelper
 */
public class IterableHelperUnitTest {

    @Test
    public void actionHandlerCallback() throws Exception {
        final String resultString = "testString";

        IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler(){
            @Override
            public void execute(String result) {
                assertEquals(result, resultString);
            }
        };
        clickCallback.execute(resultString);
    }
}