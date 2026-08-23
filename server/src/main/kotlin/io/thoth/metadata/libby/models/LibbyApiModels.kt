package io.thoth.metadata.libby.models

import kotlinx.serialization.Serializable

@Serializable
internal data class LibbyApiSearchResponse(
    val items: List<LibbyApiMedia> = emptyList(),
)

@Serializable
internal data class LibbyApiMedia(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val creators: List<LibbyApiCreator> = emptyList(),
    val languages: List<LibbyApiLanguage> = emptyList(),
    val publisher: LibbyApiPublisher? = null,
    val detailedSeries: LibbyApiDetailedSeries? = null,
    val covers: Map<String, LibbyApiCover> = emptyMap(),
    val formats: List<LibbyApiFormat> = emptyList(),
    val type: LibbyApiMediaType? = null,
    val starRating: Float? = null,
    val publishDate: String? = null,
    val estimatedReleaseDate: String? = null,
)

@Serializable
internal data class LibbyApiCreator(
    val id: Long? = null,
    val name: String? = null,
    val role: String? = null,
)

@Serializable
internal data class LibbyApiLanguage(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
internal data class LibbyApiPublisher(
    val name: String? = null,
)

@Serializable
internal data class LibbyApiDetailedSeries(
    val seriesId: Long? = null,
    val seriesName: String? = null,
    val readingOrder: String? = null,
)

@Serializable
internal data class LibbyApiCover(
    val href: String? = null,
    val width: Int? = null,
)

@Serializable
internal data class LibbyApiFormat(
    val id: String? = null,
    val isbn: String? = null,
)

@Serializable
internal data class LibbyApiMediaType(
    val id: String? = null,
)

@Serializable
internal data class LibbyApiSeriesResponse(
    val id: Long? = null,
    val name: String? = null,
    val items: List<LibbyApiSeriesItem> = emptyList(),
)

@Serializable
internal data class LibbyApiSeriesItem(
    val id: String? = null,
    val title: String? = null,
    val readingOrder: String? = null,
    val rank: Int? = null,
)
