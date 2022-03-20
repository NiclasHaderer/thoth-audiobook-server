package io.github.huiibuh.api

import com.papsign.ktor.openapigen.APITag


enum class ApiTags(override val description: String) : APITag {
    Series("All things concerning series"),
    Books("All things concerning books"),
    Rescan("Rescan folders for changes"),
    Authors("All things concerning authors"),
    Metadata("Search for metadata information for books"),
    Search("Search for books, authors and series"),
    Files("All things concerned to file transfers like images and audio files")
}
