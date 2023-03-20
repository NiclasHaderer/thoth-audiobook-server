package io.thoth.server.di

import io.thoth.common.scheduling.Scheduler
import io.thoth.config.ThothConfig
import io.thoth.config.loadPublicConfig
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.MetadataWrapper
import io.thoth.metadata.audible.client.AudibleClient
import io.thoth.server.file.TrackManager
import io.thoth.server.file.TrackManagerImpl
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import io.thoth.server.file.analyzer.impl.AudioFileAnalyzerWrapperImpl
import io.thoth.server.file.analyzer.impl.AudioFolderScanner
import io.thoth.server.file.analyzer.impl.AudioTagScanner
import io.thoth.server.file.scanner.FileTreeWatcher
import io.thoth.server.file.scanner.FileTreeWatcherImpl
import io.thoth.server.file.scanner.LibraryScanner
import io.thoth.server.file.scanner.LibraryScannerImpl
import io.thoth.server.schedules.ThothSchedules
import io.thoth.server.services.BookService
import io.thoth.server.services.BookServiceImpl
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger

fun setupDependencyInjection() = startKoin {
    val config = loadPublicConfig()
    modules(
        module {
            single { config }
            single<MetadataProvider> { MetadataWrapper(listOf(AudibleClient(get<ThothConfig>().audibleRegion))) }
            single<FileTreeWatcher> { FileTreeWatcherImpl() }
            single<AudioFileAnalyzerWrapper> {
                AudioFileAnalyzerWrapperImpl(
                    listOf(AudioTagScanner(), AudioFolderScanner()),
                )
            }
            single<LibraryScanner> { LibraryScannerImpl() }
            single { Scheduler() }
            single<BookService> { BookServiceImpl() }
            single { ThothSchedules() }
            single<TrackManager> { TrackManagerImpl(get(), get()) }
        },
    )
    slf4jLogger()
}
