package io.github.huiibuh.dependencies

import io.github.huiibuh.audible.client.AudibleClient
import io.github.huiibuh.audible.models.*
import org.koin.dsl.module


interface AudibleService {
    suspend fun search(
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: AudibleSearchLanguage? = null,
        pageSize: AudibleSearchAmount? = null,
    ): List<AudibleSearchResult>

    suspend fun getAuthorInfo(authorASIN: String): AudibleAuthor
    suspend fun getBookInfo(bookASIN: String): AudibleBook
    suspend fun getSeriesInfo(seriesASIN: String): AudibleSeries
}

class AudibleServiceImpl(private val client: AudibleClient) : AudibleService {
    override suspend fun search(
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: AudibleSearchLanguage?,
        pageSize: AudibleSearchAmount?,
    ): List<AudibleSearchResult> {
        return client.search(keywords, title, author, narrator, language, pageSize)
    }

    override suspend fun getAuthorInfo(authorASIN: String): AudibleAuthor {
        return client.getAuthorInfo(authorASIN)
    }

    override suspend fun getBookInfo(bookASIN: String): AudibleBook {
        return client.getBookInfo(bookASIN)
    }

    override suspend fun getSeriesInfo(seriesASIN: String): AudibleSeries {
        return client.getSeriesInfo(seriesASIN)
    }
}

val AudibleAppModule = module {
    single<AudibleService> { AudibleServiceImpl(AudibleClient()) }
}
