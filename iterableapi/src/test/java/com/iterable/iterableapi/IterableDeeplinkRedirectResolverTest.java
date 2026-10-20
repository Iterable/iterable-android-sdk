package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(TestRunner.class)
public class IterableDeeplinkRedirectResolverTest {
    @Test
    public void connectTimeoutReturnsOriginalUrlAndReleasesConnection()
            throws Exception {
        TimeoutConnection connection =
                new TimeoutConnection(new URL("https://example.com/a/link"));
        IterableDeeplinkRedirectResolver resolver =
                new IterableDeeplinkRedirectResolver(url -> connection);

        IterableDeeplinkRedirectResult result =
                resolver.resolve("https://example.com/a/link");

        assertEquals("https://example.com/a/link", result.url);
        assertEquals(
                IterableDeeplinkRedirectResolver.DEFAULT_TIMEOUT_MS,
                connection.getConnectTimeout()
        );
        assertEquals(
                IterableDeeplinkRedirectResolver.DEFAULT_TIMEOUT_MS,
                connection.getReadTimeout()
        );
        assertTrue(connection.disconnected);
    }

    private static final class TimeoutConnection extends HttpURLConnection {
        boolean disconnected;

        TimeoutConnection(URL url) {
            super(url);
        }

        @Override
        public int getResponseCode() throws IOException {
            throw new SocketTimeoutException("connect timed out");
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }
    }
}
