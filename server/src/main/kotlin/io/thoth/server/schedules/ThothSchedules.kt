package io.thoth.server.schedules

import io.thoth.server.common.scheduling.CronTask
import io.thoth.server.common.scheduling.EventTask
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.file.scanner.LibraryImportPipeline
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class ThothSchedules : KoinComponent {
    private val config by inject<ThothConfig>()
    private val pipeline: LibraryImportPipeline by inject()

    val fullScan =
        CronTask(
            "Full scan",
            config.fullScanCron,
            callback = {
                val libraries = transaction { LibraryEntity.all().map { it.id.value } }
                libraries.forEach { pipeline.scanLibrary(it) }
            },
        )
    val scanLibrary = EventTask<UUID>("Scan library", callback = { pipeline.scanLibrary(it.data) })
}
