#!/bin/sh

# Usage:
#   ./gpkg-tree.sh apr-glibc
#   ./gpkg-tree.sh glibc apr-glibc gcc-libs-glibc
#   ./gpkg-tree.sh --install apr-glibc
#   ./gpkg-tree.sh --install glibc apr-glibc
#   ./gpkg-tree.sh --install --out ./glibc/usr glibc apr-glibc
#   ./gpkg-tree.sh --download-only glibc apr-glibc
#   ./gpkg-tree.sh --install --clean-out glibc gcc-glibc clang-glibc cmake-glibc
#   ./gpkg-tree.sh --install --keep-symlinks glibc gcc-glibc clang-glibc cmake-glibc
#   ./gpkg-tree.sh --install --no-nss-wrapper glibc apr-glibc
#
# Requires:
#   jq, curl or wget, tar, find, cp, readlink
# Optional/fallback:
#   xz for .tar.xz if tar lacks -J
#   zstd for .tar.zst if tar lacks --zstd
#   ar when nss-wrapper install is enabled

REPO_URL="https://ftp.agdsn.de/termux-pacman/gpkg/aarch64"
NSS_WRAPPER_URL="http://ftp.debian.org/debian/pool/main/n/nss-wrapper/libnss-wrapper_1.1.16-5+b1_arm64.deb"

# Original Termux paths baked into many gpkg symlinks.
TERMUX_PREFIX="/data/data/com.termux/files/usr"
TERMUX_GLIBC_PREFIX="$TERMUX_PREFIX/glibc"

CACHE_DIR="./gpkg-cache"
OUT_DIR="./glibc/usr"

INSTALL=0
DOWNLOAD_ONLY=0
UPDATE_DB=0
INSTALL_NSS_WRAPPER=1
CLEAN_OUT=0
MATERIALIZE_SYMLINKS=1
ROOTS=""

usage() {
    echo "usage: $0 [--install|--download-only] [--out DIR] [--cache DIR] [--repo URL] [--nss-wrapper-url URL] [--no-nss-wrapper] [--clean-out] [--keep-symlinks] [--update-db] <package> [package...]" >&2
    exit 1
}

append_root() {
    if [ -z "$ROOTS" ]; then
        ROOTS="$1"
    else
        ROOTS="$ROOTS
$1"
    fi
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --install|-i)
            INSTALL=1
            ;;
        --download-only)
            DOWNLOAD_ONLY=1
            ;;
        --out|-o)
            shift
            [ "$#" -gt 0 ] || usage
            OUT_DIR="$1"
            ;;
        --cache)
            shift
            [ "$#" -gt 0 ] || usage
            CACHE_DIR="$1"
            ;;
        --repo)
            shift
            [ "$#" -gt 0 ] || usage
            REPO_URL="$1"
            ;;
        --nss-wrapper-url)
            shift
            [ "$#" -gt 0 ] || usage
            NSS_WRAPPER_URL="$1"
            ;;
        --no-nss-wrapper)
            INSTALL_NSS_WRAPPER=0
            ;;
        --clean-out)
            CLEAN_OUT=1
            ;;
        --keep-symlinks)
            MATERIALIZE_SYMLINKS=0
            ;;
        --update-db)
            UPDATE_DB=1
            ;;
        -* )
            usage
            ;;
        *)
            append_root "$1"
            ;;
    esac
    shift
done

[ -n "$ROOTS" ] || usage

DB="$CACHE_DIR/gpkg.json"
TMP_ROOT="${TMPDIR:-/tmp}/gpkg-tree.$$"

mkdir -p "$CACHE_DIR" "$TMP_ROOT" || exit 1
trap 'rm -rf "$TMP_ROOT"' EXIT INT TERM

need_cmd() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "error: missing command: $1" >&2
        exit 1
    fi
}

need_cmd jq
need_cmd tar
need_cmd find
need_cmd cp
need_cmd mkdir
need_cmd sed
need_cmd grep
need_cmd wc
need_cmd basename
need_cmd dirname
need_cmd readlink

if [ "$INSTALL_NSS_WRAPPER" = "1" ] && [ "$INSTALL" = "1" ]; then
    need_cmd ar
