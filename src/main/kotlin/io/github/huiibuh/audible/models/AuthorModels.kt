package io.github.huiibuh.audible.models

interface AudibleAuthor : AudibleSearchAuthor {
    val image: String?
    val biography: String?
}
