package io.thoth.server.schedules

import io.thoth.common.scheduling.ScheduleCollection
import io.thoth.config.ThothConfig
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
            "0 0 * * 1",
            callback = { libraryScanner.fullScan() },
        )
    val scanLibrary =
        event(
            "Scan folder",
            callback = { libraryScanner.scanLibrary(it.data) },
        )
    val getMetadata = schedule("Get Metadata", "0 0 * * 1") { /*TODO*/}
}
