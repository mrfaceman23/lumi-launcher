# Rules for Claude on this repo

Lumi is the home screen (launcher) for Jordan's mom's Samsung Galaxy Tab S7+ (SM-T970, Android 13).
It's a side project, not part of Citadel, but it follows the same way of working:

1. **GitHub is the only source.** Start every session by cloning or pulling this repo. Never build from an old copy.
2. **Every change gets pushed.** Commit and push before handing Jordan anything, and say which commit an app file was built from.
3. **The signing key is never committed.** GitHub builds sign the app from encrypted Actions secrets (see Building).
4. **Plans live in `STATUS.md`** (live / broken / next). `CHANGELOG.md` gets one plain line per change.
5. **Plain language** in anything Jordan reads. He likes copy-and-paste PowerShell commands.
6. **Nothing ships without Jordan's "go."** Keep existing behavior unless a change is asked for.

## Who it's for
Mom is about 55 and comfortable with tech. Lumi should look polished, not like a "senior phone":
big readable tiles, bold Atkinson Hyperlegible text, strong contrast, Windows Phone / Metro style.
It must work in landscape (tile groups scroll sideways) and portrait (groups stack, scroll down).

## Where things are
- `Catalog.kt`: every tile, folder and app on the start screen. Change the layout here.
- `ui/StartScreen.kt`: the landscape and portrait layouts.
- `ui/Tiles.kt`: what each tile draws (clock, keep listening, photos, storage, help, folders, apps).
- `ui/Panels.kt`: folders that slide open, and the A to Z app list.
- `ui/SettingsScreen.kt`: Jordan's setup screen (press and hold the clock tile).

## Building
- GitHub Actions builds on every push (`.github/workflows/build.yml`), signed from secrets
  `LUMI_KEYSTORE_BASE64`, `LUMI_STORE_PASSWORD`, `LUMI_KEY_ALIAS`, `LUMI_KEY_PASSWORD`.
  Each new `versionName` gets a signed GitHub Release that Obtainium on the tablet picks up.
  Without the secrets the artifact ends in `-UNSIGNED` and no Release is made.
- `./gradlew testDebugUnitTest` before pushing. `./gradlew assembleDebug` needs Android SDK 35, JDK 17 to 21.
- `LUMI_SCREENSHOTS=1 ./gradlew testDebugUnitTest --tests '*ScreenshotTest*'` renders the start screen
  to `screenshots/` (landscape and portrait) without a device. Those pictures aren't committed.
- Bump `versionCode` and `versionName` in `app/build.gradle.kts` for every build Jordan installs.
- Install from Jordan's PC: `adb install -r <apk>` with `$env:ANDROID_SERIAL = "R52NA0VCWNT"` set
  (his watch is sometimes connected too).
