#!/usr/bin/env bash
# Builds the APK and copies it to the project root as GullyScorer.apk
set -euo pipefail
cd "$(dirname "$0")"

# Gradle 8.9 + Android Gradle Plugin need Java 17 or 21.
# Codespaces may default to a newer Java (e.g. 25 → "Unsupported class file major version 69").
jdk_major() { "$1/bin/java" -version 2>&1 | grep -m1 'version "' | sed -E 's/.*version "([0-9]+).*/\1/'; }
pick_jdk() {
  local d
  for d in "${JAVA_HOME:-}" /usr/local/sdkman/candidates/java/* /usr/lib/jvm/* /opt/java/* "$HOME"/.sdkman/candidates/java/*; do
    [ -n "$d" ] && [ -x "$d/bin/java" ] || continue
    case "$(jdk_major "$d")" in 17|21) echo "$d"; return 0;; esac
  done
  return 1
}

JDK="$(pick_jdk || true)"
if [ -z "$JDK" ]; then
  echo "☕ No Java 17/21 found — installing OpenJDK 17…"
  sudo apt-get update -qq && sudo apt-get install -y -qq openjdk-17-jdk-headless
  JDK="$(pick_jdk || true)"
fi
if [ -z "$JDK" ]; then echo "❌ Could not find or install Java 17. Run: sudo apt-get install openjdk-17-jdk-headless"; exit 1; fi
export JAVA_HOME="$JDK"
export PATH="$JAVA_HOME/bin:$PATH"
echo "☕ Using Java $(jdk_major "$JAVA_HOME") at $JAVA_HOME"

[ -f local.properties ] || bash .devcontainer/setup-android.sh
chmod +x gradlew
./gradlew --stop >/dev/null 2>&1 || true
./gradlew assembleDebug --no-daemon -Dorg.gradle.java.home="$JAVA_HOME"
cp app/build/outputs/apk/debug/app-debug.apk GullyScorer.apk
echo ""
echo "✅ Built: $(pwd)/GullyScorer.apk"
echo "   In Codespaces: right-click GullyScorer.apk in the Explorer → Download"
