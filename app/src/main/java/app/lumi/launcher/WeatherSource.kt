package app.lumi.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Today's weather where you are. */
data class Weather(
    val tempF: Int,
    val code: Int,
    val isDay: Boolean,
    val highF: Int?,
    val lowF: Int?,
    val rainChance: Int?,
    val city: String?,
    val fetchedAt: Long,
) {
    enum class Mood { SUNNY, CLOUDY, RAIN, SNOW, STORM, FOG, NIGHT }

    val description: String
        get() = when (code) {
            0 -> if (isDay) "sunny" else "clear"
            1 -> if (isDay) "mostly sunny" else "mostly clear"
            2 -> "partly cloudy"
            3 -> "cloudy"
            45, 48 -> "foggy"
            in 51..57 -> "drizzle"
            in 61..67 -> "rain"
            in 71..77 -> "snow"
            in 80..82 -> "showers"
            85, 86 -> "snow showers"
            in 95..99 -> "thunderstorms"
            else -> "—"
        }

    val mood: Mood
        get() = when {
            code in 95..99 -> Mood.STORM
            code in 51..67 || code in 80..82 -> Mood.RAIN
            code in 71..77 || code == 85 || code == 86 -> Mood.SNOW
            code == 45 || code == 48 -> Mood.FOG
            !isDay -> Mood.NIGHT
            code <= 1 -> Mood.SUNNY
            else -> Mood.CLOUDY
        }

    fun toJson(): String = JSONObject().apply {
        put("t", tempF); put("c", code); put("d", isDay)
        highF?.let { put("h", it) }; lowF?.let { put("l", it) }; rainChance?.let { put("r", it) }
        city?.let { put("city", it) }; put("at", fetchedAt)
    }.toString()

    companion object {
        fun fromJson(s: String): Weather? = runCatching {
            val o = JSONObject(s)
            Weather(
                tempF = o.getInt("t"), code = o.getInt("c"), isDay = o.getBoolean("d"),
                highF = if (o.has("h")) o.getInt("h") else null,
                lowF = if (o.has("l")) o.getInt("l") else null,
                rainChance = if (o.has("r")) o.getInt("r") else null,
                city = if (o.has("city")) o.getString("city") else null,
                fetchedAt = o.getLong("at"),
            )
        }.getOrNull()
    }
}

/** Town name for an address (falls back to county, then state). */
private fun placeName(a: Address): String? = a.locality ?: a.subAdminArea ?: a.adminArea

/**
 * Gets your rough location (city-level) and asks Open-Meteo (free, no account) for the weather.
 * Location is rounded to about 1 km before it leaves the phone.
 */
class WeatherSource(private val context: Context) {

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun load(): Weather? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        val loc = location() ?: return@withContext null
        val lat = (loc.latitude * 100).roundToInt() / 100.0
        val lon = (loc.longitude * 100).roundToInt() / 100.0
        val city = cityName(lat, lon)
        fetch(lat, lon, city)
    }

    @SuppressLint("MissingPermission")
    private suspend fun location(): Location? {
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        val last = providers
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
        // A location from the last 3 hours is good enough for weather.
        if (last != null && System.currentTimeMillis() - last.time < 3 * 60 * 60 * 1000L) return last

        val provider = when {
            runCatching { lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) }.getOrDefault(false) -> LocationManager.FUSED_PROVIDER
            runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
            else -> return last
        }
        val fresh = withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc -> if (cont.isActive) cont.resume(loc) }
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
        return fresh ?: last
    }

    private suspend fun cityName(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.US)
        return withTimeoutOrNull(5_000) {
            if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine<String?> { cont ->
                    runCatching {
                        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull()?.let(::placeName))
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }.onFailure { if (cont.isActive) cont.resume(null) }
                }
            } else {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()?.let(::placeName) }.getOrNull()
            }
        }
    }

    private fun fetch(lat: Double, lon: Double, city: String?): Weather? = runCatching {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&temperature_unit=fahrenheit&timezone=auto&forecast_days=1"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 6_000
        }
        try {
            if (conn.responseCode != 200) return@runCatching null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val o = JSONObject(body)
            val cur = o.getJSONObject("current")
            val daily = o.optJSONObject("daily")
            fun firstInt(key: String): Int? =
                daily?.optJSONArray(key)?.takeIf { it.length() > 0 && !it.isNull(0) }?.getDouble(0)?.roundToInt()
            Weather(
                tempF = cur.getDouble("temperature_2m").roundToInt(),
                code = cur.getInt("weather_code"),
                isDay = cur.optInt("is_day", 1) == 1,
                highF = firstInt("temperature_2m_max"),
                lowF = firstInt("temperature_2m_min"),
                rainChance = firstInt("precipitation_probability_max"),
                city = city,
                fetchedAt = System.currentTimeMillis(),
            )
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}
