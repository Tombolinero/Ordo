package io.github.tombolinero.ordo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.tombolinero.ordo.core.designsystem.R

val Urbanist = FontFamily(
    Font(R.font.urbanist_light, FontWeight.Light),
    Font(R.font.urbanist_regular, FontWeight.Normal),
    Font(R.font.urbanist_medium, FontWeight.Medium),
    Font(R.font.urbanist_semibold, FontWeight.SemiBold),
    Font(R.font.urbanist_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Urbanist,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

/** Type scale of the reference design. Numbers are rendered with DotMatrixText instead. */
@Immutable
data class OrdoTypography(
    val display: TextStyle = style(30, FontWeight.Medium, 36, -0.01),
    val screenTitle: TextStyle = style(22, FontWeight.Medium, 28, -0.005),
    val sectionTitle: TextStyle = style(19, FontWeight.Medium, 24),
    val cardTitle: TextStyle = style(18, FontWeight.Medium, 23),
    val itemTitle: TextStyle = style(16, FontWeight.Medium, 21),
    val body: TextStyle = style(16, FontWeight.Normal, 24),
    val bodySmall: TextStyle = style(14, FontWeight.Normal, 20),
    val label: TextStyle = style(13, FontWeight.Normal, 17),
    val meta: TextStyle = style(12, FontWeight.Light, 16),
    val overline: TextStyle = style(11, FontWeight.Medium, 14, 0.12),
    val button: TextStyle = style(15, FontWeight.SemiBold, 20),
)
