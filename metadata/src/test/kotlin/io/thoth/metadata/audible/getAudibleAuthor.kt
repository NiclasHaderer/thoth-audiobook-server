package io.thoth.metadata.audible

import io.thoth.metadata.audible.client.AudibleClient
import io.thoth.metadata.audible.models.AudibleProviderWithIDMetadata
import io.thoth.metadata.responses.MetadataAuthorImpl
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AudibleAuthorTest {

    private val client = AudibleClient()

    @Test
    fun testAudibleAuthor() = runBlocking {
        val author = client.getAuthorByID(client.uniqueName, "B000AP9A6K", "us")

        assertEquals(
            author,
            MetadataAuthorImpl(
                id = AudibleProviderWithIDMetadata("B000AP9A6K"),
                name = "J.K. Rowling",
                biography =
                    "J.K. Rowling is best-known as the author of the seven Harry Potter books, which were published between 1997 and 2007. The enduringly popular adventures of Harry, Ron and Hermione have gone on to sell over 600 million copies worldwide, be translated into 85 languages and made into eight blockbuster films. Alongside the Harry Potter series, J.K. Rowling also wrote three short companion volumes for charity: Quidditch Through the Ages and Fantastic Beasts and Where to Find Them, in aid of Comic Relief, and The Tales of Beedle the Bard, in aid of her international children’s charity, Lumos. The companion books and original series are all available as audiobooks. In 2016, J.K. Rowling collaborated with playwright Jack Thorne and director John Tiffany to continue Harry’s story in a stage play, Harry Potter and the Cursed Child, which opened in London, and is now playing in multiple locations around the world. The script book was published to mark the plays opening in 2016 and instantly topped the bestseller lists. In the same year, she made her debut as a screenwriter with the film Fantastic Beasts and Where to Find Them. Inspired by the original companion volume, it was the first in a series of new adventures featuring wizarding world magizoologist Newt Scamander. The second, Fantastic Beasts: The Crimes of Grindelwald, was released in 2018 and the third, Fantastic Beasts: The Secrets of Dumbledore was released in 2022. The screenplays were published to coincide with each film’s release: Fantastic Beasts and Where to Find Them The Original Screenplay (2016), Fantastic Beasts: The Crimes of Grindelwald The Original Screenplay (2018) and Fantastic Beasts: The Secrets of Dumbledore The Complete Screenplay (2022). Fans of Fantastic Beasts and Harry Potter can find out more at www.wizardingworld.com. J.K. Rowling’s fairy tale for younger children, The Ickabog, was serialised for free online for children during the Covid-19 pandemic in the summer of 2020 and is now published as a book illustrated by children, with her royalties going to charities supporting vulnerable groups affected by the pandemic. Her latest children’s novel The Christmas Pig, is a standalone adventure story about a boy’s love for his most treasured thing and how far he will go to find it. J.K. Rowling also writes novels for adults. The Casual Vacancy was published in 2012 and adapted for television in 2015. Under the pseudonym Robert Galbraith, she is the author of the highly acclaimed ‘Strike’ crime series, featuring private detective Cormoran Strike and his partner Robin Ellacott. The first of these, The Cuckoo’s Calling, was published to critical acclaim in 2013, at first without its author’s true identity being known. The Silkworm followed in 2014, Career of Evil in 2015, Lethal White in 2018, Troubled Blood in 2020 and The Ink Black Heart in 2022. The series has also been adapted for television by the BBC and HBO. J.K. Rowling’s 2008 Harvard Commencement speech was published in 2015 as an illustrated book, Very Good Lives: The Fringe Benefits of Failure and the Importance of Imagination, sold in aid of Lumos and university-wide financial aid at Harvard. As well as receiving an OBE and Companion of Honour for repositories to children’s literature, J.K. Rowling has received many other awards and honours, including France’s Legion d’Honneur, Spain’s Prince of Asturias Award and Denmark’s Hans Christian Andersen Award. J.K. Rowling supports a number of causes through her charitable trust, Volant. She is also the founder and president of Lumos, an international children’s charity fighting for every child’s right to a family by transforming care systems around the world. www.jkrowling.com Image: Photography Debra Hurford Brown © J.K. Rowling",
                imageURL =
                    "https://images-na.ssl-images-amazon.com/images/S/amzn-author-media-prod/8cigckin175jtpsk3gs361r4ss.__01_SX500_CR0,0,0,0__.jpg",
                link = "https://audible.com/author/B000AP9A6K",
                birthDate = null,
                deathDate = null,
                bornIn = null,
                website = null,
            ),
        )
    }

    @Test
    fun testFindAudibleAuthor() = runBlocking {
        val author = client.getAuthorByName("J.K. Rowling", "us").firstOrNull()

        assertEquals(
            author,
            MetadataAuthorImpl(
                id = AudibleProviderWithIDMetadata("B000AP9A6K"),
                name = "J.K. Rowling",
                biography =
                    "J.K. Rowling is best-known as the author of the seven Harry Potter books, which were published between 1997 and 2007. The enduringly popular adventures of Harry, Ron and Hermione have gone on to sell over 600 million copies worldwide, be translated into 85 languages and made into eight blockbuster films. Alongside the Harry Potter series, J.K. Rowling also wrote three short companion volumes for charity: Quidditch Through the Ages and Fantastic Beasts and Where to Find Them, in aid of Comic Relief, and The Tales of Beedle the Bard, in aid of her international children’s charity, Lumos. The companion books and original series are all available as audiobooks. In 2016, J.K. Rowling collaborated with playwright Jack Thorne and director John Tiffany to continue Harry’s story in a stage play, Harry Potter and the Cursed Child, which opened in London, and is now playing in multiple locations around the world. The script book was published to mark the plays opening in 2016 and instantly topped the bestseller lists. In the same year, she made her debut as a screenwriter with the film Fantastic Beasts and Where to Find Them. Inspired by the original companion volume, it was the first in a series of new adventures featuring wizarding world magizoologist Newt Scamander. The second, Fantastic Beasts: The Crimes of Grindelwald, was released in 2018 and the third, Fantastic Beasts: The Secrets of Dumbledore was released in 2022. The screenplays were published to coincide with each film’s release: Fantastic Beasts and Where to Find Them The Original Screenplay (2016), Fantastic Beasts: The Crimes of Grindelwald The Original Screenplay (2018) and Fantastic Beasts: The Secrets of Dumbledore The Complete Screenplay (2022). Fans of Fantastic Beasts and Harry Potter can find out more at www.wizardingworld.com. J.K. Rowling’s fairy tale for younger children, The Ickabog, was serialised for free online for children during the Covid-19 pandemic in the summer of 2020 and is now published as a book illustrated by children, with her royalties going to charities supporting vulnerable groups affected by the pandemic. Her latest children’s novel The Christmas Pig, is a standalone adventure story about a boy’s love for his most treasured thing and how far he will go to find it. J.K. Rowling also writes novels for adults. The Casual Vacancy was published in 2012 and adapted for television in 2015. Under the pseudonym Robert Galbraith, she is the author of the highly acclaimed ‘Strike’ crime series, featuring private detective Cormoran Strike and his partner Robin Ellacott. The first of these, The Cuckoo’s Calling, was published to critical acclaim in 2013, at first without its author’s true identity being known. The Silkworm followed in 2014, Career of Evil in 2015, Lethal White in 2018, Troubled Blood in 2020 and The Ink Black Heart in 2022. The series has also been adapted for television by the BBC and HBO. J.K. Rowling’s 2008 Harvard Commencement speech was published in 2015 as an illustrated book, Very Good Lives: The Fringe Benefits of Failure and the Importance of Imagination, sold in aid of Lumos and university-wide financial aid at Harvard. As well as receiving an OBE and Companion of Honour for repositories to children’s literature, J.K. Rowling has received many other awards and honours, including France’s Legion d’Honneur, Spain’s Prince of Asturias Award and Denmark’s Hans Christian Andersen Award. J.K. Rowling supports a number of causes through her charitable trust, Volant. She is also the founder and president of Lumos, an international children’s charity fighting for every child’s right to a family by transforming care systems around the world. www.jkrowling.com Image: Photography Debra Hurford Brown © J.K. Rowling",
                imageURL =
                    "https://images-na.ssl-images-amazon.com/images/S/amzn-author-media-prod/8cigckin175jtpsk3gs361r4ss.__01_SX500_CR0,0,0,0__.jpg",
                link = "https://audible.com/author/B000AP9A6K",
                birthDate = null,
                deathDate = null,
                bornIn = null,
                website = null,
            ),
        )
    }
}
