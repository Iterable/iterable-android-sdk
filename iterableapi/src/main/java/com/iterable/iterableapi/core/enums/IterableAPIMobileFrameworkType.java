package com.iterable.iterableapi.core.enums;

public enum IterableAPIMobileFrameworkType {
    FLUTTER("flutter"),
    REACT_NATIVE("reactnative"),
    NATIVE("native");

    private final String value;

    IterableAPIMobileFrameworkType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}