package app.lumi.launcher

/**
 * Everything on Mom's start screen, in order. Plain data, so it's easy to change:
 * move a line to move a tile, add a package name to add an app to a folder.
 */

enum class TileSize(val units: Int) { MEDIUM(2), WIDE(4) }

enum class TileKind { CLOCK, LISTEN, PHOTOS, STORAGE, HELP, FOLDER, APP, MY_APPS, ALL_APPS }

/** The little line drawing on a tile. */
enum class Glyph {
    SCHOOL, TALK, MAIL, CROCHET, WATCH, SHOP, HOME, MONEY, TOOLS, MY_APPS, HELP, STORAGE, PHOTOS, LISTEN, ALL_APPS
}

data class TileDef(
    val id: String,
    val kind: TileKind,
    val size: TileSize,
    val label: String,
    /** Tile color as 0xAARRGGBB. Ignored for app tiles, which take the app icon's color. */
    val color: Long = 0xFF0A5BB5,
    val glyph: Glyph? = null,
    /** For folders: the apps inside. For app tiles: the first one installed wins. */
    val packages: List<String> = emptyList(),
    /** Fallback when we don't know an app's package name: match its name instead. */
    val labelHint: String? = null,
)

data class TileGroup(val title: String, val tiles: List<TileDef>)

object Catalog {
    // Tile colors: deep enough that bold white text is easy to read on every one.
    const val BLUE = 0xFF0A5BB5
    const val TEAL = 0xFF006B74
    const val GREEN = 0xFF217A3A
    const val ORANGE = 0xFFB34700
    const val MAGENTA = 0xFFA3005C
    const val PURPLE = 0xFF5C2D91
    const val RED = 0xFFB3121F
    const val COBALT = 0xFF1F3F99
    const val OLIVE = 0xFF5A6B12
    const val SLATE = 0xFF3D4651

    const val AUDIOBOOKSHELF = "com.audiobookshelf.app"

    /** Apps that open from the Photos tile, best first. */
    val photoApps = listOf("com.sec.android.gallery3d", "com.google.android.apps.photos")
    const val PHOTO_APP_HINT = "AiFoto"

    val school = TileDef(
        "school", TileKind.FOLDER, TileSize.MEDIUM, "School", TEAL, Glyph.SCHOOL,
        listOf(
            "com.vitalsource.bookshelf", "com.fluidtouch.noteshelf3", "com.fluidtouch.noteshelf2",
            "com.adobe.reader", "com.google.android.apps.docs.editors.docs", "com.google.android.apps.docs",
            "us.zoom.videomeetings", "com.microsoft.teams",
        ),
    )
    val talk = TileDef(
        "talk", TileKind.FOLDER, TileSize.MEDIUM, "Talk", GREEN, Glyph.TALK,
        listOf(
            "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.whatsapp",
            "com.facebook.katana", "com.facebook.orca", "com.samsung.android.app.contacts",
        ),
    )
    val mail = TileDef(
        "mail", TileKind.FOLDER, TileSize.MEDIUM, "Mail", COBALT, Glyph.MAIL,
        listOf("com.google.android.gm", "com.yahoo.mobile.client.android.mail", "me.bluemail.mail", "com.samsung.android.email.provider"),
    )
    val crochet = TileDef(
        "crochet", TileKind.FOLDER, TileSize.MEDIUM, "Crochet", MAGENTA, Glyph.CROCHET,
        listOf("com.rfsp.pocketcrochet", "com.pinterest"),
    )
    val watch = TileDef(
        "watch", TileKind.FOLDER, TileSize.MEDIUM, "Watch", PURPLE, Glyph.WATCH,
        listOf("com.crunchyroll.crunchyroid", "org.jellyfin.mobile", "com.google.android.youtube"),
    )
    val shopping = TileDef(
        "shopping", TileKind.FOLDER, TileSize.MEDIUM, "Shopping", ORANGE, Glyph.SHOP,
        listOf("com.amazon.mShop.android.shopping", "com.einnovation.temu", "com.ebay.mobile"),
    )
    val home = TileDef(
        "home", TileKind.FOLDER, TileSize.MEDIUM, "Home", OLIVE, Glyph.HOME,
        listOf(
            "com.ringapp", "com.arlo.app", "com.govee.home", "com.ge.kitchen", "com.smarthome.econet",
            "com.samsung.android.oneconnect", "com.amazon.dee.app",
        ),
    )
    val money = TileDef(
        "money", TileKind.FOLDER, TileSize.MEDIUM, "Money", GREEN, Glyph.MONEY,
        listOf("com.navyfederal.android", "com.squareup.cash"),
    )
    val tools = TileDef(
        "tools", TileKind.FOLDER, TileSize.MEDIUM, "Tools", SLATE, Glyph.TOOLS,
        listOf(
            "com.android.settings", "com.sec.android.app.popupcalculator", "com.sec.android.app.clockpackage",
            "com.samsung.android.calendar", "com.sec.android.app.voicenote", "com.sec.android.app.myfiles",
            "com.google.android.apps.adm", "com.hp.printercontrol", "com.tailscale.ipn",
            "com.sec.android.app.music", "com.samsung.android.app.notes", "com.android.vending",
            "com.sec.android.app.samsungapps", "com.google.android.googlequicksearchbox",
            "com.sec.android.gallery3d", "com.google.android.apps.photos",
        ),
    )

