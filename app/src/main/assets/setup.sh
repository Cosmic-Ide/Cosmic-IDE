#!/usr/bin/env bash

set -euo pipefail

FILES_DIR="${1:?Cosmic IDE files directory is required}"
CACHE_DIR="${2:?Cosmic IDE cache directory is required}"

KOTLIN_VERSION="262.8190.0"
KOTLIN_URL="https://download-cdn.jetbrains.com/language-server/kotlin-server/${KOTLIN_VERSION}/kotlin-server-${KOTLIN_VERSION}-aarch64.tar.gz"
JDTLS_URL="https://www.eclipse.org/downloads/download.php?file=/jdtls/milestones/1.60.0/jdt-language-server-1.60.0-202606262232.tar.gz"
COURSIER_URL="https://github.com/VirtusLab/coursier-m1/releases/latest/download/cs-aarch64-pc-linux.gz"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip"

SCALA_BIN="$FILES_DIR/scala/bin"
COURSIER_DIR="$FILES_DIR/coursier"

mkdir -p "$CACHE_DIR"

ask_to_install() {
    local name="$1"
    local answer

    while true; do
        if ! read -r -p "Install $name? [y/N] " answer; then
            echo
            return 1
        fi

        case "${answer,,}" in
            y|yes)
                return 0
                ;;
            ""|n|no)
                return 1
                ;;
            *)
                echo "Please enter y or n."
                ;;
        esac
    done
}

install_kotlin_lsp() {
    if [[ -x "$FILES_DIR/kotlin-lsp/bin/intellij-server" ]]; then
        echo "Kotlin language server is already installed."
        return
    fi

    echo "Downloading Kotlin language server..."

    local archive="$CACHE_DIR/kotlin-server.tar.gz"
    local extracted="$FILES_DIR/kotlin-server-$KOTLIN_VERSION"

    curl -fL "$KOTLIN_URL" -o "$archive"
    tar -xzf "$archive" -C "$FILES_DIR"
    rm -f "$archive"

    if [[ -d "$extracted" ]]; then
        rm -rf "$FILES_DIR/kotlin-lsp"
        mv "$extracted" "$FILES_DIR/kotlin-lsp"
    fi

    [[ -x "$FILES_DIR/kotlin-lsp/bin/intellij-server" ]]
    echo "Kotlin language support installed."
}

install_jdtls() {
    local install_dir="$FILES_DIR/jdtls"
    local archive="$CACHE_DIR/jdtls.tar.gz"

    if compgen -G "$install_dir/plugins/org.eclipse.equinox.launcher_*.jar" >/dev/null; then
        echo "Eclipse JDT language server is already installed."
        return
    fi

    echo "Downloading Eclipse JDT language server..."

    rm -rf "$install_dir"
    mkdir -p "$install_dir"

    curl -fL "$JDTLS_URL" -o "$archive"

    if ! tar -xzf "$archive" -C "$install_dir"; then
        rm -rf "$install_dir"
        rm -f "$archive"
        echo "Error: failed to extract Eclipse JDT language server." >&2
        return 1
    fi

    rm -f "$archive"

    if ! compgen -G "$install_dir/plugins/org.eclipse.equinox.launcher_*.jar" >/dev/null; then
        rm -rf "$install_dir"
        echo "Error: Eclipse launcher JAR was not found after extraction." >&2
        return 1
    fi

    echo "Java language support installed."
}

install_scala_tools() {
    mkdir -p "$SCALA_BIN" "$COURSIER_DIR"

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
    echo "Scala language support installed."
}

