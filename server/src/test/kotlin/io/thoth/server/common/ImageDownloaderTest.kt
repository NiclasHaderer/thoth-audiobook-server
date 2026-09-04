package io.thoth.server.common

import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.pngBytes
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageDownloaderTest {
    private val downloader = ImageDownloader()

    private fun dataUrl(bytes: ByteArray) = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes)

    @Test
    fun `a null source downloads nothing`() {
        assertNull(downloader.download(null))
    }

    @Test
    fun `a base64 data url is decoded`() {
        val bytes = pngBytes(1, 2, 3, 4)
        assertContentEquals(bytes, downloader.download(dataUrl(bytes)))
    }

    @Test
    fun `a data url that is not an image is rejected`() {
        // An svg would run its scripts against our own origin once somebody opened the image url directly
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
        assertFailsWith<ErrorResponse> { downloader.download(dataUrl(svg.toByteArray())) }
        assertFailsWith<ErrorResponse> { downloader.download(dataUrl("<html>hi</html>".toByteArray())) }
        assertFailsWith<ErrorResponse> { downloader.download(dataUrl(byteArrayOf(1, 2, 3, 4))) }
    }

    @Test
    fun `a data url that is not base64 is rejected`() {
        assertFailsWith<ErrorResponse> { downloader.download("data:image/svg+xml,<svg/>") }
        assertFailsWith<ErrorResponse> { downloader.download("data:image/png;base64,not valid base64!") }
    }

    @Test
    fun `an oversized data url is rejected before it is decoded`() {
        val huge = "data:image/png;base64," + "A".repeat(32 * 1024 * 1024)
        assertFailsWith<ErrorResponse> { downloader.download(huge) }
    }

    @Test
    fun `non http schemes are rejected`() {
        assertFailsWith<ErrorResponse> { downloader.download("file:///etc/passwd") }
        assertFailsWith<ErrorResponse> { downloader.download("ftp://example.com/cover.png") }
        assertFailsWith<ErrorResponse> { downloader.download("jar:file:///tmp/x.jar!/cover.png") }
        assertFailsWith<ErrorResponse> { downloader.download("/etc/passwd") }
    }

    @Test
    fun `loopback and private addresses are rejected`() {
        listOf(
            "http://127.0.0.1/cover.png",
            "http://localhost:8080/cover.png",
            "http://[::1]/cover.png",
            "http://10.0.0.5/cover.png",
            "http://192.168.1.1/cover.png",
            "http://172.16.0.1/cover.png",
            // The cloud metadata endpoint, the classic target of this attack
            "http://169.254.169.254/latest/meta-data/",
            "http://[fd00::1]/cover.png",
            "http://0.0.0.0/cover.png",
        ).forEach { url ->
            assertFailsWith<ErrorResponse>("$url must be rejected") { downloader.download(url) }
        }
    }

    @Test
    fun `a host that does not resolve is rejected`() {
        assertFailsWith<ErrorResponse> { downloader.download("http://nothing.invalid/cover.png") }
    }

    @Test
    fun `cover urls of the metadata agents are downloadable`() {
        listOf(
            // OverDrive puts braces in its paths, which are not legal in a URI and have to be escaped
            "https://img1.od-cdn.com/ImageType-150/3450-1/{5F39B28A-C85F-4679-A65E-E2087CF874D8}IMG150.JPG",
            // audiobookdb serves the bytes from a path whose last segment carries a colon
            "https://cdn.audiobookdb.org/" +
                "ec1e0d1a4ebb4193802a63d5c6319447089494beb549e4a86a1a325cf78d0e17/source.jpg:book",
        ).forEach { url ->
            assertTrue(downloader.download(url)!!.isNotEmpty(), "$url downloaded nothing")
        }
    }
}
