package io.thoth.server.scheduler

import io.thoth.common.extensions.get
import io.thoth.common.scheduling.SchedulingCollection
import io.thoth.config.ThothConfig
import io.thoth.server.file.scanner.fullScan

class ThothSchedules : SchedulingCollection {
    // TODO get schedule from config
    private val config = get<ThothConfig>()

    val completeScan = schedule("Complete Scan", "0 0 * * 1", callback = ::fullScan)
    val getMetadata = schedule("Get Metadata", "0 0 * * 1") { /*TODO*/}
}
