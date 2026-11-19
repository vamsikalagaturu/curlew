#!/usr/bin/env bash
# Prints the CHANGELOG.md section for version $1 (the lines under "## $1" up to the next "## "), without its heading.
# Fails when the section is missing or empty, so no release goes out without notes.
set -euo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
VERSION=${1:?usage: notes.sh X.Y.Z}

notes=$(awk -v v="## $VERSION" '
    $0 == v { inside = 1; next }
    inside && /^## / { exit }
    inside { print }
' "$HERE/../CHANGELOG.md" | sed -e '/./,$!d')

[ -n "$notes" ] || { echo "CHANGELOG.md has no notes under \"## $VERSION\"" >&2; exit 1; }
printf '%s\n' "$notes"
