#!/bin/bash
# Signs build/aligned.apk with the ByteSnap Inc. release keystore (credentials read from signing/keystore.properties)
set -e
P="$(cd "$(dirname "$0")/.." && pwd)"
prop(){ grep "^$1=" "$P/signing/keystore.properties" | cut -d= -f2-; }
apksigner sign --ks "$P/$(prop storeFile)" --ks-key-alias "$(prop keyAlias)" --ks-pass "pass:$(prop storePassword)" --key-pass "pass:$(prop keyPassword)" \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out "$1" "$P/build/aligned.apk"
apksigner verify --verbose --print-certs "$1"
