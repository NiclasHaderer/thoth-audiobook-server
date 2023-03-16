package io.thoth.server.api

import io.ktor.resources.*
import io.thoth.auth.Guards
import io.thoth.common.serializion.kotlin.UUID_S
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.openapi.Secured
import io.thoth.openapi.Tagged

@Resource("api")
class Api {

    @Resource("auth")
    @Tagged("Auth")
    data class Auth(private val parent: Api) {
        @Resource("login") data class Login(private val parent: Auth)

        @Resource("register") data class Register(private val parent: Auth)

        @Resource(".well-known/jwks.json") data class Jwks(private val parent: Auth)

        @Secured(Guards.Normal)
        @Resource("user")
        data class User(private val parent: Auth) {

            @Secured(Guards.Admin) @Resource("edit") data class Id(val id: UUID_S, private val parent: User)

            @Resource("username") data class Username(private val parent: User)

            @Resource("password") data class Password(private val parent: User)
        }
    }

    @Resource("ping") data class Ping(private val parent: Api)

    @Resource("libraries")
    @Tagged("Library")
    data class Libraries(private val parent: Api) {

        @Resource("search")
        data class Search(
            val q: String? = null,
            val author: String? = null,
            val book: String? = null,
            val series: String? = null,
            private val parent: Libraries
        ) {
            init {
                require(q != null || author != null || book != null || series != null) {
                    "At least one of the following parameters must be provided: q, author, book, series"
                }
            }
        }

        @Resource("rescan") data class Rescan(private val parent: Libraries)

        @Resource("{id}")
        data class Id(val id: UUID_S, private val parent: Libraries) {
            @Resource("rescan")
            data class Rescan(private val parent: Id) {
                val libraryId
                    get() = parent.id
            }

            @Resource("books")
            @Tagged("Books")
            data class Books(private val parent: Libraries.Id) {
                val libraryId
                    get() = parent.id

                @Resource("")
                data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("sorting")
                data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("autocomplete")
                data class Autocomplete(val q: String, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("{id}")
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

            @Resource("authors")
            @Tagged("Authors")
            data class Authors(private val parent: Libraries.Id) {
                val libraryId
                    get() = parent.id

                @Resource("")
                data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Authors) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("sorting")
                data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Authors) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("autocomplete")
                data class Autocomplete(val q: String, private val parent: Authors) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("{id}")
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

            @Resource("series")
            @Tagged("Series")
            data class Series(private val parent: Libraries.Id) {
                val libraryId
                    get() = parent.id

                @Resource("")
                data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("sorting")
                data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("autocomplete")
                data class Autocomplete(val q: String, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Resource("{id}")
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

    @Resource("stream")
    @Tagged("Files")
    data class Files(private val parent: Api) {
        @Resource("audio")
        data class Audio(private val parent: Files) {
            @Resource("{id}") data class Id(val id: UUID_S, private val parent: Audio)
        }

        @Resource("images")
        data class Images(private val parent: Files) {
            @Resource("{id}") data class Id(val id: UUID_S, private val parent: Images)
        }
    }

    @Resource("metadata")
    @Tagged("Metadata")
    data class Metadata(private val parent: Api) {
        @Resource("search")
        data class Search(
            val keywords: String? = null,
            val title: String? = null,
            val author: String? = null,
            val narrator: String? = null,
            val language: MetadataLanguage? = null,
            val pageSize: MetadataSearchCount? = null,
            private val parent: Metadata,
        )

        @Resource("author")
        data class Author(private val parent: Metadata) {
            @Resource("{id}") data class Id(val id: String, val provider: String, private val parent: Author)

            @Resource("search") data class Search(val q: String, private val parent: Author)
        }

        @Resource("book")
        data class Book(private val parent: Metadata) {
            @Resource("{id}") data class Id(val id: String, val provider: String, private val parent: Book)

            @Resource("search")
            data class Search(val q: String, val authorName: String? = null, private val parent: Book)
        }

        @Resource("book")
        data class Series(private val parent: Metadata) {
            @Resource("{id}") data class Id(val id: String, val provider: String, private val parent: Series)

            @Resource("search")
            data class Search(val q: String, val authorName: String? = null, private val parent: Series)
        }
    }
}
