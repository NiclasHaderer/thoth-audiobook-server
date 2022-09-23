package io.thoth.server.plugins

import io.ktor.server.application.*
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.MetadataWrapper
import io.thoth.metadata.audible.client.AudibleClient
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapperImpl
import io.thoth.server.file.analyzer.impl.AudioFolderScanner
import io.thoth.server.file.analyzer.impl.AudioTagScanner
import io.thoth.server.file.persister.FileAnalyzingScheduler
import io.thoth.server.file.persister.FileAnalyzingSchedulerImpl
import io.thoth.server.config.DevThothConfig
import io.thoth.server.config.ProdThothConfig
import io.thoth.server.config.ThothConfig
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.core.module.Module as KoinModule

fun Application.configureProdKoin() {
    install(Koin) {
        modules(
            module {
                single<ThothConfig> { ProdThothConfig }
                koinCommon()
            }
        )
        slf4jLogger()
    }
}


fun Application.configureDevKoin() {
    install(Koin) {
        modules(
            module {
                single<ThothConfig> { DevThothConfig }
                koinCommon()
            }
        )
        slf4jLogger()
    }
}


private fun KoinModule.koinCommon() {
    single<MetadataProvider> {
        val config: ThothConfig = get()
        MetadataWrapper(
            listOf(
                AudibleClient(config.audibleSearchHost, config.audibleAuthorHost)
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
