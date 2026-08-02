package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.ThirdPartyLicense
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import kotlinx.serialization.json.Json

/**
 * Compiled into the libtag_c binary that :taglib bundles as a resource, so they are shipped without ever appearing on
 * the maven classpath the license report walks. Declared by hand because neither has a maven coordinate.
 */
private val nativeLicenses =
    listOf(
        ThirdPartyLicense(
            name = "taglib",
            version = "2.3.1",
            license = "LGPL-2.1-only, MPL-1.1",
            licenseUrl = "https://github.com/taglib/taglib/blob/master/COPYING.LGPL",
            repository = "https://github.com/niclashaderer/taglib",
            text = null,
        ),
        ThirdPartyLicense(
            name = "utfcpp",
            version = "4.1.0",
            license = "BSL-1.0",
            licenseUrl = "https://www.boost.org/LICENSE_1_0.txt",
            repository = "https://github.com/nemtrif/utfcpp",
            text = null,
        ),
    )

// Written into the jar by the generateLicenseReport gradle task
private val thirdPartyLicenses: List<ThirdPartyLicense> by lazy {
    val report =
        Api::class.java.getResourceAsStream("/third-party-licenses.json")
            ?: throw ErrorResponse.internalError("third-party-licenses.json is missing from the jar")
    val fromClasspath = report.use { Json.decodeFromString<List<ThirdPartyLicense>>(it.reader().readText()) }
    (fromClasspath + nativeLicenses).sortedBy { it.name }
}

fun Routing.licenseRouting() {
    get<Api.Licenses, List<ThirdPartyLicense>> { thirdPartyLicenses }
}
