#!/usr/bin/env bash
# Publishes appVersion from app/build.gradle.kts as GitHub release vX.Y.Z of vamsikalagaturu/curlew with curlew.apk,
# the asset the in-app updater looks for. .github/workflows/release.yml runs it on main; by hand, only on a clean,
# pushed main. Refuses an APK not signed with the release key, since phones would reject it as an update.
set -euo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO=$(cd "$HERE/.." && pwd)
VERSION=$(sed -n 's/^val appVersion = "\([0-9.]*\)"$/\1/p' "$REPO/app/build.gradle.kts")
TAG=v$VERSION

[ -n "$VERSION" ] || { echo "no appVersion in app/build.gradle.kts" >&2; exit 1; }
[ -z "$(git -C "$REPO" status --porcelain --untracked-files=no)" ] || { echo "commit first: the release is built from HEAD" >&2; exit 1; }
gh release view "$TAG" -R vamsikalagaturu/curlew >/dev/null 2>&1 && { echo "$TAG exists; raise appVersion" >&2; exit 1; }

export CURLEW_KEYSTORE=${CURLEW_KEYSTORE:-$HOME/.config/curlew/release.keystore}
[ -f "$CURLEW_KEYSTORE" ] || { echo "signing key $CURLEW_KEYSTORE is missing" >&2; exit 1; }

(cd "$REPO" && ./gradlew --no-daemon assembleRelease)
APK=$REPO/app/build/outputs/apk/release/app-release.apk

SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}
APKSIGNER=$(ls -d "$SDK"/build-tools/*/ | sort -V | tail -1)apksigner
signer=$("$APKSIGNER" verify --print-certs "$APK" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
[ "$signer" = "$(cat "$HERE/signing-cert.sha256")" ] || { echo "APK is not signed with the release key ($signer)" >&2; exit 1; }

OUT=$(mktemp -d)
cp "$APK" "$OUT/curlew.apk"
gh release create "$TAG" -R vamsikalagaturu/curlew --target "$(git -C "$REPO" rev-parse HEAD)" --title "curlew $VERSION" \
    --generate-notes "$OUT/curlew.apk"
echo "released $TAG"
