package io.thoth.server.api

import io.ktor.resources.Resource
import io.ktor.server.routing.RoutingContext
import io.thoth.auth.interactions.ThothChangePasswordParams
import io.thoth.auth.interactions.ThothCurrentUserParams
import io.thoth.auth.interactions.ThothDeleteUserParams
import io.thoth.auth.interactions.ThothDisplayUserParams
import io.thoth.auth.interactions.ThothJwksParams
import io.thoth.auth.interactions.ThothListUserParams
import io.thoth.auth.interactions.ThothLoginParams
import io.thoth.auth.interactions.ThothLogoutParams
import io.thoth.auth.interactions.ThothModifyPermissionsParams
import io.thoth.auth.interactions.ThothRefreshTokenParams
import io.thoth.auth.interactions.ThothRegisterParams
import io.thoth.auth.interactions.ThothRenameUserParams
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.models.Order
import io.thoth.openapi.ktor.BeforeBodyParsing
import io.thoth.openapi.ktor.NotSecured
import io.thoth.openapi.ktor.Secured
import io.thoth.openapi.ktor.Summary
import io.thoth.openapi.ktor.Tagged
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.serializion.kotlin.UUID_S
import io.thoth.server.plugins.auth.Guards
import io.thoth.server.plugins.auth.UserScoped
import io.thoth.server.plugins.auth.assertLibraryPermissions

@Resource("api")
class Api {
    @Secured(Guards.Admin)
    @Resource("fs")
    @Summary("List folders at a certain path", method = "GET")
    @Tagged("Filesystem")
    data class FileSystem(
        val path: String,
        val showHidden: Boolean = false,
        private val parent: Api,
    )

    @Resource("auth")
    @Tagged("Auth")
    data class Auth(
        private val parent: Api,
    ) {
        @Summary("Login user", method = "POST")
        @Resource("login")
        data class Login(
            private val parent: Auth,
        ) : ThothLoginParams

        @Summary("Logout user", method = "POST")
        @Resource("logout")
        data class Logout(
            private val parent: Auth,
        ) : ThothLogoutParams

        @Summary("Register user", method = "POST", status = 201)
        @Resource("register")
        data class Register(
            private val parent: Auth,
        ) : ThothRegisterParams

        @Summary("Retrieve Jwks", method = "GET")
        @Resource("jwks.json")
        data class Jwks(
            private val parent: Auth,
        ) : ThothJwksParams

        @Secured(Guards.Normal)
        @Resource("user")
        data class User(
            private val parent: Auth,
        ) {
            @Summary("List users", method = "GET")
            @Secured(Guards.Admin)
            @Resource("")
            data class All(
                private val parent: User,
            ) : ThothListUserParams

            @NotSecured
            @Summary("Refresh access token", method = "POST")
            @Resource("refresh")
            data class Refresh(
                private val parent: User,
            ) : ThothRefreshTokenParams

            @Resource("{id}")
            @Summary("Get user", method = "GET")
            @Summary("Delete user", method = "DELETE")
            data class Id(
                override val id: UUID_S,
                private val parent: User,
            ) : ThothDeleteUserParams,
                ThothDisplayUserParams {
                @Summary("Update username", method = "POST")
                @Resource("username")
                data class Username(
                    private val parent: Id,
                ) : ThothRenameUserParams {
                    override val id: UUID_S
                        get() = parent.id
                }

                @Summary("Update password", method = "POST")
                @Resource("password")
                data class Password(
                    private val parent: Id,
                ) : ThothChangePasswordParams {
                    override val id: UUID_S
                        get() = parent.id
                }

                @Secured(Guards.Admin)
                @Summary("Update permissions", method = "PUT")
                @Resource("permissions")
                data class Permissions(
                    private val parent: Id,
                ) : ThothModifyPermissionsParams {
                    override val id: UUID_S
                        get() = parent.id
                }
            }

            @Summary("Get current user", method = "GET")
            @Resource("current")
            data class Current(
                private val parent: User,
            ) : ThothCurrentUserParams
        }
    }

    @Secured(Guards.Normal)
    @Resource("me")
    @Tagged("Me")
    data class Me(
        private val parent: Api,
    ) {
        @Summary("Get continue listening", method = "GET")
        @Resource("continue-listening")
        data class ContinueListening(
            val limit: Int = 20,
            private val parent: Me,
        )

        @Summary("Get listening history", method = "GET")
        @Resource("history")
        data class History(
            val limit: Int = 20,
            val offset: Long = 0,
            private val parent: Me,
        )
    }

    @Summary("Ping server", method = "POST")
    @Tagged("Server")
    @Resource("ping")
    data class Ping(
        private val parent: Api,
    )

    @Secured(Guards.Normal)
    @Summary("List third party licenses", method = "GET")
    @Tagged("Server")
    @Resource("licenses")
    data class Licenses(
        private val parent: Api,
    )

