package io.thoth.metadata.audible.models

import io.github.oshai.kotlinlogging.KotlinLogging.logger

private val log = logger {}

private val BOOK_NUMBER_SUFFIX = listOf(", Book .*".toRegex())

internal enum class AudibleRegions(
    tld: String,
    /**
     * Locale of the scraped pages. Marketplaces serving more than one locale otherwise pick it based on the IP of the
     * caller, which would give a server outside the region pages in a foreign language.
     */
    val locale: String,
    val titleReplacers: List<Regex> = listOf(),
) {
    AU("com.au", locale = "en_AU", titleReplacers = BOOK_NUMBER_SUFFIX),
    CA("ca", locale = "en_CA", titleReplacers = BOOK_NUMBER_SUFFIX),
    DE("de", locale = "de_DE", titleReplacers = listOf(" - Gesprochen .*".toRegex())),
    ES("es", locale = "es_ES"),
    FR("fr", locale = "fr_FR"),
    IN("in", locale = "en_IN", titleReplacers = BOOK_NUMBER_SUFFIX),
    IT("it", locale = "it_IT"),
    JP("co.jp", locale = "ja_JP"),
    US("com", locale = "en_US", titleReplacers = BOOK_NUMBER_SUFFIX),
    UK("co.uk", locale = "en_GB", titleReplacers = BOOK_NUMBER_SUFFIX),
    ;

    val host = "audible.$tld"
    val apiHost = "api.audible.$tld"

    companion object {
        fun from(region: String): AudibleRegions =
            entries.firstOrNull { it.name.equals(region, ignoreCase = true) }
                ?: US.also { log.error { "'$region' is no Audible marketplace, falling back to $it" } }
    }
}
