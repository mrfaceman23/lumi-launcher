package app.lumi.launcher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumi.launcher.AppEntry
import app.lumi.launcher.Glyph
import app.lumi.launcher.Listening
import app.lumi.launcher.Space
import app.lumi.launcher.Weather
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Side length of a medium (square) tile. Every size on a tile scales from this. */
val LocalTile = compositionLocalOf { 200.dp }

@Composable
private fun scaled(fraction: Float, min: Float, max: Float): TextUnit =
    (LocalTile.current.value * fraction).coerceIn(min, max).sp

@Composable
fun labelSize(): TextUnit = scaled(0.11f, 17f, 26f)

@Composable
fun subSize(): TextUnit = scaled(0.08f, 14f, 19f)

/** The colored square every tile is built on. */
@Composable
fun TileBox(
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    padded: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .pressable(onClick, onLongClick)
            .background(color)
            .then(if (padded) Modifier.padding(LocalTile.current * 0.07f) else Modifier),
        content = content,
    )
}

@Composable
fun TileLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier, color = Color.White, fontFamily = Atkinson, fontWeight = FontWeight.Bold,
        fontSize = labelSize(), lineHeight = labelSize() * 1.1f, maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun TileSub(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    Text(
        text, modifier = modifier, color = Color.White.copy(alpha = 0.92f), fontFamily = Atkinson,
        fontSize = subSize(), lineHeight = subSize() * 1.2f, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun glyphSize(): Dp = LocalTile.current * 0.26f

// ---------------------------------------------------------------- Clock

@Composable
fun ClockTile(color: Color, weather: Weather?, modifier: Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L * (60 - (now / 1000 % 60)).coerceAtLeast(1))
        }
    }
    val date = Date(now)
    val time = remember(now / 60_000) { SimpleDateFormat("h:mm", Locale.US).format(date) }
    val ampm = remember(now / 60_000) { SimpleDateFormat("a", Locale.US).format(date) }
    val day = remember(now / 60_000) { SimpleDateFormat("EEEE, MMMM d", Locale.US).format(date) }
    val t = LocalTile.current
    TileBox(color, modifier, onClick, onLongClick) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    time, color = Color.White, fontFamily = Atkinson, fontWeight = FontWeight.Bold,
                    fontSize = (t.value * 0.40f).sp, lineHeight = (t.value * 0.40f).sp,
                )
                Spacer(Modifier.width(t * 0.03f))
                Text(ampm, color = Color.White, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = labelSize(),
                    modifier = Modifier.padding(bottom = t * 0.04f))
            }
            Column {
                TileLabel(day)
                val w = weather
                TileSub(
                    if (w == null) "Weather shows here once location is on"
                    else buildString {
                        append("${w.tempF}°, ${w.description}")
                        if (w.highF != null && w.lowF != null) append(" · high ${w.highF}°, low ${w.lowF}°")
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Keep listening

@Composable
fun ListenTile(color: Color, listening: Listening?, setUp: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val t = LocalTile.current
    TileBox(color, modifier, onClick) {
        val l = listening
        if (l == null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                GlyphIcon(Glyph.LISTEN, Modifier.size(glyphSize()))
                Column {
                    TileLabel("Audiobooks")
                    TileSub(if (setUp) "Pick a book to see it here" else "Your books will show here")
                }
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(t * 0.08f)) {
                Box(
                    Modifier.fillMaxHeight().aspectRatio(0.72f).background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (l.cover != null) {
                        Image(l.cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        GlyphIcon(Glyph.LISTEN, Modifier.size(glyphSize()))
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    TileSub("Keep listening", maxLines = 1)
                    Column(verticalArrangement = Arrangement.spacedBy(t * 0.03f)) {
                        TileLabel(l.title)
                        l.author?.let { TileSub(it, maxLines = 1) }
                        ProgressBar(l.progress, Color.White)
                        TileSub("${(l.progress * 100).roundToInt()}% done", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(LocalTile.current * 0.035f).background(Color.White.copy(alpha = 0.28f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
    }
}

// ---------------------------------------------------------------- Photos

@Composable
fun PhotosTile(color: Color, photos: List<ImageBitmap>, hasPermission: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val motion = LocalMotionOn.current
    TileBox(color, modifier, onClick, padded = false) {
        if (photos.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(LocalTile.current * 0.07f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                GlyphIcon(Glyph.PHOTOS, Modifier.size(glyphSize()))
                Column {
                    TileLabel("Photos")
                    TileSub(if (hasPermission) "No pictures yet" else "Tap to show your pictures here")
                }
            }
        } else {
            var index by remember { mutableIntStateOf(0) }
            LaunchedEffect(photos, motion) {
                if (!motion) return@LaunchedEffect
                while (true) {
                    delay(6_500)
                    index = (index + 1) % photos.size
                }
            }
            // A slow drift-zoom on each picture, the Windows Phone "Pictures" tile feel.
            val drift = if (motion) {
                val tr = rememberInfiniteTransition(label = "drift")
                tr.animateFloat(1.0f, 1.12f, infiniteRepeatable(tween(6_500, easing = LinearEasing), RepeatMode.Reverse), label = "z").value
            } else 1f
            Crossfade(photos[index.coerceIn(0, photos.size - 1)], animationSpec = tween(900), label = "photo") { img ->
                Image(
                    img, null,
                    Modifier.fillMaxSize().graphicsLayer { scaleX = drift; scaleY = drift },
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))
                )
            )
            TileLabel("Photos", Modifier.align(Alignment.BottomStart).padding(LocalTile.current * 0.07f))
        }
    }
}

// ---------------------------------------------------------------- Storage

private val Amber = Color(0xFF8A5A00)
private val Red = Color(0xFFB3121F)

private fun gb(bytes: Long): String {
    val g = bytes / 1_000_000_000.0
    return if (g >= 10) "${g.roundToInt()} GB" else String.format(Locale.US, "%.1f GB", g)
}

@Composable
fun StorageTile(color: Color, spaces: List<Space>, modifier: Modifier, onClick: () -> Unit) {
    val worst = spaces.maxByOrNull { it.level.ordinal }?.level ?: Space.Level.OK
    val bg = when (worst) {
        Space.Level.OK -> color
        Space.Level.WARN -> Amber
        Space.Level.FULL -> Red
    }
    val t = LocalTile.current
    TileBox(bg, modifier, onClick) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(t * 0.05f)) {
                for (s in spaces.take(2)) {
                    Column(verticalArrangement = Arrangement.spacedBy(t * 0.02f)) {
                        TileSub("${s.label}: ${gb(s.freeBytes)} free", maxLines = 1)
                        ProgressBar(s.usedFraction, if (s.level == Space.Level.OK) Color.White else Color(0xFFFFE08A))
                    }
                }
            }
            Column {
                TileLabel("Storage")
                TileSub(
                    when (worst) {
                        Space.Level.OK -> "Plenty of room"
                        Space.Level.WARN -> "Getting full"
                        Space.Level.FULL -> "Almost full. Ask for help"
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Help

@Composable
fun HelpTile(color: Color, name: String, setUp: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TileBox(color, modifier, onClick) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            GlyphIcon(Glyph.HELP, Modifier.size(glyphSize()))
            Column {
                TileLabel("Call for help")
                TileSub(
                    when {
                        !setUp -> "Not set up yet"
                        name.isNotBlank() -> "Reaches $name"
                        else -> "Reaches family"
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Folders

@Composable
fun FolderTile(
    seed: Int,
    color: Color,
    label: String,
    glyph: Glyph,
    apps: List<AppEntry>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val t = LocalTile.current
    TileBox(color, modifier, onClick, padded = false) {
        FlipTile(
            seed = seed,
            modifier = Modifier.fillMaxSize(),
            front = {
                Column(
                    Modifier.fillMaxSize().background(color).padding(t * 0.07f),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        GlyphIcon(glyph, Modifier.size(glyphSize()))
                        if (apps.isNotEmpty()) TileSub("${apps.size}", maxLines = 1)
                    }
                    TileLabel(label)
                }
            },
            // The back face shows the first few apps inside, so she can see what's in the folder.
            back = if (apps.isEmpty()) null else ({
                Column(
                    Modifier.fillMaxSize().background(color.deeper(0.3f)).padding(t * 0.1f),
                    verticalArrangement = Arrangement.spacedBy(t * 0.06f, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    apps.take(4).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(t * 0.08f)) {
                            row.forEach { a -> Image(a.icon, a.label, Modifier.size(t * 0.3f)) }
                        }
                    }
                }
            }),
        )
    }
}

// ---------------------------------------------------------------- Single app

@Composable
fun AppTile(label: String, app: AppEntry?, modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val t = LocalTile.current
    val bg = app?.tint?.readable() ?: Color(0xFF2A3038)
    TileBox(bg, modifier, onClick, onLongClick) {
        if (app != null) {
            Image(app.icon, null, Modifier.align(Alignment.Center).size(t * 0.36f).padding(bottom = t * 0.04f))
            TileLabel(label, Modifier.align(Alignment.BottomStart))
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                TileLabel(label)
                TileSub("Tap to get it", maxLines = 1)
            }
        }
    }
}

@Composable
fun PlainTile(color: Color, label: String, sub: String?, glyph: Glyph, modifier: Modifier, onClick: () -> Unit) {
    TileBox(color, modifier, onClick) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            GlyphIcon(glyph, Modifier.size(glyphSize()))
            Column {
                TileLabel(label)
                sub?.let { TileSub(it, maxLines = 1) }
            }
        }
    }
}
