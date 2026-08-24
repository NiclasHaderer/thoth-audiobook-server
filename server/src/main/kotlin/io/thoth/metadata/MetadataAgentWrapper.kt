package io.thoth.metadata

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

private val log = logger {}

class MetadataAgentWrapper(
    private val agentList: List<MetadataAgent>,
) : MetadataAgent {
    override val name = agentList.joinToString(", ") { it.name }
    override val supportedRegions: List<MetadataRegion>
        get() = agentList.flatMap { it.supportedRegions }.distinct()

    private val agentsByName by lazy { agentList.associateBy { it.name } }

    override suspend fun search(
        region: MetadataRegion,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBook> =
        coroutineScope {
            agentList
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
                }.awaitAll()
                .flatten()
        }

    override suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: MetadataRegion,
    ): MetadataAuthor? = agent(providerId)?.getAuthorByID(providerId = providerId, authorId = authorId, region = region)

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: MetadataRegion,
    ): MetadataBook? = agent(providerId)?.getBookByID(providerId = providerId, bookId = bookId, region = region)

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: MetadataRegion,
    ): MetadataSeries? = agent(providerId)?.getSeriesByID(providerId = providerId, seriesId = seriesId, region = region)

    override fun getAuthorByName(
        authorName: String,
        region: MetadataRegion,
        language: MetadataLanguage?,
    ): Flow<MetadataAuthor> =
        fromEachAgent { it.getAuthorByName(authorName = authorName, region = region, language = language) }

    override fun getBookByName(
        bookName: String,
        region: MetadataRegion,
        authorName: String?,
        language: MetadataLanguage?,
    ): Flow<MetadataBook> =
        fromEachAgent {
            it.getBookByName(bookName = bookName, region = region, authorName = authorName, language = language)
        }

    override fun getSeriesByName(
        seriesName: String,
        region: MetadataRegion,
        authorName: String?,
        language: MetadataLanguage?,
    ): Flow<MetadataSeries> =
        fromEachAgent {
            it.getSeriesByName(seriesName = seriesName, region = region, authorName = authorName, language = language)
        }

    /**
     * Every agent hands out its results best match first, so they are concatenated instead of ranked again: ranking
     * across agents would mean resolving every result of every agent before the first one can be handed out.
     */
    private fun <T> fromEachAgent(query: (MetadataAgent) -> Flow<T>): Flow<T> =
        flow {
            agentList.forEach { emitAll(query(it)) }
        }

    private fun agent(providerId: String): MetadataAgent? {
        val agent = agentsByName[providerId]
        if (agent == null) {
            log.warn { "No metadata agent named '$providerId' (available: ${agentsByName.keys})" }
        }
        return agent
    }
}