fi

download_url() {
    url="$1"
    out="$2"

    if command -v curl >/dev/null 2>&1; then
        curl -L --fail -o "$out" "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$out" "$url"
    else
        echo "error: need curl or wget" >&2
        return 1
    fi
}

fetch_db() {
    if [ "$UPDATE_DB" = "1" ] || [ ! -s "$DB" ]; then
        echo "fetching gpkg.json..."
        download_url "$REPO_URL/gpkg.json" "$DB.tmp" || exit 1
        mv "$DB.tmp" "$DB" || exit 1
    fi
}

pkg_exists() {
    jq -e --arg p "$1" 'has($p)' "$DB" >/dev/null 2>&1
}

pkg_filename() {
    jq -r --arg p "$1" '.[$p].FILENAME // empty' "$DB"
}

get_deps() {
    jq -r --arg p "$1" '
        def as_array:
            if . == null then []
            elif type == "array" then .
            else [.] end;

        .[$p].DEPENDS
        | as_array[]
        | tostring
        | sub("[<>=].*$"; "")
        | sub(":.*$"; "")
    ' "$DB" 2>/dev/null | sed '/^$/d'
}

print_tree() {
    # IMPORTANT: this function is recursive. In /bin/sh, function variables are
    # global unless declared local, so without these declarations child calls
    # clobber the parent frame and the tree/order becomes garbage.
    local pkg prefix edge last seen deps_file child_prefix count i dep

    pkg="$1"
    prefix="$2"
    edge="$3"
    last="$4"
    seen="$5"

    if [ -n "$edge" ]; then
        if ! pkg_exists "$pkg"; then
            printf '%s%s%s [missing]\n' "$prefix" "$edge" "$pkg"
            return
        fi

        if grep -Fxq "$pkg" "$seen" 2>/dev/null; then
            printf '%s%s%s [already shown]\n' "$prefix" "$edge" "$pkg"
            return
        fi

        printf '%s%s%s\n' "$prefix" "$edge" "$pkg"
    else
        if ! pkg_exists "$pkg"; then
            printf '%s [missing]\n' "$pkg"
            return
        fi

        if grep -Fxq "$pkg" "$seen" 2>/dev/null; then
            printf '%s [already shown]\n' "$pkg"
            return
        fi

        printf '%s\n' "$pkg"
    fi

    echo "$pkg" >> "$seen"

    deps_file="$TMP_ROOT/deps.tree.$pkg"
    get_deps "$pkg" > "$deps_file"

    [ -s "$deps_file" ] || return

    if [ -z "$edge" ]; then
        child_prefix="$prefix"
    elif [ "$last" = "1" ]; then
        child_prefix="${prefix}    "
    else
        child_prefix="${prefix}│   "
    fi

    count="$(wc -l < "$deps_file" | tr -d ' ')"
    i=0

    while IFS= read -r dep; do
        i=$((i + 1))

        if [ "$i" -eq "$count" ]; then
            print_tree "$dep" "$child_prefix" "└── " "1" "$seen"
        else
            print_tree "$dep" "$child_prefix" "├── " "0" "$seen"
        fi
    done < "$deps_file"
}

resolve_install_order() {
    # IMPORTANT: this function is recursive. In /bin/sh, assignments inside a
    # function are global unless declared local. Without local variables, a
    # dependency call overwrites the caller's pkg/deps_file/dep variables, which
    # is why the install order showed duplicates and lost requested roots.
    local pkg seen order missing deps_file dep

    pkg="$1"
    seen="$2"
    order="$3"
    missing="$4"

    if grep -Fxq "$pkg" "$seen" 2>/dev/null; then
        return
    fi

    echo "$pkg" >> "$seen"

    if ! pkg_exists "$pkg"; then
        echo "$pkg" >> "$missing"
        return
    fi

    deps_file="$TMP_ROOT/deps.resolve.$pkg"
    get_deps "$pkg" > "$deps_file"

    while IFS= read -r dep; do
        [ -n "$dep" ] || continue
        resolve_install_order "$dep" "$seen" "$order" "$missing"
    done < "$deps_file"

    # Dependencies first, requested/current package after them.
    echo "$pkg" >> "$order"
}

