package com.iterable.iterableapi

import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.*
import org.junit.Test

class IterableConfigTest {

    @Test
    fun defaultDataRegion() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
        val config: IterableConfig = configBuilder.build()
        assertThat(config.dataRegion, `is`(IterableDataRegion.US))
    }

    @Test
    fun setDataRegionToEU() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
            .setDataRegion(IterableDataRegion.EU)
        val config: IterableConfig = configBuilder.build()
        assertThat(config.dataRegion, `is`(IterableDataRegion.EU))
    }
    
    @Test
    fun defaultWebViewBaseUrl() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
        val config: IterableConfig = configBuilder.build()
        assertThat(config.webViewBaseUrl, `is`(nullValue()))
    }
    
    @Test
    fun setWebViewBaseUrl() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
            .setWebViewBaseUrl("https://app.iterable.com")
        val config: IterableConfig = configBuilder.build()
        assertThat(config.webViewBaseUrl, `is`("https://app.iterable.com"))
    }
    
    @Test
    fun defaultDisableKeychainEncryption() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
        val config: IterableConfig = configBuilder.build()
        assertTrue(config.keychainEncryption)
    }
    
    @Test
    fun setDisableKeychainEncryption() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
            .setKeychainEncryption(false)
        val config: IterableConfig = configBuilder.build()
        assertFalse(config.keychainEncryption)
    }

    @Test
    fun defaultExpiringAuthTokenRefreshPeriodIs60Seconds() {
        val config: IterableConfig = IterableConfig.Builder().build()
        assertEquals(60_000L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    fun setExpiringAuthTokenRefreshPeriodConvertsSecondsToMillis() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(120.0)
            .build()
        assertEquals(120_000L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    fun setExpiringAuthTokenRefreshPeriodKeepsFractionalSeconds() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(0.5)
            .build()
        assertEquals(500L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    fun setExpiringAuthTokenRefreshPeriodKeepsSubSecondPrecisionOnLargerValues() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(90.25)
            .build()
        assertEquals(90_250L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    fun setExpiringAuthTokenRefreshPeriodAcceptsZero() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(0.0)
            .build()
        assertEquals(0L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    fun negativeExpiringAuthTokenRefreshPeriodFallsBackToDefault() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(-60.0)
            .build()
        assertEquals(60_000L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    fun oversizedExpiringAuthTokenRefreshPeriodIsClampedWithoutOverflowing() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(Double.MAX_VALUE)
            .build()
        assertEquals(
            IterableConfig.MAX_EXPIRING_AUTH_TOKEN_REFRESH_PERIOD_SECONDS * 1000L,
            config.expiringAuthTokenRefreshPeriodMillis
        )
        assertTrue(config.expiringAuthTokenRefreshPeriodMillis > 0)
    }

    @Test
    fun nanExpiringAuthTokenRefreshPeriodFallsBackToDefault() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(Double.NaN)
            .build()
        assertEquals(60_000L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    @Suppress("DEPRECATION")
    fun deprecatedLongOverloadStillConvertsSecondsToMillis() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(java.lang.Long.valueOf(120L))
            .build()
        assertEquals(120_000L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    @Suppress("DEPRECATION")
    fun deprecatedLongOverloadFallsBackToDefaultForMostNegativeValue() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(java.lang.Long.valueOf(Long.MIN_VALUE))
            .build()
        assertEquals(60_000L, config.expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    @Suppress("DEPRECATION")
    fun deprecatedLongOverloadClampsMaxValueWithoutOverflowing() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(java.lang.Long.valueOf(Long.MAX_VALUE))
            .build()
        assertEquals(
            IterableConfig.MAX_EXPIRING_AUTH_TOKEN_REFRESH_PERIOD_SECONDS * 1000L,
            config.expiringAuthTokenRefreshPeriodMillis
        )
        assertTrue(config.expiringAuthTokenRefreshPeriodMillis > 0)
    }

    /** Only reachable from Java, where the `@NonNull Long` parameter can still be passed null. */
    @Test
    fun nullExpiringAuthTokenRefreshPeriodFallsBackToDefault() {
        val builder = IterableConfig.Builder()
        val setter = IterableConfig.Builder::class.java
            .getMethod("setExpiringAuthTokenRefreshPeriod", java.lang.Long::class.java)
        setter.invoke(builder, null)
        assertEquals(60_000L, builder.build().expiringAuthTokenRefreshPeriodMillis)
    }

    @Test
    fun retryPolicyConvertsRetryIntervalSecondsToMillis() {
        val retryPolicy = RetryPolicy(10, 6L, RetryPolicy.Type.LINEAR)
        assertEquals(6_000L, retryPolicy.retryIntervalMillis)
    }
}
