package io.thoth.server.api

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newTrack
import io.thoth.server.registerWithAccess
import io.thoth.server.thothServer
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioContentTypeTest : ThothTest() {
    @Test
    fun `an m4b is served as audio`() = servesAs("book.m4b", "audio/mp4")

    @Test
    fun `an mp3 is served as audio`() = servesAs("book.mp3", "audio/mpeg")

    @Test
    fun `a flac is served as audio`() = servesAs("book.flac", "audio/flac")

    private fun servesAs(
        fileName: String,
        expected: String,
    ) = thothServer {
        val file = dataDir.resolve(fileName)
        file.writeBytes(ByteArray(16))
        val libId = newLibrary("lib", folders = listOf(dataDir.absolutePathString()))
        val bookId = newBook("Dune", libId)
        val trackId = newTrack("t1", file.absolutePathString(), bookId, libId, trackNr = 1)
        val token = bearer(registerWithAccess("listener", libId, LibraryPermissionLevel.READONLY))

        val response = api.getAudioFile(trackId, token)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(expected, response.headers[HttpHeaders.ContentType])
    }
}
