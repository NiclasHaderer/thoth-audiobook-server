package io.thoth.metadata

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.thoth.metadata.responses.MetadataSearchCount
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.parser.Parser
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val log = logger {}

internal fun ParametersBuilder.appendOptional(
    name: String,
    value: String?,
) {
    if (value != null) append(name, value)
}

internal fun MetadataSearchCount.toResultCount(maxResults: Int): Int =
    when (this) {
        MetadataSearchCount.Small -> 20
        MetadataSearchCount.Medium -> 30
        MetadataSearchCount.Large -> 40
        MetadataSearchCount.ExtraLarge -> maxResults
    }

internal fun fullTextQuery(vararg terms: String?): String? = terms.filterNotNull().joinToString(" ").trim().ifEmpty { null }

internal fun parseDateOrNull(
    providerName: String,
    date: String?,
    parse: (String) -> LocalDate,
): LocalDate? =
    date?.let {
        try {
            parse(it)
        } catch (e: DateTimeParseException) {
            log.warn(e) { "$providerName answered with the unparsable date '$it'" }
            null
        }
    }

internal suspend fun <T> fetchChunked(
    ids: List<String>,
    chunkSize: Int,
    fetch: suspend (chunk: List<String>) -> List<T>,
): List<T> =
    coroutineScope {
        ids.chunked(chunkSize).map { chunk -> async { fetch(chunk) } }.awaitAll().flatten()
    }

internal fun httpsApiUrl(
    host: String,
    pathSegments: List<String>,
    parameters: Parameters = Parameters.Empty,
): Url =
    URLBuilder(
        protocol = URLProtocol.HTTPS,
        host = host,
        pathSegments = pathSegments,
        parameters = parameters,
    ).build()

private val htmlLineBreak = Regex("(?i)<br\\s*/?>|</p\\s*>")
private val htmlTag = Regex("<[^>]+>")
private val paddedNewline = Regex("[^\\S\n]*\n[^\\S\n]*")
private val repeatedNewline = Regex("\n{3,}")

/** Providers serve summaries as HTML fragments, while the metadata responses are plain text. */
internal fun htmlToText(html: String?): String? =
    html
        ?.replace(htmlLineBreak, "\n")
        ?.replace(htmlTag, "")
        ?.let { Parser.unescapeEntities(it, false) }
        ?.replace(paddedNewline, "\n")
        ?.replace(repeatedNewline, "\n\n")
        ?.trim()
        ?.ifEmpty { null }
