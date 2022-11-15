package io.thoth.metadata

import java.time.LocalDate

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


interface SearchAuthorMetadata {
    val id: ProviderWithIDMetadata
    val name: String?
    val link: String
}


interface AuthorMetadata : SearchAuthorMetadata {
    val image: String?
    val biography: String?
}


interface SearchSeriesMetadata {
    val id: ProviderWithIDMetadata
    val name: String?
    val index: Float?
    val link: String
}


interface BookMetadata {
    val description: String?
    val id: ProviderWithIDMetadata
    val title: String?
    val narrator: String?
    val link: String?
    val author: SearchAuthorMetadata?
    val series: SearchSeriesMetadata?
    val image: String?
    val date: LocalDate?
}


interface SearchBookMetadata {
    val id: ProviderWithIDMetadata
    val title: String?
    val link: String?
    val author: SearchAuthorMetadata?
    val narrator: String?
    val series: SearchSeriesMetadata?
    val image: String?
    val language: String?
    val releaseDate: LocalDate?
}

interface SeriesMetadata {
    val id: ProviderWithIDMetadata
    val link: String
    val name: String?
    val description: String?
    val amount: Int?
    val books: List<SearchBookMetadata>?
    val author: String?
}
