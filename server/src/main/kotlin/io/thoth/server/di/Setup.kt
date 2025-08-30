package io.thoth.server.di

import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.audible.client.AudibleMetadataAgent
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.config.ThothConfig
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.serialization.Serialization
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import io.thoth.server.file.analyzer.impl.AudioFolderScanner
import io.thoth.server.file.analyzer.impl.AudioTagScanner
import io.thoth.server.file.scanner.LibraryScanner
import io.thoth.server.file.scanner.LibraryScannerImpl
import io.thoth.server.repositories.AuthorRepository
import io.thoth.server.repositories.AuthorServiceImpl
import io.thoth.server.repositories.BookRepository
import io.thoth.server.repositories.BookRepositoryImpl
import io.thoth.server.repositories.LibraryRepository
import io.thoth.server.repositories.LibraryRepositoryImpl
import io.thoth.server.repositories.SeriesRepository
import io.thoth.server.repositories.SeriesRepositoryImpl
import io.thoth.server.schedules.ThothSchedules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger

fun setupDependencyInjection() =
    startKoin {
        modules(
            module {
                single { ThothConfig.load() }
                single<MetadataAgents> { MetadataAgents(listOf(AudibleMetadataAgent())) }
                single<AudioFileAnalyzers> { AudioFileAnalyzers(listOf(AudioTagScanner(), AudioFolderScanner())) }
                single<LibraryScanner> { LibraryScannerImpl() }
                single<Serialization> { JacksonSerialization() }
                single<BookRepository> { BookRepositoryImpl() }
                single<AuthorRepository> { AuthorServiceImpl() }
                single<SeriesRepository> { SeriesRepositoryImpl() }
                single<LibraryRepository> { LibraryRepositoryImpl() }
                single { Scheduler() }
                single { ThothSchedules() }
            },
        )
        slf4jLogger()
    }
