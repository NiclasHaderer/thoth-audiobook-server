package io.github.huiibuh.audible.models

interface AudibleSeries {
    val asin: String
    val link: String
    val name: String?
    val description: String?
    val amount: Int?
    val books: List<AudibleSearchResult>
}
