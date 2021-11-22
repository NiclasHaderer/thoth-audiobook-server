package io.github.huiibuh.api.audible

import api.exceptions.withNotFoundHandling
import audible.models.AudibleAuthor
import audible.models.AudibleBook
import audible.models.AudibleSearchResult
import audible.models.AudibleSeries
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags

fun NormalOpenAPIRoute.registerAudibleRouting(path: String = "audible") {
    tag(ApiTags.Audible) {
        route(path) {
            routing()
        }
    }
}


internal fun NormalOpenAPIRoute.routing() {
    route("search").get(
        info("Search for audiobooks"),
        body = OpenAPIPipelineResponseContext<List<AudibleSearchResult>>::search
    )
    withNotFoundHandling {
        route("author").get(body = OpenAPIPipelineResponseContext<AudibleAuthor>::getAuthor)
        route("series").get(body = OpenAPIPipelineResponseContext<AudibleSeries>::getSeries)
        route("book").get(body = OpenAPIPipelineResponseContext<AudibleBook>::getBook)
    }
}
