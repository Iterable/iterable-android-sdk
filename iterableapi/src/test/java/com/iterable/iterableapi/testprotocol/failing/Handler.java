package com.iterable.iterableapi.testprotocol.failing;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

// URL looks this class up by name through java.protocol.handler.pkgs, hence the package and class name.
// The host picks the failure: connect-timeout fails to connect, read-timeout and dropped fail after the request is sent.
public class Handler extends URLStreamHandler {
    public static final String PACKAGES = "com.iterable.iterableapi.testprotocol";

    @Override
    protected URLConnection openConnection(URL url) {
        String host = url.getHost();
        return new HttpURLConnection(url) {
            @Override
            public void connect() throws IOException {
                if (host.equals("connect-timeout")) {
                    throw new SocketTimeoutException("connect timed out");
                }
            }

            @Override
            public OutputStream getOutputStream() {
                return new ByteArrayOutputStream();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                connect();
                if (host.equals("read-timeout")) {
                    throw new SocketTimeoutException("Read timed out");
                }
                // What OkHttp throws on devices for a dropped connection.
                throw new IOException("unexpected end of stream on http://api.iterable.com/...", new EOFException("\\n not found"));
            }

            @Override
            public void disconnect() { }

            @Override
            public boolean usingProxy() {
                return false;
            }
        };
    }
}
