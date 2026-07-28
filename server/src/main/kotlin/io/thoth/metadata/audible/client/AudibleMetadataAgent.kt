package io.thoth.metadata.audible.client

import io.thoth.metadata.SearchBasedMetadataAgent
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.audible.models.AudibleSearchAmount
import io.thoth.metadata.responses.MetadataAuthorImpl
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeriesImpl

const val AUDIBLE_PROVIDER_NAME = "audible"

class AudibleMetadataAgent(
    private val imageSize: Int = 500,
) : SearchBasedMetadataAgent() {
    override val name = AUDIBLE_PROVIDER_NAME

    override val supportedCountryCodes: List<String>
        get() = AudibleRegions.entries.map { it.name }

    override suspend fun search(
        region: String,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBookImpl> =
        getAudibleSearchResult(
            AudibleRegions.from(region),
            imageSize,
            keywords = keywords,
            title = title,
            author = author,
            narrator = narrator,
            language = language?.name?.lowercase(),
            pageSize = pageSize?.let { AudibleSearchAmount.from(it) },
        ).filter { !it.title.isNullOrBlank() }

    override suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthorImpl? = getAudibleAuthor(AudibleRegions.from(region), imageSize, authorId)

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBookImpl? = getAudibleBook(AudibleRegions.from(region), imageSize, bookId)

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: String,
    ): MetadataSeriesImpl? = getAudibleSeries(AudibleRegions.from(region), imageSize, seriesId)
}
