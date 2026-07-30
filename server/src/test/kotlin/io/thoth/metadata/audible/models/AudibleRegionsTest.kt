package io.thoth.metadata.audible.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudibleRegionsTest {
    @Test
    fun `a region knows the hosts of its marketplace`() {
        assertEquals("audible.com.au", AudibleRegions.AU.host)
        assertEquals("api.audible.com.au", AudibleRegions.AU.apiHost)
        assertEquals("audible.co.uk", AudibleRegions.UK.host)
        assertEquals("api.audible.co.jp", AudibleRegions.JP.apiHost)
    }

    @Test
    fun `every region has a locale for the pages which are scraped`() {
        assertTrue(AudibleRegions.entries.all { it.locale.matches(Regex("[a-z]{2}_[A-Z]{2}")) })
    }

    @Test
    fun `a region is recognized regardless of its casing`() {
        assertEquals(AudibleRegions.DE, AudibleRegions.from("de"))
        assertEquals(AudibleRegions.DE, AudibleRegions.from("DE"))
    }

    @Test
    fun `anything which is no marketplace falls back to the US`() {
        assertEquals(AudibleRegions.US, AudibleRegions.from("English"))
    }

    @Test
    fun `only the regions which need it strip a suffix off the titles`() {
        assertEquals(listOf(", Book .*"), AudibleRegions.US.titleReplacers.map { it.pattern })
        assertEquals(listOf(" - Gesprochen .*"), AudibleRegions.DE.titleReplacers.map { it.pattern })
        assertEquals(emptyList(), AudibleRegions.FR.titleReplacers)
    }
}
