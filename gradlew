#!/usr/bin/env sh
set -e

GRADLE_VERSION="9.3.1"
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
DIST_DIR="$ROOT/.gradle-dist"
GRADLE_HOME="$DIST_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
ZIP="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  echo "Gradle $GRADLE_VERSION is not installed for this project. Downloading..."
  mkdir -p "$DIST_DIR"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -L --fail "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget "$URL" -O "$ZIP"
  else
    echo "curl or wget is required to bootstrap Gradle."
    exit 1
  fi
  unzip -oq "$ZIP" -d "$DIST_DIR"
  rm -f "$ZIP"
fi

exec "$GRADLE_BIN" "$@"
