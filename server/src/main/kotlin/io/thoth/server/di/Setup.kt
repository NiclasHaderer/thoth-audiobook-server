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
import io.thoth.server.services.AuthorRepository
import io.thoth.server.services.AuthorServiceImpl
import io.thoth.server.services.BookRepository
import io.thoth.server.services.BookRepositoryImpl
import io.thoth.server.services.LibraryRepository
import io.thoth.server.services.LibraryRepositoryImpl
import io.thoth.server.services.SeriesRepository
import io.thoth.server.services.SeriesRepositoryImpl
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger

fun setupDependencyInjection() = startKoin {
    val config = loadPublicConfig()
    modules(
        module {
            single { config }
            single<MetadataProvider> { MetadataWrapper(listOf(AudibleClient(get<ThothConfig>().audibleRegion))) }
            single<FileTreeWatcher> { FileTreeWatcherImpl(get(), get()) }
            single<AudioFileAnalyzerWrapper> {
                AudioFileAnalyzerWrapperImpl(
                    listOf(AudioTagScanner(), AudioFolderScanner()),
                )
            }
            single<LibraryScanner> { LibraryScannerImpl() }
            single { Scheduler() }
            single<BookRepository> { BookRepositoryImpl(get(), get()) }
            single<AuthorRepository> { AuthorServiceImpl() }
            single<SeriesRepository> { SeriesRepositoryImpl(get(), get()) }
            single<LibraryRepository> { LibraryRepositoryImpl(get(), get(), get()) }
            single { ThothSchedules() }
            single<TrackManager> { TrackManagerImpl(get(), get(), get(), get()) }
        },
    )
    slf4jLogger()
}
