package com.iterable.iterableapi;

public enum IterableDataRegion {
    US("https://api.iterable.com/api/", "https://app.iterable.com"),
    EU("https://api.eu.iterable.com/api/", "https://app.eu.iterable.com");

    private final String endpoint;
    private final String webAppBaseUrl;

    IterableDataRegion(String endpoint, String webAppBaseUrl) {
        this.endpoint = endpoint;
        this.webAppBaseUrl = webAppBaseUrl;
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    String getWebAppBaseUrl() {
        return this.webAppBaseUrl;
    }
}
