# Seeker Display

An always-on dashboard for the Solana Seeker (Android): a big clock, local weather with an hourly
forecast, your upcoming calendar events, and a Spotify now-playing card with controls.
See [PLAN.md](PLAN.md) for the design.

- **Tap the time** for a full-screen flip clock.
- **Tap the song card** for the full-screen player (cover-colour background, seek bar, controls).
- **Long-press anywhere on the dashboard** for Settings.

## Build and install

You need Android Studio (its bundled JDK is used for command-line builds) and `adb`.

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android lint
./gradlew installDebug         # install on the connected phone
```

Enable **Developer options → USB debugging** on the Seeker, plug it in, accept the prompt, and check
that `adb devices` lists it. Make sure only one copy of `adb` is on your `PATH` (Android Studio's
lives in `~/Library/Android/sdk/platform-tools`); two different versions fight over the connection.

## First-run setup

1. **Permissions.** Allow Location and Calendar when asked (or in Settings inside the app).
2. **Calendars.** Add each Gmail account in Android Settings → Passwords & accounts, with Calendar
   sync on. The app reads whatever the phone syncs, and Settings lets you hide individual calendars.
3. **Spotify on the phone.** In the app's Settings, grant **Notification access**. That lets it read
   and control the Spotify app's media session. No Premium needed.
4. **Spotify on your Mac.** Run the [bridge](companion/README.md) and paste its token into
   Settings → Mac bridge. When both Spotify sources are playing, the most recently started one wins.

## Layout

| Path | What |
|---|---|
| `app/src/main/java/.../data` | Weather, calendar, location, media sources, bridge client, settings storage |
| `app/src/main/java/.../ui` | Dashboard, focus clock, now playing, settings screens |
| `app/src/main/java/.../core` | Time ticker/formatting, burn-in shift, night window |
| `companion/` | Python bridge for Spotify on macOS |
