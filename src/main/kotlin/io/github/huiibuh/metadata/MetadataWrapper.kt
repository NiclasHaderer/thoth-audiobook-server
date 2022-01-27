package io.github.huiibuh.metadata

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.huiibuh.api.exceptions.APIBadRequest
import io.ktor.features.*
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.util.*


class MetadataWrapper constructor(
    private val providerList: List<MetadataProvider>,
) : MetadataProvider {
    override var uniqueName = "MetadataWrapper"
    private val separator = "thṓth"

    private val providerMap by lazy { providerList.associateBy { it.uniqueName } }
    private val searchCache = Caffeine.newBuilder().maximumSize(1000).build<String, List<SearchBookMetadata>>()
    private val authorIdCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<AuthorMetadata>>()
    private val authorNameCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<AuthorMetadata>>()
    private val seriesIdCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<SeriesMetadata>>()
    private val seriesNameCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<SeriesMetadata>>()
    private val bookIdCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<BookMetadata>>()
    private val bookNameCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<BookMetadata>>()

    override suspend fun search(
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<SearchBookMetadata> {
        val cacheKey = getKey(keywords, title, author, narrator, language, pageSize)

        return getOrSetCache(searchCache, cacheKey) {
            providerList.flatMap { it.search(keywords, title, author, narrator, language, pageSize) }
        }
    }

    @Throws(NotFoundException::class, APIBadRequest::class)
    override suspend fun getAuthorByID(authorID: ProviderWithIDMetadata): AuthorMetadata? {
        val cacheKey = getKey(authorID.itemID, authorID.provider)

        return getOrSetCache(authorIdCache, cacheKey) {
            val provider = getProvider(authorID)
            val value = provider.getAuthorByID(authorID)
            Optional.ofNullable(value)
        }.orElse(null)
    }

    override suspend fun getAuthorByName(authorName: String): AuthorMetadata? {
        val cacheKey = getKey(authorName)

        return getOrSetCache(authorNameCache, cacheKey) {
            val authors = providerList.map { it.getAuthorByName(authorName) }.filter { it?.name != null }
            val value = if (authors.isEmpty()) {
                null
            } else {
                FuzzySearch.extractOne(authorName, authors) { it?.name }.referent
            }
            Optional.ofNullable(value)
        }.orElse(null)
    }

    override suspend fun getBookByID(bookID: ProviderWithIDMetadata): BookMetadata? {
        val cacheKey = getKey(bookID.itemID, bookID.provider)
        return getOrSetCache(bookIdCache, cacheKey) {
            val provider = getProvider(bookID)
            val value = provider.getBookByID(bookID)
            Optional.ofNullable(value)
        }.orElse(null)
    }

    override suspend fun getBookByName(bookName: String, authorName: String?): BookMetadata? {
        val cacheKey = getKey(bookName, authorName)
        return getOrSetCache(bookNameCache, cacheKey) {
            val books = providerList.map { it.getBookByName(bookName, authorName) }.filter { it?.title != null }
            if (books.isEmpty()) {
                Optional.empty<BookMetadata>()
            } else {
                Optional.ofNullable(FuzzySearch.extractOne(bookName, books) { it?.title }.referent)
            }
        }.orElse(null)
    }

    override suspend fun getSeriesByID(seriesID: ProviderWithIDMetadata): SeriesMetadata? {
        val cacheKey = getKey(seriesID.itemID, seriesID.provider)
        return getOrSetCache(seriesIdCache, cacheKey) {
            val provider = getProvider(seriesID)
            val value = provider.getSeriesByID(seriesID)
            Optional.ofNullable(value)
        }.orElse(null)
    }

    override suspend fun getSeriesByName(seriesName: String, authorName: String?): SeriesMetadata? {
        val cacheKey = getKey(seriesName, authorName)
        return getOrSetCache(seriesNameCache, cacheKey) {
            val series = providerList.map { it.getSeriesByName(seriesName, authorName) }.filter { it?.name != null }
            val value = if (series.isEmpty()) {
                null
            } else {
                FuzzySearch.extractOne(seriesName, series) { it?.name }.referent
            }
            Optional.ofNullable(value)
        }.orElse(null)
    }


    private suspend fun <K, V> getOrSetCache(cache: Cache<K, V>, key: K, getCache: suspend () -> V): V {
        var value = cache.getIfPresent(key)
        if (value != null) return value
        value = getCache()
        cache.put(key, value)
        return value
    }

    private fun getKey(vararg keys: Any?): String {
        return keys.joinToString { keys.toString() + separator }
    }

    @kotlin.jvm.Throws(APIBadRequest::class)
    private fun getProvider(providerID: ProviderWithIDMetadata): MetadataProvider {
        return providerMap[providerID.provider]
            ?: throw APIBadRequest("Provider with id ${providerID.provider} was not found")
    }
}
