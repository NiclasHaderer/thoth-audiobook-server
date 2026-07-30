package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAgentID
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataAuthorImpl
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataBookSeriesImpl
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchAuthorImpl
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.metadata.responses.MetadataSeriesImpl
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

internal data class TestId(
    override val itemID: String,
    override val provider: String = "fake",
) : MetadataAgentID

internal fun searchHit(
    id: String,
    title: String = id,
    authors: List<String> = emptyList(),
    series: List<String> = emptyList(),
    provider: String = "fake",
) = MetadataSearchBookImpl(
    id = TestId(id, provider),
    title = title,
    link = null,
    authors =
        authors.map {
            MetadataSearchAuthorImpl(id = TestId(it, provider), name = it, link = "link/$it")
        },
    series =
        series.map {
            MetadataBookSeriesImpl(id = TestId(it, provider), title = it, link = "link/$it", index = null)
        },
    language = null,
    releaseDate = null,
    coverURL = null,
    narrator = null,
)

internal fun testBook(
    id: String,
    provider: String = "fake",
) = MetadataBookImpl(
    id = TestId(id, provider),
    title = id,
    link = null,
    authors = null,
    series = emptyList(),
    releaseDate = null,
    coverURL = null,
    description = null,
    narrator = null,
    providerRating = null,
    publisher = null,
    language = null,
    isbn = null,
)

internal fun testAuthor(
    id: String,
    provider: String = "fake",
) = MetadataAuthorImpl(
    id = TestId(id, provider),
    name = id,
    link = "link/$id",
    imageURL = null,
    biography = null,
    website = null,
    bornIn = null,
    birthDate = null,
    deathDate = null,
)

internal fun testSeries(
    id: String,
    provider: String = "fake",
) = MetadataSeriesImpl(
    id = TestId(id, provider),
    title = id,
    authors = null,
    link = "link/$id",
    coverURL = null,
    description = null,
    totalBooks = null,
    primaryWorks = null,
    books = null,
)

/**
 * Provider which answers from a fixed list of hits and records what it was asked for, so the lookups derived from it can
 * be checked for the requests they cause.
 */
internal class FakeMetadataProvider(
    override val name: String = "fake",
    private val hits: List<MetadataSearchBook> = emptyList(),
    private val resolveBook: suspend (String) -> MetadataBook? = { testBook(it) },
) : MetadataProvider {
    override val supportedCountryCodes = listOf("US")

    val searchCalls = AtomicInteger()
    val bookLookups = CopyOnWriteArrayList<String>()
    val authorLookups = CopyOnWriteArrayList<String>()
    val seriesLookups = CopyOnWriteArrayList<String>()

    override suspend fun search(
        region: String,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBook> {
        searchCalls.incrementAndGet()
        return hits
    }

    override suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthor? {
        authorLookups += "$authorId@$region"
        return testAuthor(authorId, name)
    }

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBook? {
        bookLookups += "$bookId@$region"
        return resolveBook(bookId)
    }

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: String,
    ): MetadataSeries? {
        seriesLookups += "$seriesId@$region"
        return testSeries(seriesId, name)
    }
}
