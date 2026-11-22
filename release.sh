#!/usr/bin/env bash
# Build a signed release APK, then push it via push.sh.
# Run this yourself -- it needs your SSH key's passphrase interactively,
# which this script never touches or stores.
#
# Usage: bash release.sh
# Already have a build and just want to upload it (no rebuild)? Use
# push.sh directly instead.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

if [[ ! -f keystore.properties ]]; then
    echo "error: keystore.properties not found here -- a release build would be unsigned." >&2
    echo "       (it's git-ignored; this only runs on the machine that generated the keystore)" >&2
    exit 1
fi

echo "==> building release APK"
./gradlew assembleRelease

bash push.sh
