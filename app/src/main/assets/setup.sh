#!/usr/bin/env bash

set -euo pipefail

FILES_DIR="${1:?Cosmic IDE files directory is required}"
CACHE_DIR="${2:?Cosmic IDE cache directory is required}"

KOTLIN_VERSION="262.9593.0"
KOTLIN_URL="https://download-cdn.jetbrains.com/language-server/kotlin-server/${KOTLIN_VERSION}/kotlin-server-${KOTLIN_VERSION}-aarch64.tar.gz"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip"
VSCODE_GRADLE_URL="https://github.com/microsoft/vscode-gradle.git"

VSCODE_GRADLE_DIR="$FILES_DIR/vscode-gradle"
ARCH_DIR="$FILES_DIR/arch"

mkdir -p "$CACHE_DIR"
mkdir -p "$FILES_DIR/glibc/tmp"

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

extract_tar_gz() {
    local archive="$1"
    local destination="$2"

    if ! command -v unpigz >/dev/null 2>&1; then
        echo "Error: unpigz is unavailable. Update the Arch runtime to install pigz." >&2
        return 1
    fi

    mkdir -p "$destination"
    unpigz -c "$archive" | tar -xf - -C "$destination"
}

install_kotlin_lsp() {
    local install_dir="$FILES_DIR/kotlin-lsp"
    local archive="$CACHE_DIR/kotlin-lsp.tar.gz"
    local temporary="$FILES_DIR/kotlin-lsp.tmp"
    local extracted="$temporary/kotlin-server-$KOTLIN_VERSION"
    local executable="$install_dir/bin/intellij-server"

    if [[ -x "$executable" ]]; then
        echo "Kotlin language server is already installed."
        return
    fi

    echo "Downloading Kotlin language server..."

    rm -rf "$temporary"
    curl -fL "$KOTLIN_URL" -o "$archive"
    if ! extract_tar_gz "$archive" "$temporary"; then
        rm -rf "$temporary"
        rm -f "$archive"
        echo "Error: failed to extract Kotlin language server." >&2
        return 1
    fi
    rm -f "$archive"

    if [[ ! -f "$extracted/bin/intellij-server" ]]; then
        rm -rf "$temporary"
        echo "Error: Kotlin language server executable was not found after extraction." >&2
        return 1
    fi

    chmod +x "$extracted/bin/intellij-server"
    rm -rf "$install_dir"
    mv "$extracted" "$install_dir"
    rm -rf "$temporary"
    rm -rf "$FILES_DIR/kotlin-language-server"

    [[ -x "$executable" ]]
    echo "Kotlin language support installed."
}

