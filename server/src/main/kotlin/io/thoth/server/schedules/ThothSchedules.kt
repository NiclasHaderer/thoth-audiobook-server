package io.thoth.server.schedules

import io.thoth.server.common.scheduling.ScheduleCollection
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.file.scanner.LibraryScanner
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ThothSchedules :
    ScheduleCollection,
    KoinComponent {
    private val config by inject<ThothConfig>()
    private val libraryScanner: LibraryScanner by inject()

    val fullScan =
        schedule(
            "Full scan",
            config.fullScanCron,
            callback = {
                transaction { LibraryEntity.all() }.forEach { libraryScanner.scanLibrary(it) }
            },
        )
    val scanLibrary = event<LibraryEntity>("Scan library", callback = { libraryScanner.scanLibrary(it.data) })
    val retrieveMetadata = schedule("Retrieve Metadata", config.metadataRefreshCron) { /*TODO*/ }
}
