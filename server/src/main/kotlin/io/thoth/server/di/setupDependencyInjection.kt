package io.thoth.server.di

import io.thoth.common.scheduling.Scheduler
import io.thoth.config.ThothConfig
import io.thoth.config.loadPublicConfig
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.MetadataWrapper
import io.thoth.metadata.audible.client.AudibleClient
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import io.thoth.server.file.analyzer.impl.AudioFileAnalyzerWrapperImpl
import io.thoth.server.file.analyzer.impl.AudioFolderScanner
import io.thoth.server.file.analyzer.impl.AudioTagScanner
import io.thoth.server.file.persister.TrackManager
import io.thoth.server.file.persister.TrackManagerImpl
import io.thoth.server.file.scanner.FileWatcher
import io.thoth.server.file.scanner.FileWatcherImpl
import io.thoth.server.file.scanner.LibraryScanner
import io.thoth.server.file.scanner.LibraryScannerImpl
import io.thoth.server.schedules.ThothSchedules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger

fun setupDependencyInjection() = startKoin {
    val config = loadPublicConfig()
    modules(
        module {
            single { config }
            single<MetadataProvider> { MetadataWrapper(listOf(AudibleClient(get<ThothConfig>().audibleRegion))) }
            single<FileWatcher> { FileWatcherImpl() }
            single<AudioFileAnalyzerWrapper> {
                AudioFileAnalyzerWrapperImpl(
                    listOf(AudioTagScanner(get()), AudioFolderScanner(get())),
                )
            }
            single<LibraryScanner> { LibraryScannerImpl() }
            single { Scheduler() }
            single { ThothSchedules() }
            single<TrackManager> { TrackManagerImpl() }
        },
    )
    slf4jLogger()
}
