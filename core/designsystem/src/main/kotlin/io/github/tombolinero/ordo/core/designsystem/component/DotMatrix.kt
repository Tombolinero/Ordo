package io.github.tombolinero.ordo.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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
 * A dot-matrix typeface: every glyph is a set of dot centres in a box of [columns] × [rows]
 * cells. Text is laid out with one empty cell between glyphs.
 */
@Immutable
abstract class DotFont(val columns: Int, val rows: Int) {
    /** Dot centres of [c] in cell units (0..columns, 0..rows), or empty when unsupported. */
    abstract fun dots(c: Char): List<Offset>

    /** Dot diameter relative to the cell pitch. */
    open val dotDiameter: Float = 0.62f

    /** Gap between glyphs in cells. */
    open val glyphGap: Float = 1f

    /** All cell centres of the box (for the faint "unlit LED" background). */
    fun grid(): List<Offset> = buildList {
        for (r in 0 until rows) for (c in 0 until columns) add(Offset(c + 0.5f, r + 0.5f))
    }
}

/**
 * Pixel-grid font: only the dots that form the glyph light up, so the spacing between
 * dots is identical everywhere (no clumping at joints such as the waist of an 8).
 */
class GridDotFont(columns: Int, rows: Int, glyphs: Map<Char, List<String>>) : DotFont(columns, rows) {
    private val lit: Map<Char, List<Offset>> = glyphs.mapValues { (c, lines) ->
        require(lines.size == rows) { "Glyph '$c' needs $rows rows" }
        lines.flatMapIndexed { r, line ->
            require(line.length == columns) { "Glyph '$c' row $r needs $columns columns" }
            line.mapIndexedNotNull { col, ch -> if (ch == '#') Offset(col + 0.5f, r + 0.5f) else null }
        }
    }

    override fun dots(c: Char): List<Offset> = lit[c] ?: emptyList()
}

/** First version: dots sampled along the outline of a line drawing. Kept only for comparison. */
object ContourDotFont : DotFont(columns = 5, rows = 8) {
    private const val Spacing = 2.05f
    private val paths: Map<Char, List<String>> = mapOf(
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
        '%' to listOf("M8.6 1.4 1.4 14.6"),
    )
    private val extra = mapOf(
        '%' to listOf(
            Offset(1.6f, 2.2f), Offset(3.4f, 2.2f), Offset(1.6f, 4f), Offset(3.4f, 4f),
            Offset(6.6f, 12f), Offset(8.4f, 12f), Offset(6.6f, 13.8f), Offset(8.4f, 13.8f),
        ),
    )
    private val cache = HashMap<Char, List<Offset>>()
    override val dotDiameter = 0.66f
    override val glyphGap = 1.1f

    override fun dots(c: Char): List<Offset> = synchronized(cache) {
        cache.getOrPut(c) {
            val out = ArrayList<Offset>()
            paths[c]?.forEach { d ->
                val closed = 'Z' in d
                val m = android.graphics.PathMeasure(PathParser().parsePathString(d).toPath().asAndroidPath(), false)
                val pos = FloatArray(2)
                do {
                    val len = m.length
                    if (len <= 0f) continue
                    val n = max(1, (len / Spacing).roundToInt())
                    for (i in 0 until if (closed) n else n + 1) {
                        m.getPosTan(len * i / n, pos, null)
                        out += Offset(pos[0], pos[1])
                    }
                } while (m.nextContour())
            }
            extra[c]?.let { out += it }
            // Source drawings live in a 10 × 16 box; map to 5 × 8 cells.
            out.map { Offset(it.x / 2f, it.y / 2f) }
        }
    }
}

