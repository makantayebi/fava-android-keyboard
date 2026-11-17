#!/usr/bin/env bash
# Build a signed release APK and upload it to makantayebi.com.
# Run this yourself -- it needs your SSH key's passphrase interactively,
# which this script never touches or stores.
#
# Usage: bash release.sh
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

REMOTE_HOST="root@makantayebi.com"
REMOTE_PATH="/var/www/html/fav"

if [[ ! -f keystore.properties ]]; then
    echo "error: keystore.properties not found here -- a release build would be unsigned." >&2
    echo "       (it's git-ignored; this only runs on the machine that generated the keystore)" >&2
    exit 1
fi

echo "==> building release APK"
./gradlew assembleRelease

APK="$(find app/build/outputs/apk/release -maxdepth 1 -name 'fava-*.apk' | sort | tail -1)"
if [[ -z "$APK" ]]; then
    echo "error: no fava-*.apk found in app/build/outputs/apk/release after build" >&2
    exit 1
fi
echo "==> built: $APK"

echo "==> ensuring $REMOTE_PATH exists on $REMOTE_HOST"
ssh "$REMOTE_HOST" "mkdir -p '$REMOTE_PATH'"

echo "==> uploading"
scp "$APK" "$REMOTE_HOST:$REMOTE_PATH/"

echo
echo "Done: $(basename "$APK") -> $REMOTE_HOST:$REMOTE_PATH/"
echo "Likely live at: https://makantayebi.com/fav/$(basename "$APK")"
echo "(depends on how that vhost is configured -- confirm the URL actually serves it)"
