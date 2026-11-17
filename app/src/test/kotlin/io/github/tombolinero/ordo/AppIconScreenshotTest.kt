package io.github.tombolinero.ordo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.shape.SmoothCornerShape
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the adaptive launcher icon the way launchers mask it (squircle, circle, themed). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class AppIconScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun Icon(size: Dp, shape: Shape, themed: Boolean = false) {
        // Launchers show only the inner 72 of the 108 dp canvas: scale the layers by 1.5 and crop.
        Box(Modifier.size(size).clip(shape).graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }) {
            if (themed) {
                Box(Modifier.size(size).background(Color(0xFF2B2F23)))
                Image(painterResource(R.drawable.ic_launcher_monochrome), null, Modifier.size(size), colorFilter = ColorFilter.tint(Color(0xFFE3E8C8)))
            } else {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(size))
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(size))
            }
        }
    }

    @Test
    fun launcherIcon() {
        compose.setContent {
            OrdoTheme {
                Column(
                    Modifier.testTag("icons").background(Color(0xFF3A3A3A)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Bottom) {
                        Icon(150.dp, SmoothCornerShape(42.dp))
                        Icon(150.dp, CircleShape)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(56.dp, SmoothCornerShape(16.dp))
                        Icon(56.dp, CircleShape)
                        Icon(56.dp, CircleShape, themed = true)
                        Icon(40.dp, SmoothCornerShape(12.dp))
                        Text("Ordo", color = Color.White, style = OrdoTheme.type.label)
                    }
                }
            }
        }
        compose.onNodeWithTag("icons").captureRoboImage("src/test/screenshots/launcher_icon.png")
    }
}
