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
    fun defaultInAppColorSchemeIsAutomatic() {
        val config = IterableConfig.Builder().build()

        assertEquals(IterableInAppColorScheme.AUTOMATIC, config.inAppColorScheme)
        assertNull(config.inAppColorSchemeProvider)
    }

    @Test
    fun setInAppColorSchemeUsesFixedValue() {
        val config = IterableConfig.Builder()
            .setInAppColorSchemeProvider { IterableInAppColorScheme.LIGHT }
            .setInAppColorScheme(IterableInAppColorScheme.DARK)
            .build()

        assertEquals(IterableInAppColorScheme.DARK, config.inAppColorScheme)
        assertNull(config.inAppColorSchemeProvider)
    }

    @Test
    fun setInAppColorSchemeProviderUsesProvider() {
        val provider = IterableInAppColorSchemeProvider { IterableInAppColorScheme.DARK }
        val config = IterableConfig.Builder()
            .setInAppColorScheme(IterableInAppColorScheme.LIGHT)
            .setInAppColorSchemeProvider(provider)
            .build()

        assertEquals(IterableInAppColorScheme.AUTOMATIC, config.inAppColorScheme)
        assertSame(provider, config.inAppColorSchemeProvider)
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
    fun setExpiringAuthTokenRefreshPeriodKeepsFractionalSeconds() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(0.5)
            .build()
        assertEquals(500L, config.expiringAuthTokenRefreshPeriodMillis)
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
    fun invalidExpiringAuthTokenRefreshPeriodKeepsThePreviouslySetValue() {
        val config: IterableConfig = IterableConfig.Builder()
            .setExpiringAuthTokenRefreshPeriod(30.0)
            .setExpiringAuthTokenRefreshPeriod(-60.0)
            .setExpiringAuthTokenRefreshPeriod(Double.NaN)
            .build()
        assertEquals(30_000L, config.expiringAuthTokenRefreshPeriodMillis)
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

    /**
     * `encryptionEnforced` was removed in 3.5.5 and then silently reinstated by a merge in 3.6.0,
     * where it sat unread for four minor versions. Nothing in the build detects a re-added member,
     * so this asserts its absence directly.
     */
    @Test
    fun encryptionEnforcedIsNotPartOfTheConfiguration() {
        val members = listOf(IterableConfig::class.java, IterableConfig.Builder::class.java)
            .flatMap { type ->
                type.declaredFields.map { it.name } + type.declaredMethods.map { it.name }
            }
            .filter { it.contains("encryptionEnforced", ignoreCase = true) }

        assertTrue("encryptionEnforced was reintroduced: $members", members.isEmpty())
    }
}
