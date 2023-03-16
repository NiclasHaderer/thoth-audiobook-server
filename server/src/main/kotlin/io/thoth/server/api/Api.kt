package io.thoth.server.api

import io.ktor.resources.*
import io.thoth.auth.Guards
import io.thoth.common.serializion.kotlin.UUID_S
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.openapi.Secured

@Resource("/api")
class Api {

    @Resource("/")
    class Auth {
        @Resource("/login") class Login

        @Resource("/register") class Register

        @Resource(".well-known/jwks.json") class Jwks

        @Secured(Guards.Normal)
        @Resource("/user")
        class User {

            @Secured(Guards.Admin) @Resource("/edit") data class Id(val id: UUID_S)

            @Resource("/username") class Username

            @Resource("/password") class Password
        }
    }

    @Resource("ping") class Ping

    @Resource("libraries")
    class Libraries {

        @Resource("rescan") class Rescan

        @Resource("{id}")
        data class Id(val id: UUID_S) {
            @Resource("/rescan")
            data class Rescan(private val parent: Id) {
                val libraryId
                    get() = parent.id
            }

            @Resource("/books")
            class Books(private val parent: Libraries.Id) {
                val libraryId
                    get() = parent.id

                @Resource("")
                data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("/sorting")
                data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("/autocomplete")
                data class Autocomplete(val q: String, private val parent: Books) {
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
            class Authors(private val parent: Libraries.Id) {
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

                    @Resource("position")
                    data class Position(private val parent: Id) {
                        val libraryId
                            get() = parent.libraryId
                        val id
                            get() = parent.id
                    }
                }
            }

            @Resource("/series")
            class Series(private val parent: Libraries.Id) {
                val libraryId
                    get() = parent.id

                @Resource("")
                data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("/sorting")
                data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("/autocomplete")
                data class Autocomplete(val q: String, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("/{id}")
                data class Id(val id: UUID_S, private val parent: Series) {
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
        }
    }

    @Resource("/stream")
    class Files {
        @Resource("/audio")
        class Audio {
            @Resource("/{id}") data class Id(val id: UUID_S)
        }

        @Resource("/images")
        class Images {
            @Resource("/{id}") data class Id(val id: UUID_S)
        }
    }

    @Resource("/metadata")
    class Metadata {
        @Resource("/search")
        class Search(
            val keywords: String? = null,
            val title: String? = null,
            val author: String? = null,
            val narrator: String? = null,
            val language: MetadataLanguage? = null,
            val pageSize: MetadataSearchCount? = null,
        )

        @Resource("/author")
        class Author {
            @Resource("/{id}") data class Id(val id: String, val provider: String)

            @Resource("/search") data class Search(val q: String)
        }

        @Resource("/book")
        class Book {
            @Resource("/{id}") data class Id(val id: String, val provider: String)

            @Resource("/search") data class Search(val q: String, val authorName: String? = null)
        }

        @Resource("/book")
        class Series {
            @Resource("/{id}") data class Id(val id: String, val provider: String)

            @Resource("/search") data class Search(val q: String, val authorName: String? = null)
        }
    }

    @Resource("/search")
    data class Search(
        val q: String? = null,
        val author: String? = null,
        val book: String? = null,
        val series: String? = null,
    ) {
        init {
            require(q != null || author != null || book != null || series != null) {
                "At least one of the following parameters must be provided: q, author, book, series"
            }
        }
    }
}
