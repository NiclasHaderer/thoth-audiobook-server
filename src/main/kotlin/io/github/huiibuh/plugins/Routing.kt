package io.github.huiibuh.plugins

import io.ktor.application.*
import io.ktor.locations.*

@OptIn(KtorExperimentalLocationsAPI::class)
fun Application.configureRouting() {

    install(Locations) {
    }

    //    routing {
    //        get("/") {
    //            call.respondText("Hello World!")
    //        }
    //        // Static plugin. Try to access `/static/index.html`
    //        static("/static") {
    //            resources("static")
    //        }
    //        get<MyLocation> {
    //            call.respondText("Location: name=${it.name}, arg1=${it.arg1}, arg2=${it.arg2}")
    //        }
    //        // Register nested routes
    //        get<Type.Edit> {
    //            call.respondText("Inside $it")
    //        }
    //        get<Type.List> {
    //            call.respondText("Inside $it")
    //        }
    //        install(StatusPages) {
    //            exception<AuthenticationException> { cause ->
    //                call.respond(HttpStatusCode.Unauthorized)
    //            }
    //            exception<AuthorizationException> { cause ->
    //                call.respond(HttpStatusCode.Forbidden)
    //            }
    //
    //        }
    //    }
}
//
//@Location("/location/{name}")
//class MyLocation(val name: String, val arg1: Int = 42, val arg2: String = "default")
//
//@Location("/type/{name}")
//data class Type(val name: String) {
//    @Location("/edit")
//    data class Edit(val type: Type)
//
//    @Location("/list/{page}")
//    data class List(val type: Type, val page: Int)
//}
//
//class AuthenticationException : RuntimeException()
//class AuthorizationException : RuntimeException()
