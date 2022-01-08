package io.github.huiibuh.file.analyzer

data class AudioFileAnalysisValueImpl(
    override val title: String,
) : AudioFileAnalysisValue

interface AudioFileAnalysisValue {
    val title: String
}

data class AnalysisResult(
    val success: Boolean,
    val value: AudioFileAnalysisValue?,
)
