package io.thoth.metadata.audible.models


enum class AudibleRegions {
    au,
    ca,
    de,
    es,
    fr,
    `in`,
    it,
    jp,
    us,
    uk

}

class AudibleRegionValue(
    val chapterName: String,
    val tld: String,
    val datePattern: String,
    val titleReplacers: List<Regex> = listOf()
) {
    fun toHost(): String {
        return "audible.$tld"
    }
}

private const val audibleChapterName = "Chapter"
private val titleReplacers = listOf(", Book .*".toRegex())
private const val datePattern = "MM-dd-yy"

private val RegionMappings = mutableMapOf(
    AudibleRegions.au to AudibleRegionValue(
        chapterName = audibleChapterName,
        tld = "com.au",
        datePattern,
        titleReplacers
    ),
    AudibleRegions.ca to AudibleRegionValue(chapterName = audibleChapterName, tld = "ca", datePattern, titleReplacers),
    AudibleRegions.de to AudibleRegionValue(
        chapterName = "Kapitel",
        tld = "de",
        "dd.MM.yyyy",
        listOf(" - Gesprochen .*".toRegex())
    ),
    AudibleRegions.es to AudibleRegionValue(chapterName = "Capítulo", tld = "es", datePattern),
    AudibleRegions.fr to AudibleRegionValue(chapterName = "Chapitre", tld = "fr", datePattern),
    AudibleRegions.`in` to AudibleRegionValue(
        chapterName = audibleChapterName,
        tld = "in",
        datePattern,
        titleReplacers
    ),
    AudibleRegions.it to AudibleRegionValue(chapterName = "Capitolo", tld = "it", datePattern),
    AudibleRegions.jp to AudibleRegionValue(chapterName = "章", tld = "co.jp", datePattern),
    AudibleRegions.us to AudibleRegionValue(chapterName = audibleChapterName, tld = "com", datePattern, titleReplacers),
    AudibleRegions.uk to AudibleRegionValue(
        chapterName = audibleChapterName,
        tld = "co.uk",
        datePattern,
        titleReplacers
    ),
)

fun AudibleRegions.getValue(): AudibleRegionValue = RegionMappings[this]!!