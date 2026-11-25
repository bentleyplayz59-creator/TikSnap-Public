#!/bin/bash
# TikSnap manual build (no Gradle needed): aapt -> javac -> dx -> zipalign -> apksigner
set -e
P="$(cd "$(dirname "$0")/.." && pwd)"
SDKJAR=${ANDROID_JAR:-/tmp/android34.jar}
M=$P/app/src/main; B=$P/build; rm -rf $B; mkdir -p $B/gen $B/classes $B/out
aapt package -f -m -J $B/gen -M $M/AndroidManifest.xml -S $M/res -I $SDKJAR -F $B/res.apk --min-sdk-version 24 --target-sdk-version 34 --version-code 2 --version-name 1.1.0 $AAPT_EXTRA
javac -nowarn -source 8 -target 8 -bootclasspath $SDKJAR -d $B/classes $(find $M/java $B/gen -name '*.java') 2>&1 | grep -v -i "warning\|^Note\|JAVA_TOOL" || true
dalvik-exchange --dex --min-sdk-version=24 --output=$B/classes.dex $B/classes
cp $B/res.apk $B/unsigned.apk; (cd $B && zip -q -j unsigned.apk classes.dex)
zipalign -f -p 4 $B/unsigned.apk $B/aligned.apk
echo "built $B/aligned.apk"
