package io.thoth.openapi.models

import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License

class InfoContext internal constructor(private val info: Info) {
    var title: String
        get() = info.title
        set(value) {
            info.title = value
        }
    var description: String
        get() = info.description
        set(value) {
            info.description = value
        }
    var termsOfService: String
        get() = info.termsOfService
        set(value) {
            info.termsOfService = value
        }
    var version: String
        get() = info.version
        set(value) {
            info.version = value
        }

    fun license(configure: License.() -> Unit) {
        info.license = License().apply(configure)
    }

    fun contact(configure: Contact.() -> Unit) {
        info.contact = Contact().apply(configure)
    }
}
