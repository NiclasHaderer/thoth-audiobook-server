package io.github.huiibuh.file.analyzer

import io.github.huiibuh.models.ProviderIDModel

class AudioFileAnalysisValueImpl(
    override val title: String,
    override val author: String,
    override val book: String,
    override val duration: Int,
    override val path: String,
    override val lastModified: Long,
    override val description: String? = null,
    override val year: Int? = null,
    override val language: String? = null,
    override val trackNr: Int? = null,
    override val narrator: String? = null,
    override val series: String? = null,
    override val seriesIndex: Float? = null,
    override val cover: ByteArray? = null,
    override val providerId: ProviderIDModel? = null,
) : AudioFileAnalysisValue

interface AudioFileAnalysisValue {
    val title: String
    val author: String
    val book: String
    val providerId: ProviderIDModel?
    val description: String?
    val year: Int?
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

data class AnalysisResult(
    val success: Boolean,
    val value: AudioFileAnalysisValue?,
)
