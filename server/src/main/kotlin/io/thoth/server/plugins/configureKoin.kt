package io.thoth.server.plugins

import io.ktor.server.application.*
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.MetadataWrapper
import io.thoth.metadata.audible.client.AudibleClient
import io.thoth.config.public.PublicConfig
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import io.thoth.server.file.analyzer.impl.AudioFileAnalyzerWrapperImpl
import io.thoth.server.file.analyzer.impl.AudioFolderScanner
import io.thoth.server.file.analyzer.impl.AudioTagScanner
import io.thoth.server.file.persister.FileAnalyzingScheduler
import io.thoth.server.file.persister.FileAnalyzingSchedulerImpl
import io.thoth.server.file.scanner.FileWatcher
import io.thoth.server.file.scanner.FileWatcherImpl
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger


fun Application.configureKoin(config: PublicConfig) {
    install(Koin) {
        modules(
            module {
                single { config }
                single<MetadataProvider> {
                    MetadataWrapper(
                        listOf(
                            AudibleClient(get<PublicConfig>().audibleRegion)
                        )
                    )
                }
                single<FileWatcher> { FileWatcherImpl(get(), get()) }
                single<AudioFileAnalyzerWrapper> {
                    AudioFileAnalyzerWrapperImpl(listOf(AudioTagScanner(get()), AudioFolderScanner(get())))
                }
                single<FileAnalyzingScheduler> {
                    FileAnalyzingSchedulerImpl()
                }
            }
        )
        slf4jLogger()
    }
}

