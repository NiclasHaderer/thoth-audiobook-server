package io.thoth.auth

import io.ktor.server.application.*

val ThothAuthenticationPlugin =
    createApplicationPlugin(
        name = "ThothAuthPlugin",
        createConfiguration = ::ThothAuthConfig,
    ) {

        // Make sure that the key pairs are RSA key pairs
        pluginConfig.assertAreRsaKeyPairs()

        onCall { call -> call.attributes.put(PLUGIN_CONFIG_KEY, pluginConfig) }
    }
