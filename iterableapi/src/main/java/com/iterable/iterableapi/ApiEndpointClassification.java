package com.iterable.iterableapi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class ApiEndpointClassification {

    private static final Set<String> DEFAULT_UNAUTHENTICATED = new HashSet<>(Arrays.asList(
            IterableConstants.ENDPOINT_DISABLE_DEVICE,
            IterableConstants.ENDPOINT_GET_REMOTE_CONFIGURATION,
            IterableConstants.ENDPOINT_MERGE_USER,
            IterableConstants.ENDPOINT_CRITERIA_LIST,
            IterableConstants.ENDPOINT_TRACK_UNKNOWN_SESSION,
            IterableConstants.ENDPOINT_TRACK_CONSENT
    ));

    private volatile Set<String> unauthenticatedPaths = new HashSet<>(DEFAULT_UNAUTHENTICATED);

    boolean requiresJwt(String path) {
        return !unauthenticatedPaths.contains(path);
    }

    void updateFromRemoteConfig(Set<String> paths) {
        this.unauthenticatedPaths = new HashSet<>(paths);
    }
}
