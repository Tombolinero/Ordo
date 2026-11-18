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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion

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

}

/** The dot font used app-wide. Swappable for previews and comparisons. */
val LocalDotFont = staticCompositionLocalOf<DotFont> { DotFonts.Grid5x7 }

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
) {
    val pitchRatio = 1f / font.rows
    val cells = if (text.isEmpty()) 0f else text.length * font.columns + (text.length - 1) * font.glyphGap
    val width = height * (cells * pitchRatio)
    val glyphDots = remember(text, font) { text.map { font.dots(it) } }
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
