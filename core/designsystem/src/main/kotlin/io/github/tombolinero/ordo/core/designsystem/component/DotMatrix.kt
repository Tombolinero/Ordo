package io.github.tombolinero.ordo.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The signature numerals of the design: every glyph is a single-line drawing, rendered as
 * evenly spaced round dots along its strokes (like a dot-matrix / LED display).
 *
 * Glyphs live in a 10 × 16 unit box. Dots are sampled once per glyph and cached.
 */
object DotMatrixFont {
    const val GlyphWidth = 10f
    const val GlyphHeight = 16f
    const val Advance = 12.2f
    private const val DotSpacing = 2.05f
    const val DotRadius = 0.66f

    private val glyphs: Map<Char, List<String>> = mapOf(
        '0' to listOf("M5 1C1.6 1 1 4 1 8s.6 7 4 7 4-3 4-7-.6-7-4-7Z"),
        '1' to listOf("M2.4 3.6 5.6 1v14"),
        '2' to listOf("M1.2 4.6C1.5 2.1 3 1 5 1c2.5 0 4 1.5 4 3.8 0 2.2-1.5 3.5-3.5 5L1 15h8"),
        '3' to listOf("M1.4 1h7.2L4.6 6.5C7.1 6.5 9 8.1 9 10.8 9 13.5 7 15 5 15c-2 0-3.3-1-4-2.5"),
        '4' to listOf("M7 15V1L1 11h8.5"),
        '5' to listOf("M8.6 1H2.1L1.6 7c1-.8 2.2-1.2 3.5-1.2C7.6 5.8 9 7.8 9 10.4 9 13 7.3 15 5 15c-2 0-3.3-1-4-2.5"),
        '6' to listOf("M8 2C7 1.3 6 1 5 1 2.5 1 1 3.5 1 8v2.5C1 13.5 2.7 15 5 15s4-1.7 4-4.4c0-2.6-1.6-4.2-4-4.2-2 0-3.5 1.2-4 2.9"),
        '7' to listOf("M1 1h8L4 15"),
        '8' to listOf("M5 7.6C2.8 7.6 1.6 6.3 1.6 4.3S3 1 5 1s3.4 1.3 3.4 3.3S7.2 7.6 5 7.6C2.6 7.6 1 9 1 11.2S2.7 15 5 15s4-1.5 4-3.8-1.6-3.6-4-3.6Z"),
        '9' to listOf("M2 14c1 .7 2 1 3 1 2.5 0 4-2.5 4-7V5.5C9 2.5 7.3 1 5 1 2.6 1 1 2.7 1 5.4 1 8 2.6 9.6 5 9.6c2 0 3.5-1.2 4-2.9"),
        '+' to listOf("M5 3.5v9", "M.6 8h8.8"),
        '=' to listOf("M1.4 6h7.2", "M1.4 10h7.2"),
        '-' to listOf("M1.6 8h6.8"),
        '/' to listOf("M8.6 1.4 1.4 14.6"),
        '%' to listOf("M8.6 1.4 1.4 14.6"),
    )

    /** Extra loose dots that are not part of a stroke (the two little clusters of '%', '.' and ':'). */
    private val looseDots: Map<Char, List<Offset>> = mapOf(
        '%' to listOf(
            Offset(1.6f, 2.2f), Offset(3.4f, 2.2f), Offset(1.6f, 4f), Offset(3.4f, 4f),
            Offset(6.6f, 12f), Offset(8.4f, 12f), Offset(6.6f, 13.8f), Offset(8.4f, 13.8f),
        ),
        '.' to listOf(Offset(5f, 14.6f)),
        ':' to listOf(Offset(5f, 5f), Offset(5f, 12f)),
    )

    private val cache = HashMap<Char, List<Offset>>()

    fun supports(c: Char) = c == ' ' || c in glyphs || c in looseDots

    /** Dot centres of [c] in glyph units. */
    fun dots(c: Char): List<Offset> = synchronized(cache) {
        cache.getOrPut(c) { sample(c) }
    }

