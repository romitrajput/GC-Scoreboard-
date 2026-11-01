#!/usr/bin/env bash
# Installs the Android SDK packages needed to build the APK (safe to run again).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"

if ! command -v unzip >/dev/null; then sudo apt-get update -qq && sudo apt-get install -y -qq unzip; fi
if ! command -v java >/dev/null; then echo "❌ Java not found. Use the Java 17 dev container or install a JDK 17."; exit 1; fi

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "⬇  Downloading Android command-line tools…"
  mkdir -p "$SDK/cmdline-tools"
  curl -fsSL -o "/tmp/$TOOLS_ZIP" "https://dl.google.com/android/repository/$TOOLS_ZIP"
  unzip -q -o "/tmp/$TOOLS_ZIP" -d "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -f "/tmp/$TOOLS_ZIP"
fi

export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"
echo "📦 Installing SDK packages (platform-tools, Android 34, build-tools 34)…"
yes | sdkmanager --sdk_root="$SDK" --licenses >/dev/null || true
sdkmanager --sdk_root="$SDK" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "sdk.dir=$SDK" > "$ROOT/local.properties"
if ! grep -q "ANDROID_HOME=$SDK" ~/.bashrc 2>/dev/null; then
  echo "export ANDROID_HOME=$SDK" >> ~/.bashrc
  echo 'export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH' >> ~/.bashrc
fi
chmod +x "$ROOT/gradlew" "$ROOT/build-apk.sh" 2>/dev/null || true
echo "✅ Android SDK ready at $SDK"
echo "   Next: ./build-apk.sh"
