package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.Book
import io.thoth.models.ListeningHistoryEntry
import io.thoth.models.PaginatedResponse
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.put
import io.thoth.server.plugins.auth.thothPrincipal
import io.thoth.server.repositories.ProgressRepository
import org.koin.ktor.ext.inject

fun Routing.progressRouting() {
    val progressRepository by inject<ProgressRepository>()

    put<Api.Libraries.Id.Books.Id.Progress, ProgressUpdate, Unit> { route, body ->
        progressRepository.upsert(thothPrincipal().userId, route.libraryId, route.id, body.positionMs)
    }

    put<Api.Libraries.Id.Books.Id.Progress.Finished, SetFinished, Unit> { route, body ->
        progressRepository.setFinished(thothPrincipal().userId, route.libraryId, route.id, body.finished)
    }

    put<Api.Libraries.Id.Books.Id.Progress.Dismissed, SetDismissed, Unit> { route, body ->
        progressRepository.setDismissed(thothPrincipal().userId, route.libraryId, route.id, body.dismissed)
    }

    // Cross-library on purpose
    get<Api.Me.ContinueListening, List<Book>> { route ->
        progressRepository.continueListening(thothPrincipal().userId, route.limit)
    }

    get<Api.Me.History, PaginatedResponse<ListeningHistoryEntry>> { route ->
        val userId = thothPrincipal().userId
        PaginatedResponse(
            items = progressRepository.history(userId, route.limit, route.offset),
            total = progressRepository.historyTotal(userId),
            limit = route.limit,
            offset = route.offset,
        )
    }
}
