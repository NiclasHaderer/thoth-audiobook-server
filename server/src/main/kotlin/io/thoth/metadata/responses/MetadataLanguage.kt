package io.thoth.metadata.responses

enum class MetadataLanguage(
    vararg aliases: String,
) {
    Spanish("es", "spa", "espanol", "español"),
    English("en", "eng"),
    German("de", "deu", "ger", "deutsch"),
    French("fr", "fra", "fre", "francais", "français"),
    Italian("it", "ita", "italiano"),
    Danish("da", "dan", "dansk"),
    Finnish("fi", "fin", "suomi"),
    Norwegian("no", "nor", "norsk"),
    Swedish("sv", "swe", "svenska"),
    Russian("ru", "rus", "русский"),
    ;

    val tags: List<String> = aliases.toList() + name.lowercase()

    companion object {
        private val byTag = entries.flatMap { language -> language.tags.map { it to language } }.toMap()

        fun fromTag(value: String?): MetadataLanguage? {
            val tag = value?.trim()?.lowercase() ?: return null
            // A tag carries a locale form like "en-US" or "de_DE" as readily as it carries the bare code
            return byTag[tag] ?: byTag[tag.substringBefore('-').substringBefore('_')]
        }
    }
}
