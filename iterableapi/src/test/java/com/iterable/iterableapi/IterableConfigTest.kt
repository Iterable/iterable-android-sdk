package com.iterable.iterableapi

import org.hamcrest.Matchers.`is`
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
    fun defaultDisableKeychainEncryption() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
        val config: IterableConfig = configBuilder.build()
        assertFalse(config.disableKeychainEncryption)
    }
    
    @Test
    fun setDisableKeychainEncryption() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
            .setDisableKeychainEncryption(true)
        val config: IterableConfig = configBuilder.build()
        assertTrue(config.disableKeychainEncryption)
    }
}