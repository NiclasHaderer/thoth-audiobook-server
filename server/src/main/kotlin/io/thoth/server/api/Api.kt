package io.thoth.server.api

import io.ktor.resources.*
import io.thoth.generators.openapi.NotSecured
import io.thoth.generators.openapi.Secured
import io.thoth.generators.openapi.Summary
import io.thoth.generators.openapi.Tagged
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.models.Position
import io.thoth.server.common.serializion.kotlin.UUID_S
import io.thoth.server.plugins.authentication.Guards

// TODO names of libraryID, etc should be consistent
// TODO remove unused methods in the db access layer
@Resource("api")
class Api {

    @Resource("fs")
    @Summary("List folders at a certain path", method = "GET")
    @Tagged("Filesystem")
    data class FileSystem(val path: String, val showHidden: Boolean = false, private val parent: Api)

    @Resource("auth")
    @Tagged("Auth")
    data class Auth(private val parent: Api) {
        @Summary("Login user", method = "POST") @Resource("login") data class Login(private val parent: Auth)

        @Summary("Register user", method = "POST") @Resource("register") data class Register(private val parent: Auth)

        @Summary("Retrieve Jwks", method = "GET")
        @Resource(".well-known/jwks.json")
        data class Jwks(private val parent: Auth)

        @Summary("Get current user", method = "GET")
        @Summary("Create user", method = "POST")
        @Summary("Delete user", method = "DELETE")
        @Secured(Guards.Normal)
        @Resource("user")
        data class User(private val parent: Auth) {

            @Summary("List users", method = "GET")
            @Secured(Guards.Admin)
            @Resource("all")
            data class All(private val parent: User)

            @Summary("Retrieve user", method = "GET")
            @Summary("Update user", method = "PUT")
            @Secured(Guards.Admin)
            @Resource("edit")
            data class Id(val id: UUID_S, private val parent: User)

            @Summary("Update username", method = "POST")
            @Resource("username")
            data class Username(private val parent: User)

            @Summary("Update password", method = "POST")
            @Resource("password")
            data class Password(private val parent: User)

            @NotSecured
            @Summary("Refresh access token", method = "POST")
            @Resource("refresh")
            data class Refresh(private val parent: User)
        }
    }

    @Summary("Ping server", method = "GET")
    @Tagged("Server")
    @Resource("ping")
    data class Ping(private val parent: Api)

    @Summary("List file scanners", method = "GET")
    @Tagged("Scanner")
    @Resource("scanners")
    data class Scanners(private val parent: Api)

    @Summary("List metadata agents", method = "GET")
    @Tagged("Scanner")
    @Resource("metadata-agents")
    data class MetadataScanners(private val parent: Api)

    @Summary("List libraries", method = "GET")
    @Summary("Create library", method = "POST")
    @Resource("libraries")
    @Tagged("Library")
    data class Libraries(private val parent: Api) {

        @Summary("Search in all libraries", method = "GET")
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

        @Summary("Rescan all libraries", method = "POST")
        @Resource("rescan")
        data class Rescan(private val parent: Libraries)

        @Resource("{libraryId}")
        @Summary("Replace library", method = "PUT")
        @Summary("Delete library", method = "DELETE")
        @Summary("Update library", method = "PATCH")
        @Summary("Get library", method = "GET")
        data class Id(val libraryId: UUID_S, private val parent: Libraries) {
            @Summary("Rescan library", method = "POST")
            @Resource("rescan")
            data class Rescan(private val parent: Id) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("books")
            @Tagged("Books")
            data class Books(private val parent: Libraries.Id) {
                val libraryId
                    get() = parent.libraryId

