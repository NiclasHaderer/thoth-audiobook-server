package io.thoth.server.schedules

import io.thoth.server.common.scheduling.EventTask
import io.thoth.server.common.scheduling.CronTask
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.file.scanner.LibraryScanner
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ThothSchedules : KoinComponent {
    private val config by inject<ThothConfig>()
    private val libraryScanner: LibraryScanner by inject()

    val fullScan =
        CronTask(
            "Full scan",
            config.fullScanCron,
            callback = {
                val libraries = transaction { LibraryEntity.all().toList() }
                libraries.forEach { libraryScanner.scanLibrary(it) }
            },
        )
    val scanLibrary = EventTask<LibraryEntity>("Scan library", callback = { libraryScanner.scanLibrary(it.data) })
}
