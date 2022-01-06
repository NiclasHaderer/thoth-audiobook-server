package io.github.huiibuh.metadata


interface MetadataProvider {
    val uniqueName: String

    suspend fun search(
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: MetadataLanguage? = null,
        pageSize: MetadataSearchCount? = null,
    ): List<SearchResultMetadata>

    suspend fun getAuthorByID(authorID: ProviderWithID): AuthorMetadata?

    suspend fun getAuthorByName(authorName: String): AuthorMetadata?

    suspend fun getBookByID(bookID: ProviderWithID): BookMetadata?

    suspend fun getBookByName(bookName: String): BookMetadata?

    suspend fun getSeriesByID(seriesID: ProviderWithID): SeriesMetadata?

    suspend fun getSeriesByName(seriesName: String): SeriesMetadata?
}
