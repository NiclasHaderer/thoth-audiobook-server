# Ktor OpenApi

This module provides a Ktor feature for generating OpenApi documentation from Ktor routes.
The documentation is generated using the written code, therefore, a sourcecode change automatically updates the
documentation.
The code is therefore the single source of truth.

## Usage

### Setup

Make sure that you have the following plugins installed and configured.

```kotlin
fun Application.module() {
    install(DataConversion)
    install(Resources)
    install(ContentNegotiation) {
        // only tested with jackson, however other converters should work as well
        jackson {
            // configure jackson
        }
    }
}
```

And then configure the OpenApi routing.

```kotlin
import io.thoth.openapi.ktor.errors.configureStatusPages
import io.thoth.openapi.ktor.plugins.OpenAPIRouting
import io.thoth.openapi.ktor.plugins.OpenAPISchemaType
import io.thoth.openapi.ktor.plugins.OpenAPIWebUI

fun Application.openApiModule() {
    // Specify the openapi information
    install(OpenAPIRouting) {
        info {
            title = "Thoth"
            version = "0.0.1"
            description = "Audiobook server"
        }
        externalDocs {
            url = "https://google.com"
            description = "Google"
        }
    }

    // And serve the openapi.json/yaml file as well as the web-ui
    install(OpenAPIWebUI) {
        schemaType = OpenAPISchemaType.JSON
        schemaPath = "/docs/openapi"
        docsPath = "/docs"
    }

    // (Optional) configure the status pages (some defaults provided by me)
    configureStatusPages()
}
```

### Define routes

```kotlin
@Resource("api")
class Api {

    @Resource("fs")
    // Add a description to the route. This is optional, but will be used if you want to generate a client
    @Summary("List folders at a certain path", method = "GET")
    // Tag the route (and all sub-routes) with the "Filesystem" tag
    @Tagged("Filesystem")
    data class FileSystem(val path: String, val showHidden: Boolean = false, private val parent: Api)

    // The books resource is only accessible for users that are logged in
    // This extends to every embedded class of books.
    // This annotation is roughly equal to the following code (https://ktor.io/docs/authentication.html):
    // ```kotlin
    // authenticate(secured.name){
    // //...
    // }
    // ```
    @Secured("user")
    @Resource("books")
    // Add some longer description the api
    @Description("The books resource is only accessible for users that are logged in")
    data class Books(val parent: Api) {

        // Also secured
        @Resource("")
        data class Something

        // Not secured, but only this class. Children are secured again.
        @Resource("something")
        @NotSecured
        data class SomethingElse
    }
}
```

### Using the defined routes

```kotlin
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.post


class SomeModel

class BodyData

class ResponseData

fun Application.module() {
    routing {
        route("") {

            // Use the extension method to create a new route
            get<Api.Books, SomeModel> { it: Api.Books ->
                SomeModel()
            }

            // Methods which can have a body have three generic arguments
            post<Api.Fs, BodyData, ResponseData> { it: Api.Fs, body: BodyData ->
                // The returned value is automatically converted to json
                ResponseData()
            }
        }
    }
}
```

### Custom responses

By default, there are three different 'special' responses:

1. BinaryResponse (for binary data) *has an extension method called binaryResponse()*
2. FileResponse (for files) *has an extension method called fileResponse()*
3. RedirectResponse (for redirects) *has an extension method called redirectResponse()*

```kotlin
fun Route.someRoute() {
    get<Api.Fs, FileResponse> { it: Api.Fs ->
        fileResponse(it.path)
    }
}
```

If you want some custom behavior not covered by these three responses, you can create your own response (see
the binary response for an example).

```kotlin
class BinaryResponse(val bytes: ByteArray) : BaseResponse {
    override suspend fun respond(call: ApplicationCall) {
        call.respondBytes(bytes)
    }
}

fun RouteHandler.binaryResponse(byteArray: ByteArray): BinaryResponse {
    return BinaryResponse(byteArray)
}
```

### Typescript client generation

You can automatically generate a typescript client for the openapi specification.

```kotlin
TsClientCreator(
    // Get the routes from the OpenApiRouteCollector object
    routes = OpenApiRouteCollector.values(),
    // Specify where the types should be saved
    typesFile = File("models.ts"),
    // Specify where the client should be saved
    clientFile = File("client.ts"),
)
.also {
    // Save the client and types
    it.saveClient()
    it.saveTypes()
}
```