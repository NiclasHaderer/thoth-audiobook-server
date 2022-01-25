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
    ): List<SearchBookMetadata>

    suspend fun getAuthorByID(authorID: ProviderWithIDMetadata): AuthorMetadata?

    suspend fun getAuthorByName(authorName: String): AuthorMetadata?

    suspend fun getBookByID(bookID: ProviderWithIDMetadata): BookMetadata?

    suspend fun getBookByName(bookName: String, authorName: String? = null): BookMetadata?

    suspend fun getSeriesByID(seriesID: ProviderWithIDMetadata): SeriesMetadata?

    suspend fun getSeriesByName(seriesName: String, authorName: String? = null): SeriesMetadata?
}

interface MetadataProviderWrapper : MetadataProvider {
    override suspend fun getAuthorByID(authorID: ProviderWithIDMetadata): AuthorMetadata
    override suspend fun getBookByID(bookID: ProviderWithIDMetadata): BookMetadata
    override suspend fun getSeriesByID(seriesID: ProviderWithIDMetadata): SeriesMetadata
}
