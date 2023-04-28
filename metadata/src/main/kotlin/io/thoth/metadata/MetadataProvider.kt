package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries

interface MetadataProvider {
    val uniqueName: String
    val supportedCountryCodes: List<String>

    suspend fun search(
        region: String,
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: MetadataLanguage? = null,
        pageSize: MetadataSearchCount? = null,
    ): List<MetadataSearchBook>

    suspend fun getAuthorByID(providerId: String, authorId: String, region: String): MetadataAuthor?

    suspend fun getAuthorByName(authorName: String, region: String): List<MetadataAuthor>

    suspend fun getBookByID(providerId: String, bookId: String, region: String): MetadataBook?

    suspend fun getBookByName(bookName: String, region: String, authorName: String? = null): List<MetadataBook>

    suspend fun getSeriesByID(providerId: String, region: String, seriesId: String): MetadataSeries?

    suspend fun getSeriesByName(seriesName: String, region: String, authorName: String? = null): List<MetadataSeries>
}
