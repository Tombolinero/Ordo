package io.github.tombolinero.ordo.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ordo's own line icon set (24 dp grid, 1.75 stroke, round caps) so every icon matches
 * the thin, rounded look of Urbanist. Tinted at the call site.
 */
object OrdoIcons {
    val Back = line("back", "M19 12H5M11 6l-6 6 6 6")
    val Plus = line("plus", "M12 5v14M5 12h14")
    val Lists = line("lists", "M3 7.5V18a1.5 1.5 0 0 0 1.5 1.5h15A1.5 1.5 0 0 0 21 18V9.5A1.5 1.5 0 0 0 19.5 8H12l-2-2.5H4.5A1.5 1.5 0 0 0 3 7Z")
    /** Gear with eight rounded teeth around a hub. */
    val Settings = line("settings", gearPath(), "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6Z")

    val Search = line("search", "M11 4.5a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13Z", "M20 20l-4-4")
    val Share = line("share", "M12 15V4M8 7.5 12 4l4 3.5", "M5 13v5.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V13")
    val Sort = line("sort", "M7 4v16M3.5 16.5 7 20l3.5-3.5", "M17 20V4M13.5 7.5 17 4l3.5 3.5")
    val Check = line("check", "M5 12.5l4.5 4.5L19 7.5")
    val Close = line("close", "M6 6l12 12M18 6 6 18")
    val Expand = line("expand", "M14 4h6v6M20 4l-7 7", "M10 20H4v-6M4 20l7-7")
    val Star = line("star", "M12 3.5l2.6 5.3 5.8.8-4.2 4.1 1 5.8L12 16.8l-5.2 2.7 1-5.8-4.2-4.1 5.8-.8Z")
    val Trash = line("trash", "M4 7h16M9.5 7V4.8h5V7", "M6.5 7l.9 12.2A1.5 1.5 0 0 0 8.9 20.6h6.2a1.5 1.5 0 0 0 1.5-1.4L17.5 7", "M10 11v6M14 11v6")
    val Image = line(
        "image",
        "M5 4h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1Z",
        "M4 16l4.5-4.5 4 4 2.5-2.5L20 18",
        "M15.5 7.3a1.2 1.2 0 1 0 0 2.4a1.2 1.2 0 1 0 0-2.4Z",
    )
    val Camera = line(
        "camera",
        "M4 8.5A1.5 1.5 0 0 1 5.5 7h2.3L9.5 4.5h5L16.2 7h2.3A1.5 1.5 0 0 1 20 8.5v9a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 17.5Z",
        "M12 9.5a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7Z",
    )
    val Undo = line("undo", "M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11")
    val Duel = line("duel", "M4 7h12M13 4l3 3-3 3", "M20 17H8M11 14l-3 3 3 3")
    val Edit = line("edit", "M4 20h4L19 9l-4-4L4 16Z", "M13.5 6.5l4 4")
    val Pin = line("pin", "M9 4h6M10 4v5.5L7 13h10l-3-3.5V4", "M12 13v7")
    val Copy = line("copy", "M9 9h10v11H9Z", "M5 15V4h10")
    val Restore = line("restore", "M4 12a8 8 0 1 0 2.4-5.7", "M4 4v4h4")
    val Merge = line("merge", "M7 4v4a5 5 0 0 0 5 5 5 5 0 0 0 5-5V4", "M12 13v7")
    val Split = line("split", "M12 4v7", "M12 11a5 5 0 0 0-5 5v4", "M12 11a5 5 0 0 1 5 5v4")
    val Inbox = line("inbox", "M4 13l2.5-7.5h11L20 13v5.5a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5Z", "M4 13h4.5l1 2h5l1-2H20")
    val Backup = line("backup", "M12 4v11M8 11.5 12 15l4-3.5", "M5 15v3.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V15")
    val Upload = line("upload", "M12 15V4M8 7.5 12 4l4 3.5", "M5 15v3.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V15")
    val Crop = line("crop", "M7 3v14h14", "M3 7h14v14")
    val ChevronRight = line("chevron_right", "M9.5 5.5 16 12l-6.5 6.5")
    val Note = line("note", "M6 4h9l4 4v12H6Z", "M14.5 4v4.5H19", "M9 13h7M9 16.5h5")
    val Widget = line("widget", "M4 4h7v7H4ZM13 4h7v7h-7ZM4 13h7v7H4ZM13 13h7v7h-7Z")
    val Language = line("language", "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0-17Z", "M3.5 12h17", "M12 3.5c2.4 2.4 3.5 5.2 3.5 8.5s-1.1 6.1-3.5 8.5c-2.4-2.4-3.5-5.2-3.5-8.5s1.1-6.1 3.5-8.5Z")

    val More = dots("more", listOf(12f to 5f, 12f to 12f, 12f to 19f), 1.8f)
    val Grip = dots("grip", listOf(9f to 6f, 15f to 6f, 9f to 12f, 15f to 12f, 9f to 18f, 15f to 18f), 1.55f)

    private fun gearPath(teeth: Int = 8, outer: Float = 9.6f, inner: Float = 7.3f): String {
        fun pt(deg: Double, r: Float): String {
            val a = Math.toRadians(deg - 90)
            return "%.3f %.3f".format(java.util.Locale.ROOT, 12 + r * kotlin.math.cos(a), 12 + r * kotlin.math.sin(a))
        }
        val step = 360.0 / teeth
        val tipHalf = step * 0.2
        val baseHalf = step * 0.31
        return buildString {
            for (i in 0 until teeth) {
                val c = i * step
                append(if (i == 0) "M" else "L").append(pt(c - baseHalf, inner))
                append(" L").append(pt(c - tipHalf, outer))
                append(" L").append(pt(c + tipHalf, outer))
                append(" L").append(pt(c + baseHalf, inner))
                append(" A$inner $inner 0 0 1 ").append(pt(c + step - baseHalf, inner)).append(' ')
            }
            append("Z")
        }
    }

    private fun line(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    fill = null,
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.75f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun dots(name: String, centers: List<Pair<Float, Float>>, r: Float): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            centers.forEach { (x, y) ->
                val d = "M${x - r} ${y}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"
                addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.White))
            }
        }.build()
}
