package com.iterable.iterableapi

import android.util.Log
import org.hamcrest.Matchers.`is`
import org.junit.Assert.*
import org.junit.Test

class IterableConfigTest {

    @Test
    fun defaultDataRegion() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
        val config: IterableConfig = configBuilder.build()
        assertThat(config.dataRegion, `is`(IterableDataRegion.US))
        assertThat(config.dataRegion.endpoint, `is`("https://api.iterable.com/api/"))
    }

    @Test
    fun setDataRegionToEU() {
        val configBuilder: IterableConfig.Builder = IterableConfig.Builder()
            .setDataRegion(IterableDataRegion.EU)
        val config: IterableConfig = configBuilder.build()
        assertThat(config.dataRegion, `is`(IterableDataRegion.EU))
        assertThat(config.dataRegion.endpoint, `is`("https://api.eu.iterable.com/api/"))
    }
}