install_gradle_language_server() {
    local server_source="$VSCODE_GRADLE_DIR/gradle-language-server/src/main/java/com/microsoft/gradle/GradleLanguageServer.java"
    local server_executable="$VSCODE_GRADLE_DIR/gradle-language-server/build/install/gradle-language-server/bin/gradle-language-server"

    if [[ -x "$server_executable" ]]; then
        echo "Gradle/Groovy language server is already installed."
        return
    fi

    if [[ -e "$VSCODE_GRADLE_DIR" && ! -d "$VSCODE_GRADLE_DIR/.git" ]]; then
        echo "Error: $VSCODE_GRADLE_DIR exists but is not a vscode-gradle checkout." >&2
        return 1
    fi

    if [[ ! -d "$VSCODE_GRADLE_DIR/.git" ]]; then
        if ! command -v git >/dev/null 2>&1; then
            echo "Error: Git is not installed in the existing Arch runtime." >&2
            echo "Rerun setup and allow the Arch runtime update to install it." >&2
            return 1
        fi
        echo "Cloning the Gradle language server..."
        git clone "$VSCODE_GRADLE_URL" "$VSCODE_GRADLE_DIR"
    fi

    if [[ ! -f "$server_source" ]]; then
        echo "Error: Gradle language server source was not found at $server_source" >&2
        return 1
    fi

    echo "Configuring the Gradle language server for stdio..."

    sed -i \
        -e '/import com\.microsoft\.gradle\.transport\.NamedPipeStream;/d' \
        -e '/import java\.io\.IOException;/d' \
        -e '/NamedPipeStream pipeStream = new NamedPipeStream(args\[0\]);/d' \
        -e 's/pipeStream\.getInputStream()/System.in/g' \
        -e 's/pipeStream\.getOutputStream()/System.out/g' \
        -e 's/catch (IOException e)/catch (Exception e)/g' \
        "$server_source"

    if ! grep -Fq 'Object settings = initOptions.get("settings");' "$server_source"; then
        sed -i \
            '/this\.gradleServices\.applySetting(settings);/i\
		Object settings = initOptions.get("settings");' \
            "$server_source"
    fi

    if grep -Fq 'NamedPipeStream pipeStream' "$server_source" ||
        grep -Fq 'pipeStream.getInputStream()' "$server_source" ||
        grep -Fq 'pipeStream.getOutputStream()' "$server_source" ||
        ! grep -Fq 'Object settings = initOptions.get("settings");' "$server_source"; then
        echo "Error: failed to apply the Gradle language server stdio patch." >&2
        return 1
    fi

    echo "Building the Gradle/Groovy language server..."
    (
        cd "$VSCODE_GRADLE_DIR"
        ./gradlew \
            :gradle-language-server:spotlessApply \
            :gradle-language-server:build \
            :gradle-language-server:installDist
    )

    if [[ ! -x "$server_executable" ]]; then
        echo "Error: Gradle language server executable was not created." >&2
        return 1
    fi

    echo "Gradle/Groovy language support installed."
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

    local gradle_properties="$HOME/.gradle/gradle.properties"

    mkdir -p "$(dirname "$gradle_properties")"
    touch "$gradle_properties"

    local aapt2_override="android.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/$build_tools_version/aapt2"
    if grep -q '^android\.aapt2FromMavenOverride=' "$gradle_properties"; then
        sed -i "s|^android\.aapt2FromMavenOverride=.*|$aapt2_override|" "$gradle_properties"
    else
        echo "$aapt2_override" >> "$gradle_properties"
    fi

    echo
    echo "Android SDK installed at $sdk_root."
}

