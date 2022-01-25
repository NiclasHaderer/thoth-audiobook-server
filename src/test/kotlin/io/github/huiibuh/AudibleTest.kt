package io.github.huiibuh

import io.github.huiibuh.metadata.audible.models.AudibleAuthorImpl
import io.github.huiibuh.metadata.audible.models.AudibleBookImpl
import io.github.huiibuh.metadata.audible.models.AudibleSeriesImpl
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class AudibleTest : BaseTest() {
    @Test
    fun testAudibleAuthor() {
        testApp.apply {

            handleRequest(HttpMethod.Get, "/audible/author/B000AP9A6K").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val author = Json.decodeFromString<AudibleAuthorImpl>(response.content!!)
                assertEquals("J. K. Rowling", author.name)
            }

            handleRequest(HttpMethod.Get, "/audible/author/B000AP9A6T").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/audible/author/not-valid").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun testAudibleSeries() {
        testApp.apply {

            handleRequest(HttpMethod.Get, "/audible/series/B0182T3MCI").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val series = Json.decodeFromString<AudibleSeriesImpl>(response.content!!)
                assertEquals("Harry Potter", series.name)
            }

            handleRequest(HttpMethod.Get, "/audible/series/B0182T3MCL").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/audible/series/not-valid").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun testAudibleBook() {
        testApp.apply {

            handleRequest(HttpMethod.Get, "/audible/book/B017V5EJM6").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val series = Json.decodeFromString<AudibleBookImpl>(response.content!!)
                assertEquals("Harry Potter and the Philosopher's Stone", series.title)
            }

            handleRequest(HttpMethod.Get, "/audible/book/B017V5EJM3").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/audible/book/not-valid").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }
}
