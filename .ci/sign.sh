#!/usr/bin/env bash
# Sign the release APK with the repo's fixed keystore so that every build
# shares one signature (otherwise Android refuses update-installs).
set -euo pipefail

BT="${ANDROID_HOME}/build-tools/36.0.0"
KS_P12="${RUNNER_TEMP}/release.p12"
PASS="android"
ALIAS="pixel9proims"

base64 -d .ci/keystore.b64 > "$KS_P12"

UNSIGNED=$(find app/build/outputs/apk/release -name '*.apk' | head -1)
echo "unsigned apk: $UNSIGNED"

"$BT/zipalign" -f -p 4 "$UNSIGNED" app-aligned.apk

"$BT/apksigner" sign \
  --ks "$KS_P12" \
  --ks-type PKCS12 \
  --ks-pass "pass:${PASS}" \
  --key-pass "pass:${PASS}" \
  --ks-key-alias "$ALIAS" \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --out app-release.apk \
  app-aligned.apk

echo "--- signer ---"
"$BT/apksigner" verify --print-certs app-release.apk | head -6
ls -l app-release.apk
