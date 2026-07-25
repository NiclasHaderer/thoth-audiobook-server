package io.thoth.taglib

import io.thoth.taglib.ffi.TagLibC
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.io.path.absolutePathString

internal object NativeLoader {
    private val loaded by lazy { load() }

    /** Idempotent; the first caller performs the extraction and System.load. */
    fun ensureLoaded() = loaded

    private fun load() {
        val target = "$osName-$archName"
        val libraryName = System.mapLibraryName("tag_c")
        val resource = "/native/$target/$libraryName"
        val bytes =
            NativeLoader::class.java.getResourceAsStream(resource)?.use { it.readBytes() }
                ?: throw UnsatisfiedLinkError(
                    "No bundled TagLib native for $target (looked for $resource). " +
                        "Run ./gradlew :taglib:buildNativeHost to build it for this host.",
                )

        System.load(extract(libraryName, bytes).absolutePathString())

        // UTF-8 in and out is already tag_c's default, but the flag is process-global and
        // unsynchronized, so it is set once here rather than from every TagLibFile.
        TagLibC.taglib_set_strings_unicode(1)
    }

    /**
     * System.load needs a real file, so the packaged library is unpacked into a cache keyed by its
     * content hash and then reused by later JVMs. It is deliberately never deleted: the JVM cannot
     * unload a library, and Windows refuses to delete a mapped DLL, so extracting per run would
     * leave a copy behind every time.
     */
    private fun extract(
        libraryName: String,
        bytes: ByteArray,
    ): Path {
        val digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).take(32)
        val directory = cacheDirectory(digest)
        val file = directory.resolve(libraryName)
        if (matches(file, bytes)) return file

        val partial = Files.createTempFile(directory, libraryName, ".partial")
        try {
            Files.write(partial, bytes)
            // Losing this race is the normal case under concurrency: another JVM published first.
            val published = runCatching { Files.move(partial, file, StandardCopyOption.ATOMIC_MOVE) }.isSuccess
            if (!published && !matches(file, bytes)) {
                // A truncated leftover from a crashed run, which would otherwise wedge every future load.
                Files.move(partial, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(partial)
        }

        if (!matches(file, bytes)) {
            throw UnsatisfiedLinkError("Extracted TagLib native at $file does not match the bundled library")
        }
        return file
    }

    /**
     * The hash only names the entry; the bytes are compared in full before loading, so a stale or
     * substituted file in a shared cache directory is re-extracted rather than loaded.
     */
    private fun matches(
        file: Path,
        bytes: ByteArray,
    ): Boolean =
        runCatching {
            Files.size(file) == bytes.size.toLong() && Files.readAllBytes(file).contentEquals(bytes)
        }.getOrDefault(false)

    /** Prefers the per-user cache over the shared temp directory, which may not be writable. */
    private fun cacheDirectory(digest: String): Path {
        val roots =
            listOfNotNull(
                runCatching { userCacheRoot() }.getOrNull(),
                runCatching { Path.of(System.getProperty("java.io.tmpdir"), "thoth-taglib") }.getOrNull(),
            )
        for (root in roots) {
            val directory = root.resolve(digest)
            if (runCatching { Files.createDirectories(directory) }.isSuccess) return directory
        }
        throw UnsatisfiedLinkError("Could not create a cache directory for the TagLib native in any of $roots")
    }

    private fun userCacheRoot(): Path {
        val home = System.getProperty("user.home") ?: throw IOException("user.home is not set")
        val base =
            when (osName) {
                "macos" -> Path.of(home, "Library", "Caches")
                "windows" -> System.getenv("LOCALAPPDATA")?.let(Path::of) ?: Path.of(home, "AppData", "Local")
                else ->
                    System.getenv("XDG_CACHE_HOME")?.takeIf { it.isNotBlank() }?.let(Path::of)
                        ?: Path.of(home, ".cache")
            }
        return base.resolve("thoth-taglib")
    }

    private val osName: String
        get() {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.startsWith("mac") || os.startsWith("darwin") -> "macos"
                os.startsWith("windows") -> "windows"
                os.startsWith("linux") -> "linux"
                else -> throw UnsatisfiedLinkError("Unsupported operating system: $os")
            }
        }

    private val archName: String
        get() =
            when (val arch = System.getProperty("os.arch").lowercase()) {
                "aarch64", "arm64" -> "arm64"
                "x86_64", "amd64" -> "x64"
                else -> throw UnsatisfiedLinkError("Unsupported architecture: $arch")
            }
}
