package com.iterable.iterableapi;

public enum IterableDataRegion {
    US("https://api.iterable.com/api/"),
    EU("https://api.eu.iterable.com/api/");
    private final String endpoint;

    IterableDataRegion(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return this.endpoint;
    }
}
