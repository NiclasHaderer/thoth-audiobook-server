package io.thoth.metadata.audible.models

enum class AudibleRegions(
    val value: String,
) {
    AU("au"),
    CA("ca"),
    DE("de"),
    ES("es"),
    FR("fr"),
    IN("in"),
    IT("it"),
    JP("jp"),
    US("us"),
    UK("uk"),
    ;

    companion object {
        fun from(region: String): AudibleRegions =
            entries.firstOrNull { it.name.lowercase() == region.lowercase() } ?: US
    }
}

class AudibleRegionValue(
    val chapterName: String,
    val tld: String,
    val datePattern: String,
    val titleReplacers: List<Regex> = listOf(),
) {
    fun toHost(): String = "audible.$tld"
}

private const val AUDIBLE_CHAPTER_NAME = "Chapter"
private val TITLE_REPLACER = listOf(", Book .*".toRegex())
private const val DATE_PATTERN = "MM-dd-yy"

private val RegionMappings =
    mutableMapOf(
        AudibleRegions.AU to
            AudibleRegionValue(chapterName = AUDIBLE_CHAPTER_NAME, tld = "com.au", DATE_PATTERN, TITLE_REPLACER),
        AudibleRegions.CA to
            AudibleRegionValue(chapterName = AUDIBLE_CHAPTER_NAME, tld = "ca", DATE_PATTERN, TITLE_REPLACER),
        AudibleRegions.DE to
            AudibleRegionValue(chapterName = "Kapitel", tld = "de", "dd.MM.yyyy", listOf(" - Gesprochen .*".toRegex())),
        AudibleRegions.ES to AudibleRegionValue(chapterName = "Capítulo", tld = "es", DATE_PATTERN),
        AudibleRegions.FR to AudibleRegionValue(chapterName = "Chapitre", tld = "fr", DATE_PATTERN),
        AudibleRegions.IN to
            AudibleRegionValue(chapterName = AUDIBLE_CHAPTER_NAME, tld = "in", DATE_PATTERN, TITLE_REPLACER),
        AudibleRegions.IT to AudibleRegionValue(chapterName = "Capitolo", tld = "it", DATE_PATTERN),
        AudibleRegions.JP to AudibleRegionValue(chapterName = "章", tld = "co.jp", DATE_PATTERN),
        AudibleRegions.US to
            AudibleRegionValue(chapterName = AUDIBLE_CHAPTER_NAME, tld = "com", DATE_PATTERN, TITLE_REPLACER),
        AudibleRegions.UK to
            AudibleRegionValue(chapterName = AUDIBLE_CHAPTER_NAME, tld = "co.uk", DATE_PATTERN, TITLE_REPLACER),
    )

fun AudibleRegions.getValue(): AudibleRegionValue = RegionMappings[this]!!
