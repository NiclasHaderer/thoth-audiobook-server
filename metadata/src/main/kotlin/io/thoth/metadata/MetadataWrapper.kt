package io.thoth.metadata

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import me.xdrop.fuzzywuzzy.FuzzySearch

class MetadataWrapper(
    private val providerList: List<MetadataProvider>,
) : MetadataProvider {
    override var uniqueName = "MetadataWrapper"

    override val supportedCountryCodes: List<String>
        get() = providerList.flatMap { it.supportedCountryCodes }.distinct()

    private val separator = "--thṓth--"

    private val providerMap by lazy { providerList.associateBy { it.uniqueName } }
    private val searchCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataSearchBook>>()
    private val authorNameCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataAuthor>>()
    private val seriesNameCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataSeries>>()
    private val bookNameCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataBook>>()
    private val authorIdCache = Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataAuthor>>()
    private val seriesIdCache = Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataSeries>>()
    private val bookIdCache = Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataBook>>()

    override suspend fun search(
        region: String,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBook> {
        val cacheKey = getKey(keywords, title, author, narrator, language, pageSize, region)

        return getOrSetCache(searchCache, cacheKey) {
            providerList
                .map {
                    async {
                        it.search(
                            region = region,
                            keywords = keywords,
                            title = title,
                            author = author,
                            narrator = narrator,
                            language = language,
                            pageSize = pageSize,
                        )
                    }
                }
                .awaitAll()
                .flatten()
        }
    }

    override suspend fun getAuthorByID(providerId: String, authorId: String, region: String): MetadataAuthor? {
        val cacheKey = getKey(authorId, providerId, region)

        return getOrSetCache(authorIdCache, cacheKey) {
                val provider = getProvider(authorId) ?: return@getOrSetCache Optional.ofNullable(null)
                val value = provider.getAuthorByID(providerId = providerId, region = region, authorId = authorId)
                Optional.ofNullable(value)
            }
            .orElse(null)
    }

    override suspend fun getBookByID(providerId: String, bookId: String, region: String): MetadataBook? {
        val cacheKey = getKey(bookId, providerId, region)
        return getOrSetCache(bookIdCache, cacheKey) {
                val provider = getProvider(bookId) ?: return@getOrSetCache Optional.ofNullable(null)
                val value = provider.getBookByID(providerId = providerId, region = region, bookId = bookId)
                Optional.ofNullable(value)
            }
            .orElse(null)
    }

    override suspend fun getSeriesByID(providerId: String, region: String, seriesId: String): MetadataSeries? {
        val cacheKey = getKey(seriesId, providerId, region)
        return getOrSetCache(seriesIdCache, cacheKey) {
                val provider = getProvider(seriesId) ?: return@getOrSetCache Optional.ofNullable(null)
                val value = provider.getSeriesByID(providerId = providerId, region = region, seriesId = seriesId)
                Optional.ofNullable(value)
            }
            .orElse(null)
    }

    override suspend fun getAuthorByName(authorName: String, region: String): List<MetadataAuthor> {
        val cacheKey = getKey(authorName, region)

        return getOrSetCache(authorNameCache, cacheKey) {
            val authors =
                providerList
                    .map { async { it.getAuthorByName(authorName = authorName, region = region) } }
                    .awaitAll()
                    .flatten()
                    .filter { it.name != null }
            FuzzySearch.extractSorted(authorName, authors) { it.name }.map { it.referent }
        }
    }

    override suspend fun getBookByName(bookName: String, region: String, authorName: String?): List<MetadataBook> {
        val cacheKey = getKey(bookName, authorName, region)
        return getOrSetCache(bookNameCache, cacheKey) {
            val books =
                providerList
                    .map { async { it.getBookByName(bookName = bookName, region = region, authorName = authorName) } }
                    .awaitAll()
                    .flatten()
                    .filter { it.title != null }
            FuzzySearch.extractSorted(bookName, books) { it.title }.map { it.referent }
        }
    }

    override suspend fun getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String?
    ): List<MetadataSeries> {
        val cacheKey = getKey(seriesName, authorName, region)
        return getOrSetCache(seriesNameCache, cacheKey) {
            val series =
                providerList
                    .map {
                        async {
                            it.getSeriesByName(
                                seriesName = seriesName,
                                region = region,
                                authorName = authorName,
                            )
                        }
                    }
                    .awaitAll()
                    .flatten()
                    .filter { it.title != null }
            FuzzySearch.extractSorted(seriesName, series) { it.title }.map { it.referent }
        }
    }

    private suspend fun <K, V> getOrSetCache(cache: Cache<K, V>, key: K, getCache: suspend CoroutineScope.() -> V): V {
        var value = cache.getIfPresent(key)
        if (value != null) return value

        value = withContext(Dispatchers.IO) { getCache() }
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
