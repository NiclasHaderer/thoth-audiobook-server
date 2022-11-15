package io.thoth.metadata.audible.client


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
    val titleReplacers: List<Regex> = listOf()
) {
    fun toHost(): String {
        return "audible.$tld"
    }
}

private const val audibleChapterName = "Chapter"
private val titleReplacers = listOf(", Book .*".toRegex())

private val RegionMappings = mutableMapOf(
    AudibleRegions.au to AudibleRegionValue(chapterName = audibleChapterName, tld = "com.au", titleReplacers),
    AudibleRegions.ca to AudibleRegionValue(chapterName = audibleChapterName, tld = "ca", titleReplacers),
    AudibleRegions.de to AudibleRegionValue(chapterName = "Kapitel", tld = "de", listOf(" - Gesprochen .*".toRegex())),
    AudibleRegions.es to AudibleRegionValue(chapterName = "Capítulo", tld = "es"),
    AudibleRegions.fr to AudibleRegionValue(chapterName = "Chapitre", tld = "fr"),
    AudibleRegions.`in` to AudibleRegionValue(chapterName = audibleChapterName, tld = "in", titleReplacers),
    AudibleRegions.it to AudibleRegionValue(chapterName = "Capitolo", tld = "it"),
    AudibleRegions.jp to AudibleRegionValue(chapterName = "章", tld = "co.jp"),
    AudibleRegions.us to AudibleRegionValue(chapterName = audibleChapterName, tld = "com", titleReplacers),
    AudibleRegions.uk to AudibleRegionValue(chapterName = audibleChapterName, tld = "co.uk", titleReplacers),
)

fun AudibleRegions.getValue(): AudibleRegionValue = RegionMappings[this]!!