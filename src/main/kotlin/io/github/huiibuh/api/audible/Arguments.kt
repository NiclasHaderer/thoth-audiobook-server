package io.github.huiibuh.api.audible

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import io.github.huiibuh.metadata.MetadataLanguage
import io.github.huiibuh.metadata.MetadataSearchCount
import io.github.huiibuh.metadata.ProviderWithIDMetadata

internal data class AudibleSearch(
    @QueryParam("A general keyword which describes the search term") val keywords: String?,
    @QueryParam("The title of the audiobook") val title: String?,
    @QueryParam("The author of the audiobook") val author: String?,
    @QueryParam("The narrator of the audiobook") val narrator: String?,
    @QueryParam("The language of the audiobook") val language: MetadataLanguage?,
    @QueryParam("How many search results do you want to get") val pageSize: MetadataSearchCount?,
)

@Path("{id}")
internal data class AuthorID(
    @PathParam("The id of the author. Can be retrieved by using the search") override val itemID: String,
    @PathParam("The id of search provider the id is associated to") override val provider: String,
) : ProviderWithIDMetadata

@Path("{id}")
internal data class SeriesID(
    @PathParam("The id of the series. Can be retrieved by using the search") override val itemID: String,
    @PathParam("The id of search provider the id is associated to") override val provider: String,
) : ProviderWithIDMetadata

@Path("{asin}")
internal data class BookID(
    @PathParam("The asin of the series. Can be retrieved by using the search") override val itemID: String,
    @PathParam("The id of search provider the id is associated to") override val provider: String,
) : ProviderWithIDMetadata

