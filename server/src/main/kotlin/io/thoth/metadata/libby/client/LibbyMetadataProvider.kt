package io.thoth.metadata.libby.client

import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.responses.MetadataAuthorImpl
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeriesImpl

internal const val LIBBY_PROVIDER_NAME = "libby"

/**
 * The OverDrive catalog has to be queried through a library, but its metadata is the same everywhere, so all regions
 * are answered by the one configured library.
 */
class LibbyMetadataProvider(
    private val libraryKey: String = "brooklyn",
    private val imageSize: Int = 500,
) : MetadataProvider {
    override val name = LIBBY_PROVIDER_NAME

    override val supportedCountryCodes = listOf("US")

    override suspend fun search(
        region: String,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBookImpl> =
        getLibbySearchResult(
            libraryKey,
            imageSize,
            keywords = keywords,
            title = title,
            author = author,
            narrator = narrator,
            language = language?.toLibbyLanguageId(),
            pageSize =
                when (pageSize) {
                    null -> null
                    MetadataSearchCount.Small -> 20
                    MetadataSearchCount.Medium -> 30
                    MetadataSearchCount.Large -> 40
                    MetadataSearchCount.ExtraLarge -> LIBBY_API_MAX_RESULTS
                },
        ).filter { !it.title.isNullOrBlank() }

    override suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthorImpl? = getLibbyAuthor(libraryKey, authorId)

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBookImpl? = getLibbyBook(libraryKey, imageSize, bookId)

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: String,
    ): MetadataSeriesImpl? = getLibbySeries(libraryKey, imageSize, seriesId)

    private fun MetadataLanguage.toLibbyLanguageId(): String =
        when (this) {
            MetadataLanguage.Spanish -> "es"
            MetadataLanguage.English -> "en"
            MetadataLanguage.German -> "de"
            MetadataLanguage.French -> "fr"
            MetadataLanguage.Italian -> "it"
            MetadataLanguage.Danish -> "da"
            MetadataLanguage.Finnish -> "fi"
            MetadataLanguage.Norwegian -> "no"
            MetadataLanguage.Swedish -> "sv"
            MetadataLanguage.Russian -> "ru"
        }
}