object DotFonts {
    /** Classic 5 × 7 LED matrix. */
    val Grid5x7 = GridDotFont(
        5, 7,
        mapOf(
            '0' to listOf(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
            '1' to listOf("..#..", ".##..", "#.#..", "..#..", "..#..", "..#..", "..#.."),
            '2' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
            '3' to listOf("#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###."),
            '4' to listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
            '5' to listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
            '6' to listOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
            '7' to listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
            '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
            '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
            '%' to listOf("##..#", "##.#.", "...#.", "..#..", ".#...", ".#.##", "#..##"),
            '+' to listOf(".....", "..#..", "..#..", "#####", "..#..", "..#..", "....."),
            '=' to listOf(".....", ".....", "#####", ".....", "#####", ".....", "....."),
            '-' to listOf(".....", ".....", ".....", "#####", ".....", ".....", "....."),
            '/' to listOf("....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#...."),
            ' ' to listOf(".....", ".....", ".....", ".....", ".....", ".....", "....."),
        ),
    )

    /** Finer 7 × 9 matrix: rounder curves, still clearly made of dots. */
    val Grid7x9 = GridDotFont(
        7, 9,
        mapOf(
            '0' to listOf("..###..", ".#...#.", "#.....#", "#.....#", "#.....#", "#.....#", "#.....#", ".#...#.", "..###.."),
            '1' to listOf("...#...", "..##...", ".#.#...", "...#...", "...#...", "...#...", "...#...", "...#...", "...#..."),
            '2' to listOf("..###..", ".#...#.", "#.....#", "......#", ".....#.", "....#..", "..##...", ".#.....", "#######"),
            '3' to listOf("#######", ".....#.", "....#..", "...##..", ".....#.", "......#", "#.....#", ".#...#.", "..###.."),
            '4' to listOf("....#..", "...##..", "..#.#..", ".#..#..", "#...#..", "#######", "....#..", "....#..", "....#.."),
            '5' to listOf("#######", "#......", "#......", "#####..", ".....#.", "......#", "#.....#", ".#...#.", "..###.."),
            '6' to listOf("...##..", "..#....", ".#.....", "#......", "#.###..", "##...#.", "#.....#", ".#...#.", "..###.."),
            '7' to listOf("#######", "......#", ".....#.", "....#..", "...#...", "..#....", "..#....", "..#....", "..#...."),
            '8' to listOf("..###..", ".#...#.", "#.....#", ".#...#.", "..###..", ".#...#.", "#.....#", ".#...#.", "..###.."),
            '9' to listOf("..###..", ".#...#.", "#.....#", ".#...##", "..###.#", "......#", ".....#.", "....#..", "..##..."),
            '%' to listOf("##....#", "##...#.", ".....#.", "....#..", "...#...", "..#....", ".#.....", ".#...##", "#....##"),
            '+' to listOf(".......", ".......", "...#...", "...#...", "#######", "...#...", "...#...", ".......", "......."),
            '=' to listOf(".......", ".......", ".......", "#######", ".......", "#######", ".......", ".......", "......."),
            '-' to listOf(".......", ".......", ".......", ".......", "#######", ".......", ".......", ".......", "......."),
            '/' to listOf("......#", ".....#.", ".....#.", "....#..", "...#...", "..#....", ".#.....", ".#.....", "#......"),
            ' ' to listOf(".......", ".......", ".......", ".......", ".......", ".......", ".......", ".......", "......."),
        ),
    )
}

/** The dot font used app-wide. Swappable for previews and comparisons. */
val LocalDotFont = staticCompositionLocalOf<DotFont> { DotFonts.Grid7x9 }

/** Whether unlit grid positions glow faintly, like the dark LEDs of a real matrix display. */
val LocalDotFontShowsGrid = staticCompositionLocalOf { false }

/**
 * Draws [text] in dot-matrix numerals. Supported: digits and `% + = - /`.
 *
 * @param height height of one glyph (cap height).
 * @param reveal 0..1, lets the dots light up one after another (entrance animation).
 */
@Composable
fun DotMatrixText(
    text: String,
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    reveal: Float = 1f,
    font: DotFont = LocalDotFont.current,
    showGrid: Boolean = LocalDotFontShowsGrid.current,
) {
    val pitchRatio = 1f / font.rows
    val cells = if (text.isEmpty()) 0f else text.length * font.columns + (text.length - 1) * font.glyphGap
    val width = height * (cells * pitchRatio)
    val glyphDots = remember(text, font) { text.map { font.dots(it) } }
    val grid = remember(font) { font.grid() }
    Canvas(
        modifier
            .size(width, height)
            .semantics { contentDescription = text },
    ) {
        val pitch = size.height / font.rows
        val r = pitch * font.dotDiameter / 2f
        val total = glyphDots.sumOf { it.size }.coerceAtLeast(1)
        val visible = (reveal.coerceIn(0f, 1f) * total).toInt()
        var drawn = 0
        glyphDots.forEachIndexed { index, dots ->
            val x0 = index * (font.columns + font.glyphGap) * pitch
            if (showGrid && text[index] != ' ') {
                for (g in grid) drawCircle(color, r, Offset(x0 + g.x * pitch, g.y * pitch), alpha = 0.07f)
            }
            for (dot in dots) {
                if (reveal >= 1f || drawn < visible) {
                    drawCircle(color, r, Offset(x0 + dot.x * pitch, dot.y * pitch))
                }
                drawn++
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
    val font = LocalDotFont.current
    val text = value.toString().padStart(minDigits, '0') + suffix
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(height * (font.glyphGap / font.rows)),
    ) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = { rollTransition(initialState, targetState) },
                label = "digit$index",
            ) { c ->
                DotMatrixText(c.toString(), height, color = color)
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
