package app.lumi.launcher

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** How full one storage space is. */
data class Space(val label: String, val totalBytes: Long, val freeBytes: Long) {
    val usedFraction: Float get() = if (totalBytes <= 0) 0f else 1f - freeBytes.toFloat() / totalBytes
    val level: Level
        get() = when {
            usedFraction >= 0.90f -> Level.FULL
            usedFraction >= 0.80f -> Level.WARN
            else -> Level.OK
        }

    enum class Level { OK, WARN, FULL }
}

/** Internal storage plus the SD card, if one is in. */
class StorageSource(private val context: Context) {
    fun read(): List<Space> {
        val out = mutableListOf<Space>()
        runCatching {
            val s = StatFs(Environment.getDataDirectory().path)
            out += Space("Tablet", s.totalBytes, s.availableBytes)
        }
        runCatching {
            val sm = context.getSystemService(StorageManager::class.java)
            sm.storageVolumes
                .filter { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }
                .forEach { vol ->
                    val dir = vol.directory ?: return@forEach
                    val s = StatFs(dir.path)
                    out += Space("SD card", s.totalBytes, s.availableBytes)
                }
        }
        return out
    }
}

/** Her most recent pictures, for the Photos tile. Reads only small previews. */
class PhotoSource(private val context: Context) {
    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED

    suspend fun recent(limit: Int = 12): List<ImageBitmap> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val out = mutableListOf<ImageBitmap>()
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(MediaStore.Images.Media._ID), null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (c.moveToNext() && out.size < limit) {
                    val item = ContentUris.withAppendedId(uri, c.getLong(idCol))
                    runCatching {
                        context.contentResolver.loadThumbnail(item, Size(720, 720), null).asImageBitmap()
                    }.getOrNull()?.let { out += it }
                }
            }
        }
        out
    }
}

/** The book she's in the middle of, from Audiobookshelf on the NAS. */
data class Listening(
    val title: String,
    val author: String?,
    val progress: Float,
    val cover: ImageBitmap?,
)

class ListenSource(private val prefs: Prefs) {
    val isSetUp: Boolean get() = prefs.absUrl.isNotBlank() && prefs.absToken.isNotBlank()

    suspend fun load(): Listening? = withContext(Dispatchers.IO) {
        if (!isSetUp) return@withContext null
        runCatching {
            val me = JSONObject(get("/api/me") ?: return@runCatching null)
            val progress = me.optJSONArray("mediaProgress") ?: return@runCatching null
            // The most recently played book she hasn't finished.
            var best: JSONObject? = null
            for (i in 0 until progress.length()) {
                val p = progress.getJSONObject(i)
                if (p.optBoolean("isFinished") || p.optBoolean("hideFromContinueListening")) continue
                if (best == null || p.optLong("lastUpdate") > best.optLong("lastUpdate")) best = p
            }
            val pick = best ?: return@runCatching null
            val id = pick.getString("libraryItemId")
            val item = JSONObject(get("/api/items/$id") ?: return@runCatching null)
            val meta = item.optJSONObject("media")?.optJSONObject("metadata")
            Listening(
                title = meta?.optString("title")?.takeIf { it.isNotBlank() } ?: "Your book",
                author = meta?.optString("authorName")?.takeIf { it.isNotBlank() },
                progress = pick.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
                cover = cover(id),
            )
        }.getOrNull()
    }

    private fun open(path: String): HttpURLConnection =
        (URL(prefs.absUrl + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            setRequestProperty("Authorization", "Bearer " + prefs.absToken)
        }

    private fun get(path: String): String? {
        val conn = open(path)
        return try {
            if (conn.responseCode != 200) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun cover(id: String): ImageBitmap? = runCatching {
        val conn = open("/api/items/$id/cover?width=400")
        try {
            if (conn.responseCode != 200) null
            else conn.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}
