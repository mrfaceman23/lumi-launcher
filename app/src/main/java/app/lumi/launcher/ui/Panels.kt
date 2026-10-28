package app.lumi.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumi.launcher.AppEntry
import app.lumi.launcher.Catalog
import app.lumi.launcher.Glyph
import app.lumi.launcher.LauncherViewModel
import kotlinx.coroutines.launch

/** One thing inside a folder: an installed app, or a saved link (Coder's Desk). */
private sealed interface FolderItem {
    data class App(val app: AppEntry) : FolderItem
    data class Link(val label: String, val url: String) : FolderItem
}

/**
 * A folder sliding open over the start screen: from the right in landscape, from the bottom
 * in portrait. Tap outside it or press Back to close.
 */
@Composable
fun FolderPanel(vm: LauncherViewModel) {
    val id = vm.openFolder
    val pal = LocalPalette.current
    // Keep the last folder while it slides closed.
    var shownId by remember { mutableStateOf(id) }
    if (id != null) shownId = id

    AnimatedVisibility(id != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
        Box(
            Modifier.fillMaxSize().background(pal.scrim)
                .clickable(remember { MutableInteractionSource() }, indication = null) { vm.openFolder = null }
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        AnimatedVisibility(
            visible = id != null,
            modifier = Modifier.align(if (landscape) Alignment.CenterEnd else Alignment.BottomCenter),
            enter = if (landscape) slideInHorizontally(tween(280)) { it } else slideInVertically(tween(280)) { it },
            exit = if (landscape) slideOutHorizontally(tween(220)) { it } else slideOutVertically(tween(220)) { it },
        ) {
            val f = Catalog.folders.firstOrNull { it.id == shownId }
            val mine = shownId == "myapps"
            val color = Color(f?.color ?: Catalog.SLATE)
            val title = f?.label ?: "My apps"
            val glyph = f?.glyph ?: Glyph.MY_APPS
            val items: List<FolderItem> = buildList {
                if (f != null) {
                    addAll(vm.folderApps(f).map { FolderItem.App(it) })
                    if (f.id == "school" && vm.prefs.codersDeskUrl.isNotBlank()) add(0, FolderItem.Link("Coder's Desk", vm.prefs.codersDeskUrl))
                } else {
                    addAll(vm.myApps().map { FolderItem.App(it) })
                }
            }
            Column(
                (if (landscape) Modifier.fillMaxHeight().width(maxWidth * 0.58f) else Modifier.fillMaxWidth().height(maxHeight * 0.72f))
                    .background(pal.sheet)
                    // Taps inside the panel shouldn't close it.
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                Row(
                    Modifier.fillMaxWidth().background(color).padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GlyphIcon(glyph, Modifier.size(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                        if (mine) Text("New apps you install show up here", color = Color.White, fontFamily = Atkinson, fontSize = 17.sp)
                    }
                    Text(
                        "Close", color = Color.White, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 19.sp,
                        modifier = Modifier.pressable({ vm.openFolder = null }).padding(12.dp),
                    )
                }
                if (items.isEmpty()) {
                    Text(
                        if (mine) "Nothing here yet. Apps you install will appear here." else "None of these apps are installed yet.",
                        color = pal.textDim, fontFamily = Atkinson, fontSize = 20.sp, modifier = Modifier.padding(28.dp),
                    )
                } else {
                    CompositionLocalProvider(LocalTile provides 150.dp) {
                        LazyVerticalGrid(
                            GridCells.Adaptive(150.dp),
                            Modifier.fillMaxSize().padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(items) { item ->
                                val cell = Modifier.aspectRatio(1f)
                                when (item) {
                                    is FolderItem.App -> AppTile(
                                        item.app.label, item.app, cell,
                                        onClick = { vm.openFolder = null; vm.launch(item.app) },
                                        onLongClick = { vm.openAppInfo(item.app) },
                                    )
                                    is FolderItem.Link -> PlainTile(color, item.label, "Study", Glyph.SCHOOL, cell) {
                                        vm.openFolder = null; vm.openUrl(item.url)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Every app, A to Z, in big rows, with the A–Z strip on the right to jump around. */
@Composable
fun AllAppsScreen(vm: LauncherViewModel) {
    val pal = LocalPalette.current
    AnimatedVisibility(
        vm.allAppsOpen,
        enter = slideInHorizontally(tween(260)) { it } + fadeIn(tween(200)),
        exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(180)),
    ) {
        val apps = vm.apps
        val letters = remember(apps) { apps.map { it.letter }.distinct() }
        // Position of each letter's first app in the list (each letter header counts as a row).
        val firstRow = remember(apps) {
            val map = mutableMapOf<Char, Int>()
            var row = 0
            var last: Char? = null
            for (a in apps) {
                if (a.letter != last) { map[a.letter] = row; row++; last = a.letter }
                row++
            }
            map
        }
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        var active by remember { mutableStateOf<Char?>(null) }
        Box(Modifier.fillMaxSize().background(pal.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("All apps", color = pal.text, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                    Text(
                        "Back to start", color = pal.text, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 19.sp,
                        modifier = Modifier.pressable({ vm.allAppsOpen = false }).padding(12.dp),
                    )
                }
                LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(start = 28.dp, end = 56.dp)) {
                    var last: Char? = null
                    for (a in apps) {
                        if (a.letter != last) {
                            val letter = a.letter
                            last = letter
                            item(key = "h$letter") {
                                Box(
                                    Modifier.padding(top = 14.dp, bottom = 6.dp).size(52.dp).background(Color(0xFF0A5BB5)),
                                    contentAlignment = Alignment.BottomStart,
                                ) {
                                    Text(letter.toString(), color = Color.White, fontFamily = Atkinson, fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp, modifier = Modifier.padding(6.dp))
                                }
                            }
                        }
                        item(key = a.key) { AppRow(a, vm) }
                    }
                }
            }
            EdgeScrubber(
                letters = letters,
                active = active,
                onLetter = { c ->
                    active = c
                    firstRow[c]?.let { scope.launch { listState.scrollToItem(it) } }
                },
                onRelease = { active = null },
                modifier = Modifier.align(Alignment.CenterEnd).padding(top = 90.dp, bottom = 20.dp, end = 10.dp),
            )
        }
    }
}

@Composable
private fun AppRow(a: AppEntry, vm: LauncherViewModel) {
    Row(
        Modifier.fillMaxWidth().height(76.dp)
            .pressable({ vm.allAppsOpen = false; vm.launch(a) }, { vm.openAppInfo(a) }, pressedScale = 0.97f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(60.dp).background(a.tint.readable()), contentAlignment = Alignment.Center) {
            Image(a.icon, null, Modifier.size(42.dp))
        }
        Text(
            a.label, color = LocalPalette.current.text, fontFamily = Atkinson, fontSize = 24.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
