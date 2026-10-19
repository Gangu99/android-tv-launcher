# android-tv-launcher

Debloat scripts + a custom Apple TV-style home screen for Android TV / Google TV. No root; everything over ADB on the local network. README.md has the user-facing steps; this file is for coding agents.

## Layout
- `debloat/debloat.sh <tv-ip> [list]` hides packages with `pm uninstall -k --user 0` (reversible). `restore.sh <tv-ip> [pkg]` uses `cmd package install-existing`; with no package it restores the whole list and re-enables `com.google.android.tvlauncher`. Lists: one package per line, `#` comments. `adb shell -n` is required inside the `while read` loops, or adb swallows the rest of the list from stdin.
- `launcher/`: Kotlin + Jetpack Compose for TV (`androidx.tv:tv-material`), `androidx.palette` for the glow colour. AGP 8.10 / Gradle 8.11.1 / Kotlin 2.0.21, JDK 17+. minSdk 28.
  - `Apps.kt`: loads LEANBACK_LAUNCHER apps, banners and glow colour; `FAVOURITES` is the favourites row order.
  - `HomeScreen.kt`: header (greeting, clock, date, Wi-Fi + Settings pills), favourites shelf, all-apps shelf.
  - `MainActivity.kt`: HOME activity; reloads on package changes; Home re-focuses the first favourite; Back is swallowed.
  - `Hotword.kt`: `HotwordService`, a microphone-type foreground service that binds Google Assistant (`com.google.android.katniss`, action `com.google.android.apps.tvsearch.voice.search.KVS_SERVICE`) with BIND_INCLUDE_CAPABILITIES, so "Ok Google" keeps mic access after the stock launcher is disabled. Without it logcat shows `Request denied by app op: 27`.

## Build & deploy
```
cd launcher && ./gradlew assembleRelease     # needs ANDROID_HOME or local.properties (sdk.dir)
S=<tv-ip>:5555
adb -s $S install -r app/build/outputs/apk/release/app-release.apk
adb -s $S shell pm grant me.prince.launcher android.permission.ACCESS_COARSE_LOCATION
adb -s $S shell pm grant me.prince.launcher android.permission.RECORD_AUDIO
adb -s $S shell cmd package compile -m speed -f me.prince.launcher
adb -s $S shell cmd package set-home-activity me.prince.launcher/.MainActivity
```
Always ship release (R8); debug Compose is too slow on TV hardware.

## Rules when working on a user's TV
- Ask before removing any package. Never remove Live TV, `com.mediatek.*` / other chip services, the remote's Bluetooth service, `com.android.tv.settings`, Play Services, the keyboard or setup wizards.
- Disable (never uninstall) the stock launcher, and only after the custom launcher is installed and verified, otherwise Home shows a black screen. Fallback: `pm enable com.google.android.tvlauncher`.
- Test by screenshotting: `adb shell screencap -p /sdcard/s.png` then `adb pull` (some TVs print vendor log lines into `exec-out` output, corrupting the PNG).
- Before sending key events (`input keyevent`), check `dumpsys activity activities | grep mResumedActivity`: if a video app or TV input is in front, someone may be watching.

## Gotchas already handled in code
- Compose for TV pins the focused item 30% from the edge, which scrolls the header off-screen; `minimalScroll()` (a custom BringIntoViewSpec) only scrolls when an item would leave the screen.
- On cold start the first tile isn't attached for a few frames; focus is requested with a short retry loop, otherwise Android focuses an arbitrary element.
- The Wi-Fi name needs ACCESS_COARSE_LOCATION; VPN ad blockers make the VPN the active network, so the label checks all networks.
- LazyRow clips to a rectangle; shelves are clipped to their rounded shape and fade at the edge that has more to scroll.