    @Secured(Guards.Admin)
    @Summary("List file scanners", method = "GET")
    @Tagged("Scanner")
    @Resource("scanners")
    data class Scanners(
        private val parent: Api,
    )

    @Secured(Guards.Admin)
    @Summary("List metadata agents", method = "GET")
    @Tagged("Scanner")
    @Resource("metadata-agents")
    data class MetadataAgents(
        private val parent: Api,
    )

    @Secured(Guards.Normal)
    @Summary("List libraries", method = "GET")
    @Summary("Create library", method = "POST", status = 201)
    @Resource("libraries")
    @Tagged("Library")
    data class Libraries(
        private val parent: Api,
    ) {
        @Summary("Search in all libraries", method = "GET")
        @Resource("search")
        data class Search(
            val q: String? = null,
            val author: String? = null,
            val book: String? = null,
            val series: String? = null,
            private val parent: Libraries,
        ) {
            init {
                if (q == null && author == null && book == null && series == null) {
                    throw ErrorResponse.userError(
                        "At least one of the following parameters must be provided: q, author, book, series",
                    )
                }
            }
        }

        @Resource("{libraryId}")
        @Summary("Delete library", method = "DELETE")
        @Summary("Update library", method = "PATCH")
        @Summary("Get library", method = "GET")
        data class Id(
            val libraryId: UUID_S,
            private val parent: Libraries,
        ) : BeforeBodyParsing {
            override suspend fun RoutingContext.beforeBodyParsing() {
                assertLibraryPermissions(libraryId)
            }

            @Summary("Rescan library", method = "POST", status = 202)
            @Resource("rescan")
            data class Rescan(
                private val parent: Id,
            ) {
                val libraryId
                    get() = parent.libraryId
            }

            @Resource("books")
            @Tagged("Books")
            data class Books(
                private val parent: Libraries.Id,
            ) {
                val libraryId
                    get() = parent.libraryId

                @Summary("List books", method = "GET")
                @Resource("")
                data class All(
                    val limit: Int = 20,
                    val offset: Long = 0,
                    val order: Order = Order.ASC,
                    val showInvisible: Boolean = false,
                    private val parent: Books,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get book autocomplete", method = "GET")
                @Resource("autocomplete")
                data class Autocomplete(
                    val q: String,
                    private val parent: Books,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get book", method = "GET")
                @Summary("Update book", method = "PATCH")
                @Resource("{id}")
                data class Id(
                    val id: UUID_S,
                    private val parent: Books,
                ) {
                    val libraryId
                        get() = parent.libraryId

                    @Summary("Auto match book", method = "POST")
                    @Resource("automatch")
                    data class AutoMatch(
                        private val parent: Id,
                    ) {
                        val libraryId
                            get() = parent.libraryId

                        val id
                            get() = parent.id
                    }

                    @Summary("Set book progress", method = "PUT", status = 204)
                    @Resource("progress")
                    data class Progress(
                        private val parent: Id,
                    ) : UserScoped {
                        val libraryId
                            get() = parent.libraryId

                        val id
                            get() = parent.id

                        @Summary("Set book finished", method = "PUT", status = 204)
                        @Resource("finished")
                        data class Finished(
                            private val parent: Progress,
                        ) : UserScoped {
                            val libraryId
                                get() = parent.libraryId

                            val id
                                get() = parent.id
                        }

                        @Summary("Set book dismissed", method = "PUT", status = 204)
                        @Resource("dismissed")
                        data class Dismissed(
                            private val parent: Progress,
                        ) : UserScoped {
                            val libraryId
                                get() = parent.libraryId

                            val id
                                get() = parent.id
                        }
                    }
                }
            }

            @Resource("authors")
            @Tagged("Authors")
            @Summary("Create author", method = "POST", status = 201)
            data class Authors(
                private val parent: Libraries.Id,
            ) {
                val libraryId
                    get() = parent.libraryId

                @Summary("List authors", method = "GET")
                @Resource("")
                data class All(
                    val limit: Int = 20,
                    val offset: Long = 0,
                    val order: Order = Order.ASC,
                    val showInvisible: Boolean = false,
                    private val parent: Authors,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get author autocomplete", method = "GET")
                @Resource("autocomplete")
                data class Autocomplete(
                    val q: String,
                    private val parent: Authors,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get author", method = "GET")
                @Summary("Update author", method = "PATCH")
                @Resource("{id}")
                data class Id(
                    val id: UUID_S,
                    private val parent: Authors,
                ) {
                    val libraryId
                        get() = parent.libraryId

                    @Summary("Auto match author", method = "POST")
                    @Resource("automatch")
                    data class AutoMatch(
                        private val parent: Id,
                    ) {
                        val libraryId
                            get() = parent.libraryId

                        val id
                            get() = parent.id
                    }
                }
            }

            @Resource("narrators")
            @Tagged("Narrators")
            data class Narrators(
                private val parent: Id,
            ) {
                val libraryId
                    get() = parent.libraryId

                @Summary("List narrators", method = "GET")
                @Resource("")
                data class All(
                    val limit: Int = 20,
                    val offset: Long = 0,
                    val order: Order = Order.ASC,
                    private val parent: Narrators,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get narrator", method = "GET")
                @Resource("{name}")
                data class Name(
                    val name: String,
                    private val parent: Narrators,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }
            }

            @Resource("genres")
            @Tagged("Genres")
            data class Genres(
                private val parent: Id,
            ) {
                val libraryId
                    get() = parent.libraryId

                @Summary("List genres", method = "GET")
                @Resource("")
                data class All(
                    val limit: Int = 20,
                    val offset: Long = 0,
                    val order: Order = Order.ASC,
                    private val parent: Genres,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get genre", method = "GET")
                @Resource("{name}")
                data class Name(
                    val name: String,
                    private val parent: Genres,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }
            }

            @Resource("series")
            @Tagged("Series")
            @Summary("Create series", method = "POST", status = 201)
            data class Series(
                private val parent: Libraries.Id,
            ) {
                val libraryId
                    get() = parent.libraryId

                @Summary("List series", method = "GET")
                @Resource("")
                data class All(
                    val limit: Int = 20,
                    val offset: Long = 0,
                    val order: Order = Order.ASC,
                    val showInvisible: Boolean = false,
                    private val parent: Series,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get series autocomplete", method = "GET")
                @Resource("autocomplete")
                data class Autocomplete(
                    val q: String,
                    private val parent: Series,
                ) {
                    val libraryId
                        get() = parent.libraryId
                }

                @Summary("Get series", method = "GET")
                @Summary("Update series", method = "PATCH")
                @Resource("{id}")
                data class Id(
                    val id: UUID_S,
                    private val parent: Series,
                ) {
                    val libraryId
                        get() = parent.libraryId

                    @Summary("Auto match series", method = "POST")
                    @Resource("automatch")
                    data class AutoMatch(
                        private val parent: Id,
                    ) {
                        val libraryId
                            get() = parent.libraryId

                        val id
                            get() = parent.id
                    }
                }
            }

            @Secured(Guards.Normal)
            @Resource("metadata")
            @Tagged("Metadata")
            data class Metadata(
                private val parent: Id,
            ) {
                val libraryId
                    get() = parent.libraryId

                @Resource("author")
                data class Author(
                    private val parent: Metadata,
                ) {
                    @Summary("Get author metadata", method = "GET")
                    @Resource("{id}")
                    data class Id(
                        val id: String,
                        val provider: String,
                        private val parent: Author,
                    ) {
                        val libraryId
                            get() = parent.parent.libraryId
                    }

                    @Summary("Search author metadata", method = "GET")
                    @Resource("search")
                    data class Search(
                        val q: String,
                        private val parent: Author,
                    ) {
                        val libraryId
                            get() = parent.parent.libraryId
                    }
                }

                @Resource("book")
                data class Book(
                    private val parent: Metadata,
                ) {
                    @Summary("Get book metadata", method = "GET")
                    @Resource("{id}")
                    data class Id(
                        val id: String,
                        val provider: String,
                        private val parent: Book,
                    ) {
                        val libraryId
                            get() = parent.parent.libraryId
                    }

                    @Summary("Search book metadata", method = "GET")
                    @Resource("search")
                    data class Search(
                        val q: String,
                        val keywords: String? = null,
                        val authorName: String? = null,
                        val narrator: String? = null,
                        val language: MetadataLanguage? = null,
                        val pageSize: MetadataSearchCount? = null,
                        private val parent: Book,
                    ) {
                        val libraryId
                            get() = parent.parent.libraryId
                    }
                }

                @Resource("series")
                data class Series(
                    private val parent: Metadata,
                ) {
                    @Summary("Get series metadata", method = "GET")
                    @Resource("{id}")
                    data class Id(
                        val id: String,
                        val provider: String,
                        private val parent: Series,
                    ) {
                        val libraryId
                            get() = parent.parent.libraryId
                    }

                    @Summary("Search series metadata", method = "GET")
                    @Resource("search")
                    data class Search(
                        val q: String,
                        val authorName: String? = null,
                        private val parent: Series,
                    ) {
                        val libraryId
                            get() = parent.parent.libraryId
                    }
                }
            }
        }
    }

    @Secured(Guards.Media)
    @Resource("stream")
    @Tagged("Files")
    data class Files(
        private val parent: Api,
    ) {
        @Resource("audio")
        data class Audio(
            private val parent: Files,
        ) {
            @Summary("Get audio file", method = "GET")
            @Resource("{id}")
            data class Id(
                val id: UUID_S,
                private val parent: Audio,
            )
        }

        @Resource("images")
        data class Images(
            private val parent: Files,
        ) {
            @Summary("Get image file", method = "GET")
            @Resource("{id}")
            data class Id(
                val id: UUID_S,
                private val parent: Images,
            )
        }
    }
}
