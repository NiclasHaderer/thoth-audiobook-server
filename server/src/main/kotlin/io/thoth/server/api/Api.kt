package io.thoth.server.api

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S

@Resource("/api")
class Api {
    @Resource("/ping") class Ping

    @Resource("/libraries")
    class Libraries {
        @Resource("/{id}") data class Id(val id: UUID_S)

        @Resource("/books")
        class Books(private val parent: Id) {
            val libraryId
                get() = parent.id

            @Resource("")
            data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/sorting")
            data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/autocomplete")
            data class Autocomplete(val q: String, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/{id}")
            data class Id(val id: UUID_S, private val parent: Books) {
                val libraryId
                    get() = parent.libraryId

                @Resource("position")
                data class Position(private val parent: Id) {
                    val libraryId
                        get() = parent.libraryId
                    val id
                        get() = parent.id
                }
            }
        }

        @Resource("/authors")
        class Authors(private val parent: Id) {
            val libraryId
                get() = parent.id

            @Resource("")
            data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/sorting")
            data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/autocomplete")
            data class Autocomplete(val q: String, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/{id}")
            data class Id(val id: UUID_S, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }
        }

        @Resource("/series")
        class Series(private val parent: Id) {
            val libraryId
                get() = parent.id

            @Resource("")
            data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/sorting")
            data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/autocomplete")
            data class Autocomplete(val q: String, private val parent: Authors) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("/{id}")
            data class Id(val id: UUID_S, private val parent: Series) {
                val libraryId
                    get() = parent.libraryId
            }
        }
    }

    @Resource("/images")
    class Images {
        @Resource("/{id}") data class Id(val id: String)
    }

    @Resource("/stream")
    class Stream {
        @Resource("/audio")
        class Audio {
            @Resource("/{id}") data class Id(val id: String)
        }
    }

    @Resource("/metadata")
    class Metadata {
        @Resource("/search") class Search

        @Resource("/author")
        class Author {
            @Resource("/{itemID}")
            data class Id(val id: String, val provider: String) {
                @Resource("/position")
                data class Position(private val parent: Id) {
                    val id
                        get() = parent.id
                }
            }

            @Resource("/search") data class Search(val q: String)
        }

        @Resource("/book")
        class Book {
            @Resource("/{id}")
            data class Id(val id: String, val provider: String) {
                @Resource("/position")
                data class Position(private val parent: Author.Id) {
                    val id
                        get() = parent.id
                }
            }

            @Resource("/search") data class Search(val q: String)
        }

        @Resource("/book")
        class Series {
            @Resource("/{id}")
            data class Id(val id: String, val provider: String) {
                @Resource("/position")
                data class Position(private val parent: Author.Id) {
                    val id
                        get() = parent.id
                }
            }

            @Resource("/search") data class Search(val q: String)
        }
    }

    @Resource("/search")
    data class Search(
        val q: String? = null,
        val author: String? = null,
        val book: String? = null,
        val series: String? = null,
    )

    @Resource("/scan")
    class Scan {
        @Resource("/full") class Full

        @Resource("/library")
        class Library {
            @Resource("/{id}") data class Id(val id: String)
        }
    }
}
