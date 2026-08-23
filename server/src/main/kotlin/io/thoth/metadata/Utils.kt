package io.thoth.metadata

import io.ktor.http.ParametersBuilder
import org.jsoup.parser.Parser

internal fun ParametersBuilder.appendOptional(
    name: String,
    value: String?,
) {
    if (value != null) append(name, value)
}

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
