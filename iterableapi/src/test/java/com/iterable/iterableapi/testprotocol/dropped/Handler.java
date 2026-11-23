package com.iterable.iterableapi.testprotocol.dropped;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

// URL looks this class up by name through java.protocol.handler.pkgs, hence the package and class name.
public class Handler extends URLStreamHandler {
    public static final String PACKAGES = "com.iterable.iterableapi.testprotocol";

    @Override
    protected URLConnection openConnection(URL url) {
        return new HttpURLConnection(url) {
            @Override
            public OutputStream getOutputStream() throws IOException {
                throw failureFor(url.getHost());
            }

            @Override
            public int getResponseCode() throws IOException {
                throw failureFor(url.getHost());
            }

            @Override
            public void connect() { }

            @Override
            public void disconnect() { }

            @Override
            public boolean usingProxy() {
                return false;
            }
        };
    }

    private static IOException failureFor(String host) {
        switch (host) {
            case "eof":
                return new EOFException("\\n not found: limit=0 content=…");
            case "eof-cause":
                return new IOException("unexpected end of stream on http://api.iterable.com/...", new EOFException("\\n not found"));
            case "reset-cause":
                return new IOException("unexpected end of stream", new SocketException("Connection reset"));
            default:
                return new IOException("stream closed");
        }
    }
}
