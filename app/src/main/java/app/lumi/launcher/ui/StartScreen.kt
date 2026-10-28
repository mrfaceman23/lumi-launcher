package app.lumi.launcher.ui

import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumi.launcher.Catalog
import app.lumi.launcher.LauncherViewModel
import app.lumi.launcher.TileDef
import app.lumi.launcher.TileGroup
import app.lumi.launcher.TileKind
import app.lumi.launcher.TileSize
import app.lumi.launcher.packRows

private val Gap = 8.dp

/**
 * The start screen. Landscape: tile groups side by side, scrolling sideways (Windows 8).
 * Portrait: groups stacked, scrolling up and down (Windows Phone). Same tiles either way.
 */
@Composable
fun StartScreen(
    vm: LauncherViewModel,
    isDefaultHome: Boolean,
    onMakeDefault: () -> Unit,
    onAskPhotos: () -> Unit,
) {
    val pal = LocalPalette.current
    // Every tile gets a running number, so the entrance swings across the screen in order.
    val order = remember {
        Catalog.groups.flatMap { g -> g.tiles.map { it.id } }.withIndex().associate { (i, id) -> id to i }
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(pal.background).windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val landscape = maxWidth > maxHeight
        // Group titles only take room when a group actually has one.
        val titleH = if (Catalog.groups.any { it.title.isNotBlank() }) TitleHeight else 0.dp
        val bannerH = if (isDefaultHome) 0.dp else 64.dp
        if (landscape) {
            val avail = maxHeight - 24.dp - 20.dp - titleH - bannerH
            val m = ((avail - Gap * 2) / 3).coerceIn(110.dp, 250.dp)
            CompositionLocalProvider(LocalTile provides m) {
                Column(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 20.dp)) {
                    if (!isDefaultHome) DefaultBanner(onMakeDefault, Modifier.padding(horizontal = 28.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                    ) {
                        for (g in Catalog.groups) {
                            Column {
                                if (g.title.isNotBlank()) GroupTitle(g.title)
                                val rows = packRows(g.tiles.map { it.size.units }, 4)
                                Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                                    for (col in rows.chunked(3)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                                            for (row in col) TileRow(g, row, m, vm, order, onAskPhotos)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        } else {
            val side = 22.dp
            val m = ((maxWidth - side * 2 - Gap * 2) / 3).coerceIn(110.dp, 280.dp)
            CompositionLocalProvider(LocalTile provides m) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = side, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    if (!isDefaultHome) DefaultBanner(onMakeDefault)
                    for (g in Catalog.groups) {
                        Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                            if (g.title.isNotBlank()) GroupTitle(g.title)
                            for (row in packRows(g.tiles.map { it.size.units }, 6)) TileRow(g, row, m, vm, order, onAskPhotos)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private val TitleHeight = 52.dp

@Composable
private fun GroupTitle(text: String) {
    Text(
        text, color = LocalPalette.current.text, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 26.sp,
        modifier = Modifier.height(TitleHeight).padding(top = 4.dp),
    )
}

@Composable
private fun DefaultBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(52.dp).pressable(onClick).background(Color(0xFF0A5BB5)).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Make Lumi the home screen", color = Color.White, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text("Set up", color = Color.White, fontFamily = Atkinson, fontSize = 17.sp)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TileRow(
    group: TileGroup,
    indexes: List<Int>,
    m: Dp,
    vm: LauncherViewModel,
    order: Map<String, Int>,
    onAskPhotos: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
        for (i in indexes) {
            val tile = group.tiles[i]
            val w = if (tile.size == TileSize.WIDE) m * 2 + Gap else m
            Tile(
                tile, vm, onAskPhotos,
                Modifier.size(w, m).turnstile(order[tile.id] ?: 0),
            )
        }
    }
}

@Composable
private fun Tile(tile: TileDef, vm: LauncherViewModel, onAskPhotos: () -> Unit, modifier: Modifier) {
    val color = Color(tile.color)
    when (tile.kind) {
        TileKind.CLOCK -> ClockTile(
            color, vm.weather, modifier,
            onClick = { vm.startIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS), "Couldn't open the clock") },
            // Hidden door to settings, for Jordan.
            onLongClick = { vm.settingsOpen = true },
        )
        TileKind.LISTEN -> ListenTile(color, vm.listening, vm.listenSource.isSetUp, modifier) {
            vm.resolve(tile)?.let(vm::launch) ?: vm.openStore(Catalog.AUDIOBOOKSHELF)
        }
        TileKind.PHOTOS -> PhotosTile(color, vm.photos, vm.photoSource.hasPermission(), modifier) {
            when {
                !vm.photoSource.hasPermission() -> onAskPhotos()
                else -> vm.resolve(tile)?.let(vm::launch) ?: vm.toast("No photo app found")
            }
        }
        TileKind.STORAGE -> StorageTile(color, vm.spaces, modifier) {
            vm.startIntent(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS), "Couldn't open storage settings")
        }
        TileKind.HELP -> {
            val number = vm.prefs.helpNumber
            HelpTile(color, vm.prefs.helpName, number.isNotBlank(), modifier) {
                if (number.isBlank()) vm.toast("Help isn't set up yet")
                else vm.openUrl("https://wa.me/$number")
            }
        }
        TileKind.FOLDER -> FolderTile(
            seed = tile.id.hashCode(), color = color, label = tile.label, glyph = tile.glyph!!,
            apps = vm.folderApps(tile), modifier = modifier,
        ) { vm.openFolder = tile.id }
        TileKind.MY_APPS -> FolderTile(
            seed = tile.id.hashCode(), color = color, label = tile.label, glyph = tile.glyph!!,
            apps = vm.myApps(), modifier = modifier,
        ) { vm.openFolder = tile.id }
        TileKind.ALL_APPS -> PlainTile(color, tile.label, "A to Z", tile.glyph!!, modifier) { vm.allAppsOpen = true }
        TileKind.APP -> {
            val app = vm.resolve(tile)
            AppTile(
                tile.label, app, modifier,
                onClick = {
                    when {
                        app != null -> vm.launch(app)
                        tile.packages.isNotEmpty() -> vm.openStore(tile.packages.first())
                        else -> vm.toast("${tile.label} isn't installed")
                    }
                },
                onLongClick = app?.let { a -> { vm.openAppInfo(a) } },
            )
        }
    }
}
