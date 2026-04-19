#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="8.7"
DIST_NAME="gradle-${GRADLE_VERSION}-bin.zip"
DIST_URL="https://services.gradle.org/distributions/${DIST_NAME}"
INSTALL_DIR="$APP_HOME/.gradle-dist"
GRADLE_HOME="$INSTALL_DIR/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
ZIP_PATH="$INSTALL_DIR/$DIST_NAME"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$INSTALL_DIR"
  if [ ! -f "$ZIP_PATH" ]; then
    echo "Baixando Gradle ${GRADLE_VERSION}..."
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DIST_URL" -o "$ZIP_PATH"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP_PATH" "$DIST_URL"
    else
      echo "Instale curl ou wget para baixar o Gradle automaticamente." >&2
      exit 1
    fi
  fi
  if [ ! -d "$GRADLE_HOME" ]; then
    if command -v unzip >/dev/null 2>&1; then
      unzip -qo "$ZIP_PATH" -d "$INSTALL_DIR"
    else
      echo "Instale unzip para extrair o Gradle automaticamente." >&2
      exit 1
    fi
  fi
fi

exec "$GRADLE_BIN" "$@"
