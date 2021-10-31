package io.github.huiibuh.audible.api

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements


suspend fun executeSearch(url: String): List<AudibleSearchResult> {
    val document = getDocumentFromUrl(url)
    val searchResultItems = getSearchItems(document)
    return extractSearchInfo(searchResultItems)
}

private fun getSearchItems(document: Document): Elements {
    return document.select(".productListItem")
}

private fun extractSearchInfo(elementList: Elements): List<AudibleSearchResult> {
    return elementList.map { it ->
        object : AudibleSearchResult {
            override val author = extractAuthorInfo(it)
            override val title = extractTitleInfo(it)
            override val series = extractSeriesInfo(it)
            override val imageUrl = extractImageUrl(it)
        }
    }
}


private fun extractImageUrl(element: Element): String? {
    val imageElement = element.selectFirst("img") ?: return null
    return imageElement.attr("data-lazy")

}

private fun extractAuthorInfo(element: Element): AudibleAuthor? {
    val authorLink = element.selectFirst(".authorLabel a") ?: return null
    return object : AudibleAuthor {
        override val link = authorLink.attr("src")
        override val name = authorLink.text()
    }

}


private fun extractTitleInfo(element: Element): AudibleTitle? {
    val titleLink = element.selectFirst("h3 a") ?: return null
    return object : AudibleTitle {
        override val link = titleLink.attr("src")
        override val name = titleLink.text()
    }

}


private fun extractSeriesInfo(element: Element): AudibleSeries? {
    val seriesElement: Element = element.selectFirst(".seriesLabel") ?: return null;
    val seriesNameElement = seriesElement.selectFirst("a") ?: return null;

    var seriesIndex = seriesElement.select("span").text()
    seriesIndex = seriesIndex.split(",").last().trim()
    seriesIndex = seriesIndex.filter { it.isDigit() }

    return object : AudibleSeries {
        override val link = seriesNameElement.attr("href")
        override val name = seriesNameElement.text()
        override val index = seriesIndex.toFloat()
    }

}




