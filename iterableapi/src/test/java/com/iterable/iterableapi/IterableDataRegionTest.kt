package com.iterable.iterableapi

import android.util.Log
import com.iterable.iterableapi.unit.TestRunner
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog

@RunWith(TestRunner::class)
class IterableDataRegionTest {

    @Before
    fun clearLogs() {
        ShadowLog.clear()
    }

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

    /**
     * These factories run while the config is still being built, so IterableLogger is still reading
     * the pre-init config's ERROR level. Anything below ERROR is dropped before it reaches logcat,
     * which would leave a misconfigured region silently sending data to the US data center.
     */
    @Test
    fun unsupportedStringIsLoggedAtDefaultLogLevel() {
        IterableDataRegion.from("APAC")

        val message = errorLogs().single()
        assertTrue("log should name the rejected value: $message", message.contains("APAC"))
        assertTrue("log should list supported values: $message", message.contains("US (0), EU (1)"))
    }

    @Test
    fun unsupportedCodeIsLoggedAtDefaultLogLevel() {
        IterableDataRegion.from(7)

        val message = errorLogs().single()
        assertTrue("log should name the rejected code: $message", message.contains("7"))
        assertTrue("log should list supported values: $message", message.contains("US (0), EU (1)"))
    }

    @Test
    fun recognisedValuesAreNotLoggedAsErrors() {
        IterableDataRegion.from("EU")
        IterableDataRegion.from("https://api.eu.iterable.com/api/")
        IterableDataRegion.from(0)

        assertTrue(errorLogs().toString(), errorLogs().isEmpty())
    }

    private fun errorLogs(): List<String> =
        ShadowLog.getLogsForTag("IterableDataRegion")
            .filter { it.type == Log.ERROR }
            .map { it.msg }
}
