# Lumi

The home screen on Mom's Galaxy Tab S7+. Big Windows Phone style tiles, folders that slide open,
and a few live tiles (clock and weather, the audiobook she's on, her photos, storage space).

## Install on the tablet (first time)
1. Download the latest `Lumi-....apk` from **Releases** on this page.
2. Plug the tablet into the PC and run in PowerShell (change the path to where the file landed):
   ```powershell
   $env:ANDROID_SERIAL = "R52NA0VCWNT"
   adb install -r "$env:USERPROFILE\Downloads\Lumi-0.1.0-xxxxxxx.apk"
   adb shell cmd package set-home-activity app.lumi.launcher/.MainActivity
   ```
   The last line makes Lumi the home screen. Press the tablet's Home button to see it.
3. Allow location (for weather) and photos when it asks.

## Updates
Add this repo to Obtainium on the tablet. New versions show up there after each Release.

## Setup (press and hold the clock tile)
- **Call for help:** your name and WhatsApp number (with the 1 in front).
- **Coder's Desk:** the link, so it shows in the School folder.
- **Audiobookshelf:** server address and her API token, for the Keep listening tile.
- **Look:** black or white background.

## Changing tiles
Everything on the start screen is listed in `app/src/main/java/app/lumi/launcher/Catalog.kt`.

## Going back to Samsung's home screen
```powershell
adb shell cmd package set-home-activity com.sec.android.app.launcher/.activities.LauncherActivity
```
