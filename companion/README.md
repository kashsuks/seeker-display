# Seeker Spotify Bridge

Spotify's official API needs Premium, so the Seeker app gets laptop playback from this small script
instead. It reads and controls the **Spotify desktop app on your Mac** and shares it over your Wi-Fi.
Python 3 and macOS only, no packages to install.

## Run it

```sh
python3 companion/spotify_bridge.py
```

It prints the address and an **access token**. The first time Spotify is queried, macOS asks whether
Terminal (or Python) may control Spotify. Click **OK**. If you missed it:
System Settings → Privacy & Security → Automation → enable Spotify for your terminal.

The phone finds the Mac automatically (Bonjour) when both are on the same Wi-Fi. If discovery doesn't
work on your network, enter `mac-ip:8765` manually in the app's Settings.

## Start it at login

```sh
companion/install_launch_agent.sh              # install
companion/install_launch_agent.sh --uninstall  # remove
```

Logs go to `~/Library/Logs/seeker-bridge.log`.

## Get the token again

```sh
python3 companion/spotify_bridge.py --token
```

It lives in `~/.config/seeker-bridge/token` (owner-only). Paste it into the app: long-press the
dashboard → Settings → Mac bridge.

## What it exposes

All requests need `Authorization: Bearer <token>`.

| Request | Effect |
|---|---|
| `GET /state` | Track, artist, album, art URL, position, duration, playing, shuffle, repeat |
| `POST /playpause` `/next` `/previous` | Transport controls |
| `POST /seek` `{"positionMs": 12345}` | Jump to a position |
| `POST /shuffle` `/repeat` `{"on": true}` | Toggle modes |

Traffic is plain HTTP on your local network, protected only by the token. Don't expose port 8765 to the internet.

## Tests

```sh
cd companion && python3 -m unittest test_spotify_bridge
```
