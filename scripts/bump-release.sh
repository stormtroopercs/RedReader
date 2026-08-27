#!/usr/bin/env bash
#
# Trigger a MaterialReader release on demand. The nightly cron does the same
# automatically; this is just for running it immediately.
#
# Usage:
#   ./scripts/bump-release.sh              # build the next auto version now (only if dev changed)
#   ./scripts/bump-release.sh v0.0.2       # force a specific version (stable, no -alpha)
#   ./scripts/bump-release.sh v0.0.3-alpha # force a specific alpha
#
# Requires the GitHub CLI (`gh`) authenticated and `dev` pushed. The nightly
# auto-release still works without this.
set -euo pipefail

cd "$(dirname "$0")/.."
REMOTE="${REMOTE:-origin}"

if ! command -v gh >/dev/null 2>&1; then
    echo "gh (GitHub CLI) is required for on-demand releases." >&2
    echo "Install it, or use Actions -> Run workflow in the GitHub UI." >&2
    echo "(The nightly auto-release still works without gh.)" >&2
    exit 1
fi

if [ $# -ge 1 ]; then
    VER="${1#v}"
    echo "Dispatching Release workflow with forced version v${VER}..."
    gh workflow run Release.yml --ref dev -f version="v${VER}"
else
    echo "Pushing dev, then dispatching Release workflow (auto version, only if changed)..."
    git push "${REMOTE}" dev
    gh workflow run Release.yml --ref dev
fi
echo "Done. Watch progress under Actions -> Release."