download_pkg() {
    pkg="$1"

    filename="$(pkg_filename "$pkg")"
    if [ -z "$filename" ]; then
        echo "warning: no filename for $pkg" >&2
        return 1
    fi

    archive="$CACHE_DIR/$filename"

    if [ -s "$archive" ]; then
        echo "cached: $filename"
        return 0
    fi

    echo "downloading: $filename"
    download_url "$REPO_URL/$filename" "$archive.part" || {
        rm -f "$archive.part"
        return 1
    }

    mv "$archive.part" "$archive"
}

nss_wrapper_deb_path() {
    deb_basename="$(basename "$NSS_WRAPPER_URL")"

    case "$CACHE_DIR" in
        /*)
            printf '%s/%s' "$CACHE_DIR" "$deb_basename"
            ;;
        *)
            printf '%s/%s/%s' "$(pwd)" "$CACHE_DIR" "$deb_basename"
            ;;
    esac
}

download_nss_wrapper() {
    [ "$INSTALL_NSS_WRAPPER" = "1" ] || return 0

    deb="$(nss_wrapper_deb_path)"

    if [ -s "$deb" ]; then
        echo "cached: $(basename "$deb")"
        return 0
    fi

    echo "downloading: $(basename "$deb")"
    download_url "$NSS_WRAPPER_URL" "$deb.part" || {
        rm -f "$deb.part"
        return 1
    }

    mv "$deb.part" "$deb"
}

extract_archive() {
    archive="$1"
    dest="$2"

    mkdir -p "$dest" || return 1

    case "$archive" in
        *.tar.xz|*.txz|*.pkg.tar.xz)
            tar -xJf "$archive" -C "$dest" 2>/dev/null && return 0

            if command -v xz >/dev/null 2>&1; then
                xz -dc "$archive" | tar -xf - -C "$dest"
                return $?
            fi

            echo "error: tar cannot extract xz and xz command is missing: $archive" >&2
            return 1
            ;;

        *.tar.gz|*.tgz|*.pkg.tar.gz)
            tar -xzf "$archive" -C "$dest"
            ;;

        *.tar.zst|*.pkg.tar.zst)
            tar --zstd -xf "$archive" -C "$dest" 2>/dev/null && return 0

            if command -v zstd >/dev/null 2>&1; then
                zstd -dc "$archive" | tar -xf - -C "$dest"
                return $?
            fi

            echo "error: zstd package needs tar --zstd or zstd command: $archive" >&2
            return 1
            ;;

        *.tar|*.pkg.tar)
            tar -xf "$archive" -C "$dest"
            ;;

        *)
            echo "error: unsupported archive: $archive" >&2
            return 1
            ;;
    esac
}

copy_tree_contents_preserve_links() {
    src="$1"
    dst="$2"

    [ -d "$src" ] || return 0

    mkdir -p "$dst" || return 1

    (
        cd "$src" || exit 1
        tar -cf - .
    ) | (
        cd "$dst" || exit 1
        tar -xf -
    )
}

normalize_abs_path() {
    path="$1"

    case "$path" in
        /*)
            ;;
        *)
            printf '%s\n' "$path"
            return
            ;;
    esac

    old_ifs="$IFS"
    IFS=/
    set -- $path
    IFS="$old_ifs"

    out=""
    for part do
        case "$part" in
            ''|.)
                ;;
            ..)
                out="${out%/*}"
                ;;
            *)
                out="$out/$part"
                ;;
        esac
    done

    [ -n "$out" ] || out="/"
    printf '%s\n' "$out"
}

out_rel_from_original_abs() {
    abs="$(normalize_abs_path "$1")"

    case "$abs" in
        "$TERMUX_GLIBC_PREFIX"/*)
            # Flatten Termux's usr/glibc prefix into this relocated usr root.
            printf '%s\n' "${abs#"$TERMUX_GLIBC_PREFIX"/}"
            ;;
        "$TERMUX_PREFIX"/bin/*)
            printf 'bin/%s\n' "${abs#"$TERMUX_PREFIX"/bin/}"
            ;;
        "$TERMUX_PREFIX"/lib/*)
            printf 'lib/%s\n' "${abs#"$TERMUX_PREFIX"/lib/}"
            ;;
        "$TERMUX_PREFIX"/include/*)
            printf 'include/%s\n' "${abs#"$TERMUX_PREFIX"/include/}"
            ;;
        "$TERMUX_PREFIX"/share/*)
            printf 'share/%s\n' "${abs#"$TERMUX_PREFIX"/share/}"
            ;;
        "$TERMUX_PREFIX"/etc/*)
            printf 'etc/%s\n' "${abs#"$TERMUX_PREFIX"/etc/}"
            ;;
        "$TERMUX_PREFIX"/libexec/*)
            printf 'libexec/%s\n' "${abs#"$TERMUX_PREFIX"/libexec/}"
            ;;
        "$TERMUX_PREFIX"/*)
            # Last-resort mapping for uncommon usr subdirectories.
            printf '%s\n' "${abs#"$TERMUX_PREFIX"/}"
            ;;
        *)
            return 1
            ;;
    esac
}

relative_target_from_out_dir() {
    link_dir_rel="$1"
    target_rel="$2"

    if [ -z "$link_dir_rel" ] || [ "$link_dir_rel" = "." ]; then
        printf '%s\n' "$target_rel"
        return
    fi

    rel_prefix=""
    old_ifs="$IFS"
    IFS=/
    set -- $link_dir_rel
    IFS="$old_ifs"

    while [ "$#" -gt 0 ]; do
        rel_prefix="../$rel_prefix"
        shift
    done

    printf '%s%s\n' "$rel_prefix" "$target_rel"
}

rewrite_one_symlink() {
    link="$1"
    out_abs="$2"

    target="$(readlink "$link")"
    link_rel="${link#"$out_abs"/}"
    link_dir_rel="$(dirname "$link_rel")"

    case "$target" in
        /*)
            original_target="$target"
            ;;
        *)
            # OUT_DIR now mirrors /data/data/com.termux/files/usr, so relative
            # symlinks are resolved against the equivalent original usr path.
            original_target="$TERMUX_PREFIX/$link_dir_rel/$target"
            ;;
    esac

    original_target="$(normalize_abs_path "$original_target")"
    target_rel="$(out_rel_from_original_abs "$original_target" 2>/dev/null)" || return 0

    [ -n "$target_rel" ] || return 0

    new_target="$(relative_target_from_out_dir "$link_dir_rel" "$target_rel")"

    if [ "$new_target" != "$target" ]; then
        rm -f "$link" || return 1
        ln -s "$new_target" "$link" || return 1
    fi
}

rewrite_termux_symlinks() {
    [ -d "$OUT_DIR" ] || return 0

    out_abs="$(cd "$OUT_DIR" && pwd -P)" || return 1

    echo "rewriting relocated symlinks..."

    find "$out_abs" -type l | while IFS= read -r link; do
        rewrite_one_symlink "$link" "$out_abs" || exit 1
    done
}

materialize_symlinks() {
    [ "$MATERIALIZE_SYMLINKS" = "1" ] || return 0
    [ -d "$OUT_DIR" ] || return 0

    out_abs="$(cd "$OUT_DIR" && pwd -P)" || return 1

    echo "materializing file symlinks for asset zip..."

    find "$out_abs" -type l | while IFS= read -r link; do
        target="$(readlink "$link")"
        link_dir="$(dirname "$link")"
        rel_link="${link#"$out_abs"/}"

        case "$target" in
            /*)
                target_path="$target"
                ;;
            *)
                target_path="$link_dir/$target"
                ;;
        esac

        if [ -f "$target_path" ]; then
            tmp="$link.gpkg-materialized.$$"
            rm -f "$tmp"
            cp -pL "$target_path" "$tmp" || {
                rm -f "$tmp"
                echo "error: failed to materialize symlink: $rel_link -> $target" >&2
                exit 1
            }
            rm -f "$link" || exit 1
            mv "$tmp" "$link" || exit 1
        elif [ -d "$target_path" ]; then
            # Directory symlinks can explode into recursion/huge duplicates.
            # Keep them as links unless you know your extractor needs them.
            echo "warning: keeping directory symlink: $rel_link -> $target" >&2
        else
            echo "warning: broken/external symlink left as-is: $rel_link -> $target" >&2
        fi
    done
}

copy_usr_bin_into_out_bin() {
    src="$1"
    pkg="$2"

    [ -d "$src" ] || return 0

    echo "  usr bins: $pkg"

    (
        cd "$src" || exit 1

        find . \( -type f -o -type l \) | while IFS= read -r item; do
            rel="${item#./}"
            [ -n "$rel" ] || continue

            dst="$OUT_DIR/bin/$rel"
            dst_rel="bin/$rel"
            mkdir -p "$(dirname "$dst")" || exit 1

            if [ -L "$item" ]; then
                target="$(readlink "$item")"
                item_dir_rel="$(dirname "$rel")"

                case "$target" in
                    /*)
                        original_target="$target"
                        ;;
                    *)
                        original_target="$TERMUX_PREFIX/bin/$item_dir_rel/$target"
                        ;;
                esac

                original_target="$(normalize_abs_path "$original_target")"
                target_rel="$(out_rel_from_original_abs "$original_target" 2>/dev/null || true)"

                if [ -n "$target_rel" ]; then
                    # Avoid replacing an already-copied real binary with a self-symlink.
                    if [ "$target_rel" = "$dst_rel" ]; then
                        if [ -e "$dst" ] || [ -L "$dst" ]; then
                            continue
                        fi
                    fi

                    new_target="$(relative_target_from_out_dir "$(dirname "$dst_rel")" "$target_rel")"
                    rm -f "$dst"
                    ln -s "$new_target" "$dst" || exit 1
                else
                    rm -f "$dst"
                    ln -s "$target" "$dst" || exit 1
                fi
            else
                cp -a "$item" "$dst" || exit 1
            fi
        done
    )
}

copy_usr_subdir_into_out() {
    src="$1"
    dst="$2"
    label="$3"
    pkg="$4"

    [ -d "$src" ] || return 0

    echo "  $label: $pkg"
    copy_tree_contents_preserve_links "$src" "$dst"
}

copy_first_level_prefix() {
    prefixdir="$1"
    pkg="$2"

    [ -d "$prefixdir" ] || return 1

    real_prefix="$(cd "$prefixdir" && pwd -P)" || return 1
    seen_file="$TMP_ROOT/copied-prefix-dirs"
    touch "$seen_file" || return 1

    if grep -Fxq "$real_prefix" "$seen_file" 2>/dev/null; then
        return 0
    fi

    echo "$real_prefix" >> "$seen_file"
    echo "  glibc prefix flattened: $pkg"
    copy_tree_contents_preserve_links "$prefixdir" "$OUT_DIR"
}

copy_glibc_bits() {
    pkg="$1"
    root="$2"

    mkdir -p "$OUT_DIR" "$OUT_DIR/bin" "$OUT_DIR/lib" || return 1

    copied_any=0

    # Main glibc prefix. Preserve normal layout:
    #   bin/ lib/ include/ share/ etc/ libexec/ ...
    # Do not list both data/... and ./data/...; after extraction they are the same path.
    for prefixdir in \
        "$root/data/data/com.termux/files/usr/glibc" \
        "$root/usr/glibc" \
        "$root/glibc"
    do
        if [ -d "$prefixdir" ]; then
            copy_first_level_prefix "$prefixdir" "$pkg" || return 1
            copied_any=1
        fi
    done

    # Some gpkg packages expose wrapper launchers in normal usr/bin.
    for usrbindir in \
        "$root/data/data/com.termux/files/usr/bin" \
        "$root/usr/bin" \
        "$root/bin"
    do
        if [ -d "$usrbindir" ]; then
            copy_usr_bin_into_out_bin "$usrbindir" "$pkg" || return 1
            copied_any=1
        fi
    done

    # Some data/config/header files live under normal usr dirs instead of usr/glibc.
    # Copy them into the matching relocated prefix dirs.
    for usrsharedir in \
        "$root/data/data/com.termux/files/usr/share" \
        "$root/usr/share" \
        "$root/share"
    do
        if [ -d "$usrsharedir" ]; then
            copy_usr_subdir_into_out "$usrsharedir" "$OUT_DIR/share" "usr share" "$pkg" || return 1
            copied_any=1
        fi
    done

    for usretdir in \
        "$root/data/data/com.termux/files/usr/etc" \
        "$root/usr/etc" \
        "$root/etc"
    do
        if [ -d "$usretdir" ]; then
            copy_usr_subdir_into_out "$usretdir" "$OUT_DIR/etc" "usr etc" "$pkg" || return 1
            copied_any=1
        fi
    done

    for usrincludedir in \
        "$root/data/data/com.termux/files/usr/include" \
        "$root/usr/include" \
        "$root/include"
    do
        if [ -d "$usrincludedir" ]; then
            copy_usr_subdir_into_out "$usrincludedir" "$OUT_DIR/include" "usr include" "$pkg" || return 1
            copied_any=1
        fi
    done

    for usrlibexecdir in \
        "$root/data/data/com.termux/files/usr/libexec" \
        "$root/usr/libexec" \
        "$root/libexec"
    do
        if [ -d "$usrlibexecdir" ]; then
            copy_usr_subdir_into_out "$usrlibexecdir" "$OUT_DIR/libexec" "usr libexec" "$pkg" || return 1
            copied_any=1
        fi
    done

    if [ "$copied_any" = "0" ]; then
        echo "  note: no usable prefix files copied from $pkg"
    fi
}

extract_nss_wrapper_to_out() {
    [ "$INSTALL_NSS_WRAPPER" = "1" ] || return 0

    deb="$(nss_wrapper_deb_path)"

    if [ ! -s "$deb" ]; then
        echo "error: nss-wrapper deb not found: $deb" >&2
        return 1
    fi

    work="$TMP_ROOT/nss-wrapper"
    ar_root="$work/ar"
    data_root="$work/data"

    rm -rf "$work"
    mkdir -p "$ar_root" "$data_root" || return 1

    echo "extracting: nss-wrapper"

    (
        cd "$ar_root" || exit 1
        ar x "$deb"
    ) || {
        echo "error: failed to extract deb with ar: $deb" >&2
        return 1
    }

    data_tar="$(
        find "$ar_root" -maxdepth 1 -type f \( \
            -name "data.tar.xz" -o \
            -name "data.tar.gz" -o \
            -name "data.tar.zst" -o \
            -name "data.tar" \
        \) 2>/dev/null | head -n 1
    )"

    if [ -z "$data_tar" ]; then
        echo "error: data.tar.* not found inside $deb" >&2
        return 1
    fi

    extract_archive "$data_tar" "$data_root" || return 1

    mkdir -p "$OUT_DIR/lib" || return 1

    copied_any=0

    for lib in \
        "$data_root/usr/lib/aarch64-linux-gnu/libnss_wrapper.so" \
        "$data_root/usr/lib/aarch64-linux-gnu/libnss_wrapper.so.0" \
        "$data_root/usr/lib/aarch64-linux-gnu/libnss_wrapper.so."*
    do
        if [ -e "$lib" ]; then
            echo "  nss-wrapper: $(basename "$lib")"
            cp -a "$lib" "$OUT_DIR/lib/"
            copied_any=1
        fi
    done

    if [ "$copied_any" = "0" ]; then
        found="$(
            find "$data_root" \( -type f -o -type l \) 2>/dev/null |
                grep '/libnss_wrapper\.so' |
                head -n 20
        )"

        if [ -n "$found" ]; then
            echo "$found" | while IFS= read -r lib; do
                [ -n "$lib" ] || continue
                echo "  nss-wrapper: $(basename "$lib")"
                cp -a "$lib" "$OUT_DIR/lib/"
            done
            copied_any=1
        fi
    fi

    if [ "$copied_any" = "0" ]; then
        echo "error: libnss_wrapper.so not found in $deb" >&2
        return 1
    fi
}

extract_pkg_to_out() {
    pkg="$1"

    filename="$(pkg_filename "$pkg")"
    archive="$CACHE_DIR/$filename"

    if [ ! -s "$archive" ]; then
        echo "error: archive not found for $pkg: $archive" >&2
        return 1
    fi

    work="$TMP_ROOT/extract.$pkg"
    root="$work/root"
    nested_root="$work/nested-root"

    rm -rf "$work"
    mkdir -p "$root" || return 1

    echo "extracting: $pkg"
    extract_archive "$archive" "$root" || return 1

    nested="$(
        find "$root" -type f \( \
            -name "*.pkg.tar.xz" -o \
            -name "*.pkg.tar.gz" -o \
            -name "*.pkg.tar.zst" -o \
            -name "*.pkg.tar" \
        \) 2>/dev/null | head -n 1
    )"

    if [ -n "$nested" ]; then
        mkdir -p "$nested_root" || return 1
        extract_archive "$nested" "$nested_root" || return 1
        copy_glibc_bits "$pkg" "$nested_root"
    else
        copy_glibc_bits "$pkg" "$root"
    fi
}

fetch_db

ROOTS_FILE="$TMP_ROOT/roots"
printf '%s\n' "$ROOTS" | sed '/^$/d' > "$ROOTS_FILE"

while IFS= read -r root_pkg; do
    [ -n "$root_pkg" ] || continue

    if ! pkg_exists "$root_pkg"; then
        echo "error: package not found: $root_pkg" >&2
        exit 1
    fi
done < "$ROOTS_FILE"

TREE_SEEN="$TMP_ROOT/tree.seen"
: > "$TREE_SEEN"

first_tree=1

while IFS= read -r root_pkg; do
    [ -n "$root_pkg" ] || continue

    if [ "$first_tree" = "0" ]; then
        echo
    fi

    print_tree "$root_pkg" "" "" "1" "$TREE_SEEN"
    first_tree=0
done < "$ROOTS_FILE"

if [ "$INSTALL" = "0" ] && [ "$DOWNLOAD_ONLY" = "0" ]; then
    exit 0
fi

RESOLVE_SEEN="$TMP_ROOT/resolve.seen"
ORDER="$TMP_ROOT/install.order"
MISSING="$TMP_ROOT/missing"

: > "$RESOLVE_SEEN"
: > "$ORDER"
: > "$MISSING"

while IFS= read -r root_pkg; do
    [ -n "$root_pkg" ] || continue
    resolve_install_order "$root_pkg" "$RESOLVE_SEEN" "$ORDER" "$MISSING"
done < "$ROOTS_FILE"

if [ -s "$MISSING" ]; then
    echo
    echo "missing packages/virtual deps:"
    cat "$MISSING"
fi

echo
echo "install order:"
cat "$ORDER"

echo
while IFS= read -r pkg; do
    [ -n "$pkg" ] || continue
    download_pkg "$pkg" || exit 1
done < "$ORDER"

download_nss_wrapper || exit 1

if [ "$DOWNLOAD_ONLY" = "1" ]; then
    echo
    echo "downloaded packages are in: $CACHE_DIR"
    exit 0
fi

if [ "$CLEAN_OUT" = "1" ]; then
    echo
    echo "cleaning output dir: $OUT_DIR"
    rm -rf "$OUT_DIR" || exit 1
fi

mkdir -p "$OUT_DIR" || exit 1

echo
echo "extracting into: $OUT_DIR"

while IFS= read -r pkg; do
    [ -n "$pkg" ] || continue
    extract_pkg_to_out "$pkg" || exit 1
done < "$ORDER"

extract_nss_wrapper_to_out || exit 1
rewrite_termux_symlinks || exit 1
materialize_symlinks || exit 1

echo
echo "done"
echo "prefix: $OUT_DIR"
echo "bins: $OUT_DIR/bin"
echo "libs: $OUT_DIR/lib"
echo "dynamic linker: $OUT_DIR/lib/ld-linux-aarch64.so.1"
echo "includes: $OUT_DIR/include"
echo "share: $OUT_DIR/share"
echo "etc: $OUT_DIR/etc"
if [ "$INSTALL_NSS_WRAPPER" = "1" ]; then
    echo "nss-wrapper: $OUT_DIR/lib/libnss_wrapper.so"
fi
if [ "$MATERIALIZE_SYMLINKS" = "1" ]; then
    echo "symlinks: materialized file symlinks for asset zip"
else
    echo "symlinks: kept as symlinks"
fi
