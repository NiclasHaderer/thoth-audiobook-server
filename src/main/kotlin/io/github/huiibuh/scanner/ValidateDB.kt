package io.github.huiibuh.scanner

import io.github.huiibuh.db.models.*
import io.github.huiibuh.db.models.Collection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.getLastModifiedTime

fun validateDB() {
    validateTracksInDB()
    removeEmptyAlbums()
    removeEmptyArtists()
    removeEmptyCollections()
}

fun removeEmptyCollections() {
    Collection.all().forEach {
        if (Track.find { Tracks.collection eq it.id.value }.empty()) {
            it.delete()
        }
    }
}

fun removeEmptyArtists() {
    Artist.all().forEach {
        if (Track.find { Tracks.artist eq it.id.value }.empty()) {
            it.delete()
        }
    }
}

fun removeEmptyAlbums() {
    Album.all().forEach {
        if (Track.find { Tracks.album eq it.id.value }.empty()) {
            it.delete()
        }
    }
}

fun validateTracksInDB() {
    Track.all().forEach {
        if (!fileExists(it.path)) {
            it.delete()
            return@forEach
        }
        val file = Paths.get(it.path)
        val time = file.getLastModifiedTime().to(TimeUnit.MILLISECONDS)
        if (time != it.accessTime) {
            updateTrack(it)
        }
    }
}

fun updateTrack(track: Track) {
    val file = Paths.get(track.path)
    val trackReference = TrackReference.fromPath(track.path)
    track.apply {
        title = trackReference.title
        trackNr = trackReference.trackNr
        duration = trackReference.duration
        accessTime = file.getLastModifiedTime().to(TimeUnit.MILLISECONDS)
        artist = getOrCreateArtist(trackReference.artist)
        collection =
            if (trackReference.collection != null) getOrCreateCollection(trackReference.collection!!, artist) else null
        composer = if (trackReference.composer != null) getOrCreateArtist(trackReference.composer!!) else null
        album = getOrCreateAlbum(trackReference.album,
                                 artist,
                                 composer,
                                 collection,
                                 trackReference.collectionIndex,
                                 trackReference.cover)
        collectionIndex = trackReference.collectionIndex
    }
}

fun getOrCreateArtist(name: String): Artist {
    val artistList = Artist.find { Artists.name eq name }
    return if (artistList.empty()) {
        Artist.new {
            this.name = name
        }
    } else {
        artistList.first()
    }
}

fun getOrCreateAlbum(
    name: String,
    artist: Artist,
    composer: Artist?,
    collection: Collection?,
    collectionIndex: Int?,
    cover: ByteArray?,
): Album {
    val artistList = Album.find { Albums.name eq name and (Albums.artist eq artist.id.value) }
    return if (artistList.empty()) {
        Album.new {
            this.name = name
            this.artist = artist
            this.composer = composer
            this.collection = collection
            this.collectionIndex = collectionIndex
            this.cover = if (cover != null) ExposedBlob(cover) else null

        }
    } else {
        artistList.first()
    }
}

fun getOrCreateCollection(name: String, artist: Artist): Collection {
    val collection = Collection.find { Collections.name eq name }
    return if (collection.empty()) {
        Collection.new {
            this.name = name
            this.artist = artist
        }
    } else {
        collection.first()
    }
}
