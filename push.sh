#!/usr/bin/env bash
# Upload the most recently built signed release APK to makantayebi.com --
# no rebuild. Use this when you already have a build (from release.sh or
# a manual `./gradlew assembleRelease`) and just want to push it. Run this
# yourself -- it needs your SSH key's passphrase interactively, which this
# script never touches or stores.
#
# Usage: bash push.sh
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

REMOTE_HOST="root@makantayebi.com"
REMOTE_PATH="/var/www/html/fava"

APK="$(find app/build/outputs/apk/release -maxdepth 1 -name 'fava-*.apk' | sort | tail -1)"
if [[ -z "$APK" ]]; then
    echo "error: no fava-*.apk found in app/build/outputs/apk/release -- build one first" >&2
    echo "       (./gradlew assembleRelease, or just run release.sh)" >&2
    exit 1
fi
echo "==> pushing: $APK"

echo "==> ensuring $REMOTE_PATH exists on $REMOTE_HOST"
ssh "$REMOTE_HOST" "mkdir -p '$REMOTE_PATH'"

echo "==> uploading"
scp "$APK" "$REMOTE_HOST:$REMOTE_PATH/"

echo
echo "Done: $(basename "$APK") -> $REMOTE_HOST:$REMOTE_PATH/"
echo "Likely live at: https://makantayebi.com/fava/$(basename "$APK")"
echo "(depends on how that vhost is configured -- confirm the URL actually serves it)"
