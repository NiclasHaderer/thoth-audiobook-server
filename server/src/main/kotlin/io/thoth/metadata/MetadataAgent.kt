package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataChapters
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.flow.Flow

interface MetadataAgent {
    val name: String
    val supportedRegions: List<MetadataRegion>

    suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: MetadataRegion,
    ): MetadataAuthor?

    suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: MetadataRegion,
    ): MetadataBook?

    suspend fun getBookChapters(
        providerId: String,
        bookId: String,
        region: MetadataRegion,
    ): MetadataChapters? = null

    suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: MetadataRegion,
    ): MetadataSeries?

    fun getAuthorByName(
        authorName: String,
        region: MetadataRegion,
        language: MetadataLanguage? = null,
    ): Flow<MetadataAuthor>

    fun getBookByName(
        bookName: String,
        region: MetadataRegion,
        keywords: String? = null,
        authorName: String? = null,
        narrator: String? = null,
        language: MetadataLanguage? = null,
    ): Flow<MetadataBook>

    fun getSeriesByName(
        seriesName: String,
        region: MetadataRegion,
        authorName: String? = null,
        language: MetadataLanguage? = null,
    ): Flow<MetadataSeries>
}
