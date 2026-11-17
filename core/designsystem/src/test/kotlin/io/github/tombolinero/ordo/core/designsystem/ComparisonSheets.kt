package io.github.tombolinero.ordo.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.component.ContourDotFont
import io.github.tombolinero.ordo.core.designsystem.component.DotFont
import io.github.tombolinero.ordo.core.designsystem.component.DotFonts
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.DotPillButton
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.component.LocalDotFont
import io.github.tombolinero.ordo.core.designsystem.component.LocalDotFontShowsGrid
import io.github.tombolinero.ordo.core.designsystem.component.LocalSurfaceGlow
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.PillowStyle
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.component.SurfaceGlow
import io.github.tombolinero.ordo.core.designsystem.component.contentColor
import io.github.tombolinero.ordo.core.designsystem.component.secondaryContentColor
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Side-by-side variants for design decisions. Images land in src/test/screenshots/compare. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class ComparisonSheets {

    @get:Rule
    val compose = createComposeRule()

    private fun sheet(name: String, height: Dp, content: @Composable () -> Unit) {
        compose.setContent {
            OrdoTheme {
                Box(Modifier.size(393.dp, height).testTag("sheet")) {
                    OrdoBackground { content() }
                }
            }
        }
        compose.onNodeWithTag("sheet").captureRoboImage("src/test/screenshots/compare/$name.png")
    }

    // ---------- F1: dot-matrix numerals ----------

    private fun digits(name: String, font: DotFont, grid: Boolean) = sheet("digits_$name", 420.dp) {
        CompositionLocalProvider(LocalDotFont provides font, LocalDotFontShowsGrid provides grid) {
            Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DotMatrixText("47%", 16.dp, color = OrdoTheme.colors.textTertiary)
                    DotMatrixText("48%", 72.dp)
                    DotMatrixText("49%", 16.dp, color = OrdoTheme.colors.textTertiary)
                }
                DotMatrixText("0123456789", 34.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixText("01", 26.dp, color = OrdoTheme.colors.accent)
                    DotMatrixText("02", 26.dp)
                    DotMatrixText("03", 18.dp)
                    DotMatrixText("28", 14.dp, color = OrdoTheme.colors.textSecondary)
                }
                PillowSurface(Modifier.fillMaxWidth().height(80.dp), shape = OrdoShapes.CardSmall) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DotMatrixText("08", 28.dp)
                        Text("Mercedes-Benz CLA", style = OrdoTheme.type.itemTitle, color = Color.White)
                    }
                }
            }
        }
    }

    @Test fun digitsA() = digits("A_aktuell_kontur", ContourDotFont, false)
    @Test fun digitsB() = digits("B_raster_5x7", DotFonts.Grid5x7, false)
    @Test fun digitsC() = digits("C_raster_7x9", DotFonts.Grid7x9, false)
    @Test fun digitsD() = digits("D_raster_7x9_mit_led", DotFonts.Grid7x9, true)

    // ---------- F2: glow ----------

    private fun glow(name: String, glow: SurfaceGlow) = sheet("glow_$name", 470.dp) {
        CompositionLocalProvider(LocalSurfaceGlow provides glow, LocalDotFont provides DotFonts.Grid7x9) {
            Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PillowSurface(Modifier.size(160.dp, 200.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Beste Autos", style = OrdoTheme.type.cardTitle, color = Color.White)
                            Spacer(Modifier.weight(1f))
                            DotMatrixText("08", 34.dp)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PillowSurface(Modifier.size(160.dp, 92.dp), style = PillowStyle.Magenta, shape = OrdoShapes.CardSmall) {
                            Text("Babynamen", Modifier.padding(16.dp), style = OrdoTheme.type.itemTitle, color = Color.White)
                        }
                        PillowSurface(Modifier.size(160.dp, 92.dp), style = PillowStyle.Violet, shape = OrdoShapes.CardSmall) {
                            Text("Filme", Modifier.padding(16.dp), style = OrdoTheme.type.itemTitle, color = Color.White)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(OrdoIcons.Back, null, {})
                    GlassPillButton("Abbrechen", {})
                    GlassIconButton(OrdoIcons.More, null, {})
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(OrdoIcons.Lists, null, {}, size = 56.dp)
                    DotPillButton({}, icon = OrdoIcons.Plus)
                    GlassIconButton(OrdoIcons.Settings, null, {}, size = 56.dp)
                }
            }
        }
    }

    @Test fun glowA() = glow("A_aktuell", SurfaceGlow.None)
    @Test fun glowB() = glow("B_hauch", SurfaceGlow(radius = 26.dp, alpha = 0.05f))
    @Test fun glowC() = glow("C_leicht", SurfaceGlow(radius = 34.dp, alpha = 0.09f))
    @Test fun glowD() = glow("D_farbig", SurfaceGlow(radius = 34.dp, alpha = 0.30f, tinted = true))
    @Test fun glowE() = glow("E_lichtkante", SurfaceGlow(radius = 24.dp, alpha = 0.05f, rim = true))

    // ---------- F3: list colours ----------

    @Test fun listColours() = sheet("list_colours", 720.dp) {
        CompositionLocalProvider(LocalDotFont provides DotFonts.Grid7x9, LocalSurfaceGlow provides SurfaceGlow(radius = 26.dp, alpha = 0.05f)) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(
                    Triple(PillowStyle.Graphite, "Graphit (bleibt)", "12"),
                    Triple(PillowStyle.Magenta, "Magenta (bleibt)", "08"),
                    Triple(PillowStyle.Violet, "Violett (bleibt)", "28"),
                    Triple(PillowStyle.Ruby, "Kandidat 1 · Rubin", "41"),
                    Triple(PillowStyle.Petrol, "Kandidat 2 · Petrol", "09"),
                    Triple(PillowStyle.Cobalt, "Kandidat 3 · Kobalt", "17"),
                    Triple(PillowStyle.Pearl, "Kandidat 4 · Perle", "05"),
                ).forEach { (style, title, n) ->
                    PillowSurface(Modifier.fillMaxWidth().height(80.dp), style = style, shape = OrdoShapes.CardSmall) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(title, style = OrdoTheme.type.cardTitle, color = style.contentColor())
                                Text("$n Einträge", style = OrdoTheme.type.label, color = style.secondaryContentColor())
                            }
                            DotMatrixText(n, 26.dp, color = style.contentColor())
                        }
                    }
                }
            }
        }
    }

    // ---------- F4: settings icon ----------

    private val oldSun: ImageVector = ImageVector.Builder("old", 24.dp, 24.dp, 24f, 24f).apply {
        listOf(
            "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6Z",
            "M12 2.8v2.4M12 18.8v2.4M2.8 12h2.4M18.8 12h2.4M5.5 5.5l1.7 1.7M16.8 16.8l1.7 1.7M5.5 18.5l1.7-1.7M16.8 7.2l1.7-1.7",
        ).forEach {
            addPath(PathParser().parsePathString(it).toNodes(), stroke = SolidColor(Color.White), strokeLineWidth = 1.75f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }.build()

    @Test fun settingsIcon() = sheet("settings_icon", 300.dp) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            listOf("A · bisher" to oldSun, "B · Zahnrad" to OrdoIcons.Settings, "C · Regler" to OrdoIcons.Tune).forEach { (label, icon) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(label, Modifier.width(92.dp), style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary)
                    GlassIconButton(OrdoIcons.Lists, null, {}, size = 56.dp)
                    DotPillButton({}, icon = OrdoIcons.Plus, width = 88.dp, height = 52.dp)
                    GlassIconButton(icon, null, {}, size = 56.dp)
                }
            }
        }
    }
}
