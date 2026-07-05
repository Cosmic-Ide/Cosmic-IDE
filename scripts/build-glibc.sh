#!/bin/sh

set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

OUT_ROOT="$ROOT/glibc"
OUT_DIR="$OUT_ROOT/usr"
ASSETS_ZIP="$ROOT/app/src/main/assets/glibc.zip"

if ! command -v zip >/dev/null 2>&1; then
    echo "error: missing zip" >&2
    exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
    echo "error: missing jq" >&2
    exit 1
fi

cd "$ROOT"

echo "cleaning $OUT_ROOT"
rm -rf "$OUT_ROOT"

echo "building glibc..."
chmod +x "$ROOT/scripts/glibc.sh"
chmod +x "$ROOT/scripts/build-shims.sh"

set -- \
    glibc \
    gcc-libs-glibc \
    coreutils-glibc \
    bash-glibc \
    binutils-glibc \
    ca-certificates-glibc \
    gcc-glibc \
    clang-glibc \
    cmake-glibc \
    llvm-glibc \
    make-glibc

echo "requested packages:"
printf '  %s\n' "$@"

"$ROOT/scripts/glibc.sh" --install --out "$OUT_DIR" "$@"

echo "checking required downloaded roots..."
for pkg in "$@"; do
    filename="$(jq -r --arg p "$pkg" '.[$p].FILENAME // empty' "$ROOT/gpkg-cache/gpkg.json")"

    if [ -z "$filename" ]; then
        echo "error: package missing from gpkg.json: $pkg" >&2
        exit 1
    fi

    if [ ! -s "$ROOT/gpkg-cache/$filename" ]; then
        echo "error: package was not downloaded: $pkg -> $filename" >&2
        exit 1
    fi
done

"$ROOT/scripts/build-shims.sh"

echo "checking required binaries..."
require_bin() {
    name="$1"
    if [ -e "$OUT_DIR/bin/$name" ]; then
        return 0
    fi
    echo "error: missing required bin: $OUT_DIR/bin/$name" >&2
    exit 1
}

for bin in ls mkdir cp rm cat chmod chown ln sh bash gcc g++ make cmake ar ranlib; do
    require_bin "$bin"
done

echo "creating asset zip..."
mkdir -p "$(dirname "$ASSETS_ZIP")" "$OUT_ROOT"
rm -f "$ASSETS_ZIP"

SYMLINK_MANIFEST="$OUT_ROOT/.symlinks"
: > "$SYMLINK_MANIFEST"

normalize_rel_path() {
    path="$1"

    old_ifs="$IFS"
    IFS=/
    set -- $path
    IFS="$old_ifs"

    out=""
    for part do
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
            */*) from="${from#*/}" ;;
            *) from="" ;;
        esac
    done

    printf '%s%s\n' "$up" "$to"
}

map_abs_target_to_out_rel() {
    abs="$1"

    case "$abs" in
        /data/data/com.termux/files/usr/glibc/*)
            printf 'usr/%s\n' "${abs#/data/data/com.termux/files/usr/glibc/}"
            ;;
        /data/data/com.termux/files/usr/bin/*)
            printf 'usr/bin/%s\n' "${abs#/data/data/com.termux/files/usr/bin/}"
            ;;
        /data/data/com.termux/files/usr/lib/*)
            printf 'usr/lib/%s\n' "${abs#/data/data/com.termux/files/usr/lib/}"
            ;;
        /data/data/com.termux/files/usr/include/*)
            printf 'usr/include/%s\n' "${abs#/data/data/com.termux/files/usr/include/}"
            ;;
        /data/data/com.termux/files/usr/share/*)
            printf 'usr/share/%s\n' "${abs#/data/data/com.termux/files/usr/share/}"
            ;;
        /data/data/com.termux/files/usr/etc/*)
            printf 'usr/etc/%s\n' "${abs#/data/data/com.termux/files/usr/etc/}"
            ;;
        /data/data/com.termux/files/usr/libexec/*)
            printf 'usr/libexec/%s\n' "${abs#/data/data/com.termux/files/usr/libexec/}"
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
            target_rel="$(map_abs_target_to_out_rel "$raw_target" 2>/dev/null)" || {
                echo "warning: skipping external symlink target: $link_rel -> $raw_target" >&2
                return 1
            }
            ;;
        *)
            if [ "$link_dir_rel" = "." ]; then
                target_rel="$(normalize_rel_path "$raw_target")"
            else
                target_rel="$(normalize_rel_path "$link_dir_rel/$raw_target")"
            fi
            ;;
    esac

    target_abs="$OUT_ROOT/$target_rel"

    if [ ! -e "$target_abs" ] && [ ! -L "$target_abs" ]; then
        echo "warning: skipping missing symlink target: $link_rel -> $raw_target resolved=$target_rel" >&2
        return 1
    fi

    relative_path_from_dir "$link_dir_rel" "$target_rel"
}

find "$OUT_DIR" -type l | sort | while IFS= read -r link; do
    rel="${link#$OUT_ROOT/}"
    raw_target="$(readlink "$link")"

    target="$(find_final_target_for_symlink "$rel" "$raw_target")" || continue

    printf '%s\t%s\n' "$rel" "$target" >> "$SYMLINK_MANIFEST"
done

(
    cd "$ROOT"
    find glibc \( -type d -o -type f \) -print | sort | zip -q "$ASSETS_ZIP" -@
)

echo "cleaning up"
# rm -rf "$OUT_DIR"

echo "done: $ASSETS_ZIP"