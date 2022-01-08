package io.github.huiibuh.di

import io.github.huiibuh.file.analyzer.AudioFileAnalyzer
import io.github.huiibuh.file.analyzer.AudioFileAnalyzerWrapper
import io.github.huiibuh.file.analyzer.impl.AudioFolderScanner
import io.github.huiibuh.file.analyzer.impl.AudioTagScanner
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.MetadataWrapper
import io.github.huiibuh.metadata.impl.audible.client.AudibleClient
import io.github.huiibuh.settings.DevSettings
import io.github.huiibuh.settings.ProdSettings
import io.github.huiibuh.settings.Settings
import io.github.huiibuh.settings.isProduction
import org.koin.dsl.module

val DI_MODULE = module {
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