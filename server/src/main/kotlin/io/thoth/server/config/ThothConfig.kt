package io.thoth.server.config

import com.cronutils.model.Cron
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource
import java.nio.file.Path
import kotlin.io.path.absolutePathString

data class ThothConfig(
    val ignoreFile: String,
    val production: Boolean,
    val allowNewSignups: Boolean,
    val fullScanCron: Cron,
    val port: Int,
    val domain: String,
    val TLS: Boolean,
    val database: DatabaseConnection,
    val jwtCertificate: String,
) {
    companion object {
        fun load(): ThothConfig {
            val configPathStr = System.getenv("THOTH_CONFIG_PATH") ?: error("Set THOTH_CONFIG_PATH config variable")
            val configPath = Path.of(configPathStr).absolutePathString()

            return ConfigLoaderBuilder
                .default()
                .addDecoder(CronDecoder())
                .addResourceOrFileSource(configPath)
                .build()
                .loadConfigOrThrow<ThothConfig>()
        }
    }
}
