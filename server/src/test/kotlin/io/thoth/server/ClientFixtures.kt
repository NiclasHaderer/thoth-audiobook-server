package io.thoth.server

import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.util.reflect.reifiedType
import io.thoth.client.gen.ThothClientImpl
import io.thoth.server.di.serialization.JacksonSerialization
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.assertNotNull

class TestThothClient(
    private val http: HttpClient,
) : ThothClientImpl(Url("http://localhost")) {
    private val mapper get() = getKoin().get<JacksonSerialization>().objectMapper

    override fun createClient() = http

    init {
        serialize<Any> { builder, body, _ ->
            builder.contentType(ContentType.Application.Json)
            builder.setBody(mapper.writeValueAsString(body))
        }
        deserialize<Any> { response, typeInfo ->
            // The type argument is explicit because inference would otherwise unify it with the Unit
            // branch and throw the parsed value away
            if (typeInfo.type == Unit::class) {
                Unit
            } else {
                mapper.readValue<Any>(response.bodyAsText(), mapper.constructType(typeInfo.reifiedType))
            }
        }
    }
}

val ApplicationTestBuilder.api: TestThothClient get() = TestThothClient(client)

fun bearer(token: String): Headers = Headers.build { append(HttpHeaders.Authorization, "Bearer $token") }

fun cookie(
    name: String,
    value: String,
): Headers = Headers.build { append(HttpHeaders.Cookie, "$name=$value") }

fun HttpResponse.authCookie(name: String) = assertNotNull(setCookie().firstOrNull { it.name == name }, "no $name cookie")
