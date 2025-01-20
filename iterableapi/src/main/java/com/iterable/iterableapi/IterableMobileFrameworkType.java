package com.iterable.iterableapi;

public enum IterableMobileFrameworkType {
    FLUTTER("flutter"),
    REACT_NATIVE("reactnative"),
    NATIVE("native");

    private final String value;

    IterableMobileFrameworkType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 