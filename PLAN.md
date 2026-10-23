# Seeker Display — Implementation Plan

A landscape, always-on dashboard app for the Solana Seeker (Android). It shows a clock, the weather,
upcoming calendar events, and what's playing on Spotify, with two fullscreen "focus" views.

Follow `CLAUDE.md` for every commit and PR (format: `<Type>[<Scope>]: <Subject>`, with each
part capitalized, e.g. `Feat[Weather]: Add hourly forecast row`). Make sure the build passes
(`./gradlew assembleDebug`) before each commit. Commit at the end of each phase below.

---

## 0. Decisions already made

| Topic | Decision |
|---|---|
| Platform | Native Android, Kotlin, Jetpack Compose, single Activity |
| Orientation | Landscape locked, immersive fullscreen, `FLAG_KEEP_SCREEN_ON` |
| Time format | **24-hour** everywhere (`21:24`, hourly labels `22`, `00`, `02`…) |
| Location | **GPS / network based** (coarse is enough), last fix cached |
| Weather | Open-Meteo (no API key) |
| Calendar | Android `CalendarContract` (user adds each Gmail account to the phone; system sync does the rest) |
| Spotify | **User has no Premium.** The Spotify Web API can't be used: since Feb 2026, Development Mode apps require the app owner to have Premium, and playback control has always required Premium. Instead, use two free sources: (1) **Android MediaSession** for playback on the phone itself, and (2) **a small Mac companion "bridge"** that reads and controls the Spotify desktop app through AppleScript and exposes it on the LAN. |
| DI | Manual (`AppContainer` held by the `Application`), no Hilt |
| Networking | Ktor client (OkHttp engine) + kotlinx.serialization |
| Persistence | DataStore (Preferences) |
| Images | Coil 3 (album art URLs from the bridge) |
| Colours | `androidx.palette` |

### Tooling notes
- Package/applicationId: `com.kashsuks.seekerdisplay`
- `minSdk 31`, `targetSdk`/`compileSdk` = latest stable. The Seeker runs Android 15.
- Use a Gradle version catalog (`gradle/libs.versions.toml`), Kotlin 2.x, and the Compose BOM.
- The Mac has system JDK 24. For command-line Gradle, set `JAVA_HOME` to Android Studio's bundled JBR
  (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) so the JDK doesn't conflict with Gradle/AGP.
- Add an Android `.gitignore` (plus `.DS_Store`, `local.properties`, `/build`, `.idea/`, etc.).

---

## 1. Screens

### A. Dashboard (default)
```
┌──────────────────────────────┬──────────────────────────────────┐
│ SATURDAY            (red)    │ WATERLOO (blue)          ☾       │
│ 21:24              (huge)    │ 9°                     Clear     │
│ October 3          (grey)    │                    H:16° L:5°    │
│                              │                 Feels Like: 6°   │
│                              │ Now  22  00  02  04  06  08      │
│ ┌──────┐ Song title          │  ☾   ☾   ☾   ☁   ☁   ☁   ☀       │
│ │ art  │ Artist              │  9°  8°  7°  6°  6°  5°  5°      │
│ └──────┘ ━━━━━━━━━━───────   │ UP NEXT (red)                    │
│          1:42         3:30   │ ▌MATH 137 (LEC)          (large) │
│                              │ ▌Monday, 12:30 – 13:20           │
│                              │ ▌MATH 135 (LEC)            Mon   │
│                              │ ▌YSWS Start of Week Mee…   Mon   │
└──────────────────────────────┴──────────────────────────────────┘
```
- The background is pure black (OLED). The font is bold and rounded (Inter, or a free SF Pro Rounded look-alike
  bundled in `res/font`).
- **Left column:** weekday (uppercase, red), time (very large, light blue/white), date (`MMMM d`, grey).
  The Now Playing mini-card goes below. If nothing is playing, the card is hidden and the clock block
  centers vertically in the column (animated).
