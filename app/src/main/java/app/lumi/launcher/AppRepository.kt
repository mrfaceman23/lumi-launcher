package app.lumi.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap

/** One app that can be launched from the home screen. */
data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
    /** The icon's main color, used to tint its tile. */
    val tint: Color = Color(0xFF2A2A2A),
) {
    /** The letter this app files under in the A–Z scrubber. Non-letters go under '#'. */
    val letter: Char
        get() = label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
}

/**
 * The icon's main color: averages the colorful pixels (skips see-through, near-white and
 * near-black ones, and counts more-saturated pixels more), so Spotify comes out green,
 * Instagram pink-purple, and so on.
 */
private fun dominantColor(bmp: Bitmap): Color {
    var r = 0.0; var g = 0.0; var b = 0.0; var total = 0.0
    val hsv = FloatArray(3)
    var y = 0
    while (y < bmp.height) {
        var x = 0
        while (x < bmp.width) {
            val p = bmp.getPixel(x, y)
            if ((p ushr 24) > 160) {
                android.graphics.Color.colorToHSV(p, hsv)
                val sat = hsv[1]
                val v = hsv[2]
                if (v > 0.12f && !(sat < 0.12f && v > 0.9f)) {
                    val wgt = 0.15 + sat * sat
                    r += ((p shr 16) and 0xFF) * wgt
                    g += ((p shr 8) and 0xFF) * wgt
                    b += (p and 0xFF) * wgt
                    total += wgt
                }
            }
            x += 3
        }
        y += 3
    }
    if (total < 1.0) return Color(0xFF2A2A2A)
    return Color((r / total).toInt(), (g / total).toInt(), (b / total).toInt())
}

/** Finds installed apps and launches them. Uses Android's LauncherApps service. */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private var callback: LauncherApps.Callback? = null

    /**
     * Remembered tile colors, so each app's color is worked out once instead of on every load.
     * The key includes where the app is installed, which changes when the app updates (and its
     * icon might too), so updated apps get a fresh color automatically.
     */
    private val tintCache = context.getSharedPreferences("tint_cache", Context.MODE_PRIVATE)

    fun loadApps(): List<AppEntry> {
        val iconPx = (48 * context.resources.displayMetrics.density).toInt()
        val result = mutableListOf<AppEntry>()
        val remembered = tintCache.all
        val fresh = mutableMapOf<String, Int>()
        for (user in userManager.userProfiles) {
            val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            for (info in activities) {
                if (info.componentName.packageName == context.packageName) continue
                val bmp = runCatching { info.getBadgedIcon(0).toBitmap(iconPx, iconPx) }.getOrNull() ?: continue
                val icon = bmp.asImageBitmap()
                result += AppEntry(
                    key = keyFor(info.componentName, user),
                    label = info.label?.toString()?.trim().orEmpty().ifEmpty { info.componentName.packageName },
                    component = info.componentName,
                    user = user,
                    icon = icon,
                    tint = run {
                        val cacheKey = info.componentName.flattenToString() + "|" + (info.applicationInfo?.sourceDir ?: "")
                        val known = remembered[cacheKey] as? Int
                        Color(known ?: dominantColor(bmp).let { c -> c.toArgb().also { fresh[cacheKey] = it } })
                    },
                )
            }
        }
        if (fresh.isNotEmpty()) tintCache.edit().apply { fresh.forEach { (k, v) -> putInt(k, v) } }.apply()
        // A–Z first, then '#' (numbers/symbols) at the end, alphabetical inside each.
        return result.sortedWith(
            compareBy<AppEntry>({ if (it.letter == '#') 1 else 0 }, { it.label.lowercase() })
        )
    }

    private fun keyFor(component: ComponentName, user: UserHandle): String =
        component.flattenToString() + "#" + userManager.getSerialNumberForUser(user)

    fun launch(app: AppEntry) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, null, null) }
    }

    fun openAppInfo(app: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.component.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Calls [onChange] whenever an app is installed, removed or updated. */
    fun watch(onChange: () -> Unit) {
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
        }
        callback = cb
        launcherApps.registerCallback(cb, Handler(Looper.getMainLooper()))
    }

    fun stopWatching() {
        callback?.let { launcherApps.unregisterCallback(it) }
        callback = null
    }
}
