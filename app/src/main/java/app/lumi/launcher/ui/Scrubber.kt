package app.lumi.launcher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

/**
 * The A–Z strip along the right edge. Slide your thumb along it to jump through apps.
 *
 * It sits a little in from the very edge and asks Android not to treat it as the
 * "back" swipe. (Android only allows a limited strip to be excluded, so it's kept compact.)
 */
@Composable
fun EdgeScrubber(
    letters: List<Char>,
    active: Char?,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return
    val view = LocalView.current
    // A tiny tick on every letter, like a watch crown. Feels premium, costs nothing.
    val tick: () -> Unit = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    Column(
        modifier
            .width(40.dp)
            .fillMaxHeight()
            .systemGestureExclusion()
            .pointerInput(letters) {
                fun letterAt(y: Float): Char {
                    val i = ((y / size.height) * letters.size).toInt().coerceIn(0, letters.size - 1)
                    return letters[i]
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var last = letterAt(down.position.y)
                    tick()
                    onLetter(last)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        val now = letterAt(change.position.y)
                        if (now != last) {
                            last = now
                            tick()
                            onLetter(now)
                        }
                    }
                    onRelease()
                }
            }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (c in letters) {
            val on = c == active
            Text(
                c.toString(),
                color = if (on) Color(0xFF3D8BFF) else if (active != null) LocalPalette.current.textDim else LocalPalette.current.textDim.copy(alpha = 0.6f),
                fontSize = if (on) 20.sp else 15.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
