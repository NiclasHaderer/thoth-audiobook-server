//package io.github.huiibuh.scanner
//
//import org.jaudiotagger.audio.AudioFile
//import org.jaudiotagger.audio.AudioFileIO
//import org.jaudiotagger.tag.FieldKey
//import org.jaudiotagger.tag.images.ArtworkFactory
//import org.slf4j.LoggerFactory
//import java.io.File
//
//data class CoverInformation(
//    val cover: ByteArray,
//    val mimetype: String,
//) {
//    val extension: String
//        get() = mimetype.split("/").last()
//}
//
//

//
//class Album {
//    private val trackMap = mutableMapOf<String, Track>()
//    private val tracks get() = trackMap.values
//
//    companion object {
//        fun fromTrack(track: AudioFile): Album {
//            val album = Album()
//            album.upsertTrack(track)
//            return album
//        }
//    }
//
//    fun upsertTrack(track: AudioFile) {
//        trackMap[track.file.absolutePath] = Track(track)
//    }
//
//    var composer: String?
//        get() = tracks.firstOrNull()?.composer
//        set(value) = tracks.forEach { it.composer = value; it.save() }
//    var artist: String?
//        get() = tracks.firstOrNull()?.artist
//        set(value) = tracks.forEach { it.artist = value; it.save() }
//    var name: String?
//        get() = tracks.firstOrNull()?.album
//        set(value) = tracks.forEach { it.album = value; it.save() }
//    var collectionIndex: Number?
//        get() = tracks.firstOrNull()?.collectionIndex
//        set(value) = tracks.forEach { it.collectionIndex = value; it.save() }
//    var collection: String?
//        get() = tracks.firstOrNull()?.collection
//        set(value) = tracks.forEach { it.collection = value; it.save() }
//    var cover: ByteArray?
//        get() = tracks.firstOrNull()?.cover
//        set(value) = tracks.forEach { it.cover = value; it.save() }
//
//    val coverInformation: CoverInformation?
//        get() = tracks.firstOrNull()?.coverInformation
//}
//
//class Artist(
//    val name: String,
//) {
//    private val albumMap = mutableMapOf<String, Album>()
//
//    companion object {
//        fun fromAlbum(album: Album): Artist {
//            return Artist(album.artist!!)
//        }
//    }
//
//    fun getAlbum(title: String): Album? {
//        return albumMap[title]
//    }
//
//    fun createAlbum(album: Album) {
//        if (album.name in albumMap) throw Exception("Album is already assigned to artist")
//        albumMap[album.name!!] = album
//    }
//
//    fun upsertAlbum(album: Album) {
//        albumMap[album.name!!] = album
//    }
//}
//
//class Collection(val basePath: String? = null) {
//
//    private var _artistMap = mutableMapOf<String, Artist>()
//
//    companion object {
//        private val log = LoggerFactory.getLogger(this::class.java)
//
//        fun fromFileList(fileList: List<File>, path: String? = null): Collection {
//            val collection = Collection(path)
//            for (file in fileList) {
//                try {
//                    collection.upsertTrack(AudioFileIO.read(file))
//                } catch (e: Exception) {
//                    log.info("Could not add track ${file.absolutePath}:\n${e.localizedMessage}")
//                }
//            }
//            return collection
//        }
//
//        fun fromPath(path: String): Collection {
//            return fromFileList(getAllAudioFiles(path), path)
//        }
//    }
//
//    fun getArtist(name: String): Artist? {
//        return _artistMap[name]
//    }
//
//    fun createArtist(artist: Artist) {
//        if (artist.name in _artistMap) throw Exception("Artist is already in Collection")
//        _artistMap[artist.name] = artist
//    }
//
//    fun upsertArtist(artist: Artist) {
//        _artistMap[artist.name] = artist
//    }
//
//    fun createAlbum(album: Album) {
//        var artist = Artist.fromAlbum(album)
//        if (album.artist in _artistMap) artist = _artistMap[artist.name]!!
//        else _artistMap[artist.name] = artist
//        artist.createAlbum(album)
//    }
//
//    fun upsertTrack(track: AudioFile) {
//        var album = Album.fromTrack(track)
//        var artist = Artist.fromAlbum(album)
//        if (artist.name in _artistMap) artist = _artistMap[artist.name]!!
//        else _artistMap[artist.name] = artist
//
//        if (artist.getAlbum(album.name!!) != null) album = artist.getAlbum(album.name!!)!!
//        else artist.createAlbum(album)
//
//        album.upsertTrack(track)
//    }
//
//    fun reload() {
//        if (basePath == null) throw Exception("Cannot reload files if no base path was provided")
//        val collection = fromPath(basePath)
//        this._artistMap = collection._artistMap
//    }
//}
