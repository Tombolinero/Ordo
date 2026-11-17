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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.DotPillButton
import io.github.tombolinero.ordo.core.designsystem.component.FloatingNavBar
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.OrdoToggle
import io.github.tombolinero.ordo.core.designsystem.component.OrdoTopBar
import io.github.tombolinero.ordo.core.designsystem.component.PillowStyle
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.component.ProgressRing
import io.github.tombolinero.ordo.core.designsystem.component.RankWheel
import io.github.tombolinero.ordo.core.designsystem.component.contentColor
import io.github.tombolinero.ordo.core.designsystem.component.secondaryContentColor
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class DesignSystemScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent { OrdoTheme { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun components() = shoot("components") {
        OrdoBackground {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Komponenten", style = OrdoTheme.type.screenTitle, color = Color.White)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    DotMatrixText("48%", 56.dp)
                    DotMatrixText("0123", 28.dp, color = OrdoTheme.colors.textTertiary)
                }
                DotMatrixText("456789+=", 22.dp, color = OrdoPalette.Yellow)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(OrdoIcons.Back, null, {})
                    GlassIconButton(OrdoIcons.Share, null, {})
                    GlassIconButton(OrdoIcons.Sort, null, {}, highlighted = true)
                    OrdoToggle(true, {})
                    OrdoToggle(false, {})
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(PillowStyle.Graphite, PillowStyle.Magenta, PillowStyle.Violet, PillowStyle.Gold).forEach { s ->
                        PillowSurface(Modifier.size(78.dp, 96.dp), style = s, shape = OrdoShapes.CardSmall) {
                            Text(s.name, Modifier.padding(12.dp), style = OrdoTheme.type.label, color = s.contentColor())
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassPillButton("Abbrechen", {})
                    GlassPillButton("Speichern", {}, accent = true, icon = OrdoIcons.Check)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(0.32f) { DotMatrixText("32%", 16.dp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            OrdoIcons.Plus, OrdoIcons.Lists, OrdoIcons.Settings, OrdoIcons.Search, OrdoIcons.Trash, OrdoIcons.Image,
                            OrdoIcons.Undo, OrdoIcons.Duel, OrdoIcons.Merge, OrdoIcons.Split,
                        ).forEach { androidx.compose.material3.Icon(it, null, Modifier.size(22.dp), tint = Color.White) }
                    }
                }
                Spacer(Modifier.weight(1f))
                FloatingNavBar(
                    Modifier.fillMaxWidth(),
                    leading = { GlassIconButton(OrdoIcons.Lists, null, {}, size = 56.dp) },
                    primary = { DotPillButton({}, icon = OrdoIcons.Plus) },
                    trailing = { GlassIconButton(OrdoIcons.Settings, null, {}, size = 56.dp) },
                )
            }
        }
    }

    @Test
    fun rankingMock() = shoot("ranking_mock") {
        OrdoBackground {
            Column(Modifier.fillMaxSize()) {
                OrdoTopBar(
                    "Beste Autos",
                    navigation = { GlassIconButton(OrdoIcons.Back, null, {}) },
                    action = { GlassIconButton(OrdoIcons.More, null, {}) },
                )
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillowSurface(Modifier.fillMaxWidth().height(116.dp), style = PillowStyle.Magenta) {
                        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            DotMatrixText("01", 34.dp)
                            FakeThumb(listOf(OrdoPalette.YellowLight, OrdoPalette.YellowDeep), 76)
                            Column {
                                Text("Koenigsegg Jesko", style = OrdoTheme.type.cardTitle, color = Color.White)
                                Text("Über 1.600 PS, Kunstwerk", style = OrdoTheme.type.label, color = PillowStyle.Magenta.secondaryContentColor())
                            }
                        }
                    }
                    listOf(
                        Triple("02", "Porsche Taycan", listOf(OrdoPalette.PurpleGlow, OrdoPalette.Purple)),
                        Triple("03", "Hyundai Ioniq 5", listOf(OrdoPalette.Violet, OrdoPalette.VioletMuted)),
                    ).forEach { (r, t, g) ->
                        PillowSurface(Modifier.fillMaxWidth().height(84.dp), shape = OrdoShapes.CardSmall) {
                            Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                DotMatrixText(r, 26.dp)
                                FakeThumb(g, 56)
                                Text(t, style = OrdoTheme.type.itemTitle, color = Color.White)
                            }
                        }
                    }
                    listOf("04" to "Mercedes-Benz CLA", "05" to "BMW i4", "06" to "Polestar 2").forEach { (r, t) ->
                        PillowSurface(Modifier.fillMaxWidth().height(64.dp), shape = OrdoShapes.Row, elevation = 8.dp) {
                            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                DotMatrixText(r, 18.dp, color = Color.White.copy(alpha = 0.85f))
                                FakeThumb(listOf(Color(0xFF9A9A9A), Color(0xFF2A2A2A)), 40)
                                Text(t, style = OrdoTheme.type.itemTitle, color = Color.White)
                            }
                        }
                    }
                }
            }
            FloatingNavBar(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                leading = { GlassIconButton(OrdoIcons.Lists, null, {}, size = 56.dp) },
                primary = { DotPillButton({}, icon = OrdoIcons.Plus) },
                trailing = { GlassIconButton(OrdoIcons.Settings, null, {}, size = 56.dp) },
            )
        }
    }

    @Test
    fun detailMock() = shoot("detail_mock") {
        OrdoBackground {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                OrdoTopBar(
                    "Porsche Taycan",
                    navigation = { GlassIconButton(OrdoIcons.Back, null, {}) },
                    action = { GlassIconButton(OrdoIcons.More, null, {}) },
                )
                Spacer(Modifier.height(8.dp))
                RankWheel(count = 8, position = 2, diameter = 290.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DotMatrixText("01", 14.dp, color = OrdoTheme.colors.textTertiary)
                            DotMatrixText("02", 60.dp)
                            DotMatrixText("03", 14.dp, color = OrdoTheme.colors.textTertiary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Platz 2 von 8", style = OrdoTheme.type.body, color = OrdoTheme.colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(24.dp))
                PillowSurface(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(170.dp)) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Notiz", style = OrdoTheme.type.cardTitle, color = Color.White)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Fährt sich am besten von allen. Platz 2, weil der Jesko einfach unerreichbar ist. Probefahrt war im März.",
                            style = OrdoTheme.type.body, color = OrdoTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FakeThumb(colors: List<Color>, size: Int) {
    Box(
        Modifier.size(size.dp).clip(OrdoShapes.Thumb).drawBehind {
            drawRect(Brush.linearGradient(colors, Offset.Zero, Offset(this.size.width, this.size.height)))
            drawCircle(Color.White.copy(alpha = 0.25f), this.size.minDimension * 0.4f, Offset(this.size.width * 0.3f, this.size.height * 0.25f))
        },
    )
}

@Suppress("unused")
private val keep = Modifier.width(0.dp)
