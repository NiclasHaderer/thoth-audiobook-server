package io.thoth.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.thoth.auth.models.ThothUserPermissions

object ThothAuthenticationPlugin {
    fun <ID : Any, PERMISSIONS : ThothUserPermissions> build(): ApplicationPlugin<ThothAuthConfig<ID, PERMISSIONS>> {
        return createApplicationPlugin(
            name = "ThothAuthPlugin",
            createConfiguration = ::ThothAuthConfig,
        ) {

            // Make sure that the key pairs are RSA key pairs
            pluginConfig.assertAreRsaKeyPairs()
            pluginConfig.run { application.applyGuards() }

            onCall { call -> call.attributes.put(PLUGIN_CONFIG_KEY, pluginConfig) }
        }
    }
}
