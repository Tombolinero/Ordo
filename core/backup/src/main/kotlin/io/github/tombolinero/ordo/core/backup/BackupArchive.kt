package io.github.tombolinero.ordo.core.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Reads and writes `.ordobackup` files: a ZIP with
 *
 * ```
 * manifest.json     format id + version, app version, counts, SHA-256 of every file
 * data.json         lists, entries, settings
 * images/<name>     image files referenced by entries
 * ```
 */
object BackupArchive {
    const val FILE_EXTENSION = "ordobackup"
    const val MANIFEST = "manifest.json"
    const val DATA = "data.json"
    const val IMAGE_DIR = "images/"

    // Safety limits against damaged or malicious files.
    private const val MAX_DATA_BYTES = 64L * 1024 * 1024
    private const val MAX_IMAGE_BYTES = 40L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 4L * 1024 * 1024 * 1024
    private const val MAX_ENTRIES = 100_000
    private val SAFE_IMAGE_NAME = Regex("^[A-Za-z0-9._-]{1,128}$")

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
    }

    /** Fixed entry time so identical content gives identical files (reproducible tests). */
    private const val ENTRY_TIME = 315_532_800_000L // 1980-01-01, the ZIP epoch

    fun isSafeImageName(name: String) = SAFE_IMAGE_NAME.matches(name) && name != "." && name != ".."

    /**
     * Writes a backup to [out]. [images] maps image file name → opener of its bytes;
     * only images referenced by entries are written.
     */
    fun write(
        out: OutputStream,
        data: BackupData,
        images: Map<String, () -> InputStream>,
        appVersionName: String,
        appVersionCode: Int,
        createdAt: Instant = Instant.now(),
    ) {
        val referenced = data.lists.flatMap { l -> l.items.mapNotNull { it.image } }.toSortedSet()
        val checksums = sortedMapOf<String, String>()
        var imageCount = 0
        ZipOutputStream(out).use { zip ->
            val dataBytes = json.encodeToString(BackupData.serializer(), data).toByteArray()
            checksums[DATA] = sha256(dataBytes)
            zip.putEntry(DATA) { it.write(dataBytes) }
            for (name in referenced) {
                require(isSafeImageName(name)) { "Unsafe image name: $name" }
                val open = images[name] ?: continue
                val digest = MessageDigest.getInstance("SHA-256")
                zip.putEntry(IMAGE_DIR + name) { entryOut ->
                    open().use { input ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            digest.update(buf, 0, n)
                            entryOut.write(buf, 0, n)
                        }
                    }
                }
                checksums[IMAGE_DIR + name] = digest.digest().toHex()
                imageCount++
            }
            val manifest = BackupManifest(
                formatVersion = BackupMigrations.CURRENT_FORMAT_VERSION,
                appVersionName = appVersionName,
                appVersionCode = appVersionCode,
                createdAt = createdAt.toString(),
                counts = BackupManifest.Counts(
                    lists = data.lists.size,
                    items = data.lists.sumOf { it.items.size },
                    images = imageCount,
                ),
                files = checksums,
            )
            zip.putEntry(MANIFEST) { it.write(json.encodeToString(BackupManifest.serializer(), manifest).toByteArray()) }
        }
    }

    /**
     * Reads a backup. Every image is handed to [imageSink] as it is read (stream it to a
     * temporary location); only call it trusted once this function returns without throwing,
     * because checksums are verified at the end.
     */
    fun read(
        input: InputStream,
        currentAppVersionCode: Int,
        imageSink: (name: String, data: InputStream) -> Unit,
    ): BackupContent {
        var manifestBytes: ByteArray? = null
        var dataBytes: ByteArray? = null
        val actual = HashMap<String, String>()
        var total = 0L
        var entries = 0
        try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++entries > MAX_ENTRIES) throw BackupException.TooLarge("too many files")
                    if (entry.isDirectory) continue
                    val name = entry.name
                    when {
                        name == MANIFEST -> manifestBytes = zip.readLimited(MAX_DATA_BYTES).also { total += it.size }
                        name == DATA -> dataBytes = zip.readLimited(MAX_DATA_BYTES).also {
                            actual[DATA] = sha256(it)
                            total += it.size
                        }
                        name.startsWith(IMAGE_DIR) -> {
                            val file = name.removePrefix(IMAGE_DIR)
                            if (!isSafeImageName(file)) throw BackupException.Corrupted("unsafe file name $name")
                            val counting = DigestingLimitedStream(zip, MAX_IMAGE_BYTES)
                            imageSink(file, counting)
                            counting.drain()
                            actual[name] = counting.hex()
                            total += counting.count
                        }
                        else -> Unit // unknown files from newer versions are ignored
                    }
                    if (total > MAX_TOTAL_BYTES) throw BackupException.TooLarge("over ${MAX_TOTAL_BYTES / (1024 * 1024)} MB")
                }
            }
        } catch (e: ZipException) {
            throw BackupException.NotABackup(e)
        } catch (e: java.io.EOFException) {
            throw BackupException.Corrupted("file is incomplete", e)
        } catch (e: java.io.IOException) {
            throw BackupException.Corrupted("file could not be read", e)
        }

        val mBytes = manifestBytes ?: throw BackupException.NotABackup()
        val manifest = try {
            json.decodeFromString(BackupManifest.serializer(), mBytes.decodeToString())
        } catch (e: SerializationException) {
            throw BackupException.NotABackup(e)
        } catch (e: IllegalArgumentException) {
            throw BackupException.NotABackup(e)
        }
        if (manifest.format != BackupManifest.FORMAT_ID) throw BackupException.NotABackup()
        if (manifest.formatVersion > BackupMigrations.CURRENT_FORMAT_VERSION) throw BackupException.NewerFormat(manifest.formatVersion)
        if (manifest.formatVersion < 1) throw BackupException.Corrupted("invalid format version")

        val dBytes = dataBytes ?: throw BackupException.Corrupted("data.json missing")
        for ((path, expected) in manifest.files) {
            val got = actual[path] ?: throw BackupException.Corrupted("$path missing")
            if (!got.equals(expected, ignoreCase = true)) throw BackupException.Corrupted("$path checksum mismatch")
        }

        val data = try {
            val raw = json.parseToJsonElement(dBytes.decodeToString()).jsonObject
            val upgraded: JsonObject = BackupMigrations.upgrade(raw, manifest.formatVersion)
            json.decodeFromJsonElement<BackupData>(upgraded)
        } catch (e: SerializationException) {
            throw BackupException.Corrupted("data.json unreadable", e)
        } catch (e: IllegalArgumentException) {
            throw BackupException.Corrupted("data.json unreadable", e)
        }
        return BackupContent(manifest, data, fromNewerApp = manifest.appVersionCode > currentAppVersionCode)
    }

    private inline fun ZipOutputStream.putEntry(name: String, body: (OutputStream) -> Unit) {
        val entry = ZipEntry(name).apply { time = ENTRY_TIME }
        putNextEntry(entry)
        body(this)
        closeEntry()
    }

    private fun InputStream.readLimited(limit: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = read(buf)
            if (n < 0) break
            total += n
            if (total > limit) throw BackupException.TooLarge("file over ${limit / (1024 * 1024)} MB")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    /** Hashes and counts what the sink reads, enforces a size limit, never closes the ZIP. */
    private class DigestingLimitedStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
        private val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
            private set

        override fun read(): Int {
            val b = super.read()
            if (b >= 0) track(byteArrayOf(b.toByte()), 0, 1)
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) track(b, off, n)
            return n
        }

        private fun track(b: ByteArray, off: Int, n: Int) {
            digest.update(b, off, n)
            count += n
            if (count > limit) throw BackupException.TooLarge("image over ${limit / (1024 * 1024)} MB")
        }

        fun drain() {
            val buf = ByteArray(16 * 1024)
            while (read(buf, 0, buf.size) >= 0) Unit
        }

        fun hex(): String = digest.digest().toHex()

        override fun close() = Unit
    }
}
