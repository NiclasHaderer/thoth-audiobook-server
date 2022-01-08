package io.github.huiibuh.plugins

import io.github.huiibuh.file.analyzer.AudioFileAnalyzer
import io.github.huiibuh.file.analyzer.AudioFileAnalyzerWrapper
import io.github.huiibuh.file.analyzer.impl.tag.AudioFolderScanner
import io.github.huiibuh.file.analyzer.impl.tag.AudioTagScanner
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.MetadataWrapper
import io.github.huiibuh.settings.DevSettings
import io.github.huiibuh.settings.ProdSettings
import io.github.huiibuh.settings.Settings
import io.github.huiibuh.settings.isProduction
import io.ktor.application.*
import metadata.audible.client.AudibleClient
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        module {
            single {
                if (isProduction()) ProdSettings else DevSettings
            }
            single<MetadataProvider> {
                val settings: Settings = get()
                MetadataWrapper(listOf(
                    AudibleClient(settings.audibleSearchHost, settings.audibleAuthorHost)
                ))
            }
            single<AudioFileAnalyzer> {
                AudioFileAnalyzerWrapper(get(), listOf(AudioTagScanner(get()), AudioFolderScanner(get())))
            }
        }
    }
}
