package io.github.huiibuh.plugins

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.sessions.*
import kotlin.collections.set

fun Application.configureSecurity() {

    class MySession(val count: Int = 0)
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    routing {
        //        get("/session/increment") {
        //            val session = call.sessions.get<MySession>() ?: MySession()
        //            call.sessions.set(session.copy(count = session.count + 1))
        //            call.respondText("Counter is ${session.count}. Refresh to increment.")
        //        }
    }
}
