#!/bin/bash
# Fort Launcher build — manual pipeline, no Gradle needed
set -e
SDK=/opt/android-sdk
BT=$SDK/build-tools/34.0.0
PLATFORM=$SDK/platforms/android-34/android.jar
APP=$(dirname "$0")
BUILD=$APP/build

rm -rf "$BUILD"
mkdir -p "$BUILD/dex" "$BUILD/classes" "$BUILD/gen"

echo "==> [1/6] aapt2 compile resources"
"$BT/aapt2" compile --dir "$APP/app/res" -o "$BUILD/res.zip"

echo "==> [2/6] aapt2 link (manifest + R.java)"
"$BT/aapt2" link \
    -o "$BUILD/unsigned.apk" \
    -I "$PLATFORM" \
    --manifest "$APP/app/AndroidManifest.xml" \
    --java "$BUILD/gen" \
    --min-sdk-version 26 \
    --target-sdk-version 34 \
    --version-code 3 \
    --version-name "1.2" \
    "$BUILD/res.zip"

echo "==> [3/6] javac"
javac -source 8 -target 8 \
    -classpath "$PLATFORM" \
    -d "$BUILD/classes" \
    "$BUILD/gen/com/fort/launcher/R.java" \
    "$APP/app/src/com/fort/launcher/MainActivity.java" 2>&1 | grep -v "^warning" || true

echo "==> [4/6] d8 (dex)"
"$BT/d8" --release \
    --lib "$PLATFORM" \
    --min-api 26 \
    --output "$BUILD/dex" \
    $(find "$BUILD/classes" -name "*.class")

echo "==> [5/6] package classes.dex into APK"
cd "$BUILD"
zip -q -j unsigned.apk dex/classes.dex

echo "==> [6/6] align + sign"
"$BT/zipalign" -f 4 unsigned.apk aligned.apk
if [ ! -f "$APP/debug.keystore" ]; then
    keytool -genkeypair -keystore "$APP/debug.keystore" \
        -storepass fortfort -keypass fortfort -alias fort \
        -dname "CN=Fort Launcher,O=Our Little Fort,C=US" \
        -keyalg RSA -keysize 2048 -validity 10000 2>/dev/null
fi
"$BT/apksigner" sign --ks "$APP/debug.keystore" \
    --ks-pass pass:fortfort --key-pass pass:fortfort \
    --out "$APP/fort-launcher.apk" aligned.apk

"$BT/apksigner" verify "$APP/fort-launcher.apk" && echo "APK signature verified."
echo "==> DONE: $APP/fort-launcher.apk"
ls -lh "$APP/fort-launcher.apk"