install_android_sdk() {
    local sdk_root="$FILES_DIR/Android/sdk"
    local cmdline_tools="$sdk_root/cmdline-tools"
    local sdkmanager="$cmdline_tools/latest/bin/sdkmanager"
    local archive="$CACHE_DIR/cmdline-tools.zip"
    local temporary="$sdk_root/cmdline-tools.tmp"
    local bashrc="$HOME/.bashrc"

    if [[ -f "$sdkmanager" ]]; then
        echo "Android SDK command-line tools are already installed."
    else
        echo "Downloading Android SDK command-line tools..."

        mkdir -p "$sdk_root"
        rm -rf "$temporary" "$cmdline_tools"

        curl -fL "$CMDLINE_TOOLS_URL" -o "$archive"
        unzip -q "$archive" -d "$sdk_root"
        rm -f "$archive"

        if [[ ! -d "$cmdline_tools" ]]; then
            echo "Error: archive does not contain cmdline-tools." >&2
            return 1
        fi

        mv "$cmdline_tools" "$temporary"
        mkdir -p "$cmdline_tools"
        mv "$temporary" "$cmdline_tools/latest"

        if [[ ! -f "$sdkmanager" ]]; then
            echo "Error: sdkmanager was not found after extraction." >&2
            return 1
        fi

        chmod +x "$sdkmanager"
    fi

    local path_entry="export PATH=\"\$PATH:$sdk_root/cmdline-tools/latest/bin\""

    touch "$bashrc"

    if ! grep -Fqx "$path_entry" "$bashrc"; then
        echo "$path_entry" >> "$bashrc"
        echo "Added Android SDK command-line tools to PATH in $bashrc"
    fi

    export ANDROID_HOME="$sdk_root"

    if ! grep -Fqx "export ANDROID_HOME=\"$sdk_root\"" "$bashrc"; then
        echo "export ANDROID_HOME=\"$sdk_root\"" >> "$bashrc"
        echo "Added ANDROID_HOME to $bashrc"
    fi

    export PATH="$PATH:$cmdline_tools/latest/bin"

    echo "Checking available Android SDK packages..."

    local package_list
    local build_tools_version
    local platform_api

    package_list="$(
        "$sdkmanager" \
            --sdk_root="$sdk_root" \
            --list \
            --channel=0
    )"

    build_tools_version="$(
        printf '%s\n' "$package_list" |
            sed -n \
                's/^[[:space:]]*build-tools;\([^[:space:]|]*\)[[:space:]]*|.*/\1/p' |
            grep -Ev 'rc|alpha|beta' |
            sort -V |
            tail -n 1
    )"

    platform_api="$(
        printf '%s\n' "$package_list" |
            sed -n \
                's/^[[:space:]]*platforms;android-\([0-9][0-9]*\)[[:space:]]*|.*/\1/p' |
            sort -n |
            tail -n 1
    )"

    if [[ -z "$build_tools_version" ]]; then
        echo "Error: could not determine the latest stable Build Tools version." >&2
        return 1
    fi

    if [[ -z "$platform_api" ]]; then
        echo "Error: could not determine the latest stable Android platform." >&2
        return 1
    fi

    echo "Installing Android SDK packages:"
    echo "  platform-tools"
    echo "  platforms;android-$platform_api"
    echo "  build-tools;$build_tools_version"

    "$sdkmanager" \
        --sdk_root="$sdk_root" \
        --channel=0 \
        "platform-tools" \
        "platforms;android-$platform_api" \
        "build-tools;$build_tools_version"


    curl -fsSL https://raw.githubusercontent.com/Commit451/android-arm-build-tools/main/install.sh | bash

    # Force gradle to use the new aapt2

    local gradle_properties="$HOME/.gradle/gradle.properties"

    mkdir -p "$(dirname "$gradle_properties")"
    touch "$gradle_properties"

    echo "android.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/$build_tools_version/aapt2" >> "$gradle_properties"

    echo
    echo "Android SDK installed at $sdk_root."
}

echo "Configure Cosmic IDE development tools:"
echo

installed_any=false

if ask_to_install "Java language support"; then
    install_jdtls
    installed_any=true
else
    echo "Skipping Java language support."
fi

echo

if ask_to_install "Kotlin language support"; then
    install_kotlin_lsp
    installed_any=true
else
    echo "Skipping Kotlin language support."
fi

echo

if ask_to_install "Scala language support"; then
    install_scala_tools
    installed_any=true
else
    echo "Skipping Scala language support."
fi

echo

if ask_to_install "Android SDK"; then
    install_android_sdk
    installed_any=true
else
    echo "Skipping Android SDK."
fi

echo

echo "Setting up pacman"

mkdir arch

./alarm-pkg gnupg gpgme libassuan libgpg-error pacman pacman-mirrorlist archlinuxarm-keyring sqlite libgcrypt npth

export APP_FILES_DIR="$FILES_DIR/arch"
export PATH="$FILES_DIR/arch/usr/bin:$FILES_DIR/arch/usr/sbin:$PATH"
export LD_LIBRARY_PATH="$FILES_DIR/arch/usr/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
unset GNUPGHOME
hash -r

sed -i "s/DownloadUser/#DownloadUser/" "$FILES_DIR/arch/usr/etc/pacman.conf"
sed -i "s/CheckSpace/#CheckSpace/" "$FILES_DIR/arch/usr/etc/pacman.conf"
pacman-key --init
pacman-key --populate archlinuxarm
pacman -Sy
pacman -S glibc

echo

if [[ "$installed_any" == true ]]; then
    echo "Selected development tools are ready."
else
    echo "No development tools were selected."
fi