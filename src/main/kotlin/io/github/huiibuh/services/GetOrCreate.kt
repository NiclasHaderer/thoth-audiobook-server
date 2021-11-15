package io.github.huiibuh.services

import io.github.huiibuh.db.findOne
import io.github.huiibuh.db.tables.*
import io.github.huiibuh.db.tables.Collection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction

object GetOrCreate {
    fun artist(name: String) = transaction {
        val artist = Artist.findOne { Artists.name like name }
        artist ?: Artist.new {
            this.name = name
        }
    }

    fun album(
        name: String,
        artist: Artist,
        composer: Artist?,
        collection: Collection?,
        collectionIndex: Int?,
        cover: ByteArray?,
    ) = transaction {
        val album = Album.findOne { Albums.title like name and (Albums.artist eq artist.id.value) }
        album
            ?: Album.new {
                this.title = name
                this.artist = artist
                this.composer = composer
                this.collection = collection
                this.collectionIndex = collectionIndex
                this.cover = if (cover != null) image(cover) else null
            }
    }

    fun collection(name: String, artist: Artist) = transaction {
        val collection = Collection.findOne { Collections.title like name }
        collection ?: Collection.new {
            this.title = name
            this.artist = artist
        }
    }

    fun image(imageBytes: ByteArray) = transaction {
        val imageBlob = ExposedBlob(imageBytes)

        val img = Image.findOne { Images.image eq imageBlob }
        img ?: Image.new { image = imageBlob }
    }
}
