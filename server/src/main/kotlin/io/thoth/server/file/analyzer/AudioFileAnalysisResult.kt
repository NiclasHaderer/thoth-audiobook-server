package io.thoth.server.file.analyzer

import java.time.LocalDate

class AudioFileAnalysisResultImpl(
    override val title: String,
    override val authors: List<String>,
    override val book: String,
    override val duration: Int,
    override val path: String,
    override val lastModified: Long,
    override val description: String? = null,
    override val date: LocalDate? = null,
    override val language: String? = null,
    override val trackNr: Int? = null,
    override val narrator: String? = null,
    override val series: String? = null,
    override val seriesIndex: Float? = null,
    override val cover: ByteArray? = null,
) : AudioFileAnalysisResult

interface AudioFileAnalysisResult {
    val title: String
    val authors: List<String>
    val book: String
    val description: String?
    val date: LocalDate?
    val language: String?
    val trackNr: Int?
    val narrator: String?
    val series: String?
    val seriesIndex: Float?
    val cover: ByteArray?
    val duration: Int
    val path: String
    val lastModified: Long
}
