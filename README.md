# Zenith

Zenith is a calm Android **study helper and attention-management launcher** for students. It combines a focused Home surface, study timers, planning widgets, a categorized App Drawer, and lightweight behavioral friction around distracting app launches. It is designed to support studying, routines, and intentional phone use without being tied to one exam, curriculum, or age group.

Built with Jetpack Compose + Material 3, single-activity MVVM, and no third-party backend. Settings and personal planning data stay on-device in Android DataStore.

## Requirements

- Android Studio (recent Ladybug/Koala-era release or newer)
- Android SDK 34
- Android 8.0 (API 26) or newer

## Getting started

1. Open the project in Android Studio and let Gradle sync.
2. Run the `app` configuration on a device or emulator.
3. Zenith installs as a regular app first. Accept its default Home-app prompt or choose Zenith manually in Android's Default apps > Home app settings.
4. Long-press the Home key (or the device equivalent) to switch back to another launcher when needed.

## Home surface

Zenith starts with a three-column masonry layout matching the reference design:

- **Left:** Deadlines, Focus Mode, Targets, Status
- **Middle:** Study Timer, Shortcuts
- **Right:** Todo, Backlog, Library

The widget order and heights are persisted, so users can hold and drag widgets to create their own arrangement. Stable widget IDs are intentionally retained internally so existing saved layouts remain compatible even though the user-facing names have been simplified.

### Widget names

- **Deadlines** — countdowns for user-defined dates.
- **Focus Mode** — an allow-list based focused app surface.
- **Targets** — self-set progress targets.
- **Study Timer** — Pomodoro, stopwatch, and alarms.
- **Todo** — a quick daily checklist.
- **Backlog** — pending chapters, topics, or weak areas.
- **Library** — quick links to study PDFs and other reference files.
- **Shortcuts** — pinned one-tap app launches.
- **Status** — Wi-Fi, battery, and connected Bluetooth glance.

The Home header provides the live clock/date, weather when permission and connectivity are available, and rotating greeting copy. Double-tapping the Home surface can lock the screen when Device Admin is enabled.

## Attention protection

### Focus Mode

Focus Mode shows only apps explicitly allowed for focused use. The rest of the app surface is removed while the mode is active. Leaving Focus Mode includes a non-interactive reflection interval before the mode changes.

### Distractions

Apps can be marked as **Distractions**. Zenith inserts a short, non-interactive reflection before launching a marked app. The reflection deliberately does not announce the app name or explain what will happen when the interval ends; it simply creates a moment between impulse and action.

### Pomodoro

While a Pomodoro is running, the Home surface is protected: most widgets are visually softened and touch-blocked, Settings is disabled, the App Drawer edge gesture is disabled, and the timer cannot be switched to Stopwatch or Alarm. Todo, Backlog, and **Library remain outside the blur** so study notes and active planning remain accessible.

Stopping a running Pomodoro uses the same full-screen, non-interactive reflection treatment as other deliberate pause moments, with a seven-second interval. Normal Pause remains immediate. A completed Pomodoro also retains its existing transition/reflection behavior.

The visual lock uses a restrained blur rather than an extreme blur, keeping context recognizable while making the active timer the dominant surface.

## App Drawer

The App Drawer opens from the right edge and groups installed apps by user-managed categories. The built-in category set includes Study, Productivity, Communication, Social, Entertainment, Games, Media, Storage, Internet, Development, Finance, Shopping, Travel, News, Utilities, and Other. Users can add categories and reassign apps.

Long-pressing an app opens an action sheet with: 

- Pin to Home
- App info
- Available in Focus Mode
- Distraction
- Category selection across the configured drawer categories
- Uninstall when Android permits it

Focus and Distraction assignments are kept mutually exclusive: marking an app for one removes it from the other. The same controls are also available from Settings.

## Settings

Settings are grouped into focused sections:

- **Profile & Deadlines** — display name and user-managed deadline dates.
- **Appearance** — theme, font, icon pack, and Home/Lock Screen background options.
- **Widgets** — show or hide individual Home widgets.
- **Gestures & System** — double-tap lock and attention-protection strength.
- **Apps** — Focus Mode apps, Distractions, and App Drawer categories.

The widget visibility labels use the same simplified names as Home: Deadlines, Focus Mode, Study Timer, Todo, Backlog, Library, Targets, Shortcuts, and Status.

## Rearranging widgets

Widgets are locked by default. Long-press a widget to enter rearranging mode, then drag it within or across columns. The edge gestures pause while editing so they do not compete with a drag. A Done pill, empty-space tap, or Back gesture finishes editing.

## Recent Apps

Zenith keeps its own lightweight list of apps launched through Zenith, most-recent-first. Android does not expose the system's actual task snapshots/thumbnails to an ordinary launcher app, so Zenith does not pretend to provide those.

## Attention protection modes

- **Normal** — no system-UI changes.
- **Strong** — hides Android system bars during Focus Mode and reflection moments.
- **Dedicated device** — uses Android Lock Task when Zenith is provisioned as a device owner; otherwise it falls back to Strong.

A regular Android app cannot fully disable the notification shade or replace the system navigation surface. Zenith therefore uses the public system APIs available to an ordinary launcher, rather than pretending to have OS-level control it does not have.

## Architecture

```text
app/src/main/java/com/zenith/launcher/
├── data/
│   ├── local/          DataStore-backed PreferencesManager
│   ├── model/          Plain data classes/enums
│   └── repository/     AppRepository + SettingsRepository
├── service/             DeviceAdminReceiver
├── ui/
│   ├── home/            HomeScreen, HomeViewModel, widgets and drawer
│   ├── settings/        SettingsScreen, SettingsViewModel and sections
│   ├── navigation/      Home <-> Settings NavHost
│   └── theme/           Material 3 theme and typography
└── util/                 Small platform/stateless helpers
```

The Home and Settings screens consume immutable `StateFlow` snapshots. Persistence is centralized in `PreferencesManager`. UI copy is centralized in `LauncherCopy.kt` so behavioral language can evolve without scattering strings across widgets.

## Permissions

| Permission | Used for |
|---|---|
| `QUERY_ALL_PACKAGES` | Listing installed launcher apps, App Drawer content, and icon-pack matching. |
| `ACCESS_NETWORK_STATE` | Status widget connectivity indicator. |
| `SET_WALLPAPER` | Optional background/Lock Screen wallpaper synchronization. |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` | Connected Bluetooth device status on supported Android versions. |
| `ACCESS_COARSE_LOCATION` | Coarse weather lookup for the Home header. |
| `INTERNET` | Weather lookup. |
| Device Admin | Optional double-tap-to-lock action. |

## Known Android limitations

- A normal launcher cannot replace or permanently disable Android's system navigation or notification shade.
- Zenith's Recent Apps deck cannot access system task snapshots without privileged/system access.
- Lock Screen wallpaper behavior can vary on heavily customized OEM builds.
- Some Bluetooth battery-level APIs vary by device/OEM.
- Alarms are not currently re-registered automatically after a device reboot.
- Weather depends on a coarse location fix and connectivity.

## Privacy / network

Zenith has no analytics backend and no third-party account system. The only network feature is the Home weather request to the key-less Open-Meteo service; planning data and app classifications are stored locally.
