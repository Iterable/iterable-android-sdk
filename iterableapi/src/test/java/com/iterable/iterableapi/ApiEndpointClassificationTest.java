package com.iterable.iterableapi;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiEndpointClassificationTest {

    private ApiEndpointClassification classification;

    @Before
    public void setUp() {
        classification = new ApiEndpointClassification();
    }

    @Test
    public void testDefaultUnauthenticatedEndpoints() {
        // THIS IS IMPORTANT SO IF WE CHANGE IT FOR TESTING WE WILL HAVE THIS FAILING
        assertFalse(classification.requiresJwt(IterableConstants.ENDPOINT_DISABLE_DEVICE));
        assertFalse(classification.requiresJwt(IterableConstants.ENDPOINT_GET_REMOTE_CONFIGURATION));
        assertFalse(classification.requiresJwt(IterableConstants.ENDPOINT_MERGE_USER));
        assertFalse(classification.requiresJwt(IterableConstants.ENDPOINT_CRITERIA_LIST));
        assertFalse(classification.requiresJwt(IterableConstants.ENDPOINT_TRACK_UNKNOWN_SESSION));
        assertFalse(classification.requiresJwt(IterableConstants.ENDPOINT_TRACK_CONSENT));
    }

    @Test
    public void testUnknownEndpointRequiresJwt() {
        assertTrue(classification.requiresJwt("unknown/endpoint"));
    }

    @Test
    public void testUpdateFromRemoteConfigOverridesDefaults() {
        // Override: now only "events/track" is unauthenticated
        classification.updateFromRemoteConfig(
                new HashSet<>(Arrays.asList(IterableConstants.ENDPOINT_TRACK))
        );

        assertFalse(classification.requiresJwt(IterableConstants.ENDPOINT_TRACK));
        // Previously unauthenticated endpoints now require JWT
        assertTrue(classification.requiresJwt(IterableConstants.ENDPOINT_DISABLE_DEVICE));
        assertTrue(classification.requiresJwt(IterableConstants.ENDPOINT_MERGE_USER));
    }

    @Test
    public void testIterableTaskRequiresJwtDelegation() {
        IterableTask authTask = new IterableTask(IterableConstants.ENDPOINT_TRACK, IterableTaskType.API, "{}");
        IterableTask unauthTask = new IterableTask(IterableConstants.ENDPOINT_DISABLE_DEVICE, IterableTaskType.API, "{}");

        assertTrue(authTask.requiresJwt(classification));
        assertFalse(unauthTask.requiresJwt(classification));
    }
}
