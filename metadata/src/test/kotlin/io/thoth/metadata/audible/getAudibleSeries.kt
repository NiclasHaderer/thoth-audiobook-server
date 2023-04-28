package io.thoth.metadata.audible

import io.thoth.metadata.audible.client.AudibleClient
import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.responses.MetadataBookSeriesImpl
import io.thoth.metadata.responses.MetadataSearchAuthorImpl
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSeriesImpl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AudibleSeriesTest {

    private val client = AudibleClient(AudibleRegions.us)
    private val expectedSeries =
        MetadataSeriesImpl(
            id = AudibleProviderWithIDMetadata(itemID = "B0182NWM9I"),
            link = "https://audible.com/series/B0182NWM9I",
            title = "Harry Potter",
            description =
                "Turning the envelope over, his hand trembling, Harry saw a purple wax seal bearing a coat of arms; a lion, an eagle, a badger and a snake surrounding a large letter 'H'. Harry Potter has never even heard of Hogwarts when the letters start dropping on the doormat at number four, Privet Drive. Addressed in green ink on yellowish parchment with a purple seal, they are swiftly confiscated by his grisly aunt and uncle. Then, on Harry's eleventh birthday, a great beetle-eyed giant of a man called Rubeus Hagrid bursts in with some astonishing news: Harry Potter is a wizard, and he has a place at Hogwarts School of Witchcraft and Wizardry. An incredible adventure is about to begin! Having become classics of our time, the Harry Potter stories never fail to bring comfort and escapism. With their message of hope, belonging and the enduring power of truth and love, the story of the Boy Who Lived continues to delight generations of new listeners.",
            totalBooks = 7,
            books =
                listOf(
                    MetadataSearchBookImpl(
                        id = AudibleProviderWithIDMetadata(itemID = "B017V4IM1G"),
                        title = "Harry Potter and the Sorcerer's Stone",
                        link =
                            "https://audible.com/pd/Harry-Potter-and-the-Sorcerers-Stone-Book-1-Audiobook/B017V4IM1G",
                        author =
                            MetadataSearchAuthorImpl(
                                id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                                name = "J.K. Rowling",
                                link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                            ),
                        series =
                            listOf(
                                MetadataBookSeriesImpl(
                                    id = AudibleProviderWithIDMetadata(itemID = "B0182NWM9I"),
                                    title = "Harry Potter",
                                    link = "https://audible.com/series/B0182NWM9I",
                                    index = 1f
                                )
                            ),
                        coverURL = "https://m.media-amazon.com/images/I/51xJbFMRsxL._SL500_.jpg",
                        releaseDate = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        narrator = "Jim Dale",
                        language = "English"
                    ),
                    MetadataSearchBookImpl(
                        id = AudibleProviderWithIDMetadata(itemID = "B017V4IWVG"),
                        title = "Harry Potter and the Chamber of Secrets",
                        link =
                            "https://audible.com/pd/Harry-Potter-and-the-Chamber-of-Secrets-Book-2-Audiobook/B017V4IWVG",
                        author =
                            MetadataSearchAuthorImpl(
                                id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                                name = "J.K. Rowling",
                                link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                            ),
                        series =
                            listOf(
                                MetadataBookSeriesImpl(
                                    id = AudibleProviderWithIDMetadata(itemID = "B0182NWM9I"),
                                    title = "Harry Potter",
                                    link = "https://audible.com/series/B0182NWM9I",
                                    index = 2f
                                )
                            ),
                        coverURL = "https://m.media-amazon.com/images/I/61fmfnA-uCL._SL500_.jpg",
                        releaseDate = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        narrator = "Jim Dale",
                        language = "English"
                    ),
                    MetadataSearchBookImpl(
                        id = AudibleProviderWithIDMetadata(itemID = "B017V4JA2Q"),
                        title = "Harry Potter and the Prisoner of Azkaban",
                        link =
                            "https://audible.com/pd/Harry-Potter-and-the-Prisoner-of-Azkaban-Book-3-Audiobook/B017V4JA2Q",
                        author =
                            MetadataSearchAuthorImpl(
                                id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                                name = "J.K. Rowling",
                                link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                            ),
                        series =
                            listOf(
                                MetadataBookSeriesImpl(
                                    id = AudibleProviderWithIDMetadata(itemID = "B0182NWM9I"),
                                    title = "Harry Potter",
                                    link = "https://audible.com/series/B0182NWM9I",
                                    index = 3f
                                )
                            ),
                        coverURL = "https://m.media-amazon.com/images/I/51O29wSqEsL._SL500_.jpg",
                        releaseDate = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        narrator = "Jim Dale",
                        language = "English"
                    ),
                    MetadataSearchBookImpl(
                        id = AudibleProviderWithIDMetadata(itemID = "B017V4NUPO"),
                        title = "Harry Potter and the Goblet of Fire",
                        link = "https://audible.com/pd/Harry-Potter-and-the-Goblet-of-Fire-Book-4-Audiobook/B017V4NUPO",
                        author =
                            MetadataSearchAuthorImpl(
                                id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                                name = "J.K. Rowling",
                                link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                            ),
                        series =
                            listOf(
                                MetadataBookSeriesImpl(
                                    id = AudibleProviderWithIDMetadata(itemID = "B0182NWM9I"),
                                    title = "Harry Potter",
                                    link = "https://audible.com/series/B0182NWM9I",
                                    index = 4f
                                )
                            ),
                        coverURL = "https://m.media-amazon.com/images/I/61m99NmM4jL._SL500_.jpg",
                        releaseDate = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        narrator = "Jim Dale",
                        language = "English"
                    ),
                    MetadataSearchBookImpl(
                        id = AudibleProviderWithIDMetadata(itemID = "B017V4NMX4"),
                        title = "Harry Potter and the Order of the Phoenix",
                        link =
                            "https://audible.com/pd/Harry-Potter-and-the-Order-of-the-Phoenix-Book-5-Audiobook/B017V4NMX4",
                        author =
                            MetadataSearchAuthorImpl(
                                id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                                name = "J.K. Rowling",
                                link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                            ),
                        series =
                            listOf(
                                MetadataBookSeriesImpl(
                                    id = AudibleProviderWithIDMetadata(itemID = "B0182NWM9I"),
                                    title = "Harry Potter",
                                    link = "https://audible.com/series/B0182NWM9I",
                                    index = 5f
                                )
                            ),
                        coverURL = "https://m.media-amazon.com/images/I/51KHVovUpGL._SL500_.jpg",
                        releaseDate = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        narrator = "Jim Dale",
                        language = "English"
                    ),
                    MetadataSearchBookImpl(
                        id = AudibleProviderWithIDMetadata(itemID = "B017V4NOZ0"),
                        title = "Harry Potter and the Half-Blood Prince",
                        link =
                            "https://audible.com/pd/Harry-Potter-and-the-Half-Blood-Prince-Book-6-Audiobook/B017V4NOZ0",
                        author =
                            MetadataSearchAuthorImpl(
                                id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                                name = "J.K. Rowling",
                                link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                            ),
                        series =
                            listOf(
                                MetadataBookSeriesImpl(
                                    id = AudibleProviderWithIDMetadata(itemID = "B0182NWM9I"),
                                    title = "Harry Potter",
                                    link = "https://audible.com/series/B0182NWM9I",
                                    index = 6f
                                )
                            ),
                        coverURL = "https://m.media-amazon.com/images/I/51sImF7gqML._SL500_.jpg",
                        releaseDate = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        narrator = "Jim Dale",
                        language = "English"
                    ),
                    MetadataSearchBookImpl(
                        id = AudibleProviderWithIDMetadata(itemID = "B017WJ5ZK6"),
                        title = "Harry Potter and the Deathly Hallows",
                        link =
                            "https://audible.com/pd/Harry-Potter-and-the-Deathly-Hallows-Book-7-Audiobook/B017WJ5ZK6",
                        author =
                            MetadataSearchAuthorImpl(
                                id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                                name = "J.K. Rowling",
                                link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                            ),
                        series =
                            listOf(
                                MetadataBookSeriesImpl(
                                    id = AudibleProviderWithIDMetadata(itemID = "B0182NWM9I"),
                                    title = "Harry Potter",
                                    link = "https://audible.com/series/B0182NWM9I",
                                    index = 7f
                                )
                            ),
                        coverURL = "https://m.media-amazon.com/images/I/61yMjtQzKcL._SL500_.jpg",
                        releaseDate = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        narrator = "Jim Dale",
                        language = "English"
                    )
                ),
            author = "J.K. Rowling",
            coverURL = null,
            primaryWorks = 7
        )

    @Test
    fun testAudibleSeries() = runBlocking {
        val series = client.getSeriesByID(client.uniqueName,, "B0182NWM9I")

        assertEquals(
            expectedSeries,
            series,
        )
    }

    @Test
    fun testFindAudibleSeries() = runBlocking {
        val series = client.getSeriesByName("Harry Potter",, "J.K. Rowling").firstOrNull()

        assertEquals(
            expectedSeries,
            series,
        )
    }
}
