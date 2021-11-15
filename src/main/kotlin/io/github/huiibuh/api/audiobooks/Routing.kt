package io.github.huiibuh.api.audiobooks

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.api.audiobooks.albums.albumsRouting
import io.github.huiibuh.api.audiobooks.artists.artistsRouting
import io.github.huiibuh.api.audiobooks.collections.collectionsRouting

fun NormalOpenAPIRoute.registerAudiobookRouting(route: String = "audiobooks") {
    tag(ApiTags.Audiobook) {
        route(route) {
            albumsRouting()
            artistsRouting()
            collectionsRouting()
        }
    }
}
