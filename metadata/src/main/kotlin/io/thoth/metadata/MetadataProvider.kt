package io.thoth.metadata

import io.thoth.metadata.responses.*

interface MetadataProvider {
  val uniqueName: String

  suspend fun search(
      keywords: String? = null,
      title: String? = null,
      author: String? = null,
      narrator: String? = null,
      language: MetadataLanguage? = null,
      pageSize: MetadataSearchCount? = null,
  ): List<MetadataSearchBook>

  suspend fun getAuthorByID(providerId: String, authorId: String): MetadataAuthor?

  suspend fun getAuthorByName(authorName: String): List<MetadataAuthor>

  suspend fun getBookByID(providerId: String, bookId: String): MetadataBook?

  suspend fun getBookByName(bookName: String, authorName: String? = null): List<MetadataBook>

  suspend fun getSeriesByID(providerId: String, seriesId: String): MetadataSeries?

  suspend fun getSeriesByName(seriesName: String, authorName: String? = null): List<MetadataSeries>
}
