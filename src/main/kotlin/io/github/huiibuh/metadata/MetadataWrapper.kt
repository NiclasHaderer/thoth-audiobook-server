package io.github.huiibuh.metadata

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.huiibuh.api.exceptions.APIBadRequest
import io.ktor.features.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.util.*


class MetadataWrapper constructor(
    private val providerList: List<MetadataProvider>,
    private val byNameSearchAmount: Int = 5
) : MetadataProvider {
    override var uniqueName = "MetadataWrapper"
    private val separator = "--thṓth--"

    private val providerMap by lazy { providerList.associateBy { it.uniqueName } }
    private val searchCache = Caffeine.newBuilder().maximumSize(1000).build<String, List<SearchBookMetadata>>()
    private val authorNameCache = Caffeine.newBuilder().maximumSize(1000).build<String, List<AuthorMetadata>>()
    private val seriesNameCache = Caffeine.newBuilder().maximumSize(1000).build<String, List<SeriesMetadata>>()
    private val bookNameCache = Caffeine.newBuilder().maximumSize(1000).build<String, List<BookMetadata>>()
    private val authorIdCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<AuthorMetadata>>()
    private val seriesIdCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<SeriesMetadata>>()
    private val bookIdCache = Caffeine.newBuilder().maximumSize(1000).build<String, Optional<BookMetadata>>()

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
            providerList.map {
                coroutineScope {
                    async {
                        it.search(
                            keywords,
                            title,
                            author,
                            narrator,
                            language,
                            pageSize
                        )
                    }
                }
            }.awaitAll().flatten()
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


    override suspend fun getBookByID(bookID: ProviderWithIDMetadata): BookMetadata? {
        val cacheKey = getKey(bookID.itemID, bookID.provider)
        return getOrSetCache(bookIdCache, cacheKey) {
            val provider = getProvider(bookID)
            val value = provider.getBookByID(bookID)
            Optional.ofNullable(value)
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

    override suspend fun getAuthorByName(authorName: String): List<AuthorMetadata> {
        val cacheKey = getKey(authorName)

        return getOrSetCache(authorNameCache, cacheKey) {
            val authors = coroutineScope {
                providerList.map { async { it.getAuthorByName(authorName) } }
                    .awaitAll().flatten().filter { it.name != null }
            }
            FuzzySearch.extractSorted(authorName, authors) { it.name }
                .take(byNameSearchAmount)
                .map { it.referent }
        }
    }

    override suspend fun getBookByName(bookName: String, authorName: String?): List<BookMetadata> {
        val cacheKey = getKey(bookName, authorName)
        return getOrSetCache(bookNameCache, cacheKey) {
            val books = coroutineScope {
                providerList.map { async { it.getBookByName(bookName, authorName) } }
                    .awaitAll().flatten().filter { it.title != null }
            }
            FuzzySearch.extractSorted(bookName, books) { it.title }
                .take(byNameSearchAmount)
                .map { it.referent }
        }
    }

    override suspend fun getSeriesByName(seriesName: String, authorName: String?): List<SeriesMetadata> {
        val cacheKey = getKey(seriesName, authorName)
        return getOrSetCache(seriesNameCache, cacheKey) {
            val series = coroutineScope {
                providerList.map { async { it.getSeriesByName(seriesName, authorName) } }
                    .awaitAll().flatten().filter { it.name != null }
            }
            FuzzySearch.extractSorted(seriesName, series) { it.name }
                .take(byNameSearchAmount)
                .map { it.referent }
        }
    }

    private suspend fun <K, V> getOrSetCache(cache: Cache<K, V>, key: K, getCache: suspend () -> V): V {
        var value = cache.getIfPresent(key)
        if (value != null) return value
        value = getCache()
        cache.put(key, value)
        return value
    }

    private fun getKey(vararg keys: Any?): String {
        return keys.joinToString { it.toString() + separator }
    }

    @kotlin.jvm.Throws(APIBadRequest::class)
    private fun getProvider(providerID: ProviderWithIDMetadata): MetadataProvider {
        return providerMap[providerID.provider]
            ?: throw APIBadRequest("Provider with id ${providerID.provider} was not found")
    }
}