                @Summary("List books", method = "GET")
                @Resource("")
                data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("List book sorting", method = "GET")
                @Resource("sorting")
                data class Sorting(val limit: Int = 20, val offset: Long = 0, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get book autocomplete", method = "GET")
                @Resource("autocomplete")
                data class Autocomplete(val q: String, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get book", method = "GET")
                @Summary("Update book", method = "PATCH")
                @Summary("Replace book", method = "PUT")
                @Resource("{id}")
                data class Id(val id: UUID_S, private val parent: Books) {
                    val libraryId
                        get() = parent.libraryId

                    @Summary("Get book position", method = "GET")
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
                    get() = parent.libraryId

                @Summary("List authors", method = "GET")
                @Resource("")
                data class All(
                    val limit: Int = 20,
                    val offset: Long = 0,
                    val order: Position.Order = Position.Order.ASC,
                    private val parent: Authors
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("List author sorting", method = "GET")
                @Resource("sorting")
                data class Sorting(
                    val limit: Int = 20,
                    val offset: Long = 0,
                    val order: Position.Order = io.thoth.models.Position.Order.ASC,
                    private val parent: Authors,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get author autocomplete", method = "GET")
                @Resource("autocomplete")
                data class Autocomplete(val q: String, private val parent: Authors) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get author", method = "GET")
                @Summary("Update author", method = "PATCH")
                @Summary("Replace author", method = "PUT")
                @Resource("{id}")
                data class Id(val id: UUID_S, private val parent: Authors) {
                    val libraryId
                        get() = parent.libraryId

                    @Summary("Get author position", method = "GET")
                    @Resource("position")
                    data class Position(
                        val order: io.thoth.models.Position.Order = io.thoth.models.Position.Order.ASC,
                        private val parent: Id,
                    ) {
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
                    get() = parent.libraryId

                @Summary("List series", method = "GET")
                @Resource("")
                data class All(val limit: Int = 20, val offset: Long = 0, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("List series sorting", method = "GET")
                @Resource("sorting")
                data class Sorting(
                    val limit: Int = 20,
                    val offset: Long = 0,
                    val order: Position.Order = Position.Order.ASC,
                    private val parent: Series
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get series autocomplete", method = "GET")
                @Resource("autocomplete")
                data class Autocomplete(val q: String, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get series", method = "GET")
                @Summary("Update series", method = "PATCH")
                @Summary("Replace series", method = "PUT")
                @Resource("{id}")
                data class Id(val id: UUID_S, private val parent: Series) {
                    val libraryId
                        get() = parent.libraryId

                    @Summary("Get series position", method = "GET")
                    @Resource("position")
                    data class Position(
                        val order: io.thoth.models.Position.Order = io.thoth.models.Position.Order.ASC,
                        private val parent: Id
                    ) {
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
            @Summary("Get audio file", method = "GET")
            @Resource("{id}")
            data class Id(val id: UUID_S, private val parent: Audio)
        }

        @Resource("images")
        data class Images(private val parent: Files) {
            @Summary("Get image file", method = "GET")
            @Resource("{id}")
            data class Id(val id: UUID_S, private val parent: Images)
        }
    }

    @Resource("metadata")
    @Tagged("Metadata")
    data class Metadata(private val parent: Api) {
        @Summary("Search metadata", method = "GET")
        @Resource("search")
        data class Search(
            val region: String,
            val keywords: String? = null,
            val title: String? = null,
            val author: String? = null,
            val narrator: String? = null,
            val language: MetadataLanguage? = null,
            val pageSize: MetadataSearchCount? = null,
            private val parent: Metadata,
        )

        @Summary("Search author metadata", method = "GET")
        @Resource("author")
        data class Author(private val parent: Metadata) {
            @Summary("Get author metadata", method = "GET")
            @Resource("{id}")
            data class Id(val id: String, val region: String, val provider: String, private val parent: Author)

            @Summary("Search author metadata", method = "GET")
            @Resource("search")
            data class Search(val q: String, val region: String, private val parent: Author)
        }

        @Resource("book")
        data class Book(private val parent: Metadata) {
            @Summary("Get book metadata", method = "GET")
            @Resource("{id}")
            data class Id(val id: String, val region: String, val provider: String, private val parent: Book)

            @Summary("Search book metadata", method = "GET")
            @Resource("search")
            data class Search(
                val q: String,
                val region: String,
                val authorName: String? = null,
                private val parent: Book
            )
        }

        @Resource("series")
        data class Series(private val parent: Metadata) {
            @Summary("Get series metadata", method = "GET")
            @Resource("{id}")
            data class Id(val id: String, val region: String, val provider: String, private val parent: Series)

            @Summary("Search series metadata", method = "GET")
            @Resource("search")
            data class Search(
                val q: String,
                val region: String,
                val authorName: String? = null,
                private val parent: Series
            )
        }
    }
}
