package io.github.huiibuh.metadata

import java.util.*

enum class MetadataLanguage {
    Spanish, English, German, French, Italian, Danish, Finnish, Norwegian, Swedish, Russian,
}

enum class MetadataSearchCount {
    Small, Medium, Large, ExtraLarge,
}

interface ProviderWithIDMetadata {
    val provider: String
    val itemID: String
}

open class ProviderWithIDMetadataImpl(
    override val provider: String,
    override val itemID: String
) :
    ProviderWithIDMetadata

interface SearchAuthorMetadata {
    val id: ProviderWithIDMetadata
    val name: String?
    val link: String
}

open class SearchAuthorMetadataImpl(
    override val id: ProviderWithIDMetadata,
    override val name: String?,
    override val link: String
) : SearchAuthorMetadata

interface AuthorMetadata : SearchAuthorMetadata {
    val image: String?
    val biography: String?
}

open class AuthorMetadataImpl(
    override val id: ProviderWithIDMetadata,
    override val name: String?,
    override val link: String,
    override val image: String?,
    override val biography: String?
) : AuthorMetadata

interface SearchSeriesMetadata {
    val id: ProviderWithIDMetadata
    val name: String
    val index: Float?
    val link: String
}

open class SearchSeriesMetadataImpl(
    override val id: ProviderWithIDMetadata,
    override val name: String,
    override val index: Float?,
    override val link: String
) : SearchSeriesMetadata

interface BookMetadata {
    val description: String?
    val id: ProviderWithIDMetadata
    val title: String?
    val narrator: String?
    val link: String?
    val author: SearchAuthorMetadata?
    val series: SearchSeriesMetadata?
    val image: String?
    val year: Int?
}

open class BookMetadataImpl(
    override val description: String?,
    override val id: ProviderWithIDMetadata,
    override val title: String?,
    override val narrator: String?,
    override val link: String?,
    override val author: SearchAuthorMetadata?,
    override val series: SearchSeriesMetadata?,
    override val image: String?,
    override val year: Int?
) : BookMetadata


interface SearchBookMetadata {
    val id: ProviderWithIDMetadata
    val title: String?
    val link: String?
    val author: SearchAuthorMetadata?
    val narrator: String?
    val series: SearchSeriesMetadata?
    val image: String?
    val language: String?
    val releaseDate: Date?
}

open class SearchBookMetadataImpl(
    override val id: ProviderWithIDMetadata,
    override val title: String?,
    override val link: String?,
    override val author: SearchAuthorMetadata?,
    override val narrator: String?,
    override val series: SearchSeriesMetadata?,
    override val image: String?,
    override val language: String?,
    override val releaseDate: Date?
) : SearchBookMetadata

interface SeriesMetadata {
    val id: ProviderWithIDMetadata
    val link: String
    val name: String?
    val description: String?
    val amount: Int?
    val books: List<SearchBookMetadata>?
    val author: String?
}

open class SeriesMetadataImpl(
    override val id: ProviderWithIDMetadata,
    override val link: String,
    override val name: String?,
    override val description: String?,
    override val amount: Int?,
    override val books: List<SearchBookMetadata>?,
    override val author: String?
) : SeriesMetadata
