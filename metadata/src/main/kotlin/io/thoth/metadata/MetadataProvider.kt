package io.thoth.metadata

interface MetadataProvider {
    val uniqueName: String

    suspend fun search(
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: MetadataLanguage? = null,
        pageSize: MetadataSearchCount? = null,
    ): List<SearchBookMetadata>

    suspend fun getAuthorByID(providerId: String, authorId: String): AuthorMetadata?

    suspend fun getAuthorByName(authorName: String): List<AuthorMetadata>

    suspend fun getBookByID(providerId: String, bookId: String): BookMetadata?

    suspend fun getBookByName(bookName: String, authorName: String? = null): List<BookMetadata>

    suspend fun getSeriesByID(providerId: String, seriesId: String): SeriesMetadata?

    suspend fun getSeriesByName(seriesName: String, authorName: String? = null): List<SeriesMetadata>
}
