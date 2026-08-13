package com.iterable.iterableapi

import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test

class IterableDataRegionTest {

    @Test
    fun endpointsMatchDataCenters() {
        assertEquals("https://api.iterable.com/api/", IterableDataRegion.US.endpoint)
        assertEquals("https://api.eu.iterable.com/api/", IterableDataRegion.EU.endpoint)
    }

    /** These identifiers are part of the cross-SDK contract; changing them breaks the wrappers. */
    @Test
    fun stableIdentifiersMatchOtherSdks() {
        assertEquals(0, IterableDataRegion.US.code)
        assertEquals(1, IterableDataRegion.EU.code)
        assertEquals("US", IterableDataRegion.US.regionCode)
        assertEquals("EU", IterableDataRegion.EU.regionCode)
    }

    @Test
    fun fromRegionCode() {
        assertThat(IterableDataRegion.from("US"), `is`(IterableDataRegion.US))
        assertThat(IterableDataRegion.from("EU"), `is`(IterableDataRegion.EU))
    }

    @Test
    fun fromRegionCodeIsCaseAndWhitespaceInsensitive() {
        assertThat(IterableDataRegion.from("eu"), `is`(IterableDataRegion.EU))
        assertThat(IterableDataRegion.from("Eu"), `is`(IterableDataRegion.EU))
        assertThat(IterableDataRegion.from("  EU  "), `is`(IterableDataRegion.EU))
    }

    /** iOS expresses the data region as the endpoint URL itself, so those values must resolve. */
    @Test
    fun fromIosStyleEndpointUrl() {
        assertThat(
            IterableDataRegion.from("https://api.eu.iterable.com/api/"),
            `is`(IterableDataRegion.EU)
        )
        assertThat(
            IterableDataRegion.from("https://api.iterable.com/api/"),
            `is`(IterableDataRegion.US)
        )
    }

    @Test
    fun fromUnsupportedStringFallsBackToUs() {
        assertThat(IterableDataRegion.from("APAC"), `is`(IterableDataRegion.US))
        assertThat(IterableDataRegion.from(""), `is`(IterableDataRegion.US))
        assertThat(IterableDataRegion.from("   "), `is`(IterableDataRegion.US))
        assertThat(IterableDataRegion.from(null as String?), `is`(IterableDataRegion.US))
    }

    @Test
    fun fromCode() {
        assertThat(IterableDataRegion.from(0), `is`(IterableDataRegion.US))
        assertThat(IterableDataRegion.from(1), `is`(IterableDataRegion.EU))
    }

    @Test
    fun fromUnsupportedCodeFallsBackToUs() {
        assertThat(IterableDataRegion.from(2), `is`(IterableDataRegion.US))
        assertThat(IterableDataRegion.from(-1), `is`(IterableDataRegion.US))
        assertThat(IterableDataRegion.from(Int.MAX_VALUE), `is`(IterableDataRegion.US))
    }

    /** Round-trips guard the wrappers, which serialize the region and resolve it back. */
    @Test
    fun identifiersRoundTrip() {
        for (region in IterableDataRegion.values()) {
            assertThat(IterableDataRegion.from(region.code), `is`(region))
            assertThat(IterableDataRegion.from(region.regionCode), `is`(region))
            assertThat(IterableDataRegion.from(region.endpoint), `is`(region))
        }
    }
}
