#!/bin/sh

set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

OUT_ROOT="$ROOT/glibc"
OUT_DIR="$OUT_ROOT/usr"
ASSETS_TAR_ZST="$ROOT/app/src/main/assets/glibc.tar.zst"
REUSE_GLIBC=0

usage() {
    echo "usage: $0 [--reuse-glibc]" >&2
    echo "  --reuse-glibc  reuse existing ./glibc tree; skip clean + gpkg extraction" >&2
    exit 1
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --reuse-glibc|--no-extract)
            REUSE_GLIBC=1
            ;;
        --help|-h)
            usage
            ;;
        *)
            echo "error: unknown option: $1" >&2
            usage
            ;;
    esac
    shift
done

if ! command -v tar >/dev/null 2>&1; then
    echo "error: missing tar" >&2
    exit 1
fi

if ! command -v zstd >/dev/null 2>&1; then
    echo "error: missing zstd" >&2
    exit 1
fi

if [ "$REUSE_GLIBC" = "0" ] && ! command -v jq >/dev/null 2>&1; then
    echo "error: missing jq" >&2
    exit 1
fi

download_file() {
    url="$1"
    destination="$2"
    temporary="$destination.tmp.$$"

    mkdir -p "$(dirname "$destination")"
    rm -f "$temporary"

    if command -v curl >/dev/null 2>&1; then
        curl \
            --fail \
            --location \
            --retry 3 \
            --retry-delay 2 \
            --connect-timeout 30 \
            --output "$temporary" \
            "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget \
            --tries=3 \
            --timeout=30 \
            --output-document="$temporary" \
            "$url"
    else
        echo "error: curl or wget is required to download pacman" >&2
        return 1
    fi

    if [ ! -s "$temporary" ]; then
        echo "error: downloaded empty file: $url" >&2
        rm -f "$temporary"
        return 1
    fi

    mv -f "$temporary" "$destination"
}

cd "$ROOT"

chmod +x "$ROOT/scripts/build-shims.sh"

if [ "$REUSE_GLIBC" = "1" ]; then
    echo "reusing existing glibc tree: $OUT_ROOT"

    if [ ! -d "$OUT_DIR" ]; then
        echo "error: cannot reuse missing directory: $OUT_DIR" >&2
        exit 1
    fi
else
    echo "cleaning $OUT_ROOT"
    rm -rf "$OUT_ROOT"

    echo "building glibc..."
    chmod +x "$ROOT/scripts/glibc.sh"

    set -- \
        glibc \
        coreutils-glibc \
        bash-glibc \
        ncurses-utils-glibc \
        bsdtar-glibc \
        xz-utils-glibc \
        ca-certificates-glibc \
        libgpg-error-glibc \
        libidn2-glibc \
        libssh2-glibc \
        libnghttp2-glibc \
        libseccomp-glibc \
        libsqlite-glibc \
        libgcrypt-glibc \
        curl-glibc


    echo "requested packages:"
    printf '  %s\n' "$@"

    "$ROOT/scripts/glibc.sh" \
        --install \
        --keep-symlinks \
        --out "$OUT_DIR" \
        "$@"

    echo "checking required downloaded roots..."

    for pkg in "$@"; do
        filename="$(
            jq -r \
                --arg p "$pkg" \
                '.[$p].FILENAME // empty' \
                "$ROOT/gpkg-cache/gpkg.json"
        )"

        if [ -z "$filename" ]; then
            echo "error: package missing from gpkg.json: $pkg" >&2
            exit 1
        fi

        if [ ! -s "$ROOT/gpkg-cache/$filename" ]; then
            echo "error: package was not downloaded: $pkg -> $filename" >&2
            exit 1
        fi
    done
fi

"$ROOT/scripts/build-shims.sh"

echo "creating asset archive..."
mkdir -p "$(dirname "$ASSETS_TAR_ZST")" "$OUT_ROOT"
rm -f "$ASSETS_TAR_ZST"
rm -f "$ROOT/app/src/main/assets/glibc.zip"

SYMLINK_MANIFEST="$OUT_ROOT/.symlinks"
: > "$SYMLINK_MANIFEST"

normalize_rel_path() {
    path="$1"
    out=""

    while [ -n "$path" ]; do
        case "$path" in
            */*)
                part="${path%%/*}"
                path="${path#*/}"
                ;;
            *)
                part="$path"
                path=""
                ;;
        esac

        case "$part" in
            ""|.)
                ;;
            ..)
                out="${out%/*}"
                ;;
            *)
                if [ -z "$out" ]; then
                    out="$part"
                else
                    out="$out/$part"
                fi
                ;;
        esac
    done

    printf '%s\n' "$out"
}

