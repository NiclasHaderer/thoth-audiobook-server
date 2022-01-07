package plugin.loader

import metadata.audible.models.AudibleSearchAmount
import metadata.audible.models.AudibleSearchLanguage

interface SearchPlugin {
    fun authorById(id: String)
    fun bookById(id: String)
    fun seriesById(id: String)
    fun authorByName(id: String)
    fun bookByName(id: String)
    fun seriesByName(id: String)
    fun search(
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: AudibleSearchLanguage? = null,
        pageSize: AudibleSearchAmount? = null,
    )

}

