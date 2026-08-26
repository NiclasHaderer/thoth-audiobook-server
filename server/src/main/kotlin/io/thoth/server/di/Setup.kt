package io.thoth.server.di

import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.audible.AudibleMetadataAgent
import io.thoth.metadata.audiobookdb.AudiobookDbMetadataAgent
import io.thoth.metadata.libby.LibbyMetadataAgent
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.config.ThothConfig
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.serialization.Serialization
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import io.thoth.server.file.analyzer.impl.AudioFolderScanner
import io.thoth.server.file.analyzer.impl.AudioTagScanner
import io.thoth.server.file.TrackManager
import io.thoth.server.file.scanner.LibraryCleanup
import io.thoth.server.file.scanner.LibraryImportPipeline
import io.thoth.server.file.scanner.LibraryRoots
import io.thoth.server.file.scanner.LibraryWatcher
import io.thoth.server.file.scanner.LibraryWatcherImpl
import io.thoth.server.repositories.AuthorRepository
import io.thoth.server.repositories.AuthorServiceImpl
import io.thoth.server.repositories.BookRepository
import io.thoth.server.repositories.BookRepositoryImpl
import io.thoth.server.repositories.LibraryRepository
import io.thoth.server.repositories.LibraryRepositoryImpl
import io.thoth.server.repositories.GenreRepository
import io.thoth.server.repositories.GenreRepositoryImpl
import io.thoth.server.repositories.NarratorRepository
import io.thoth.server.repositories.NarratorRepositoryImpl
import io.thoth.server.repositories.SeriesRepository
import io.thoth.server.repositories.SeriesRepositoryImpl
import io.thoth.server.schedules.ThothSchedules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger

fun thothModule(config: ThothConfig) =
    module {
        single { config }
        single<MetadataAgents> {
            MetadataAgents(listOf(AudibleMetadataAgent(), LibbyMetadataAgent(), AudiobookDbMetadataAgent()))
        }
        single<AudioFileAnalyzers> { AudioFileAnalyzers(listOf(AudioTagScanner(), AudioFolderScanner())) }
        single { ImageDownloader() }
        single { LibraryRoots() }
        single { LibraryCleanup() }
        single { TrackManager() }
        single<LibraryImportPipeline> { LibraryImportPipeline() }
        single<LibraryWatcher> { LibraryWatcherImpl() }
        single { JacksonSerialization() }
        single<Serialization> { get<JacksonSerialization>() }
        single<BookRepository> { BookRepositoryImpl() }
        single<AuthorRepository> { AuthorServiceImpl() }
        single<SeriesRepository> { SeriesRepositoryImpl() }
        single<NarratorRepository> { NarratorRepositoryImpl() }
        single<GenreRepository> { GenreRepositoryImpl() }
        single<LibraryRepository> { LibraryRepositoryImpl() }
        single { Scheduler() }
        single { ThothSchedules() }
    }

fun setupDependencyInjection(config: ThothConfig) {
    startKoin {
        modules(thothModule(config))
        slf4jLogger()
    }
}
