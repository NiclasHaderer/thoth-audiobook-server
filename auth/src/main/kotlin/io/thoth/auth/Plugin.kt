package io.thoth.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*

val ThothAuthenticationPlugin =
    createApplicationPlugin(
        name = "ThothAuthPlugin",
        createConfiguration = ::ThothAuthConfig,
    ) {

        // Make sure that the key pairs are RSA key pairs
        pluginConfig.assertAreRsaKeyPairs()
        pluginConfig.run { application.applyGuards() }

        onCall { call -> call.attributes.put(PLUGIN_CONFIG_KEY, pluginConfig) }
    }
