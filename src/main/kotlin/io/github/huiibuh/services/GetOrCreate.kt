package io.github.huiibuh.services

import io.github.huiibuh.db.findOne
import io.github.huiibuh.db.models.*
import io.github.huiibuh.db.models.Collection
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction

object GetOrCreate {
    fun artist(name: String): Artist {
        return transaction {
            val artist = Artist.findOne { Artists.name like name }
            artist ?: Artist.new {
                this.name = name
            }
        }
    }

    fun album(
        name: String,
        artist: Artist,
        composer: Artist?,
        collection: Collection?,
        collectionIndex: Int?,
        cover: ByteArray?,
    ): Album {
        return transaction {
            val album = Album.findOne { Albums.name like name and (Albums.artist eq artist.id.value) }
            album
                ?: Album.new {
                    this.name = name
                    this.artist = artist
                    this.composer = composer
                    this.collection = collection
                    this.collectionIndex = collectionIndex
                    this.cover = if (cover != null) ExposedBlob(cover) else null

                }
        }
    }

    fun collection(name: String, artist: Artist): Collection {
        return transaction {
            val collection = Collection.findOne { Collections.name like name }
            collection ?: Collection.new {
                this.name = name
                this.artist = artist
            }
        }
    }
}
