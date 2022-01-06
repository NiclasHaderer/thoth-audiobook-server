package io.github.huiibuh.metadata

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
enum class MetadataLanguage {
    Spanish,
    English,
    German,
    French,
    Italian,
    Danish,
    Finnish,
    Norwegian,
    Swedish,
    Russian,
}

@Serializable
enum class MetadataSearchCount {
    Small,
    Medium,
    Large,
    ExtraLarge,
}

interface AuthorMetadata : SearchAuthorMetadata {
    val image: String?
    val biography: String?
}

interface ProviderWithID {
    val uniqueProviderName: String
    val id: String
}

interface SearchAuthorMetadata {
    val id: ProviderWithID
    val name: String?
    val link: String
}

interface BookMetadata {
    val description: String?
    val id: ProviderWithID
    val title: String?
    val link: String?
    val author: SearchAuthorMetadata?
    val series: SearchSeriesMetadata?
    val image: String?
}

interface SeriesMetadata {
    val id: ProviderWithID
    val link: String
    val name: String?
    val description: String?
    val amount: Int?
    val books: List<SearchResultMetadata>
}

interface SearchSeriesMetadata {
    val id: ProviderWithID
    val name: String
    val index: Float?
    val link: String
}

interface SearchResultMetadata {
    val id: ProviderWithID
    val title: String?
    val link: String?
    val author: SearchAuthorMetadata?
    val narrator: SearchAuthorMetadata?
    val series: SearchSeriesMetadata?
    val image: String?
    val language: String?
    val releaseDate: Date?
}
