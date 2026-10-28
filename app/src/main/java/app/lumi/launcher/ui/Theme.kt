package app.lumi.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.scale
import app.lumi.launcher.Glyph
import app.lumi.launcher.R

/** Atkinson Hyperlegible: made by the Braille Institute so letters never blur together. */
val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

/** The few colors outside the tiles. Tiles keep their own deep colors in both looks. */
data class Palette(
    val background: Color,
    val text: Color,
    val textDim: Color,
    val sheet: Color,
    val scrim: Color,
    val line: Color,
)

val DarkPalette = Palette(
    background = Color(0xFF000000),
    text = Color(0xFFFFFFFF),
    textDim = Color(0xFFB8C0CA),
    sheet = Color(0xFF14181D),
    scrim = Color(0xCC000000),
    line = Color(0xFF2A3038),
)

val LightPalette = Palette(
    background = Color(0xFFF4F6F8),
    text = Color(0xFF0E1216),
    textDim = Color(0xFF4A5560),
    sheet = Color(0xFFFFFFFF),
    scrim = Color(0x99000000),
    line = Color(0xFFD5DBE1),
)

val LocalPalette = compositionLocalOf { DarkPalette }

/** The line drawings, as SVG path data on a 24×24 grid. Drawn in white. */
private val glyphPaths: Map<Glyph, String> = mapOf(
    Glyph.SCHOOL to "M2 8l10-5 10 5-10 5z M6 10v5c3 2.5 9 2.5 12 0v-5",
    Glyph.TALK to "M4 5h16v11H9l-5 4z",
    Glyph.MAIL to "M3 6h18v12H3z M3 6l9 7 9-7",
    Glyph.CROCHET to "M11 3a8 8 0 1 0 0.01 0z M5 7c4 0 9 4 10 11 M8 4c3 1 7 6 7 12 M3.5 11c4-1 9 2 11 8.5 M17.5 16.5c2 1 4 2.5 3.5 5",
    Glyph.WATCH to "M3 6h18v12H3z M8 21h8 M9 2l3 4 3-4",
    Glyph.SHOP to "M5 8h14l-1 13H6z M9 8a3 3 0 0 1 6 0",
    Glyph.HOME to "M3 11l9-7 9 7 M5 10v10h14V10 M10 20v-6h4v6",
    Glyph.MONEY to "M3 10l9-6 9 6z M5 10v8 M10 10v8 M14 10v8 M19 10v8 M3 20h18",
    Glyph.TOOLS to "M4 6h9 M17 6h3 M4 12h3 M11 12h9 M4 18h11 M19 18h1 M15 4v4 M9 10v4 M17 16v4",
    Glyph.MY_APPS to "M4 4h7v7H4z M13 4h7v7h-7z M4 13h7v7H4z M16.5 13v7 M13 16.5h7",
    Glyph.HELP to "M12 21s-7-4.4-9.3-9A5.4 5.4 0 0 1 12 6a5.4 5.4 0 0 1 9.3 6c-2.3 4.6-9.3 9-9.3 9z",
    Glyph.STORAGE to "M4 5h16v14H4z M4 14h16 M16 16.5h1.5",
    Glyph.PHOTOS to "M3 6h18v13H3z M21 16l-5-5-8 8 M8.5 9a1.5 1.5 0 1 0 0.01 0z",
    Glyph.LISTEN to "M4 15v-3a8 8 0 0 1 16 0v3 M4 15h3v5H4z M17 15h3v5h-3z",
    Glyph.ALL_APPS to "M5 12h14 M13 6l6 6-6 6",
)

private val parsed = glyphPaths.mapValues { (_, d) -> PathParser().parsePathString(d).toPath() }

@Composable
fun GlyphIcon(glyph: Glyph, modifier: Modifier, color: Color = Color.White) {
    val path = parsed[glyph] ?: return
    Canvas(modifier) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(
                path, color,
                style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** A tile color made a touch darker, for the back face of flipping tiles. */
fun Color.deeper(amount: Float = 0.25f): Color =
    Color(red * (1 - amount), green * (1 - amount), blue * (1 - amount), alpha)

/** Darkens icon-derived colors that are too pale for bold white text. */
fun Color.readable(): Color {
    val lum = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    // Keeps white text at a strong contrast on every app's tile color.
    return if (lum > 0.32f) deeper(1f - 0.32f / lum) else this
}
