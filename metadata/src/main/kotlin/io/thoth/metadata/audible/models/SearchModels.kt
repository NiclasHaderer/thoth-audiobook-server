package io.thoth.metadata.audible.models

import io.thoth.metadata.*
import java.time.LocalDate

data class AudibleSearchAuthorImpl(
    override val id: AudibleProviderWithIDMetadata,
    override val name: String?,
    override val link: String,
) : SearchAuthorMetadata

data class AudibleSearchSeriesImpl(
    override val id: AudibleProviderWithIDMetadata,
    override val name: String?,
    override val index: Float?,
    override val link: String,
) : SearchSeriesMetadata

data class AudibleSearchBookImpl(
    override val id: AudibleProviderWithIDMetadata,
    override val title: String?,
    override val link: String?,
    override val author: AudibleSearchAuthorImpl?,
    override val narrator: String?,
    override var series: AudibleSearchSeriesImpl?,
    override val image: String?,
    override val language: String?,
    override val releaseDate: LocalDate?,
) : SearchBookMetadata

enum class AudibleSearchLanguage(val language: Long) {
    Spanish(16290345031),
    English(16290310031),
    German(16290314031),
    French(16290313031),
    Italian(16290322031),
    Danish(16290308031),
    Finnish(16290312031),
    Norwegian(16290333031),
    Swedish(16290346031),
    Russian(16290340031);


    companion object {
        fun from(language: MetadataLanguage): AudibleSearchLanguage {
            return when (language) {
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
}

enum class AudibleSearchAmount(val size: Int) {
    Twenty(20),
    Thirty(30),
    Forty(40),
    Fifty(50);


    companion object {
        fun from(searchCount: MetadataSearchCount): AudibleSearchAmount {
            return when (searchCount) {
                MetadataSearchCount.Small -> Twenty
                MetadataSearchCount.Medium -> Thirty
                MetadataSearchCount.Large -> Forty
                MetadataSearchCount.ExtraLarge -> Fifty
            }
        }
    }
}
