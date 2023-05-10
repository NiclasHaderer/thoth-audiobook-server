package io.thoth.server.di

import io.ktor.http.*
import io.thoth.metadata.MetadataProviders
import io.thoth.metadata.MetadataWrapper
import io.thoth.metadata.audible.client.AudibleClient
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.config.loadPublicConfig
import io.thoth.server.file.TrackManager
import io.thoth.server.file.TrackManagerImpl
import io.thoth.server.file.analyzer.AudioFileAnalyzerWrapper
import io.thoth.server.file.analyzer.AudioFileAnalyzers
import io.thoth.server.file.analyzer.impl.AudioFileAnalyzerWrapperImpl
import io.thoth.server.file.analyzer.impl.AudioFolderScanner
import io.thoth.server.file.analyzer.impl.AudioTagScanner
import io.thoth.server.file.scanner.FileTreeWatcher
import io.thoth.server.file.scanner.FileTreeWatcherImpl
import io.thoth.server.file.scanner.LibraryScanner
import io.thoth.server.file.scanner.LibraryScannerImpl
import io.thoth.server.plugins.authentication.AuthConfig
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
            single<MetadataProviders> {
                val allProviders = listOf(AudibleClient())
                MetadataProviders(
                    allProviders + MetadataWrapper(allProviders),
                )
            }
            single<MetadataWrapper> { MetadataWrapper(get<MetadataProviders>()) }
            single<FileTreeWatcher> { FileTreeWatcherImpl() }
            single<AudioFileAnalyzers> {
                AudioFileAnalyzers(
                    listOf(
                        AudioTagScanner(),
                        AudioFolderScanner(),
                    ),
                )
            }
            single<AudioFileAnalyzerWrapper> { AudioFileAnalyzerWrapperImpl() }
            single<LibraryScanner> { LibraryScannerImpl() }
            single<BookRepository> { BookRepositoryImpl() }
            single<AuthorRepository> { AuthorServiceImpl() }
            single<SeriesRepository> { SeriesRepositoryImpl() }
            single<LibraryRepository> { LibraryRepositoryImpl() }
            single<TrackManager> { TrackManagerImpl() }
            single { Scheduler() }
            single { ThothSchedules() }
            single {
                AuthConfig.Builder()
                    .configure {
                        domain = "127.0.0.1:${config.port}"
                        protocol = if (config.TLS) URLProtocol.HTTPS else URLProtocol.HTTP
                        keyPairPath = "${config.configDirectory}/jwt.pem"
                    }
                    .build()
            }
        },
    )
    slf4jLogger()
}
