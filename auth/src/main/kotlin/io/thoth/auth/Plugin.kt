package io.thoth.auth

import io.ktor.server.application.*
import io.thoth.auth.models.ThothUserPermissions

object ThothAuthenticationPlugin {
    fun <ID : Any, PERMISSIONS : ThothUserPermissions> build():
        ApplicationPlugin<ThothAuthConfigBuilder<ID, PERMISSIONS>> {
        return createApplicationPlugin(
            name = "ThothAuthPlugin",
            createConfiguration = ::ThothAuthConfigBuilder,
        ) {
            val pluginConfig = pluginConfig.build()
            // Make sure that the key pairs are RSA key pairs
            pluginConfig.run { application.applyGuards() }

            onCall { call -> call.attributes.put(PLUGIN_CONFIG_KEY, pluginConfig) }
        }
    }
}
