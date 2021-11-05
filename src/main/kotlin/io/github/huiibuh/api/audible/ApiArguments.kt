package io.github.huiibuh.api.audible

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import io.github.huiibuh.audible.models.AudibleSearchAmount
import io.github.huiibuh.audible.models.AudibleSearchLanguage

class AudibleSearch(
    @QueryParam("A general keyword which describes the search term") val keywords: String?,
    @QueryParam("The title of the audiobook") val title: String?,
    @QueryParam("The author of the audiobook") val author: String?,
    @QueryParam("The narrator of the audiobook") val narrator: String?,
    @QueryParam("The language of the audiobook") val language: AudibleSearchLanguage?,
    @QueryParam("How many search results do you want to get") val pageSize: AudibleSearchAmount?,
)

@Path("{asin}")
open class AuthorASIN(
    @PathParam("The asin of the author. Can be retrieved by using the search") val asin: String,
)

@Path("{asin}")
open class SeriesASIN(
    @PathParam("The asin of the series. Can be retrieved by using the search") val asin: String,
)

@Path("{asin}")
open class AudiobookASIN(
    @PathParam("The asin of the audiobook. Can be retrieved by using the search") val asin: String,
)
