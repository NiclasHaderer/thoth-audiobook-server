package io.github.huiibuh.api.audible

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import audible.models.AudibleAuthor
import audible.models.AudibleBook
import audible.models.AudibleSearchResult
import audible.models.AudibleSeries
import io.github.huiibuh.services.AudibleService

suspend fun OpenAPIPipelineResponseContext<List<AudibleSearchResult>>.search(query: AudibleSearch) {
    val response = AudibleService.search(query.keywords,
                                         query.title,
                                         query.author,
                                         query.narrator,
                                         query.language,
                                         query.pageSize)
    respond(response)
}


suspend fun OpenAPIPipelineResponseContext<AudibleAuthor>.getAuthor(author: AuthorASIN) {
    val response = AudibleService.getAuthorInfo(author.asin)
    respond(response)
}


suspend fun OpenAPIPipelineResponseContext<AudibleSeries>.getSeries(series: SeriesASIN) {
    val response = AudibleService.getSeriesInfo(series.asin)
    respond(response)
}


suspend fun OpenAPIPipelineResponseContext<AudibleBook>.getBook(author: AudiobookASIN) {
    val response = AudibleService.getBookInfo(author.asin)
    respond(response)
}

