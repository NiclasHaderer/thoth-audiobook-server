package io.thoth.metadata.audible.models

import io.github.oshai.kotlinlogging.KotlinLogging.logger

private val log = logger {}

enum class AudibleRegions {
    AU,
    CA,
    DE,
    ES,
    FR,
    IN,
    IT,
    JP,
    US,
    UK;

    companion object {
        fun from(region: String): AudibleRegions =
            entries.firstOrNull { it.name.equals(region, ignoreCase = true) }
                ?: US.also { log.warn { "'$region' is no Audible marketplace, falling back to $it" } }
    }
}

class AudibleRegionValue(
    val tld: String,
    val titleReplacers: List<Regex> = listOf(),
    /**
     * Locale of the scraped pages. Marketplaces serving more than one locale otherwise pick it based on the IP of the
     * caller, which would give a server outside the region pages in a foreign language.
     */
    val locale: String,
) {
    fun toHost(): String = "audible.$tld"

    fun toApiHost(): String = "api.audible.$tld"
}

private val TITLE_REPLACER = listOf(", Book .*".toRegex())

private val RegionMappings =
    mapOf(
        AudibleRegions.AU to AudibleRegionValue(tld = "com.au", TITLE_REPLACER, locale = "en_AU"),
        AudibleRegions.CA to AudibleRegionValue(tld = "ca", TITLE_REPLACER, locale = "en_CA"),
        AudibleRegions.DE to AudibleRegionValue(tld = "de", listOf(" - Gesprochen .*".toRegex()), locale = "de_DE"),
        AudibleRegions.ES to AudibleRegionValue(tld = "es", locale = "es_ES"),
        AudibleRegions.FR to AudibleRegionValue(tld = "fr", locale = "fr_FR"),
        AudibleRegions.IN to AudibleRegionValue(tld = "in", TITLE_REPLACER, locale = "en_IN"),
        AudibleRegions.IT to AudibleRegionValue(tld = "it", locale = "it_IT"),
        AudibleRegions.JP to AudibleRegionValue(tld = "co.jp", locale = "ja_JP"),
        AudibleRegions.US to AudibleRegionValue(tld = "com", TITLE_REPLACER, locale = "en_US"),
        AudibleRegions.UK to AudibleRegionValue(tld = "co.uk", TITLE_REPLACER, locale = "en_GB"),
    )

fun AudibleRegions.getValue(): AudibleRegionValue = RegionMappings[this]!!
