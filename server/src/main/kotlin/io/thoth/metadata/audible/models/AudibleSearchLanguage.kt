package io.thoth.metadata.audible.models

import io.thoth.metadata.responses.MetadataLanguage

enum class AudibleSearchLanguage(
    val language: Long,
) {
    Spanish(16290345031),
    English(16290310031),
    German(16290314031),
    French(16290313031),
    Italian(16290322031),
    Danish(16290308031),
    Finnish(16290312031),
    Norwegian(16290333031),
    Swedish(16290346031),
    Russian(16290340031),
    ;

    companion object {
        fun from(language: MetadataLanguage): AudibleSearchLanguage =
            when (language) {
                MetadataLanguage.Danish -> Danish
                MetadataLanguage.English -> English
                MetadataLanguage.Finnish -> Finnish
                MetadataLanguage.Spanish -> Spanish
                MetadataLanguage.German -> German
                MetadataLanguage.French -> French
                MetadataLanguage.Italian -> Italian
                MetadataLanguage.Norwegian -> Norwegian
                MetadataLanguage.Swedish -> Swedish
                MetadataLanguage.Russian -> Russian
            }
    }
}
