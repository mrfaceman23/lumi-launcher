package app.lumi.launcher

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()

    val prefs = Prefs(app)
    private val repo = AppRepository(app)
    private val weatherSource = WeatherSource(app)
    private val storageSource = StorageSource(app)
    val photoSource = PhotoSource(app)
    val listenSource = ListenSource(prefs)

    var apps by mutableStateOf<List<AppEntry>>(emptyList()); private set
    var weather by mutableStateOf<Weather?>(null); private set
    var spaces by mutableStateOf<List<Space>>(emptyList()); private set
    var photos by mutableStateOf<List<ImageBitmap>>(emptyList()); private set
    var listening by mutableStateOf<Listening?>(null); private set
    var light by mutableStateOf(prefs.lightMode); private set

    // What's open on top of the start screen.
    /** The folder showing, by tile id ("myapps" for My apps), or null. */
    var openFolder by mutableStateOf<String?>(null)
    var allAppsOpen by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)

    /** Closes whatever is open. Returns false if nothing was. */
    fun closeTop(): Boolean = when {
        settingsOpen -> { settingsOpen = false; true }
        openFolder != null -> { openFolder = null; true }
        allAppsOpen -> { allAppsOpen = false; true }
        else -> false
    }

    fun closeAll() {
        settingsOpen = false
        openFolder = null
        allAppsOpen = false
    }

    /** Goes up each time the start screen comes back into view, so tiles can replay their entrance. */
    var showCount by mutableIntStateOf(0); private set

    private var weatherAt = 0L
    private var ticker: Job? = null

    init {
        loadApps()
        repo.watch { loadApps() }
    }

    private fun loadApps() {
        viewModelScope.launch {
            apps = withContext(Dispatchers.Default) { repo.loadApps() }
        }
    }

    /** Called every time the start screen is shown. */
    fun onShown() {
        showCount++
        refreshStorage()
        refreshPhotos()
        refreshListening()
        refreshWeather()
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(5 * 60_000L)
                refreshStorage()
                refreshListening()
                refreshWeather()
            }
        }
    }

    fun onHidden() {
        ticker?.cancel()
        ticker = null
    }

    fun refreshStorage() {
        viewModelScope.launch { spaces = withContext(Dispatchers.IO) { storageSource.read() } }
    }

    fun refreshPhotos() {
        viewModelScope.launch { photos = photoSource.recent() }
    }

    fun refreshListening() {
        viewModelScope.launch { listening = listenSource.load() }
    }

    fun refreshWeather(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - weatherAt < 30 * 60_000L && weather != null) return
        viewModelScope.launch {
            weatherSource.load()?.let { weather = it; weatherAt = now }
        }
    }

    fun changeLook(lightOn: Boolean) {
        prefs.lightMode = lightOn
        light = lightOn
    }

    // ---- Finding apps for tiles ----

    private fun byPackage(pkg: String): AppEntry? = apps.firstOrNull { it.component.packageName == pkg }

    private fun byLabel(label: String): AppEntry? = apps.firstOrNull { it.label.equals(label, ignoreCase = true) }

    /** The app a single tile opens: first installed package, else an app with the hinted name. */
    fun resolve(tile: TileDef): AppEntry? =
        tile.packages.firstNotNullOfOrNull(::byPackage) ?: tile.labelHint?.let(::byLabel)

    /** The installed apps inside a folder, in the folder's own order. */
    fun folderApps(tile: TileDef): List<AppEntry> = tile.packages.mapNotNull(::byPackage)

    /** Every app no tile or folder already shows: new installs land here on their own. */
    fun myApps(): List<AppEntry> = apps.filter {
        it.component.packageName !in Catalog.claimedPackages && it.label.lowercase() !in Catalog.claimedLabels
    }

    fun launch(app: AppEntry) = repo.launch(app)

    fun openAppInfo(app: AppEntry) = repo.openAppInfo(app)

    /** Opens a link, or says plainly why it couldn't. */
    fun openUrl(url: String) {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { toast("Couldn't open that link") }
    }

    /** A tile whose app isn't installed opens its Play Store page instead. */
    fun openStore(pkg: String) {
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { toast("That app isn't installed") }
    }

    fun startIntent(intent: Intent, failMessage: String) {
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { toast(failMessage) }
    }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    override fun onCleared() {
        repo.stopWatching()
        super.onCleared()
    }
}
