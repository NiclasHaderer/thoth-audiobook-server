package io.github.huiibuh.audible.api

interface AudibleAuthor {
    val name: String
    val link: String
}

interface AudibleTitle {
    val name: String
    val link: String
}

interface AudibleSeries {
    val name: String
    val index: Number
    val link: String
}

interface AudibleSearchResult {
    val author: AudibleAuthor?
    val title: AudibleTitle?
    val series: AudibleSeries?
    val imageUrl: String?
}

enum class AudibleLanguage(val language: Long) {
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

enum class AudiblePageSize(val size: Int) {
    Twenty(20),
    Thirty(30),
    Forty(40),
    Fifty(50),
}
