#!/usr/bin/env python3
"""Seeker Display bridge: exposes the Spotify desktop app on your Mac to the Seeker over Wi-Fi.

Runs a tiny HTTP server that reads and controls Spotify through AppleScript, and announces itself
on the local network (Bonjour) so the phone can find it. Standard library only; macOS only.

    python3 spotify_bridge.py            # start the server
    python3 spotify_bridge.py --token    # just print the access token and exit
"""

import argparse
import json
import os
import secrets
import signal
import socket
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DEFAULT_PORT = 8765
SERVICE_TYPE = "_seekerspotify._tcp"
CACHE_SECONDS = 0.3
SEP = "\x1f"  # ASCII unit separator: will not appear in track names

CONFIG_DIR = os.path.expanduser("~/.config/seeker-bridge")
TOKEN_PATH = os.path.join(CONFIG_DIR, "token")

# Returns one line of SEP-separated fields, or a single word if there is nothing to report.
READ_SCRIPT = """
if application "Spotify" is not running then return "notrunning"
set fieldSep to ASCII character 31
tell application "Spotify"
    set stateText to player state as string
    if stateText is "stopped" then return "stopped"
    set trk to current track
    set artUrl to ""
    try
        set artUrl to artwork url of trk
    end try
    return stateText & fieldSep & (name of trk) & fieldSep & (artist of trk) & fieldSep & (album of trk) & fieldSep & (duration of trk) & fieldSep & (id of trk) & fieldSep & artUrl & fieldSep & (player position as string) & fieldSep & (shuffling as string) & fieldSep & (repeating as string)
end tell
"""


def to_float(text):
    """AppleScript may format decimals with a comma depending on the Mac's locale."""
    try:
        return float(text.replace(",", "."))
    except ValueError:
        return 0.0


def parse_state(raw, host_name=""):
    """Turn the AppleScript output into the JSON the phone expects."""
    raw = raw.strip()
    base = {
        "running": True, "isPlaying": False, "title": "", "artist": "", "album": "",
        "trackId": "", "artworkUrl": "", "durationMs": 0, "positionMs": 0,
        "shuffle": False, "repeat": False, "host": host_name,
        "serverTimeMs": int(time.time() * 1000),
    }
    if raw == "notrunning":
        base["running"] = False
        return base
    if raw == "stopped" or raw == "":
        return base

    parts = raw.split(SEP)
    if len(parts) < 10:
        return base
    state, title, artist, album, duration, track_id, art, position, shuffle, repeat = parts[:10]
    base.update({
        "isPlaying": state == "playing",
        "title": title,
        "artist": artist,
        "album": album,
        "trackId": track_id,
        "artworkUrl": art,
        "durationMs": int(to_float(duration)),
        "positionMs": int(to_float(position) * 1000),
        "shuffle": shuffle.strip().lower() == "true",
        "repeat": repeat.strip().lower() == "true",
    })
    return base


def run_osascript(script):
    result = subprocess.run(["osascript", "-e", script], capture_output=True, text=True, timeout=5)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "osascript failed")
    return result.stdout


def spotify_command(command):
    """Send a one-line command to Spotify, but never launch it if it isn't running."""
    script = f'if application "Spotify" is running then tell application "Spotify" to {command}'
    run_osascript(script)


class StateCache:
    """Avoids spawning osascript for every request when several clients poll at once."""

    def __init__(self, host_name):
        self.host_name = host_name
        self.lock = threading.Lock()
        self.value = None
        self.at = 0.0

    def get(self):
        with self.lock:
            if self.value is None or time.monotonic() - self.at > CACHE_SECONDS:
                try:
                    self.value = parse_state(run_osascript(READ_SCRIPT), self.host_name)
                except Exception as error:  # report as "nothing playing" rather than failing the request
                    print(f"read failed: {error}", file=sys.stderr)
                    self.value = parse_state("stopped", self.host_name)
                self.at = time.monotonic()
            return dict(self.value, serverTimeMs=int(time.time() * 1000))

    def invalidate(self):
        with self.lock:
            self.value = None


