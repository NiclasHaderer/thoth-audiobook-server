package io.thoth.metadata

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import java.util.Optional

abstract class MetadataAgent {
    private companion object {
        private val separator = "--thṓth--"
        private val searchCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataSearchBook>>()
        private val authorNameCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataAuthor>>()
        private val seriesNameCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataSeries>>()
        private val bookNameCache = Caffeine.newBuilder().maximumSize(50).build<String, List<MetadataBook>>()
        private val authorIdCache = Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataAuthor>>()
        private val seriesIdCache = Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataSeries>>()
        private val bookIdCache = Caffeine.newBuilder().maximumSize(50).build<String, Optional<MetadataBook>>()
    }

    abstract val name: String
    abstract val supportedCountryCodes: List<String>

    suspend fun search(
        region: String, // TODO make enum
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: MetadataLanguage? = null,
        pageSize: MetadataSearchCount? = null,
    ): List<MetadataSearchBook> {
        val cacheKey = getKey(keywords, title, author, narrator, language, pageSize, region)
        return getOrSetCache(searchCache, cacheKey) {
            searchImpl(
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

    protected abstract suspend fun searchImpl(
        region: String,
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: MetadataLanguage? = null,
        pageSize: MetadataSearchCount? = null,
    ): List<MetadataSearchBook>

    suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthor? {
        val cacheKey = getKey(providerId, authorId, region)
        return getOrSetCache(authorIdCache, cacheKey) { getAuthorByIDImpl(providerId, authorId, region).optional() }
            .orElse(null)
    }

    protected abstract suspend fun getAuthorByIDImpl(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthor?

    suspend fun getAuthorByName(
        authorName: String,
        region: String,
    ): List<MetadataAuthor> {
        val cacheKey = getKey(authorName, region)
        return getOrSetCache(authorNameCache, cacheKey) { getAuthorByNameImpl(authorName, region) }
    }

    protected abstract suspend fun getAuthorByNameImpl(
        authorName: String,
        region: String,
    ): List<MetadataAuthor>

    suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBook? {
        val cacheKey = getKey(providerId, bookId, region)
        return getOrSetCache(bookIdCache, cacheKey) { getBookByIDImpl(providerId, bookId, region).optional() }.orElse(
            null,
        )
    }

    protected abstract suspend fun getBookByIDImpl(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBook?

    suspend fun getBookByName(
        bookName: String,
        region: String,
        authorName: String? = null,
    ): List<MetadataBook> {
        val cacheKey = getKey(bookName, region, authorName)
        return getOrSetCache(bookNameCache, cacheKey) { getBookByNameImpl(bookName, region, authorName) }
    }

    protected abstract suspend fun getBookByNameImpl(
        bookName: String,
        region: String,
        authorName: String? = null,
    ): List<MetadataBook>

    suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: String,
    ): MetadataSeries? {
        val cacheKey = getKey(providerId, seriesId, region)
        return getOrSetCache(seriesIdCache, cacheKey) { getSeriesByIDImpl(providerId, region, seriesId).optional() }
            .orElse(null)
    }

    protected abstract suspend fun getSeriesByIDImpl(
        providerId: String,
        region: String,
        seriesId: String,
    ): MetadataSeries?

    suspend fun getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String? = null,
    ): List<MetadataSeries> {
        val cacheKey = getKey(seriesName, region, authorName)
        return getOrSetCache(seriesNameCache, cacheKey) { getSeriesByNameImpl(seriesName, region, authorName) }
    }

    protected abstract suspend fun getSeriesByNameImpl(
        seriesName: String,
        region: String,
        authorName: String? = null,
    ): List<MetadataSeries>

    private suspend fun <K, V> getOrSetCache(
        cache: Cache<K, V>,
        key: K,
        getValue: suspend () -> V,
    ): V {
        var value = cache.getIfPresent(key)
        if (value != null) return value

        value = getValue()
        cache.put(key, value)
        return value
    }

    private fun getKey(vararg keys: Any?): String = (listOf(keys) + name).joinToString(separator)
}
