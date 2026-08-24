package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.flow.Flow

/** The lookups a metadata provider has to implement itself. Everything else can be derived from them. */
interface MetadataProvider {
    val name: String
    val supportedRegions: List<MetadataRegion>

    suspend fun search(
        region: MetadataRegion,
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: MetadataLanguage? = null,
        pageSize: MetadataSearchCount? = null,
    ): List<MetadataSearchBook>

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

    suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: MetadataRegion,
    ): MetadataSeries?
}

/**
 * A name is not something a provider can be asked for directly, so answering a lookup by name can cost one request per
 * result. The results are therefore returned as a flow which resolves while it is collected: a caller which only needs
 * the best match does not pay for the ones behind it.
 */
interface MetadataAgent : MetadataProvider {
    fun getAuthorByName(
        authorName: String,
        region: MetadataRegion,
        language: MetadataLanguage? = null,
    ): Flow<MetadataAuthor>

    fun getBookByName(
        bookName: String,
        region: MetadataRegion,
        authorName: String? = null,
        language: MetadataLanguage? = null,
    ): Flow<MetadataBook>

    fun getSeriesByName(
        seriesName: String,
        region: MetadataRegion,
        authorName: String? = null,
        language: MetadataLanguage? = null,
    ): Flow<MetadataSeries>
}