- **Right column:** weather block on top, hourly row, then Up Next.
- **Tap the time** → Focus Clock. **Tap the mini-card** → Now Playing.
  **Long-press anywhere** → Settings.

### B. Focus Clock
- Fullscreen split-flap clock `HH:MM` (24h). An optional seconds pair can be turned on in Settings (default off).
- Each digit is a `FlipDigit` composable: a card split into top and bottom halves. When the value changes:
  1. The top half of the old digit rotates `rotationX` 0 → -90° (pivot at the bottom edge).
  2. The bottom half of the new digit rotates 90° → 0 (pivot at the top edge).
  3. Total ≈ 500–600 ms with ease-in for the first half and ease-out with a tiny overshoot for the second.
     A darkening gradient is applied to the rotating half for depth, and a thin hinge line goes across the middle.
  - Use `graphicsLayer { rotationX; cameraDistance = 12 * density }` and clip each half with a custom `Shape`.
  - Only the digits that change animate.
- Show the weekday and date small below the clock. Tap or back → return to the dashboard. Use the
  `SharedTransitionLayout` + `sharedBounds` animation for the clock text.

### C. Now Playing
- Fullscreen. Large square album art on the left (rounded 16dp, soft shadow). On the right: title (large,
  bold, marquee if too long), artist, the source/device label ("On this phone" / "On MacBook"), a
  progress bar with elapsed/remaining times, and controls: previous, play/pause (large), next.
  Shuffle and repeat are shown only if the active source supports them (the Mac bridge does).
- **Background:** extract a `Palette` from the album art bitmap. Use the dominant + vibrant/muted swatches to build
  a slow-drifting linear/radial gradient (animate the gradient offset over ~20 s, looping). Crossfade colours over
  ~800 ms when the track changes. Pick white or near-black text from swatch luminance (WCAG contrast ≥ 4.5).
  Darken the gradient slightly so it stays easy on the eyes at night.
- Tapping the seek bar seeks. Tapping empty space or pressing back returns to the dashboard.
  Shared-element transition: the mini-card art grows into the large art.

### D. Settings (long-press)
A simple scrolling list:
- Permissions status rows with "Grant" buttons: Location, Calendar, Notification access.
- Calendars: every calendar from `CalendarContract.Calendars`, grouped by account, with an on/off switch
  each (colour dot shown). Saved in DataStore as a set of hidden calendar IDs (default: all visible).
- Mac bridge: discovered host (or manual `host:port`), token field, and a "Test connection" button.
- Focus clock: show seconds toggle.
- Night dim: on/off + start/end time (default 23:00–07:00).

---

## 2. Architecture

```
app/src/main/java/com/kashsuks/seekerdisplay/
  SeekerApp.kt                 Application; builds AppContainer
  MainActivity.kt              immersive, keep-screen-on, landscape, NavHost/AnimatedContent
  di/AppContainer.kt
  core/
    TimeTicker.kt              Flow<ZonedDateTime> aligned to minute (and a seconds variant)
    BurnInShift.kt             Modifier that shifts content ±6dp every ~3 min, animated
    NightDim.kt                adjusts window.attributes.screenBrightness in the night window
    Result.kt / UiState.kt     Loading / Data / Error wrapper
  data/
    settings/SettingsRepository.kt         DataStore
    weather/OpenMeteoApi.kt, WeatherRepository.kt, WeatherCode.kt (WMO → icon + label)
    location/LocationProvider.kt           LocationManager.getCurrentLocation + cached last fix
    calendar/CalendarRepository.kt         CalendarContract queries + ContentObserver
    media/
      PlaybackSnapshot.kt                  common model
      MediaSource.kt                       interface
      LocalMediaSessionSource.kt           phone playback (MediaSession)
      MacBridgeSource.kt                   laptop playback (HTTP to companion)
      BridgeDiscovery.kt                   NsdManager discovery of _seekerspotify._tcp
      PlaybackRepository.kt                merges sources, picks the active one
      MediaListenerService.kt              NotificationListenerService (required for session access)
  ui/
    theme/                     colours, typography, rounded font
    dashboard/  DashboardScreen, ClockPanel, NowPlayingCard, WeatherPanel, HourlyRow, UpNextPanel
    focusclock/ FocusClockScreen, FlipDigit
    nowplaying/ NowPlayingScreen, PaletteBackground, PlaybackControls, SeekBar
    settings/   SettingsScreen
    DashboardViewModel.kt      combines all repos into one UiState
companion/
  spotify_bridge.py            Mac bridge (Python 3 stdlib only)
  com.kashsuks.spotifybridge.plist   launchd agent template
  README.md                    setup instructions
```

