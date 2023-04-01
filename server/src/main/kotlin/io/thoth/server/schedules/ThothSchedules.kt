package io.thoth.server.schedules

import io.thoth.server.common.scheduling.ScheduleCollection
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.tables.Library
import io.thoth.server.file.scanner.LibraryScanner
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ThothSchedules : ScheduleCollection, KoinComponent {
    // TODO get schedule from config
    private val config by inject<ThothConfig>()
    private val libraryScanner: LibraryScanner by inject()

    val fullScan =
        schedule(
            "Full scan",
            config.fullScanCron,
            callback = { libraryScanner.fullScan() },
        )
    val scanLibrary =
        event<Library>(
            "Scan folder",
            callback = { libraryScanner.scanLibrary(it.data) },
        )
    val retrieveMetadata = schedule("Retrieve Metadata", config.metadataRefreshCron) { /*TODO*/}
}
