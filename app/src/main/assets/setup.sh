#!/usr/bin/env bash

set -euo pipefail

FILES_DIR="${1:?Cosmic IDE files directory is required}"
CACHE_DIR="${2:?Cosmic IDE cache directory is required}"

mkdir -p "$CACHE_DIR"
mkdir -p "$FILES_DIR/glibc/tmp"

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

echo "Configure pacman-managed Arch root..."
echo

setup_pacman

echo

update-ca-trust

echo

echo "Arch runtime is ready."
