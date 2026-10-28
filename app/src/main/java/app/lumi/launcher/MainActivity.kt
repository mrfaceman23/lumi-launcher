package app.lumi.launcher

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import app.lumi.launcher.ui.AllAppsScreen
import app.lumi.launcher.ui.DarkPalette
import app.lumi.launcher.ui.FolderPanel
import app.lumi.launcher.ui.LightPalette
import app.lumi.launcher.ui.LocalMotionOn
import app.lumi.launcher.ui.LocalPalette
import app.lumi.launcher.ui.LocalShowCount
import app.lumi.launcher.ui.SettingsScreen
import app.lumi.launcher.ui.StartScreen
import app.lumi.launcher.ui.systemAllowsMotion

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private var isDefaultHome by mutableStateOf(true)
    private var onScreen by mutableStateOf(false)

    /** First run: one prompt for location (weather) and photos (Photos tile). */
    private val firstRunPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            vm.refreshWeather(force = true)
            vm.refreshPhotos()
        }

    private val photoPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) vm.refreshPhotos() else vm.toast("Photos stay off. You can turn them on in Android settings.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // A launcher never "goes back" anywhere. Back closes whatever is open on top.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                vm.closeTop()
            }
        })

        val versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: ""

        setContent {
            val palette = if (vm.light) LightPalette else DarkPalette
            // Status bar icons dark on the white look, light on black.
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = vm.light
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = vm.light
            val motion = systemAllowsMotion() && onScreen
            MaterialTheme(colorScheme = if (vm.light) lightColorScheme() else darkColorScheme()) {
                CompositionLocalProvider(
                    LocalPalette provides palette,
                    LocalShowCount provides vm.showCount,
                    LocalMotionOn provides motion,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        StartScreen(
                            vm = vm,
                            isDefaultHome = isDefaultHome,
                            onMakeDefault = ::openHomeSettings,
                            onAskPhotos = { photoPermission.launch(Manifest.permission.READ_MEDIA_IMAGES) },
                        )
                        FolderPanel(vm)
                        AllAppsScreen(vm)
                        SettingsScreen(vm, ::openHomeSettings, versionName)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onScreen = true
        isDefaultHome = checkDefaultHome()
        vm.onShown()
        if (!vm.prefs.askedPermissions) {
            vm.prefs.askedPermissions = true
            firstRunPermissions.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_MEDIA_IMAGES)
            )
        }
    }

    override fun onPause() {
        onScreen = false
        vm.onHidden()
        super.onPause()
    }

    /** Pressing Home while already on Lumi closes folders and lists, like any home screen. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) vm.closeAll()
    }

    private fun checkDefaultHome(): Boolean = runCatching {
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    }.getOrDefault(true)

    private fun openHomeSettings() {
        vm.startIntent(Intent(Settings.ACTION_HOME_SETTINGS), "Open Android settings, then Apps, then Default apps")
    }
}
