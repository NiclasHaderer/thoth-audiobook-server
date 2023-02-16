package io.thoth.server.api.metadata

import io.ktor.resources.*
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataProviderWithID
import io.thoth.metadata.responses.MetadataSearchCount

@Resource("")
internal class MetadataSearch(
    val keywords: String? = null,
    val title: String? = null,
    val author: String? = null,
    val narrator: String? = null,
    val language: MetadataLanguage? = null,
    val pageSize: MetadataSearchCount? = null,
)

@Resource("{itemID}")
internal class AuthorID(
    override val itemID: String,
    override val provider: String,
) : MetadataProviderWithID

@Resource("{itemID}")
internal class SeriesID(
    override val itemID: String,
    override val provider: String,
) : MetadataProviderWithID

@Resource("{itemID}")
internal class BookID(
    override val itemID: String,
    override val provider: String,
) : MetadataProviderWithID

@Resource("")
internal class SeriesName(
    val name: String,
    val authorName: String? = null,
)

@Resource("")
internal class BookName(
    val name: String,
    val authorName: String?= null,
)

@Resource("")
internal class AuthorName(
    val name: String,
)