Each repository exposes a `StateFlow`. Polling and observers run only while the Activity is at least STARTED
(`repeatOnLifecycle` / `SharingStarted.WhileSubscribed(5_000)`).

---

## 3. Data sources

### 3.1 Clock
- `TimeTicker`: emits now, then delays until the next minute boundary (or second boundary for the
  seconds variant). Listen for `ACTION_TIME_CHANGED` / `ACTION_TIMEZONE_CHANGED` to re-sync.
- Formats: weekday `EEEE` uppercased, time `HH:mm`, date `MMMM d`.

### 3.2 Weather (Open-Meteo)
- Endpoint: `https://api.open-meteo.com/v1/forecast` with
  `latitude, longitude,
   current=temperature_2m,apparent_temperature,weather_code,is_day,
   hourly=temperature_2m,weather_code,is_day,
   daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,
   timezone=auto, forecast_days=2`
- Location: `ACCESS_COARSE_LOCATION`, `LocationManager.getCurrentLocation(NETWORK_PROVIDER or FUSED)`.
  If no fix, fall back to the cached last coordinates in DataStore.
- City name: `Geocoder.getFromLocation(lat, lon, 1, listener)` (API 33 async). Use `locality`, uppercased, and cache it.
- Hourly row: "Now" + the next 6 entries at a 2-hour step starting from the next even hour, labelled `HH`.
- Icons: map WMO codes → {clear, mostly clear, partly cloudy, cloudy, fog, drizzle, rain, snow, thunder},
  with day/night variants via `is_day`. Use vector drawables. Colours like the reference: sun yellow,
  moon/clouds white, rain light blue.
- Refresh: every 15 min while visible, on resume, and on location change > 2 km.
- Error: keep showing the last good data with a small "stale" dot. Show a placeholder if there's never been data.

### 3.3 Calendar (CalendarContract)
- Permission: `READ_CALENDAR`.
- Query `CalendarContract.Instances` for `[now, now + 7 days]`, projection:
  `EVENT_ID, TITLE, BEGIN, END, ALL_DAY, DISPLAY_COLOR, CALENDAR_ID, SELF_ATTENDEE_STATUS, EVENT_LOCATION`.
  - Exclude hidden calendars (Settings), events with `SELF_ATTENDEE_STATUS == DECLINED`, and events whose
    `END <= now`.
  - Sort by BEGIN. Events in progress are labelled "Now".
  - All-day events: show them with the label "All day" and sort before timed events on the same day.
- Up Next UI: the first event is large, with a second line: `EEEE, HH:mm – HH:mm` (or "Today"/"Tomorrow"
  instead of the weekday). The next ~4 events are compact rows with a right-aligned day label
  (`Today` → `HH:mm`, otherwise `EEE`). The coloured bar uses `DISPLAY_COLOR`. Ellipsize long titles.
- Refresh: `ContentObserver` on `CalendarContract.Instances.CONTENT_URI` / `Events.CONTENT_URI`, plus each
  minute tick (so finished events drop off).
- Empty state: "Nothing coming up".

### 3.4 Now Playing (no Premium)

