package io.thoth.server.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource
import com.sksamuel.hoplite.addResourceSource
import java.io.File
import java.nio.file.Path

private fun isProduction(): Boolean {
    return System.getenv("PRODUCTION")?.toBooleanStrictOrNull() ?: false
}

private fun getConfigPath(): String {
    val path =  if (isProduction()) {
        System.getenv("THOTH_CONFIG_PATH")
    } else {
        System.getenv("THOTH_CONFIG_PATH") ?: "config/thoth-config.yaml"
    }

    return Path.of(path).toAbsolutePath().toString()
}


fun loadConfig(): ThothConfig {
    val configFile = getConfigPath()

    return ConfigLoaderBuilder.default()
        .addResourceOrFileSource(configFile)
        .build()
        .loadConfigOrThrow<ThothConfigImpl>()
}
