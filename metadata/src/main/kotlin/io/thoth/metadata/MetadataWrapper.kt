package io.thoth.metadata

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.thoth.metadata.responses.*
import kotlinx.coroutines.*
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.util.*


class MetadataWrapper constructor(
    private val providerList: List<MetadataProvider>,
) : MetadataProvider {
    override var uniqueName = "MetadataWrapper"
    private val separator = "--thṓth--"

    private val providerMap by lazy { providerList.associateBy { it.uniqueName } }
    private val searchCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataSearchBook>>()
    private val authorNameCache =
        Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataAuthor>>()
    private val seriesNameCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataSeries>>()
    private val bookNameCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataBook>>()
    private val authorIdCache =
        Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataAuthor>>()
    private val seriesIdCache = Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataSeries>>()
    private val bookIdCache = Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataBook>>()

    override suspend fun search(
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBook> {
        val cacheKey = getKey(keywords, title, author, narrator, language, pageSize)

        return getOrSetCache(searchCache, cacheKey) {
            providerList.map {
                async {
                    it.search(
                        keywords, title, author, narrator, language, pageSize
                    )
                }
            }.awaitAll().flatten()

        }
    }

    override suspend fun getAuthorByID(providerId: String, authorId: String): MetadataAuthor? {
        val cacheKey = getKey(authorId, providerId)

        return getOrSetCache(authorIdCache, cacheKey) {
            val provider = getProvider(authorId) ?: return@getOrSetCache Optional.ofNullable(null)
            val value = provider.getAuthorByID(providerId, authorId)
            Optional.ofNullable(value)
        }.orElse(null)
    }

    override suspend fun getBookByID(providerId: String, bookId: String): MetadataBook? {
        val cacheKey = getKey(bookId, providerId)
        return getOrSetCache(bookIdCache, cacheKey) {
            val provider = getProvider(bookId) ?: return@getOrSetCache Optional.ofNullable(null)
            val value = provider.getBookByID(providerId, bookId)
            Optional.ofNullable(value)
        }.orElse(null)
    }

    override suspend fun getSeriesByID(providerId: String, seriesId: String): MetadataSeries? {
        val cacheKey = getKey(seriesId, providerId)
        return getOrSetCache(seriesIdCache, cacheKey) {
            val provider = getProvider(seriesId) ?: return@getOrSetCache Optional.ofNullable(null)
            val value = provider.getSeriesByID(providerId, seriesId)
            Optional.ofNullable(value)
        }.orElse(null)
    }

    override suspend fun getAuthorByName(authorName: String): List<MetadataAuthor> {
        val cacheKey = getKey(authorName)

        return getOrSetCache(authorNameCache, cacheKey) {
            val authors = providerList.map { async { it.getAuthorByName(authorName) } }.awaitAll().flatten()
                .filter { it.name != null }
            FuzzySearch.extractSorted(authorName, authors) { it.name }.map { it.referent }
        }
    }

    override suspend fun getBookByName(bookName: String, authorName: String?): List<MetadataBook> {
        val cacheKey = getKey(bookName, authorName)
        return getOrSetCache(bookNameCache, cacheKey) {
            val books = providerList.map { async { it.getBookByName(bookName, authorName) } }.awaitAll().flatten()
                .filter { it.title != null }
            FuzzySearch.extractSorted(bookName, books) { it.title }.map { it.referent }
        }
    }

    override suspend fun getSeriesByName(seriesName: String, authorName: String?): List<MetadataSeries> {
        val cacheKey = getKey(seriesName, authorName)
        return getOrSetCache(seriesNameCache, cacheKey) {
            val series = providerList.map { async { it.getSeriesByName(seriesName, authorName) } }.awaitAll().flatten()
                .filter { it.name != null }
            FuzzySearch.extractSorted(seriesName, series) { it.name }.map { it.referent }
        }
    }

    private suspend fun <K, V> getOrSetCache(cache: Cache<K, V>, key: K, getCache: suspend CoroutineScope.() -> V): V {
        var value = cache.getIfPresent(key)
        if (value != null) return value

        value = withContext(Dispatchers.IO) {
            getCache()
        }
        cache.put(key, value)
        return value
    }

    private fun getKey(vararg keys: Any?): String {
        return keys.joinToString { it.toString() + separator }
    }

    private fun getProvider(providerID: String): MetadataProvider? {
        return providerMap[providerID]
    }
}