ask_to_reinstall_arch() {
    local answer

    while true; do
        if ! read -r -p "An Arch runtime already exists. Reinstall/update it? [y/N] " answer; then
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

activate_arch_runtime() {
    local arch_root="$FILES_DIR/arch"
    local glibc_root="$FILES_DIR/glibc"

    export APP_FILES_DIR="$arch_root"
    export PATH="$arch_root/usr/bin:$arch_root/usr/sbin:$glibc_root/usr/bin:$glibc_root/usr/sbin:$PATH"
    export LD_LIBRARY_PATH="$arch_root/usr/lib:$glibc_root/usr/lib:$glibc_root/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
    export HOME="$arch_root/home"
    mkdir -p "$HOME"
    hash -r
}

setup_pacman() {
    local arch_root="$FILES_DIR/arch"
    local glibc_root="$FILES_DIR/glibc"

    local arch_etc="$arch_root/etc"
    local arch_pacman_conf="$arch_etc/pacman.conf"
    local arch_mirrorlist="$arch_etc/pacman.d/mirrorlist"
    local arch_gpg_dir="$arch_etc/pacman.d/gnupg"
    local arch_db_path="$arch_root/var/lib/pacman"
    local arch_cache_dir="$arch_root/var/cache/pacman/pkg"
    local arch_log_file="$arch_root/var/log/pacman.log"

    if [[ -d "$arch_root/usr" ]]; then
        if ! ask_to_reinstall_arch; then
            activate_arch_runtime
            echo "Keeping the existing Arch runtime at $arch_root."
            return
        fi
        echo "Updating the existing Arch runtime..."
    fi

    echo "Setting up pacman bootstrap runtime in $glibc_root"

    mkdir -p "$glibc_root" "$arch_root"

    # alarm-pkg is only a bootstrap extractor. Keep everything it installs in
    # glibc/, never in the pacman-managed arch/ root.
    ./alarm-pkg \
        --prefix "$glibc_root" \
        gnupg \
        gpgme \
        libassuan \
        pacman \
        pacman-mirrorlist \
        archlinuxarm-keyring \
        npth

    export APP_FILES_DIR="$glibc_root"
    export PATH="$glibc_root/usr/bin:$glibc_root/usr/sbin:$PATH"
    export LD_LIBRARY_PATH="$glibc_root/usr/lib:$glibc_root/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
    export HOME="$glibc_root/home"
    mkdir -p "$HOME"
    unset GNUPGHOME
    hash -r

    local bootstrap_conf="$glibc_root/usr/etc/pacman.conf"
    local bootstrap_mirrorlist="$glibc_root/usr/etc/pacman.d/mirrorlist"

    if [[ ! -f "$bootstrap_conf" ]]; then
        echo "Error: bootstrap pacman.conf was not installed at $bootstrap_conf" >&2
        return 1
    fi

    if [[ ! -f "$bootstrap_mirrorlist" ]]; then
        echo "Error: bootstrap mirrorlist was not installed at $bootstrap_mirrorlist" >&2
        return 1
    fi

    mkdir -p \
        "$arch_etc/pacman.d" \
        "$arch_gpg_dir" \
        "$arch_db_path" \
        "$arch_cache_dir" \
        "$(dirname "$arch_log_file")"

    cp -f "$bootstrap_conf" "$arch_pacman_conf"
    cp -f "$bootstrap_mirrorlist" "$arch_mirrorlist"

    # pacman --root does not chroot. Make Include point at the real host path.
    sed -i \
        -e 's/^[[:space:]]*DownloadUser/#DownloadUser/' \
        -e 's/^[[:space:]]*CheckSpace/#CheckSpace/' \
        -e 's/^[[:space:]]*#[[:space:]]*IgnorePkg[[:space:]]*=/IgnorePkg = glibc/' \
        -e "s|^[[:space:]]*Include[[:space:]]*=[[:space:]]*/etc/pacman.d/mirrorlist|Include = $arch_mirrorlist|" \
        "$arch_pacman_conf"

    if ! grep -Fq '[gpkg]' "$arch_pacman_conf"; then
        printf '\n\n[gpkg]\nSigLevel = Never\nServer = http://ftp.agdsn.de/termux-pacman/gpkg/aarch64\n' \
            >> "$arch_pacman_conf"
    fi

    echo "Bootstrap root: $glibc_root"
    echo "Pacman root:    $arch_root"
    echo "Pacman config:  $arch_pacman_conf"

    pacman-key \
        --config "$arch_pacman_conf" \
        --gpgdir "$arch_gpg_dir" \
        --init

    pacman-key \
        --config "$arch_pacman_conf" \
        --gpgdir "$arch_gpg_dir" \
        --populate archlinuxarm

    local pacman_args=(
        --root "$arch_root"
        --config "$arch_pacman_conf"
        --dbpath "$arch_db_path"
        --cachedir "$arch_cache_dir"
        --gpgdir "$arch_gpg_dir"
        --logfile "$arch_log_file"
        --arch aarch64
    )

    pacman "${pacman_args[@]}" -Sy

    rm -rf "$arch_root/data"
    mkdir -p "$arch_root/data/data/com.termux/files/usr/glibc"

    pacman "${pacman_args[@]}" \
        -S \
        --noconfirm \
        --noscriptlet \
        --hookdir "$CACHE_DIR" \
        --ignore "" \
        gpkg/glibc

    pacman "${pacman_args[@]}" \
        -S \
        --needed \
        --noconfirm \
        --quiet \
        --noscriptlet \
        --hookdir "$CACHE_DIR" \
        filesystem \
        bash \
        pacman \
        pacman-mirrorlist \
        archlinuxarm-keyring \
        linux-api-headers \
        tzdata \
        curl \
        git \
        pigz \
        tar \
        unzip \
        jdk-openjdk

    echo "Configuring SSL root certificates bundle..."
    mkdir -p "$arch_root/etc/ssl/certs"

    cp -f --remove-destination "$glibc_root/usr/etc/ssl/certs/ca-certificates.crt" "$arch_root/etc/ssl/certs/ca-certificates.crt"
    cp -f --remove-destination "$glibc_root/usr/etc/ssl/certs/ca-bundle.crt" "$arch_root/etc/ssl/certs/ca-bundle.crt"
    cp -f --remove-destination "$glibc_root/usr/etc/ssl/cert.pem" "$arch_root/etc/ssl/cert.pem"

    local termux_glibc="$arch_root/data/data/com.termux/files/usr/glibc"
    if [[ -d "$termux_glibc" ]]; then
        echo "Relocating extracted Termux glibc environment to root and /usr targets..."

        (cd "$termux_glibc" && tar --exclude='./share/doc' --exclude='./share/LICENSES' -cf - .) | \
        tee >(cd "$arch_root/usr" && tar -xf -) | \
        (cd "$arch_root" && tar -xf -)

        rm -rf "$arch_root/data"
    fi

    local bashrc="$arch_root/home/.bashrc"
    local home_declaration='export HOME=$(cd /data/data/org.cosmicide/files/arch/home && pwd -P)'
    mkdir -p "$(dirname "$bashrc")"
    touch "$bashrc"

    if ! grep -Fqx "$home_declaration" "$bashrc"; then
        {
            printf '%s\n' '[[ $- != *i* ]] && return'
            printf '%s\n' '[[ $DISPLAY ]] && shopt -s checkwinsize'
            printf '%s\n' "$home_declaration"
            printf '%s\n' "PS1='\[\e[0;32m\]\w\[\e[0m\] \[\e[0;97m\]\$\[\e[0m\] '"
            printf '%s\n' 'case ${TERM} in'
            printf '%s\n' '  Eterm*|alacritty*|aterm*|foot*|gnome*|konsole*|kterm*|putty*|rxvt*|tmux*|xterm*)'
            printf '%s\n' '    PROMPT_COMMAND+=('\''printf "\033]0;%s@%s:%s\007" "${USER}" "${HOSTNAME%%.*}" "${PWD/#$HOME/\~}"'\'')'
            printf '%s\n' '    ;;'
            printf '%s\n' '  screen*)'
            printf '%s\n' '    PROMPT_COMMAND+=('\''printf "\033_%s@%s:%s\033\\" "${USER}" "${HOSTNAME%%.*}" "${PWD/#$HOME/\~}"'\'')'
            printf '%s\n' '    ;;'
            printf '%s\n' 'esac'
        } >> "$bashrc"
    fi

    # From this point onward, prefer the real pacman-managed Arch userspace.
    activate_arch_runtime

    echo "Pacman-managed Arch root is ready at $arch_root."
}

update-ca-trust

echo "Configure Cosmic IDE development tools:"
echo

installed_any=false

setup_pacman

echo

if ask_to_install "Kotlin language support"; then
    install_kotlin_lsp
    installed_any=true
else
    echo "Skipping Kotlin language support."
fi

echo

#if ask_to_install "Gradle/Groovy language support"; then
#    install_gradle_language_server
#    installed_any=true
#else
#    echo "Skipping Gradle/Groovy language support."
#fi

echo

if ask_to_install "Android SDK"; then
    install_android_sdk
    installed_any=true
else
    echo "Skipping Android SDK."
fi

echo

if [[ "$installed_any" == true ]]; then
    echo "Selected development tools are ready."
else
    echo "No development tools were selected."
fi
