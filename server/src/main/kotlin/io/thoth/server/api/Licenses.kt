package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.ThirdPartyLicense
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import kotlinx.serialization.json.Json

// Written into the jar by the generateLicenseReport gradle task
private val thirdPartyLicenses: List<ThirdPartyLicense> by lazy {
    val report =
        Api::class.java.getResourceAsStream("/third-party-licenses.json")
            ?: throw ErrorResponse.internalError("third-party-licenses.json is missing from the jar")
    report.use { Json.decodeFromString<List<ThirdPartyLicense>>(it.reader().readText()) }
}

fun Routing.licenseRouting() {
    get<Api.Licenses, List<ThirdPartyLicense>> { thirdPartyLicenses }
}
