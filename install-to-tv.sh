#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOCAL_PROPS="${SCRIPT_DIR}/local.properties"

PROP_SDK_DIR=""
PROP_TV_IP=""
if [ -f "$LOCAL_PROPS" ]; then
    PROP_SDK_DIR=$(sed -n 's/^sdk\.dir=//p' "$LOCAL_PROPS" | head -n 1)
    PROP_TV_IP=$(sed -n 's/^TV_IP=//p' "$LOCAL_PROPS" | head -n 1)
fi

TV_IP="${1:-$PROP_TV_IP}"
if [ -z "$TV_IP" ]; then
    echo "Usage: ./install-to-tv.sh <ANDROID_TV_IP>"
    exit 1
fi

if [ -n "$PROP_SDK_DIR" ] && [ -x "${PROP_SDK_DIR}/platform-tools/adb" ]; then
    ADB="${PROP_SDK_DIR}/platform-tools/adb"
elif command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
else
    echo "Error: 'adb' not found. Install Android platform-tools or set sdk.dir in local.properties."
    exit 1
fi

APK="${SCRIPT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    "${SCRIPT_DIR}/gradlew" assembleDebug
fi

echo "1/3 Connecting to Android TV at ${TV_IP}:5555..."
"$ADB" kill-server >/dev/null 2>&1 || true
"$ADB" start-server
"$ADB" connect "${TV_IP}:5555"

echo "2/3 Installing NanoTV (app.nanotv.tv)..."
"$ADB" -s "${TV_IP}:5555" install -r "$APK"

echo "3/3 Launching NanoTV..."
"$ADB" -s "${TV_IP}:5555" shell am start -n app.nanotv.tv/.MainActivity

echo "🎉 NanoTV is live on your Android TV!"
