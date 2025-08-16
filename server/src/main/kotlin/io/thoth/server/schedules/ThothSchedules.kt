package io.thoth.server.schedules

import io.thoth.server.common.scheduling.ScheduleCollection
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.tables.Library
import io.thoth.server.file.scanner.LibraryScanner
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class ThothSchedules :
    ScheduleCollection,
    KoinComponent {
    private val config by inject<ThothConfig>()
    private val libraryScanner: LibraryScanner by inject()

    val fullScan =
        schedule(
            "Full scan",
            config.fullScanCron,
            callback = { libraryScanner.fullScan(transaction { Library.all().map { it.id.value } }) },
        )
    val scanLibrary = event<Library>("Scan library", callback = { libraryScanner.scanLibrary(it.data) })

    val scanLibraries = event<List<UUID>>("Scan libraries", callback = { libraryScanner.fullScan(it.data) })
    val retrieveMetadata = schedule("Retrieve Metadata", config.metadataRefreshCron) { /*TODO*/ }
}
