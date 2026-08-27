#!/usr/bin/env bash
#
# Generate the release signing keystore for MaterialReader and print the
# exact values to paste into GitHub Secrets.
#
# Usage:
#   ./scripts/generate_keystore.sh
#
# This writes release.keystore to the repo root. .gitignore already excludes
# *.keystore, so it will NOT be committed. Keep the password safe.
set -euo pipefail

KEY_ALIAS="materialreader"
VALIDITY_DAYS=10000
KEYSTORE="${PWD}/release.keystore"

read -rsp "Keystore + key password (shared): " KEY_PASS
echo

if [ -f "${KEYSTORE}" ]; then
    echo "release.keystore already exists — aborting to avoid overwriting." >&2
    exit 1
fi

keytool -genkeypair \
    -v \
    -keystore "${KEYSTORE}" \
    -alias "${KEY_ALIAS}" \
    -keyalg RSA \
    -keysize 4096 \
    -validity "${VALIDITY_DAYS}" \
    -storepass "${KEY_PASS}" \
    -keypass "${KEY_PASS}" \
    -dname "CN=MaterialReader, OU=stormtroopercs, O=stormtroopercs"

echo
echo "=== Add these to GitHub Secrets (repo Settings -> Secrets and variables -> Actions) ==="
echo
echo "KEYSTORE_FILE:"
base64 -w0 "${KEYSTORE}"
echo
echo
echo "KEYSTORE_PASSWORD: ${KEY_PASS}"
echo "KEY_ALIAS:         ${KEY_ALIAS}"
echo "KEY_PASSWORD:      ${KEY_PASS}"
echo
echo "(release.keystore is gitignored and must be kept safe — it is required to sign every future update.)"
