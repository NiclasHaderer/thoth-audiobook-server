package io.thoth.openapi.common

fun String.padLinesStart(paddingChar: Char, times: Int): String {
    val lines = this.lines()
    val paddedLines = lines.map { line -> "$paddingChar".repeat(times) + line }
    return paddedLines.joinToString("\n")
}