    val folders = listOf(school, talk, mail, crochet, watch, shopping, home, money, tools)

    // One continuous run of tiles, no headers: sideways in landscape, down in portrait.
    // The count is chosen so the run ends on a full column / full row either way.
    val groups: List<TileGroup> = listOf(
        TileGroup(
            "",
            listOf(
                TileDef("clock", TileKind.CLOCK, TileSize.WIDE, "Clock", BLUE),
                TileDef("listen", TileKind.LISTEN, TileSize.WIDE, "Keep listening", PURPLE, Glyph.LISTEN, listOf(AUDIOBOOKSHELF), "Audiobookshelf"),
                TileDef("photos", TileKind.PHOTOS, TileSize.MEDIUM, "Photos", MAGENTA, Glyph.PHOTOS, photoApps, PHOTO_APP_HINT),
                TileDef("storage", TileKind.STORAGE, TileSize.MEDIUM, "Storage", SLATE, Glyph.STORAGE),
                // Easy to find, but a tap does nothing: it only calls on press and hold.
                TileDef("help", TileKind.HELP, TileSize.MEDIUM, "Call for help", RED, Glyph.HELP),
                school, talk, mail, crochet, watch,
                TileDef("spotify", TileKind.APP, TileSize.MEDIUM, "Spotify", packages = listOf("com.spotify.music")),
                TileDef("kindle", TileKind.APP, TileSize.MEDIUM, "Kindle", packages = listOf("com.amazon.kindle")),
                TileDef("chrome", TileKind.APP, TileSize.MEDIUM, "Internet", packages = listOf("com.android.chrome", "com.sec.android.app.sbrowser")),
                TileDef("camera", TileKind.APP, TileSize.MEDIUM, "Camera", packages = listOf("com.sec.android.app.camera")),
                TileDef("files", TileKind.APP, TileSize.MEDIUM, "My files", labelHint = "AiData", packages = listOf("com.asustor.aidata")),
                TileDef("game", TileKind.APP, TileSize.MEDIUM, "Royal Match", packages = listOf("com.dreamgames.royalmatch")),
                shopping, home, money, tools,
                TileDef("myapps", TileKind.MY_APPS, TileSize.MEDIUM, "My apps", SLATE, Glyph.MY_APPS),
                TileDef("allapps", TileKind.ALL_APPS, TileSize.MEDIUM, "All apps", BLUE, Glyph.ALL_APPS),
            ),
        ),
    )

    /** The start screen's tiles in their default order. */
    val startTiles: List<TileDef> get() = groups.flatMap { it.tiles }

    /** Every package some tile or folder already shows. Anything else goes in "My apps". */
    val claimedPackages: Set<String> by lazy {
        groups.flatMap { g -> g.tiles.flatMap { it.packages } }.toSet() + AUDIOBOOKSHELF
    }

    /** Names (lowercase) that tiles find by name, so those apps don't also land in "My apps". */
    val claimedLabels: Set<String> by lazy {
        groups.flatMap { g -> g.tiles.mapNotNull { it.labelHint?.lowercase() } }.toSet()
    }
}

/**
 * Packs tiles into rows [width] units wide. Each tile goes into the first row that still has
 * room for it, so a square tile can tuck in beside a wide one instead of leaving a gap.
 * Returns the tile indexes for each row.
 */
fun packRows(sizes: List<Int>, width: Int): List<List<Int>> {
    val rows = mutableListOf<MutableList<Int>>()
    val used = mutableListOf<Int>()
    sizes.forEachIndexed { i, s ->
        val w = s.coerceAtMost(width)
        val r = used.indexOfFirst { it + w <= width }
        if (r >= 0) {
            rows[r] += i
            used[r] += w
        } else {
            rows += mutableListOf(i)
            used += w
        }
    }
    return rows
}

/**
 * Puts tiles in the order she saved (comma-separated ids). Ids that no longer exist are skipped,
 * and tiles added in a later version of Lumi go at the end, so an update never loses a tile.
 */
fun applyOrder(defaults: List<TileDef>, saved: String): List<TileDef> {
    if (saved.isBlank()) return defaults
    val byId = defaults.associateBy { it.id }
    val seen = mutableSetOf<String>()
    val out = saved.split(',').mapNotNull { byId[it.trim()] }.filter { seen.add(it.id) }.toMutableList()
    defaults.filterTo(out) { it.id !in seen }
    return out
}

/**
 * Swaps two tiles. Only tiles of the same size swap, which keeps the screen free of holes;
 * anything else returns the list unchanged.
 */
fun swapTiles(tiles: List<TileDef>, a: String, b: String): List<TileDef> {
    val i = tiles.indexOfFirst { it.id == a }
    val j = tiles.indexOfFirst { it.id == b }
    if (i < 0 || j < 0 || i == j || tiles[i].size != tiles[j].size) return tiles
    return tiles.toMutableList().also { it[i] = tiles[j]; it[j] = tiles[i] }
}
