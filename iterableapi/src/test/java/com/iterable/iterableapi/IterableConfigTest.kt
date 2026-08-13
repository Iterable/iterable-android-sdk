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

    /** Only reachable from Java, where the `@NonNull` parameter can still be passed null. */
    @Test
    fun nullDataRegionFallsBackToUs() {
        val builder = IterableConfig.Builder()
        val setter = IterableConfig.Builder::class.java
            .getMethod("setDataRegion", IterableDataRegion::class.java)
        setter.invoke(builder, null)
        assertThat(builder.build().dataRegion, `is`(IterableDataRegion.US))
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
}