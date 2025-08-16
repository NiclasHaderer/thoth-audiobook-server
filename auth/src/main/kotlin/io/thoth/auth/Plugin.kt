package io.thoth.auth

import io.ktor.server.application.*

object ThothAuthenticationPlugin {
    fun <PERMISSIONS> build(): ApplicationPlugin<ThothAuthConfigBuilder<PERMISSIONS>> =
        createApplicationPlugin(name = "ThothAuthPlugin", createConfiguration = ::ThothAuthConfigBuilder) {
            val pluginConfig = pluginConfig.build()
            // Make sure that the key pairs are RSA key pairs
            pluginConfig.run { application.applyGuards() }

            onCall { call -> call.attributes.put(PLUGIN_CONFIG_KEY, pluginConfig) }
        }
}
