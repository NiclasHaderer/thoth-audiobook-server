package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.flow.Flow

/** The lookups a metadata provider has to implement itself. Everything else can be derived from them. */
interface MetadataProvider {
    val name: String
    val supportedCountryCodes: List<String>

    suspend fun search(
        region: String, // TODO make enum
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
        region: String,
    ): MetadataAuthor?

    suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBook?

    suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: String,
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
        region: String,
    ): Flow<MetadataAuthor>

    fun getBookByName(
        bookName: String,
        region: String,
        authorName: String? = null,
    ): Flow<MetadataBook>

    fun getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String? = null,
    ): Flow<MetadataSeries>
}
