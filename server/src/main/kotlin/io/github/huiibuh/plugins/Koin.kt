package io.github.huiibuh.plugins

import io.github.huiibuh.file.analyzer.AudioFileAnalyzerWrapper
import io.github.huiibuh.file.analyzer.AudioFileAnalyzerWrapperImpl
import io.github.huiibuh.file.analyzer.impl.AudioFolderScanner
import io.github.huiibuh.file.analyzer.impl.AudioTagScanner
import io.github.huiibuh.file.persister.FileAnalyzingScheduler
import io.github.huiibuh.file.persister.FileAnalyzingSchedulerImpl
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.MetadataWrapper
import io.github.huiibuh.metadata.audible.client.AudibleClient
import io.github.huiibuh.settings.DevSettings
import io.github.huiibuh.settings.ProdSettings
import io.github.huiibuh.settings.Settings
import io.github.huiibuh.settings.isProduction
import io.ktor.application.*
import org.koin.dsl.module
import org.koin.core.module.Module as KoinModule
import org.koin.ktor.ext.Koin
import org.koin.logger.slf4jLogger

fun Application.configureProdKoin() {
    install(Koin) {
        module {
            single<Settings> {
                if (isProduction()) ProdSettings else DevSettings
            }
            koinCommon()
        }
        slf4jLogger()
    }
}


fun Application.configureDevKoin() {
    install(Koin) {
        module {
            single<Settings> { DevSettings }
            koinCommon()
        }
        slf4jLogger()
    }
}


private fun KoinModule.koinCommon() {
    single<MetadataProvider> {
        val settings: Settings = get()
        MetadataWrapper(
            listOf(
                AudibleClient(settings.audibleSearchHost, settings.audibleAuthorHost)
            )
        )
    }
    single<AudioFileAnalyzerWrapper> {
        AudioFileAnalyzerWrapperImpl(listOf(AudioTagScanner(get()), AudioFolderScanner(get())))
    }
    single<FileAnalyzingScheduler> {
        FileAnalyzingSchedulerImpl()
    }
}