**Common model**
```
PlaybackSnapshot(
  source: Source (PHONE | MAC),
  sourceLabel: String,
  isPlaying: Boolean,
  title, artist, album: String,
  artwork: Bitmap? or artworkUrl: String?,
  durationMs: Long,
  positionMs: Long,          // at capturedAtElapsedRealtime
  capturedAtElapsedRealtime: Long,
  supportsShuffleRepeat: Boolean, shuffle: Boolean?, repeat: Boolean?
)
interface MediaSource {
  val state: StateFlow<PlaybackSnapshot?>
  suspend fun playPause(); suspend fun next(); suspend fun previous()
  suspend fun seekTo(ms: Long); suspend fun setShuffle(on: Boolean); suspend fun cycleRepeat()
}
```
The UI computes the live position as `positionMs + (elapsedRealtime - capturedAt)` while playing. Tick it with
`withFrameMillis` on Now Playing and once per second on the dashboard, clamped to `durationMs`.

**`PlaybackRepository` selection rule:** prefer a source that is playing. If both are playing, use the one
that most recently changed track or state. If neither is playing, show the most recently active one in its
paused state for 10 minutes, then hide it. Controls are routed to the selected source. Update optimistically,
then reconcile with the next snapshot.

**Source 1: phone (MediaSession).**
- Declare `MediaListenerService : NotificationListenerService` in the manifest (with
  `BIND_NOTIFICATION_LISTENER_SERVICE`). The user grants "Notification access" from Settings
  (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`).
- `MediaSessionManager.getActiveSessions(ComponentName(ctx, MediaListenerService::class.java))` plus
  `addOnActiveSessionsChangedListener`. Pick the controller with package `com.spotify.music` (any app's
  media could be supported later, but filter to Spotify for now).
- `MediaController.Callback` → `onMetadataChanged` (TITLE, ARTIST, ALBUM, DURATION, ALBUM_ART / ART bitmap)
  and `onPlaybackStateChanged` (state, position, lastPositionUpdateTime, playbackSpeed).
- Controls: `transportControls.play/pause/skipToNext/skipToPrevious/seekTo`.
- Note: free-tier skip limits on mobile are enforced by Spotify, not us. Just reflect whatever state results.

**Source 2: Mac bridge (laptop playback).**
- Companion script `companion/spotify_bridge.py` (Python 3 stdlib only, runs on the user's Mac):
  - Uses `osascript` to talk to the Spotify desktop app. Always check
    `application "Spotify" is running` first so that polling never launches Spotify.
  - Reads: `player state`, `name/artist/album/duration/id/artwork url of current track`,
    `player position` (seconds, float), `shuffling`, `repeating`. Fetch them all in one osascript call
    and return a delimited string.
  - Writes: `playpause`, `next track`, `previous track`, `set player position to X`,
    `set shuffling to`, `set repeating to`.
  - HTTP server (`http.server.ThreadingHTTPServer`) on `0.0.0.0:8765`:
    - `GET /state` → JSON `{running, isPlaying, title, artist, album, trackId, artworkUrl, durationMs,
      positionMs, shuffle, repeat, serverTimeMs}`
    - `POST /playpause | /next | /previous`, `POST /seek {"positionMs": n}`,
      `POST /shuffle {"on": bool}`, `POST /repeat {"on": bool}`
    - Every request must carry `Authorization: Bearer <token>`. The token is generated on first run and
      stored in `~/.config/seeker-bridge/token` (chmod 600), and printed on startup.
  - Advertise via Bonjour by spawning `dns-sd -R "Seeker Spotify Bridge" _seekerspotify._tcp local 8765`
    (built into macOS, no deps). Kill it on exit.
  - A cache of ~300 ms on `/state` stops osascript from being spammed.
  - `com.kashsuks.spotifybridge.plist` is a launchd LaunchAgent (RunAtLoad, KeepAlive) so the bridge starts at login.
  - `companion/README.md`: how to run it, the first-run macOS "allow control of Spotify" Automation prompt,
    installing the LaunchAgent, and where to find the token.
- Android side (`MacBridgeSource`):
  - Discover the host with `NsdManager` (`_seekerspotify._tcp`). Acquire a `WifiManager.MulticastLock` during
    discovery (`CHANGE_WIFI_MULTICAST_STATE`). Allow manual `host:port` override in Settings.
  - Cleartext HTTP on the LAN: add `network_security_config.xml` with `cleartextTrafficPermitted="true"`
    (personal app; acceptable).
  - Poll `GET /state` every 1 s on Now Playing and every 3 s on the dashboard. Back off to 15 s after
    failures, and treat the source as unavailable (null) when unreachable or Spotify isn't running.
  - Load artwork with Coil (`allowHardware(false)` so Palette can read the bitmap).
  - Source label: "On MacBook" (or the Bonjour host name).
- Leave the `MediaSource` interface open so a `SpotifyWebApiSource` can be added later if the user gets Premium.

---

## 4. Always-on polish
- `BurnInShift`: the whole dashboard is offset by a random ±6dp x/y every 3 min (animated over 2 s).
- Night dim: inside the configured window, `screenBrightness = 0.05f` and text alpha goes down a little. Restore it
  outside the window.
- Immersive: `WindowInsetsControllerCompat.hide(systemBars())`, `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`.
- Manifest: `android:screenOrientation="sensorLandscape"`, `configChanges` for orientation/screenSize so
  the activity isn't recreated.
- Permissions requested on first launch with a short explanation screen, never in a loop. Anything denied shows
  a placeholder in that panel with a "Grant" button.

---

## 5. Build phases (commit after each; build must pass)

| # | Phase | Done when | Example commit |
|---|---|---|---|
| 1 | Project skeleton: Gradle, Compose, theme/font, .gitignore, immersive landscape activity, dashboard grid with fake data | App installs on Seeker and shows a static layout matching the mock | `Chore[Setup]: Create Compose project skeleton` |
| 2 | Live clock + Focus Clock with `FlipDigit` and shared transition | Time updates on the minute; tap → flip clock animates on change; back returns | `Feat[Clock]: Add flip clock focus view` |
| 3 | Weather: location, Open-Meteo, icons, hourly row | Real temp/city/H/L/feels-like and 7-slot row; survives offline with cached data | `Feat[Weather]: Add Open-Meteo weather panel` |
| 4 | Calendar: permission, Instances query, Up Next, Settings calendar toggles | Events from all synced Google accounts appear with their colours; hidden calendars disappear | `Feat[Calendar]: Add Up Next from device calendars` |
| 5 | Media (phone): NotificationListener + MediaSession source, mini-card, Now Playing screen, palette background, controls | Spotify playing on the phone shows on the card; controls work; background matches art | `Feat[Media]: Add phone Spotify now playing` |
| 6 | Media (Mac): companion bridge + LaunchAgent + README, NSD discovery, `MacBridgeSource`, source merging, shuffle/repeat | Spotify on the Mac shows and is controllable from the Seeker | `Feat[Bridge]: Add Mac Spotify bridge` |
| 7 | Polish: burn-in shift, night dim, settings screen completion, error/empty states | Runs all night without issues | `Feat[Display]: Add burn-in shift and night dim` |

Test each phase on the device: `./gradlew installDebug` with the Seeker connected over adb.
Unit-test pure logic: WMO mapping, hourly slot selection, calendar filtering/sorting, source-selection rule,
position interpolation.

---

## 6. User setup checklist (outside the code)
1. **adb:** enable Developer options + USB debugging on the Seeker and accept the RSA prompt; `adb devices` must list it.
2. **Google accounts:** add each Gmail account on the Seeker (Settings → Passwords & accounts → Add account →
   Google), make sure Calendar sync is on for each, and turn on any extra calendars you want synced to the phone.
3. **Mac bridge:** run `companion/spotify_bridge.py` once, allow the Automation prompt, install the LaunchAgent,
   then paste the token into the app's Settings.
4. In the app: grant Location, Calendar, and Notification access.
