#!/bin/bash
# Installs (or removes, with --uninstall) a LaunchAgent so the bridge starts whenever you log in.
set -euo pipefail

LABEL="com.kashsuks.seekerbridge"
DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET="$HOME/Library/LaunchAgents/$LABEL.plist"
DOMAIN="gui/$(id -u)"

if [[ "${1:-}" == "--uninstall" ]]; then
    launchctl bootout "$DOMAIN/$LABEL" 2>/dev/null || true
    rm -f "$TARGET"
    echo "Removed $LABEL"
    exit 0
fi

PYTHON="$(command -v python3)"
mkdir -p "$HOME/Library/LaunchAgents" "$HOME/Library/Logs"
sed -e "s|__PYTHON__|$PYTHON|g" -e "s|__SCRIPT__|$DIR/spotify_bridge.py|g" -e "s|__HOME__|$HOME|g" \
    "$DIR/$LABEL.plist.template" > "$TARGET"

launchctl bootout "$DOMAIN/$LABEL" 2>/dev/null || true
launchctl bootstrap "$DOMAIN" "$TARGET"
echo "Installed $LABEL. Logs: ~/Library/Logs/seeker-bridge.log"
echo "Access token: $("$PYTHON" "$DIR/spotify_bridge.py" --token)"
