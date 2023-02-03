package io.thoth.server.api.metadata

import io.ktor.resources.*
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataProviderWithID
import io.thoth.metadata.responses.MetadataSearchCount
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
) : MetadataProviderWithID

@Resource("{id}")
@Serializable
internal class SeriesID(
    override val itemID: String,
    override val provider: String,
) : MetadataProviderWithID

@Resource("{asin}")
@Serializable
internal class BookID(
    override val itemID: String,
    override val provider: String,
) : MetadataProviderWithID

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
