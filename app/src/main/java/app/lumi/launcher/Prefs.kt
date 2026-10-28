package app.lumi.launcher

import android.content.Context

/** The few things Jordan sets up once in the hidden settings screen. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("lumi", Context.MODE_PRIVATE)

    /** White background instead of black. */
    var lightMode: Boolean
        get() = sp.getBoolean("light", false)
        set(v) = sp.edit().putBoolean("light", v).apply()

    /** Phone number the help tile messages on WhatsApp, digits only with country code (1 for the US). */
    var helpNumber: String
        get() = sp.getString("help_number", "").orEmpty()
        set(v) = sp.edit().putString("help_number", v.filter { it.isDigit() }).apply()

    /** Who the help tile reaches, shown on the tile ("Reaches Jordan"). */
    var helpName: String
        get() = sp.getString("help_name", "").orEmpty()
        set(v) = sp.edit().putString("help_name", v.trim()).apply()

    /** Link to her Coder's Desk study app. Shows up in the School folder when set. */
    var codersDeskUrl: String
        get() = sp.getString("coders_desk", "").orEmpty()
        set(v) = sp.edit().putString("coders_desk", v.trim()).apply()

    /** Audiobookshelf server on the NAS, e.g. http://192.168.1.82:13378 */
    var absUrl: String
        get() = sp.getString("abs_url", "").orEmpty()
        set(v) = sp.edit().putString("abs_url", v.trim().trimEnd('/')).apply()

    /** Her Audiobookshelf API token (Settings → Users → her account → API token). */
    var absToken: String
        get() = sp.getString("abs_token", "").orEmpty()
        set(v) = sp.edit().putString("abs_token", v.trim()).apply()

    /** Asked for location and photo access once already. */
    var askedPermissions: Boolean
        get() = sp.getBoolean("asked_perms", false)
        set(v) = sp.edit().putBoolean("asked_perms", v).apply()
}
