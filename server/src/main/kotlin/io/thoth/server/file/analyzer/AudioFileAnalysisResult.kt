package io.thoth.server.file.analyzer

import io.thoth.metadata.responses.MetadataLanguage
import java.time.LocalDate
import java.time.Instant

class AudioFileAnalysisResultImpl(
    override val title: String,
    override val authors: List<String>,
    override val book: String,
    override val durationMs: Long,
    override val path: String,
    override val lastModified: Instant,
    override val description: String? = null,
    override val date: LocalDate? = null,
    override val language: MetadataLanguage? = null,
    override val trackNr: Int? = null,
    override val narrators: List<String> = emptyList(),
    override val series: String? = null,
    override val seriesIndex: Float? = null,
    override val genres: List<String> = emptyList(),
    override val cover: ByteArray? = null,
) : AudioFileAnalysisResult

interface AudioFileAnalysisResult {
    val title: String
    val authors: List<String>
    val book: String
    val description: String?
    val date: LocalDate?
    val language: MetadataLanguage?
    val trackNr: Int?
    val narrators: List<String>
    val series: String?
    val seriesIndex: Float?
    val genres: List<String>
    val cover: ByteArray?
    val durationMs: Long
    val path: String
    val lastModified: Instant
}
