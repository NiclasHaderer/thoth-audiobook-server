package io.github.huiibuh.api

import com.papsign.ktor.openapigen.APITag


enum class ApiTags(override val description: String = "hello world") : APITag {
    Series("All things concerning series"),
    Books("All things concerning books"),
    Authors("All things concerning authors"),
    Audible("Wrapper for the audible api"),
    Search("Search for books, authors and series"),
    Files("All things concerned to file transfers like images and audio files")
}
