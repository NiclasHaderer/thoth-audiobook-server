package io.thoth.metadata.audible

import io.thoth.metadata.audible.client.AudibleClient
import io.thoth.metadata.audible.client.AudibleRegions
import io.thoth.metadata.audible.models.AudibleBookImpl
import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.audible.models.AudibleSearchAuthorImpl
import io.thoth.metadata.audible.models.AudibleSearchSeriesImpl
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

class AudibleBookTest {

    private val client = AudibleClient(AudibleRegions.us)

    @Test
    fun testAudibleBook() = runBlocking {
        val book = client.getBookByID(client.uniqueName, "B017V54W6O")

        assertEquals(
            AudibleBookImpl(
                id = AudibleProviderWithIDMetadata(itemID = "B017V54W6O"),
                description = "\"Turning the envelope over, his hand trembling, Harry saw a purple wax seal bearing a coat of arms; a lion, an eagle, a badger and a snake surrounding a large letter 'H'.\" Harry Potter has never even heard of Hogwarts when the letters start dropping on the doormat at number four, Privet Drive. Addressed in green ink on yellowish parchment with a purple seal, they are swiftly confiscated by his grisly aunt and uncle. Then, on Harry's eleventh birthday, a great beetle-eyed giant of a man called Rubeus Hagrid bursts in with some astonishing news: Harry Potter is a wizard, and he has a place at Hogwarts School of Witchcraft and Wizardry. An incredible adventure is about to begin! Having now become classics of our time, the Harry Potter audiobooks never fail to bring comfort and escapism to listeners of all ages. With its message of hope, belonging and the enduring power of truth and love, the story of the Boy Who Lived continues to delight generations of new listeners. Theme music composed by James Hannigan.",
                title = "Harry Potter and the Philosopher's Stone",
                link = "https://audible.com/pd/B017V54W6O",
                author = AudibleSearchAuthorImpl(
                    id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                    name = "J.K. Rowling",
                    link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                ),
                series = AudibleSearchSeriesImpl(
                    id = AudibleProviderWithIDMetadata(itemID = "B0182T24GS"),
                    name = "Harry Potter",
                    index = 1.0f,
                    link = "https://audible.com/series/Harry-Potter-Audiobooks/B0182T24GS"
                ),
                image = "https://m.media-amazon.com/images/I/51DoG9xDIKL._SL500_.jpg",
                narrator = "Stephen Fry",
                date = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            ),
            book,
        )
    }

    @Test
    fun testFindAudibleBook() = runBlocking {
        val book = client.getBookByName("Harry Potter and the Philosopher's Stone", "J.K. Rowling").firstOrNull()

        assertEquals(
            AudibleBookImpl(
                id = AudibleProviderWithIDMetadata(itemID = "B017V54W6O"),
                description = "\"Turning the envelope over, his hand trembling, Harry saw a purple wax seal bearing a coat of arms; a lion, an eagle, a badger and a snake surrounding a large letter 'H'.\" Harry Potter has never even heard of Hogwarts when the letters start dropping on the doormat at number four, Privet Drive. Addressed in green ink on yellowish parchment with a purple seal, they are swiftly confiscated by his grisly aunt and uncle. Then, on Harry's eleventh birthday, a great beetle-eyed giant of a man called Rubeus Hagrid bursts in with some astonishing news: Harry Potter is a wizard, and he has a place at Hogwarts School of Witchcraft and Wizardry. An incredible adventure is about to begin! Having now become classics of our time, the Harry Potter audiobooks never fail to bring comfort and escapism to listeners of all ages. With its message of hope, belonging and the enduring power of truth and love, the story of the Boy Who Lived continues to delight generations of new listeners. Theme music composed by James Hannigan.",
                title = "Harry Potter and the Philosopher's Stone",
                link = "https://audible.com/pd/B017V54W6O",
                author = AudibleSearchAuthorImpl(
                    id = AudibleProviderWithIDMetadata(itemID = "B000AP9A6K"),
                    name = "J.K. Rowling",
                    link = "https://audible.com/author/JK-Rowling/B000AP9A6K"
                ),
                series = AudibleSearchSeriesImpl(
                    id = AudibleProviderWithIDMetadata(itemID = "B0182T24GS"),
                    name = "Harry Potter",
                    index = 1.0f,
                    link = "https://audible.com/series/Harry-Potter-Audiobooks/B0182T24GS"
                ),
                image = "https://m.media-amazon.com/images/I/51DoG9xDIKL._SL500_.jpg",
                narrator = "Stephen Fry",
                date = LocalDate.parse("2015-11-20", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            ),
            book,
        )
    }
}
