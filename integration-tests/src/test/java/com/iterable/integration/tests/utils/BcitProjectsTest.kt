package com.iterable.integration.tests.utils

import com.iterable.iterableapi.IterableDataRegion
import org.junit.Assert.assertEquals
import org.junit.Test

class BcitProjectsTest {

    private val projects = listOf(
        BcitProject("A", "aaaa1111aaaa", "server-a", IterableDataRegion.US),
        BcitProject("B", "bbbb2222bbbb", "server-b", IterableDataRegion.EU),
        BcitProject("C", "", "", IterableDataRegion.US)
    )

    @Test
    fun `resolves the label from an exact key match`() {
        assertEquals("A", BcitProjects.labelFor("aaaa1111aaaa", projects))
        assertEquals("B", BcitProjects.labelFor("bbbb2222bbbb", projects))
    }

    @Test
    fun `unknown, null or blank key resolves to question mark`() {
        assertEquals("?", BcitProjects.labelFor("zzzz9999zzzz", projects))
        assertEquals("?", BcitProjects.labelFor(null, projects))
        assertEquals("?", BcitProjects.labelFor("", projects))
    }

    @Test
    fun `server base url follows the project region`() {
        assertEquals("https://api.iterable.com/api/", projects[0].serverBaseUrl)
        assertEquals("https://api.eu.iterable.com/api/", projects[1].serverBaseUrl)
    }
}
