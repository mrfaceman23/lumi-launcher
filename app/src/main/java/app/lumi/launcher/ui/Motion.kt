package app.lumi.launcher.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Goes up each time the start screen comes back into view. Entrance animations replay on it. */
val LocalShowCount = compositionLocalOf { 0 }

/** False when the tablet has animations turned off, or the start screen isn't showing. */
val LocalMotionOn = compositionLocalOf { true }

/** Reads the system "remove animations" setting, so Lumi stays still when asked. */
@Composable
fun systemAllowsMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
            .getOrDefault(1f) > 0f
    }
}

/**
 * Tap + long-press with the Windows Phone "tilt": the tile presses in under the finger,
 * then springs back, so she always knows the tap landed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    pressedScale: Float = 0.94f,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
}

/**
 * Tiles swing in one after another each time the start screen shows, like Windows Phone's
 * "turnstile". Quick: the whole screen settles in under half a second.
 */
@Composable
fun Modifier.turnstile(index: Int): Modifier {
    val show = LocalShowCount.current
    val motion = LocalMotionOn.current
    val anim = remember { Animatable(1f) }
    LaunchedEffect(show) {
        if (!motion) { anim.snapTo(1f); return@LaunchedEffect }
        anim.snapTo(0f)
        delay((index * 30L).coerceAtMost(330L))
        anim.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        val t = anim.value
        alpha = t
        rotationY = (1f - t) * -55f
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
        cameraDistance = 14f * density
    }
}

/**
 * A live tile that flips between a front and a back face now and then.
 * [seed] staggers tiles so they never all flip at once.
 */
@Composable
fun FlipTile(
    seed: Int,
    modifier: Modifier = Modifier,
    holdFrontMs: Long = 9_000,
    holdBackMs: Long = 4_000,
    front: @Composable () -> Unit,
    back: (@Composable () -> Unit)?,
) {
    val motion = LocalMotionOn.current
    val angle = remember { Animatable(0f) }
    if (back != null && motion) {
        LaunchedEffect(seed) {
            val rnd = Random(seed)
            delay(2_000L + rnd.nextLong(0, 9_000))
            while (true) {
                angle.animateTo(180f, tween(700, easing = FastOutSlowInEasing))
                delay(holdBackMs)
                angle.animateTo(360f, tween(700, easing = FastOutSlowInEasing))
                angle.snapTo(0f)
                delay(holdFrontMs + rnd.nextLong(0, 6_000))
            }
        }
    } else {
        LaunchedEffect(Unit) { angle.snapTo(0f) }
    }
    val a = angle.value
    val showingBack = a in 90f..270f
    Box(
        modifier.graphicsLayer {
            rotationX = a
            cameraDistance = 16f * density
        }
    ) {
        if (showingBack && back != null) {
            // Undo the mirror so the back face reads the right way round.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationX = 180f }) { back() }
        } else {
            front()
        }
    }
}
