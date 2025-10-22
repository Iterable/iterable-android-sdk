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
    const val TEST_PUSH_CAMPAIGN_ID = 14332358
    const val TEST_EMBEDDED_CAMPAIGN_ID = 14332359
    
    // Test timeouts
    const val TIMEOUT_SECONDS = 5L
    const val POLL_INTERVAL_SECONDS = 1L
}

