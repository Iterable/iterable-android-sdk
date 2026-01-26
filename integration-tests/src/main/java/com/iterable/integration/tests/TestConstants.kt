package com.iterable.integration.tests

import com.iterable.integration.tests.BuildConfig

/**
 * Centralized constants for integration tests
 */
object TestConstants {
    
    // Test user email - centralized location for all test email references
    val TEST_USER_EMAIL = BuildConfig.ITERABLE_TEST_USER_EMAIL

    // Test campaign IDs - these should be configured in your Iterable project
    const val TEST_INAPP_CAMPAIGN_ID = 14332357
    const val TEST_PUSH_CAMPAIGN_ID = 15671239
    const val TEST_EMBEDDED_CAMPAIGN_ID = 14332359
    
    // Test placement IDs
    const val TEST_EMBEDDED_PLACEMENT_ID = 2157L
    
    // Test timeouts (increased for CI stability)
    const val TIMEOUT_SECONDS = 15L  // Increased from 5s for slower CI emulators
    const val POLL_INTERVAL_SECONDS = 2L  // Increased from 1s for better stability
}

