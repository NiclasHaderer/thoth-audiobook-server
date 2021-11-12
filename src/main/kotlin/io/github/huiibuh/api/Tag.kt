package io.github.huiibuh.api

import com.papsign.ktor.openapigen.APITag


enum class ApiTags(override val description: String = "hello world") : APITag {
    Audiobook("All things concerning audiobooks"),
    Audible("Wrapper for the audible api"),
    Stream("Stream the audio files")
}
