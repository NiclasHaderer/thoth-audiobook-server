package io.github.huiibuh.audible.models

import java.util.*

interface AudibleSearchAuthor {
    val asin: String
    val name: String?
    val link: String
}


interface AudibleSearchSeries {
    val asin: String
    val name: String
    val index: Float
    val link: String
}

interface AudibleSearchResult {
    val asin: String
    val title: String?
    val link: String?
    val author: AudibleSearchAuthor?
    val series: AudibleSearchSeries?
    val image: String?
    val language: String?
    val releaseDate: Date?
}

enum class AudibleSearchLanguage(val language: Long) {
    Spanish(16290345031),
    English(16290310031),
    German(16290314031),
    French(16290313031),
    Italian(16290322031),
    Danish(16290308031),
    Finnish(16290312031),
    Norwegian(16290333031),
    Swedish(16290346031),
    Russian(16290340031),
}

enum class AudibleSearchAmount(val size: Int) {
    Twenty(20),
    Thirty(30),
    Forty(40),
    Fifty(50),
}
