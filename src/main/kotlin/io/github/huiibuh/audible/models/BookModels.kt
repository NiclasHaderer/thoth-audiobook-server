package io.github.huiibuh.audible.models

interface AudibleBook {
    val description: String?
    val asin: String
    val title: String?
    val link: String?
    val author: AudibleSearchAuthor?
    val series: AudibleSearchSeries?
    val image: String?
}
