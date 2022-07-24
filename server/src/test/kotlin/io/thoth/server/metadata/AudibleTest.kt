package io.thoth.server.metadata

import io.ktor.http.*
import io.ktor.server.testing.*
//import io.thoth.metadata.audible.models.AudibleAuthorImpl
//import io.thoth.metadata.audible.models.AudibleBookImpl
//import io.thoth.metadata.audible.models.AudibleSeriesImpl
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class AudibleTest : BaseTest() {
//    @Test
//    fun testAudibleAuthor() {
//        testApp.apply {
//
//            handleRequest(HttpMethod.Get, "/audible/author/B000AP9A6K").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                val author = mapper.readValue(response.content, AudibleAuthorImpl::class.java)
//                assertEquals("J. K. Rowling", author.name)
//            }
//
//            handleRequest(HttpMethod.Get, "/audible/author/B000AP9A6T").apply {
//                assertEquals(HttpStatusCode.NotFound, response.status())
//            }
//            handleRequest(HttpMethod.Get, "/audible/author/not-valid").apply {
//                assertEquals(HttpStatusCode.NotFound, response.status())
//            }
//        }
//    }
//
//    @Test
//    fun testAudibleSeries() {
//        testApp.apply {
//
//            handleRequest(HttpMethod.Get, "/audible/series/B0182T3MCI").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                val series = mapper.readValue(response.content, AudibleSeriesImpl::class.java)
//                assertEquals("Harry Potter", series.name)
//            }
//
//            handleRequest(HttpMethod.Get, "/audible/series/B0182T3MCL").apply {
//                assertEquals(HttpStatusCode.NotFound, response.status())
//            }
//            handleRequest(HttpMethod.Get, "/audible/series/not-valid").apply {
//                assertEquals(HttpStatusCode.NotFound, response.status())
//            }
//        }
//    }
//
//    @Test
//    fun testAudibleBook() {
//        testApp.apply {
//
//            handleRequest(HttpMethod.Get, "/audible/book/B017V5EJM6").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                val series = mapper.readValue(response.content, AudibleBookImpl::class.java)
//                assertEquals("Harry Potter and the Philosopher's Stone", series.title)
//            }
//
//            handleRequest(HttpMethod.Get, "/audible/book/B017V5EJM3").apply {
//                assertEquals(HttpStatusCode.NotFound, response.status())
//            }
//            handleRequest(HttpMethod.Get, "/audible/book/not-valid").apply {
//                assertEquals(HttpStatusCode.NotFound, response.status())
//            }
//        }
//    }
}
