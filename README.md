# Zenith Launcher

A calm, distraction-light Android home screen built for exam aspirants (JEE and similar) - a
widget-driven Home screen (study timer, exam countdowns, focus mode, to-do list, chapter backlog,
quick PDF links, pinned app shortcuts, a system status glance) plus a categorized App Drawer and a
custom Recent Apps deck, all skinned to match a single wallpaper across Home, the Drawer, the
Recents deck, and (optionally) the actual Lock Screen.

Built with Jetpack Compose + Material 3, single-activity, MVVM, no third-party backend - every
setting lives in local `DataStore` on-device.

## Requirements

- Android Studio (a recent Ladybug/Koala-era release or newer)
- Android SDK 34 installed
- A device or emulator running **Android 8.0 (API 26) or newer**

## Getting started

1. Open this folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on a device or emulator.
3. The app installs as a regular app first - to actually use it as your Home screen, either:
   - accept the "Set as default Home app" prompt Zenith shows on first launch, or
   - do it manually later via Settings > Apps > Default apps > Home app.
4. Long-press the Home key (or your device's equivalent) any time to switch back to your
   previous launcher if you want to stop using Zenith temporarily.

There is no build variant or flavor setup required - one `app` module, one APK.

## Feature tour

### Home screen
- **3-column widget grid**, hold-and-drag to rearrange (see "Rearranging widgets" below).
- **Widgets**: Exam Countdown, Focus Mode toggle, Milestone/Target (a goal for your next mock
  test with a readiness gauge), Study Timer (Pomodoro + stopwatch), Daily To-Do, Chapter Backlog,
  PDF quick-launch, App Shortcuts (pin any app for one-tap access), and a Status glance (Wi-Fi,
  battery, connected Bluetooth device).
- **Greeting header**: live clock + date, rotates through a few greeting phrases daily.
- **Double-tap the header** to lock the screen (requires granting Device Admin once - see
  Permissions below).
- **Right-edge swipe** opens the App Drawer. **Left-edge swipe** opens the Recent Apps deck.

### App Drawer
Every installed app, grouped under categories (Study / Games / Social / Entertainment / Other -
fully user-assignable, not guessed). Long-press an app for a quick-action sheet: pin to Home,
open its system App Info page, move it to a different category, or uninstall it.

### Recent Apps deck
A launcher has no access to the system's actual task snapshots/thumbnails (that requires system
privilege Android doesn't grant third-party apps) - so rather than fake that, this shows a clean,
fast list of apps you've actually launched through Zenith, most-recent-first, styled with the
same glass cards and wallpaper as everywhere else. Removing an entry only forgets it from this
list; it can't force-stop the app.

### Settings
Organized as a category menu (Profile & Exams, Appearance, Widgets, Gestures & System, Apps)
rather than one long scrolling page:

- **Profile & Exams**: your display name, and a fully user-managed list of exams (add as many as
  you like, rename, set/change each one's target date, remove).
- **Appearance**: dark/light theme, font (Classic/Elegant/Playful/Technical), icon pack, and
  Home background (any photo or a flat color, with an option to also apply it as your actual
  Lock Screen wallpaper).
- **Widgets**: show/hide any widget on the grid.
- **Gestures & System**: double-tap-to-lock (Device Admin), and a note on the no-permission
  Recent Apps gesture.
- **Apps**: Focus Mode's allow-list, and bulk App Drawer category assignment.

### Rearranging widgets
Widgets are locked by default. Long-press any widget to pick it up - this also puts the whole
grid into a "rearranging" state (a "Done rearranging" pill appears, and the edge swipes for the
Drawer/Recents pause) so dragging doesn't fight with anything else. Tap "Done", tap empty grid
space, or press back to finish.

## Architecture

```
app/src/main/java/com/zenith/launcher/
├── data/
│   ├── local/          DataStore-backed PreferencesManager (single source of persisted state)
│   ├── model/           Plain data classes/enums - no Compose or Android framework dependency
│   └── repository/      AppRepository (PackageManager access) + SettingsRepository (facade over
│                        PreferencesManager)
├── service/             DeviceAdminReceiver for the lock-screen gesture
├── ui/
│   ├── home/            HomeScreen + HomeViewModel + every Home widget/overlay composable
│   ├── settings/        SettingsScreen + SettingsViewModel + every settings section composable
│   ├── navigation/       Two-destination NavHost: Home <-> Settings
│   └── theme/            Material3 theme, typography (parameterized by the chosen font)
└── util/                 Small stateless helpers (bitmap downsampling, wallpaper sync, icon pack
                          resolution, countdown math, default-launcher prompt, lock/admin helper)
```

- **State flow**: each screen has one `ViewModel` exposing a single `StateFlow<UiState>` built by
  combining every relevant `Flow` from `SettingsRepository`/`AppRepository`. Composables read that
  one state object and never touch the repository/DataStore layer directly.
- **Persistence**: everything is a value in a single `DataStore<Preferences>` (see
  `PreferencesManager`) - lists/maps are stored as JSON strings via `kotlinx.serialization`.
- **No network, no analytics, no third-party backend.** Every permission below exists to talk to
  the Android OS directly, not to any external service.

## Permissions this app requests, and why

| Permission | Used for |
|---|---|
| `QUERY_ALL_PACKAGES` | Listing every installed app for the Home grid, App Drawer, and icon-pack matching. Google Play grants launcher apps an explicit policy exception for this. |
| `ACCESS_NETWORK_STATE` | The Status widget's Wi-Fi connected/disconnected indicator. |
| `SET_WALLPAPER` | Syncing your chosen Home background photo to the actual system/Lock Screen wallpaper (opt-in toggle in Settings). |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` | The Status widget's "connected Bluetooth device" row. Requested at runtime on Android 12+; the app works fine if you deny it, that row just stays hidden. |
| Device Admin (via `ZenithDeviceAdminReceiver`) | Only requested if you turn on "double-tap to lock" in Settings > Gestures. Used solely for `DevicePolicyManager.lockNow()` - the app requests no other admin policy (no password rules, no wipe, nothing else). |

Nothing here is requested at install time except `QUERY_ALL_PACKAGES`/`ACCESS_NETWORK_STATE`/
`SET_WALLPAPER` (normal permissions); Bluetooth and Device Admin are both opt-in, asked for only
when you turn on the feature that needs them.

## Known limitations

- **No custom navigation bar.** A launcher cannot replace or disable Android's system 3-button/
  gesture navigation - that's OS-level. What's built here instead: Home is handled automatically
  by being the default launcher, system Back works as normal everywhere, and "Recents" is
  Zenith's own in-app deck (see above) rather than a hook into the system Overview screen.
- **Recent Apps shows icons, not live thumbnails.** Third-party apps can't read the system's
  actual task snapshots without system-level privilege.
- **Lock Screen wallpaper sync** uses the public `WallpaperManager` API; a handful of heavily
  OEM-skinned devices (some Samsung/Xiaomi builds in particular) are known to handle
  `FLAG_LOCK` inconsistently. Home background always works regardless.
- **Bluetooth battery level** is read via a hidden, undocumented `BluetoothDevice` method that
  exists on stock Android but isn't guaranteed by every OEM - if it's unavailable, the Status
  widget just shows the device name without a battery percentage.
- **Custom font upload** (importing your own `.ttf`/`.otf`) isn't implemented yet - Settings >
  Appearance currently offers four built-in type faces (Classic, Elegant, Playful, Technical).

## Low-memory considerations

Background photos are decoded at roughly screen resolution (not full camera resolution) via
`BitmapUtils`, using `RGB_565` instead of the default `ARGB_8888` config, and that one decoded
bitmap is shared across Home, the App Drawer, and the Recent Apps deck rather than each screen
decoding its own copy. This is the single biggest avoidable memory cost in an app like this, so
if you're profiling for a low-RAM target device, start there.
