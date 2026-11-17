package io.github.tombolinero.ordo.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import io.github.tombolinero.ordo.core.designsystem.component.DotFonts

/** Dot-matrix numbers as bitmaps: widgets cannot draw custom shapes, but they can show images. */
internal fun dotNumberBitmap(text: String, heightPx: Int, color: Int): Bitmap {
    val font = DotFonts.Grid5x7
    val pitch = heightPx / font.rows.toFloat()
    val cells = text.length * font.columns + (text.length - 1) * font.glyphGap
    val bmp = Bitmap.createBitmap((cells * pitch).toInt().coerceAtLeast(1), heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val r = pitch * font.dotDiameter / 2f
    text.forEachIndexed { i, c ->
        val x0 = i * (font.columns + font.glyphGap) * pitch
        font.dots(c).forEach { d -> canvas.drawCircle(x0 + d.x * pitch, d.y * pitch, r, paint) }
    }
    return bmp
}
