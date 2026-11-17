package io.github.tombolinero.ordo.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import io.github.tombolinero.ordo.core.model.ImageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Part of an image to keep, as fractions (0..1) of the upright image. */
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left in 0f..1f && right in 0f..1f && top in 0f..1f && bottom in 0f..1f && left < right && top < bottom)
    }

    companion object {
        val Full = CropRect(0f, 0f, 1f, 1f)
    }
}

/**
 * Stores entry images inside the app (`files/images/<id>.webp`), independent of the gallery:
 * deleting the original photo never breaks a list. Images are rotated upright, cropped,
 * scaled to at most [MAX_SIDE] px and saved as WebP.
 */
class ImageStore(private val dir: File) {
    constructor(context: Context) : this(File(context.filesDir, "images"))

    init {
        dir.mkdirs()
    }

    fun file(id: ImageId): File = File(dir, fileName(id))

    fun exists(id: ImageId): Boolean = file(id).isFile

    /** Every stored image, keyed by its file name (for backups). */
    fun allFiles(): Map<String, File> = dir.listFiles().orEmpty().filter { it.isFile && !it.name.endsWith(".tmp") }.associateBy { it.name }

    suspend fun import(context: Context, uri: Uri, crop: CropRect = CropRect.Full): ImageId = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val orientation = resolver.openInputStream(uri)?.use { readOrientation(it) } ?: ExifInterface.ORIENTATION_NORMAL
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not an image" }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: error("Not an image")
        save(process(decoded, orientation, crop))
    }

    /** Stores an already decoded bitmap (e.g. from tests or the share sheet). */
    suspend fun save(bitmap: Bitmap): ImageId = withContext(Dispatchers.IO) {
        val id = ImageId.new()
        val target = file(id)
        val tmp = File(dir, target.name + ".tmp")
        tmp.outputStream().use { out ->
            @Suppress("DEPRECATION")
            val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            bitmap.compress(format, QUALITY, out)
        }
        check(tmp.renameTo(target)) { "Could not store image" }
        id
    }

    /** Adds a file coming from a backup under its original name. Returns false for unsafe names. */
    fun adopt(name: String, source: File): Boolean {
        if (!SAFE_NAME.matches(name)) return false
        val target = File(dir, name)
        if (target.exists()) target.delete()
        return source.renameTo(target) || run { source.copyTo(target, overwrite = true); source.delete(); true }
    }

    fun delete(id: ImageId) {
        file(id).delete()
    }

    /** Deletes every image that is not in [keep] (unreferenced leftovers, leftover temp files). */
    fun deleteAllExcept(keep: Set<ImageId>) {
        val keepNames = keep.map(::fileName).toSet()
        dir.listFiles().orEmpty().filter { it.isFile && it.name !in keepNames }.forEach { it.delete() }
    }

    private fun readOrientation(input: InputStream): Int =
        runCatching { ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            .getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    companion object {
        const val MAX_SIDE = 2048
        private const val QUALITY = 88
        private val SAFE_NAME = Regex("^[A-Za-z0-9._-]{1,128}$")

        /** Image ids are stored without extension; files carry ".webp". */
        fun fileName(id: ImageId) = if (id.value.endsWith(".webp")) id.value else id.value + ".webp"

        internal fun sampleSize(w: Int, h: Int): Int {
            var s = 1
            while (max(w, h) / (s * 2) >= MAX_SIDE) s *= 2
            return s
        }

        /** Rotates per EXIF, crops, then scales so the longer side is at most [MAX_SIDE]. */
        internal fun process(source: Bitmap, orientation: Int, crop: CropRect): Bitmap {
            val m = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            }
            val upright = if (m.isIdentity) source else Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
            val x = (crop.left * upright.width).roundToInt().coerceIn(0, upright.width - 1)
            val y = (crop.top * upright.height).roundToInt().coerceIn(0, upright.height - 1)
            val w = ((crop.right - crop.left) * upright.width).roundToInt().coerceIn(1, upright.width - x)
            val h = ((crop.bottom - crop.top) * upright.height).roundToInt().coerceIn(1, upright.height - y)
            val cropped = if (x == 0 && y == 0 && w == upright.width && h == upright.height) upright else Bitmap.createBitmap(upright, x, y, w, h)
            val scale = MAX_SIDE.toFloat() / max(cropped.width, cropped.height)
            return if (scale >= 1f) {
                cropped
            } else {
                Bitmap.createScaledBitmap(cropped, (cropped.width * scale).roundToInt(), (cropped.height * scale).roundToInt(), true)
            }
        }
    }
}