    private fun sample(c: Char): List<Offset> {
        val result = ArrayList<Offset>()
        glyphs[c]?.forEach { d ->
            val closed = d.contains('Z') || d.contains('z')
            val path = PathParser().parsePathString(d).toPath().asAndroidPath()
            val measure = android.graphics.PathMeasure(path, false)
            val pos = FloatArray(2)
            do {
                val length = measure.length
                if (length <= 0f) continue
                val n = max(1, (length / DotSpacing).roundToInt())
                val count = if (closed) n else n + 1
                for (i in 0 until count) {
                    measure.getPosTan(length * i / n, pos, null)
                    result += Offset(pos[0], pos[1])
                }
            } while (measure.nextContour())
        }
        looseDots[c]?.let { result += it }
        return result
    }

    fun widthUnits(text: String): Float = if (text.isEmpty()) 0f else text.length * Advance - (Advance - GlyphWidth)
}

/**
 * Draws [text] in dot-matrix numerals. Supported: digits, `% + = - / . :` and space.
 *
 * @param height cap height of the glyphs.
 * @param reveal 0..1, lets the dots appear one after another (used for entrance animations).
 */
@Composable
fun DotMatrixText(
    text: String,
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    reveal: Float = 1f,
    dotScale: Float = 1f,
) {
    val units = DotMatrixFont.widthUnits(text)
    val width = height * ((units + 2 * DotMatrixFont.DotRadius) / DotMatrixFont.GlyphHeight)
    val glyphDots = remember(text) { text.map { DotMatrixFont.dots(it) } }
    Canvas(
        modifier
            .size(width, height)
            .semantics { contentDescription = text },
    ) {
        val scale = size.height / DotMatrixFont.GlyphHeight
        val r = DotMatrixFont.DotRadius * scale * dotScale
        val total = glyphDots.sumOf { it.size }.coerceAtLeast(1)
        var drawn = 0
        val visible = (reveal.coerceIn(0f, 1f) * total).toInt()
        glyphDots.forEachIndexed { index, dots ->
            val x0 = (DotMatrixFont.DotRadius + index * DotMatrixFont.Advance) * scale
            for (dot in dots) {
                val alpha = when {
                    reveal >= 1f -> 1f
                    drawn < visible -> 1f
                    else -> 0f
                }
                drawn++
                if (alpha > 0f) {
                    drawCircle(color, r, Offset(x0 + dot.x * scale, dot.y * scale), alpha = alpha)
                }
            }
        }
    }
}

/**
 * A number that rolls like an odometer when it changes: each digit slides up (value grew)
 * or down (value shrank) on its own spring. Used for ranks: "02" → "03".
 */
@Composable
fun DotMatrixNumber(
    value: Int,
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    minDigits: Int = 2,
    suffix: String = "",
) {
    val text = value.toString().padStart(minDigits, '0') + suffix
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = { rollTransition(initialState, targetState) },
                label = "digit$index",
            ) { c ->
                DotMatrixText(c.toString(), height, color = color)
            }
            if (index < text.lastIndex) {
                androidx.compose.foundation.layout.Spacer(
                    Modifier.size(height * ((DotMatrixFont.Advance - DotMatrixFont.GlyphWidth - 2 * DotMatrixFont.DotRadius) / DotMatrixFont.GlyphHeight)),
                )
            }
        }
    }
}

private fun rollTransition(from: Char, to: Char): ContentTransform {
    val up = (to.digitToIntOrNull() ?: 0) >= (from.digitToIntOrNull() ?: 0)
    val dir = if (up) 1 else -1
    return ContentTransform(
        targetContentEnter = slideInVertically(OrdoMotion.smooth(IntOffset.VisibilityThreshold)) { it * dir } +
            fadeIn(OrdoMotion.smooth()),
        initialContentExit = slideOutVertically(OrdoMotion.smooth(IntOffset.VisibilityThreshold)) { -it * dir } +
            fadeOut(OrdoMotion.smooth()),
        sizeTransform = SizeTransform(clip = true),
    )
}