def load_or_create_token():
    os.makedirs(CONFIG_DIR, exist_ok=True)
    if os.path.exists(TOKEN_PATH):
        with open(TOKEN_PATH) as f:
            token = f.read().strip()
        if token:
            return token
    token = secrets.token_urlsafe(9)
    fd = os.open(TOKEN_PATH, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w") as f:
        f.write(token + "\n")
    return token


def make_handler(token, cache):
    class Handler(BaseHTTPRequestHandler):
        server_version = "SeekerBridge/1.0"

        def log_message(self, fmt, *args):
            pass  # polling every second would drown the terminal

        def _authorized(self):
            header = self.headers.get("Authorization", "")
            return header.startswith("Bearer ") and secrets.compare_digest(header[7:].strip(), token)

        def _send(self, status, payload):
            body = json.dumps(payload).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def _body(self):
            length = int(self.headers.get("Content-Length", "0") or 0)
            if length <= 0:
                return {}
            try:
                return json.loads(self.rfile.read(length))
            except ValueError:
                return {}

        def do_GET(self):
            if not self._authorized():
                return self._send(401, {"error": "unauthorized"})
            if self.path == "/state":
                return self._send(200, cache.get())
            self._send(404, {"error": "not found"})

        def do_POST(self):
            if not self._authorized():
                return self._send(401, {"error": "unauthorized"})
            body = self._body()
            try:
                if self.path == "/playpause":
                    spotify_command("playpause")
                elif self.path == "/next":
                    spotify_command("next track")
                elif self.path == "/previous":
                    spotify_command("previous track")
                elif self.path == "/seek":
                    seconds = max(0.0, float(body.get("positionMs", 0)) / 1000.0)
                    spotify_command(f"set player position to {seconds:.3f}")
                elif self.path == "/shuffle":
                    spotify_command(f"set shuffling to {'true' if body.get('on') else 'false'}")
                elif self.path == "/repeat":
                    spotify_command(f"set repeating to {'true' if body.get('on') else 'false'}")
                else:
                    return self._send(404, {"error": "not found"})
            except Exception as error:
                return self._send(500, {"error": str(error)})
            cache.invalidate()
            self._send(200, {"ok": True})

    return Handler


def start_bonjour(port):
    """Advertise the service with macOS's built-in dns-sd so the phone can discover it."""
    try:
        return subprocess.Popen(
            ["dns-sd", "-R", "Seeker Spotify Bridge", SERVICE_TYPE, "local", str(port)],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
    except FileNotFoundError:
        print("dns-sd not found; the phone will need the address entered manually.", file=sys.stderr)
        return None


def lan_address():
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(("10.255.255.255", 1))
            return s.getsockname()[0]
    except OSError:
        return "your-mac-ip"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--token", action="store_true", help="print the access token and exit")
    parser.add_argument("--no-bonjour", action="store_true", help="don't advertise on the local network")
    args = parser.parse_args()

    token = load_or_create_token()
    if args.token:
        print(token)
        return

    host_name = socket.gethostname().split(".")[0]
    cache = StateCache(host_name)
    server = ThreadingHTTPServer(("0.0.0.0", args.port), make_handler(token, cache))
    bonjour = None if args.no_bonjour else start_bonjour(args.port)

    def shutdown(*_):
        if bonjour:
            bonjour.terminate()
        threading.Thread(target=server.shutdown, daemon=True).start()

    signal.signal(signal.SIGINT, shutdown)
    signal.signal(signal.SIGTERM, shutdown)

    print(f"Seeker bridge running on http://{lan_address()}:{args.port}")
    print(f"Access token: {token}")
    try:
        server.serve_forever()
    finally:
        if bonjour:
            bonjour.terminate()


if __name__ == "__main__":
    main()