relative_path_from_dir() {
    from_dir="$1"
    to_path="$2"

    [ "$from_dir" = "." ] && from_dir=""

    from="$from_dir"
    to="$to_path"

    while [ -n "$from" ] && [ -n "$to" ]; do
        from_head="${from%%/*}"
        to_head="${to%%/*}"

        [ "$from_head" = "$to_head" ] || break

        if [ "$from" = "$from_head" ]; then
            from=""
        else
            from="${from#*/}"
        fi

        if [ "$to" = "$to_head" ]; then
            to=""
        else
            to="${to#*/}"
        fi
    done

    up=""

    while [ -n "$from" ]; do
        up="../$up"

        case "$from" in
            */*)
                from="${from#*/}"
                ;;
            *)
                from=""
                ;;
        esac
    done

    printf '%s%s\n' "$up" "$to"
}

map_abs_target_to_out_rel() {
    abs="$1"

    case "$abs" in
        /data/data/com.termux/files/usr/glibc/*)
            printf 'usr/%s\n' \
                "${abs#/data/data/com.termux/files/usr/glibc/}"
            ;;
        /data/data/com.termux/files/usr/bin/*)
            printf 'usr/bin/%s\n' \
                "${abs#/data/data/com.termux/files/usr/bin/}"
            ;;
        /data/data/com.termux/files/usr/lib/*)
            printf 'usr/lib/%s\n' \
                "${abs#/data/data/com.termux/files/usr/lib/}"
            ;;
        /data/data/com.termux/files/usr/include/*)
            printf 'usr/include/%s\n' \
                "${abs#/data/data/com.termux/files/usr/include/}"
            ;;
        /data/data/com.termux/files/usr/share/*)
            printf 'usr/share/%s\n' \
                "${abs#/data/data/com.termux/files/usr/share/}"
            ;;
        /data/data/com.termux/files/usr/etc/*)
            printf 'usr/etc/%s\n' \
                "${abs#/data/data/com.termux/files/usr/etc/}"
            ;;
        /data/data/com.termux/files/usr/libexec/*)
            printf 'usr/libexec/%s\n' \
                "${abs#/data/data/com.termux/files/usr/libexec/}"
            ;;
        *)
            return 1
            ;;
    esac
}

find_final_target_for_symlink() {
    link_rel="$1"
    raw_target="$2"

    link_dir_rel="$(dirname "$link_rel")"

    case "$raw_target" in
        /*)
            target_rel="$(
                map_abs_target_to_out_rel "$raw_target" 2>/dev/null
            )" || {
                echo \
                    "warning: skipping external symlink target: $link_rel -> $raw_target" \
                    >&2
                return 1
            }
            ;;
        *)
            if [ "$link_dir_rel" = "." ]; then
                target_rel="$(normalize_rel_path "$raw_target")"
            else
                target_rel="$(
                    normalize_rel_path "$link_dir_rel/$raw_target"
                )"
            fi
            ;;
    esac

    target_abs="$OUT_ROOT/$target_rel"

    if [ ! -e "$target_abs" ] && [ ! -L "$target_abs" ]; then
        echo \
            "warning: skipping missing symlink target: $link_rel -> $raw_target resolved=$target_rel" \
            >&2
        return 1
    fi

    relative_path_from_dir "$link_dir_rel" "$target_rel"
}

out_prefix="$OUT_ROOT/"

find "$OUT_DIR" -type l | sort | while IFS= read -r link; do
    rel="${link#"$out_prefix"}"
    raw_target="$(readlink "$link")"

    target="$(find_final_target_for_symlink "$rel" "$raw_target")" ||
        continue

    printf '%s\t%s\n' "$rel" "$target" >> "$SYMLINK_MANIFEST"
done

# Disable macOS copyfile metadata to avoid "xattrs not supported"
# errors when extracting the tar on Linux.
export COPYFILE_DISABLE=1

TAR_LIST="$ROOT/.glibc-tar-files.$$"
ASSETS_TMP="$ASSETS_TAR_ZST.tmp.$$"

cleanup_archive() {
    rm -f "$TAR_LIST" "$ASSETS_TMP"
}

trap cleanup_archive EXIT HUP INT TERM

find glibc -type f -print | LC_ALL=C sort > "$TAR_LIST"

tar \
    --no-xattrs \
    -cf - \
    -T "$TAR_LIST" |
    zstd \
        -19 \
        -T0 \
        --long=30 \
        -f \
        -o "$ASSETS_TMP"

mv -f "$ASSETS_TMP" "$ASSETS_TAR_ZST"
mv "$OUT_DIR/lib/ld-linux-aarch64.so.1" "$ROOT/app/src/main/jniLibs/arm64-v8a/libld_linux.so"

trap - EXIT HUP INT TERM
rm -f "$TAR_LIST"

echo "done: $ASSETS_TAR_ZST"
