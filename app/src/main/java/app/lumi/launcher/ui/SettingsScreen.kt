package app.lumi.launcher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumi.launcher.LauncherViewModel

/** Jordan's setup screen. Opened by pressing and holding the clock tile. */
@Composable
fun SettingsScreen(vm: LauncherViewModel, onMakeDefault: () -> Unit, versionName: String) {
    val pal = LocalPalette.current
    AnimatedVisibility(vm.settingsOpen, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        var helpName by remember { mutableStateOf(vm.prefs.helpName) }
        var helpNumber by remember { mutableStateOf(vm.prefs.helpNumber) }
        var coders by remember { mutableStateOf(vm.prefs.codersDeskUrl) }
        var absUrl by remember { mutableStateOf(vm.prefs.absUrl) }
        var absToken by remember { mutableStateOf(vm.prefs.absToken) }

        fun save() {
            vm.prefs.helpName = helpName
            vm.prefs.helpNumber = helpNumber
            vm.prefs.codersDeskUrl = coders
            vm.prefs.absUrl = absUrl
            vm.prefs.absToken = absToken
            vm.refreshListening()
        }

        Column(
            Modifier.fillMaxSize().background(pal.background).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Lumi settings", color = pal.text, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                Button("Save and close", Color(0xFF0A5BB5)) { save(); vm.settingsOpen = false; vm.toast("Saved") }
            }

            Heading("Look")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Switch(checked = vm.light, onCheckedChange = { vm.changeLook(it) })
                Body(if (vm.light) "White background" else "Black background (Lumia style)")
            }

            Heading("Call for help tile")
            Field("Name on the tile (for example, Jordan)", helpName) { helpName = it }
            Field("WhatsApp number, with country code (1 for the US)", helpNumber, KeyboardType.Phone) { helpNumber = it }

            Heading("School folder")
            Field("Coder's Desk link", coders, KeyboardType.Uri) { coders = it }

            Heading("Audiobookshelf (Keep listening tile)")
            Field("Server address, like http://192.168.1.82:13378", absUrl, KeyboardType.Uri) { absUrl = it }
            Field("Her API token", absToken, KeyboardType.Password, secret = true) { absToken = it }
            Button("Save and test", Color(0xFF5C2D91)) {
                save()
                vm.toast(if (vm.listenSource.isSetUp) "Checking… the tile updates in a few seconds" else "Add both the address and the token first")
            }

            Heading("Tablet")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button("Make Lumi the home screen", Color(0xFF217A3A), onMakeDefault)
                Button("Android settings", Color(0xFF3D4651)) {
                    vm.startIntent(Intent(Settings.ACTION_SETTINGS), "Couldn't open settings")
                }
            }
            Body("Lumi $versionName", dim = true)
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, color = LocalPalette.current.text, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 22.sp,
        modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Body(text: String, dim: Boolean = false) {
    Text(text, color = if (dim) LocalPalette.current.textDim else LocalPalette.current.text, fontFamily = Atkinson, fontSize = 19.sp)
}

@Composable
private fun Button(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text, color = Color.White, fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 18.sp,
        modifier = Modifier.pressable(onClick).background(color).padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    type: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    onChange: (String) -> Unit,
) {
    val pal = LocalPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontFamily = Atkinson) },
        singleLine = true,
        textStyle = TextStyle(fontFamily = Atkinson, fontSize = 19.sp, color = pal.text),
        keyboardOptions = KeyboardOptions(keyboardType = type),
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = pal.text, unfocusedTextColor = pal.text,
            focusedLabelColor = pal.text, unfocusedLabelColor = pal.textDim,
            focusedBorderColor = pal.text, unfocusedBorderColor = pal.line, cursorColor = pal.text,
        ),
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
    )
}
