#!/usr/bin/env bash
#
# Bump the MaterialReader version, tag it, and push to trigger the Release
# workflow. Versioning scheme: v0.0.x-alpha (x increments by 1 each release).
#
# Usage:
#   ./scripts/bump-release.sh            # next patch, keeps -alpha suffix
#   ./scripts/bump-release.sh --stable   # drop the -alpha suffix (e.g. v0.0.2)
#   ./scripts/bump-release.sh v0.0.5-alpha  # explicit version
set -euo pipefail

cd "$(dirname "$0")/.."

REMOTE="${REMOTE:-origin}"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

explicit="${1:-}"
stable=0
[[ "${explicit}" == "--stable" ]] && { stable=1; explicit=""; }

if [[ -n "${explicit}" ]]; then
    new="${explicit#v}"
else
    # derive from the latest matching tag
    latest="$(git tag --list 'v0.0.*' | sort -V | tail -n1)"
    if [[ -z "${latest}" ]]; then
        new="0.0.1-alpha"
    else
        base="${latest#v}"
        num="${base%%-*}"            # 0.0.x
        patch="${num##*.}"           # x
        next=$((patch + 1))
        prefix="${num%.*}"           # 0.0
        new="${prefix}.${next}-alpha"
        [[ "${stable}" -eq 1 ]] && new="${prefix}.${next}"
    fi
fi

# sanity: must look like 0.0.N or 0.0.N-alpha
if ! [[ "${new}" =~ ^0\.0\.[0-9]+(-alpha)?$ ]]; then
    echo "Refusing to tag '${new}' — must match v0.0.N or v0.0.N-alpha" >&2
    exit 1
fi

tag="v${new}"

echo "Current branch: ${BRANCH}"
echo "Latest tag:     $(git tag --list 'v0.0.*' | sort -V | tail -n1 || echo none)"
echo "New tag:        ${tag}"
read -rp "Push ${tag} to ${REMOTE} and trigger the release? [y/N] " confirm
[[ "${confirm,,}" == "y" ]] || { echo "Aborted."; exit 1; }

git tag -a "${tag}" -m "Release ${tag}"
git push "${REMOTE}" "${tag}"
echo "Pushed ${tag} — the Release workflow will build and publish MaterialReader-${new}.apk"
