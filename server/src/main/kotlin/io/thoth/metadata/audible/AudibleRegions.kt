package io.thoth.metadata.audible

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.metadata.responses.MetadataRegion

private val log = logger {}

private val BOOK_NUMBER_SUFFIX = listOf(", Book .*".toRegex())

internal enum class AudibleRegions(
    val region: MetadataRegion,
    tld: String,
    val locale: String,
    val titleReplacers: List<Regex> = listOf(),
) {
    AU(MetadataRegion.AU, "com.au", locale = "en_AU", titleReplacers = BOOK_NUMBER_SUFFIX),
    CA(MetadataRegion.CA, "ca", locale = "en_CA", titleReplacers = BOOK_NUMBER_SUFFIX),
    DE(MetadataRegion.DE, "de", locale = "de_DE", titleReplacers = listOf(" - Gesprochen .*".toRegex())),
    ES(MetadataRegion.ES, "es", locale = "es_ES"),
    FR(MetadataRegion.FR, "fr", locale = "fr_FR"),
    IN(MetadataRegion.IN, "in", locale = "en_IN", titleReplacers = BOOK_NUMBER_SUFFIX),
    IT(MetadataRegion.IT, "it", locale = "it_IT"),
    JP(MetadataRegion.JP, "co.jp", locale = "ja_JP"),
    US(MetadataRegion.US, "com", locale = "en_US", titleReplacers = BOOK_NUMBER_SUFFIX),
    UK(MetadataRegion.UK, "co.uk", locale = "en_GB", titleReplacers = BOOK_NUMBER_SUFFIX),
    ;

    val host = "audible.$tld"
    val apiHost = "api.audible.$tld"

    companion object {
        private val byRegion = entries.associateBy { it.region }

        fun from(region: MetadataRegion): AudibleRegions =
            byRegion[region] ?: US.also { log.error { "'$region' is no Audible marketplace, falling back to $it" } }
    }
}
