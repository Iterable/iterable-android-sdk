package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(TestRunner.class)
public class IterableApiDeeplinkUnitTest {

    @Test
    public void testIsIterableDeeplinkReturnsTrueForValidDeeplink() {
        assertTrue(IterableApi.isIterableDeeplink("https://links.iterable.com/a/abc123"));
    }

    @Test
    public void testIsIterableDeeplinkReturnsFalseForNonRewriteLink() {
        assertFalse(IterableApi.isIterableDeeplink("https://links.iterable.com/u/60402396fbd5433eb35397b47ab2fb83"));
    }

    @Test
    public void testIsIterableDeeplinkReturnsFalseForNonIterableLink() {
        assertFalse(IterableApi.isIterableDeeplink("https://example.com/some/path"));
    }

    @Test
    public void testIsIterableDeeplinkReturnsFalseForNull() {
        assertFalse(IterableApi.isIterableDeeplink(null));
    }

    @Test
    public void testIsIterableDeeplinkReturnsFalseForEmptyString() {
        assertFalse(IterableApi.isIterableDeeplink(""));
    }
}
