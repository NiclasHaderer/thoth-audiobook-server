package io.github.huiibuh.db.tables

import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.db.ToModel
import io.github.huiibuh.db.update.interceptor.TimeUpdatable
import io.github.huiibuh.extensions.findOne
import io.github.huiibuh.models.TitledId
import io.github.huiibuh.models.TrackModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*
import kotlin.io.path.absolutePathString


object TTracks : UUIDTable("Tracks") {
    val title = varchar("title", 255)
    val duration = integer("duration")
    val accessTime = long("accessTime")
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val path = text("path").uniqueIndex()
    val book = reference("book", TBooks)
    val scanIndex = long("scanIndex")
    val trackNr = integer("trackNr").nullable()
}


class Track(id: EntityID<UUID>) : UUIDEntity(id), ToModel<TrackModel>, TimeUpdatable {
    companion object : UUIDEntityClass<Track>(TTracks) {
        fun removeUntouched() = transaction {
            val kvSettings = KeyValueSettings.get()
            Track.find { TTracks.scanIndex eq kvSettings.scanIndex }.forEach {
                it.delete()
            }
        }

        @Throws(APINotFound::class)
        fun getById(uuid: UUID) = transaction {
            Track.findById(uuid)?.toModel() ?: throw APINotFound("Requested track was not found")
        }

        fun getByPath(path: Path) = getByPath(path.absolutePathString())
        fun getByPath(path: String) = transaction { Track.findOne { TTracks.path eq path } }

        fun forBook(bookID: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            rawForBook(bookID, order).map { it.toModel() }
        }

        fun rawForBook(bookID: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            Track.find { TTracks.book eq bookID }.orderBy(TTracks.trackNr to order).toList()
        }

        fun forAuthor(uuid: UUID) = transaction {
            Book.find { TBooks.author eq uuid }.flatMap {
                Track.find { TTracks.book eq it.id }.toList()
            }
        }

        fun forSeries(uuid: UUID) = transaction {
            Book.find { TBooks.series eq uuid }.flatMap {
                Track.find { TTracks.book eq it.id }.toList()
            }
        }


    }

    var title by TTracks.title
    var trackNr by TTracks.trackNr
    override var updateTime by TTracks.updateTime
    var duration by TTracks.duration
    var accessTime by TTracks.accessTime
    var path by TTracks.path
    var book by Book referencedOn TTracks.book

    var scanIndex by TTracks.scanIndex

    fun markAsTouched() {
        val kvSettings = KeyValueSettings.get()
        transaction { this@Track.scanIndex = kvSettings.scanIndex + 1 }
    }

    fun hasBeenUpdated(updateTime: Long) = this.accessTime >= updateTime

    override fun toModel() = TrackModel(
        id = id.value,
        title = title,
        path = path,
        trackNr = trackNr,
        updateTime = updateTime,
        duration = duration,
        accessTime = accessTime,
        book = TitledId(
            title = book.title,
            id = book.id.value
        ),
    )
}
