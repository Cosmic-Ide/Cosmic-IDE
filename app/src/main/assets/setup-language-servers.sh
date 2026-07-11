#!/usr/bin/env bash

set -euo pipefail

FILES_DIR="${1:?Cosmic IDE files directory is required}"
CACHE_DIR="${2:?Cosmic IDE cache directory is required}"
KOTLIN_VERSION="262.8190.0"
KOTLIN_URL="https://download-cdn.jetbrains.com/language-server/kotlin-server/${KOTLIN_VERSION}/kotlin-server-${KOTLIN_VERSION}-aarch64.tar.gz"
JDTLS_URL="https://www.eclipse.org/downloads/download.php?file=/jdtls/snapshots/jdt-language-server-latest.tar.gz"
COURSIER_URL="https://github.com/VirtusLab/coursier-m1/releases/latest/download/cs-aarch64-pc-linux.gz"
SCALA_BIN="$FILES_DIR/scala/bin"
COURSIER_DIR="$FILES_DIR/coursier"

mkdir -p "$CACHE_DIR" "$SCALA_BIN" "$COURSIER_DIR"

install_kotlin_lsp() {
    if [[ -x "$FILES_DIR/kotlin-lsp/bin/intellij-server" ]]; then
        echo "Kotlin language server is already installed."
        return
    fi

    echo "Downloading Kotlin language server..."
    local archive="$CACHE_DIR/kotlin-server.tar.gz"
    curl -fL "$KOTLIN_URL" -o "$archive"
    tar -xzf "$archive" -C "$FILES_DIR"
    rm -f "$archive"

    local extracted="$FILES_DIR/kotlin-server-$KOTLIN_VERSION"
    if [[ -d "$extracted" ]]; then
        rm -rf "$FILES_DIR/kotlin-lsp"
        mv "$extracted" "$FILES_DIR/kotlin-lsp"
    fi

    [[ -x "$FILES_DIR/kotlin-lsp/bin/intellij-server" ]]
}

install_jdtls() {
    if compgen -G "$FILES_DIR/jdtls/plugins/org.eclipse.equinox.launcher_*.jar" >/dev/null; then
        echo "Eclipse JDT language server is already installed."
        return
    fi

    echo "Downloading Eclipse JDT language server..."
    local archive="$CACHE_DIR/jdtls.tar.gz"
    local staging="$CACHE_DIR/jdtls-install"
    rm -rf "$staging"
    mkdir -p "$staging"
    curl -fL "$JDTLS_URL" -o "$archive"
    tar -xzf "$archive" -C "$staging"
    rm -f "$archive"

    compgen -G "$staging/plugins/org.eclipse.equinox.launcher_*.jar" >/dev/null
    rm -rf "$FILES_DIR/jdtls"
    mv "$staging" "$FILES_DIR/jdtls"
}

install_scala_tools() {
    local cs="$COURSIER_DIR/cs"
    if [[ ! -x "$cs" ]]; then
        echo "Downloading Coursier..."
        curl -fL "$COURSIER_URL" | gzip -d > "$cs.tmp"
        chmod +x "$cs.tmp"
        mv "$cs.tmp" "$cs"
    fi

    echo "Installing the Scala toolchain with Coursier..."
    "$cs" setup --yes --install-dir "$SCALA_BIN"

    echo "Installing Metals..."
    "$cs" install --install-dir "$SCALA_BIN" metals
    [[ -x "$SCALA_BIN/metals" ]]
}

echo "Configuring Cosmic IDE language servers..."
install_kotlin_lsp
install_jdtls
install_scala_tools
echo
echo "Java, Kotlin, and Scala language support is ready."
