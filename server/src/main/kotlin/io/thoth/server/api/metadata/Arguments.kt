package io.thoth.server.api.metadata

import io.ktor.resources.*
import io.thoth.metadata.MetadataLanguage
import io.thoth.metadata.MetadataSearchCount
import io.thoth.metadata.ProviderWithIDMetadata
import kotlinx.serialization.Serializable

@Resource("")
@Serializable
internal class MetadataSearch(
    val keywords: String?,
    val title: String?,
    val author: String?,
    val narrator: String?,
    val language: MetadataLanguage?,
    val pageSize: MetadataSearchCount?,
)

@Resource("{id}")
@Serializable
internal class AuthorID(
    override val itemID: String,
    override val provider: String,
) : ProviderWithIDMetadata

@Resource("{id}")
@Serializable
internal class SeriesID(
    override val itemID: String,
    override val provider: String,
) : ProviderWithIDMetadata

@Resource("{asin}")
@Serializable
internal class BookID(
    override val itemID: String,
    override val provider: String,
) : ProviderWithIDMetadata

@Resource("{name}")
@Serializable
internal class SeriesName(
    val name: String,
    val authorName: String?,
)

@Resource("{name}")
@Serializable
internal class BookName(
    val name: String,
    val authorName: String?,
)

@Resource("{name}")
@Serializable
internal class AuthorName(
    val name: String,
)
