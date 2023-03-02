package io.thoth.openapi.models

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License

class InfoContext internal constructor(private val api: OpenAPI) {
  init {
    api.info = Info()
  }

  var title: String
    get() = api.info.title
    set(value) {
      api.info.title = value
    }
  var description: String
    get() = api.info.description
    set(value) {
      api.info.description = value
    }
  var termsOfService: String
    get() = api.info.termsOfService
    set(value) {
      api.info.termsOfService = value
    }
  var version: String
    get() = api.info.version
    set(value) {
      api.info.version = value
    }

  fun license(configure: License.() -> Unit) {
    api.info.license = License().apply(configure)
  }

  fun contact(configure: Contact.() -> Unit) {
    api.info.contact = Contact().apply(configure)
  }
}
