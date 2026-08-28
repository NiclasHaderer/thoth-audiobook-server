package io.thoth.server.config

import com.cronutils.model.Cron
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import io.thoth.server.common.extensions.toCron
import java.nio.file.Path
import kotlin.io.path.createDirectories

data class ThothConfig(
    val dataDir: Path = Path.of("data"),
    val port: Int = 8080,
    val baseUrl: String = "/",
    // Only marks the auth cookies as secure, the server itself never terminates TLS
    val tls: Boolean = false,
    val allowNewSignups: Boolean = true,
    val fullScanCron: Cron = "0 2 * * *".toCron(),
    // 0 derives it from the CPU count. Keep low, if you have hdds if you have SSDs go wild
    val scanThreads: Int = 0,
    // How long a file has to keep the mtime before it is read
    val settleMillis: Long = 2000,
    // Progress older than this drops out of "continue listening", but stays resumable
    val continueListeningWeeks: Int = 8,
) {
    val importThreads: Int
        get() = if (scanThreads > 0) scanThreads else (Runtime.getRuntime().availableProcessors() - 1).coerceIn(2, 6)

    val jwtKeyFile: Path get() = dataDir.resolve("jwt.pem")
    val sqliteFile: Path get() = dataDir.resolve("thoth.db")

    companion object {
        private const val ENV_PREFIX = "THOTH_"
        private const val CONFIG_PATH_ENV = "THOTH_CONFIG_PATH"

        fun load(): ThothConfig {
            val loader =
                ConfigLoaderBuilder
                    .default()
                    .withResolveTypesCaseInsensitive()
                    .addDecoder(CronDecoder())
                    .addPropertySource(
                        EnvironmentVariablesPropertySource(
                            useUnderscoresAsSeparator = true,
                            allowUppercaseNames = true,
                            environmentVariableMap = ::thothEnvironment,
                            prefix = ENV_PREFIX,
                        ),
                    )

            val configPath = System.getenv(CONFIG_PATH_ENV)
            if (configPath != null) {
                loader.addFileSource(configPath)
            } else {
                loader.addFileSource("thoth-config.yaml", optional = true)
                loader.addFileSource("thoth-config.json", optional = true)
            }

            return loader.build().loadConfigOrThrow<ThothConfig>().also { it.dataDir.createDirectories() }
        }

        private fun thothEnvironment(): Map<String, String> =
            System
                .getenv()
                .filterKeys { it.startsWith(ENV_PREFIX) && it != CONFIG_PATH_ENV }
                // Hoplite nests on "__", but THOTH_DATABASE_HOST is friendlier than THOTH_DATABASE__HOST
                .mapKeys { (key, _) -> key.replace(Regex("^THOTH_DATABASE_+"), "THOTH_DATABASE__") }
    }
}
