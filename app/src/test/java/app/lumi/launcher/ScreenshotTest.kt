package app.lumi.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import app.lumi.launcher.ui.DarkPalette
import app.lumi.launcher.ui.LocalMotionOn
import app.lumi.launcher.ui.LocalPalette
import app.lumi.launcher.ui.StartScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the start screen to a picture for review. Runs only with LUMI_SCREENSHOTS=1. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Before fun onlyWhenAsked() = assumeTrue(System.getenv("LUMI_SCREENSHOTS") == "1")

    private fun shot(name: String) {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext())
        compose.setContent {
            CompositionLocalProvider(LocalPalette provides DarkPalette, LocalMotionOn provides false) {
                Box(Modifier.fillMaxSize()) { StartScreen(vm, isDefaultHome = true, onMakeDefault = {}, onAskPhotos = {}) }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    // Galaxy Tab S7+ is about 1180 x 737 dp.
    @Config(qualifiers = "w1180dp-h737dp-land-xhdpi", sdk = [34])
    @Test fun landscape() = shot("landscape")

    @Config(qualifiers = "w737dp-h1180dp-port-xhdpi", sdk = [34])
    @Test fun portrait() = shot("portrait")
}
