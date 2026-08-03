#!/bin/sh

set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

OUT_ROOT="$ROOT/glibc"
OUT_DIR="$OUT_ROOT/usr"
ASSETS_TAR_ZST="$ROOT/app/src/main/assets/glibc.tar.zst"
REUSE_GLIBC=0

OLD_EMBEDDED_PREFIX="/data/data/com.termux/files/usr/glibc"
NEW_EMBEDDED_PREFIX="/data/data/org.cosmicide/files/arch/usr"

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

if ! command -v python3 >/dev/null 2>&1; then
    echo "error: missing python3" >&2
    exit 1
fi

if ! command -v patchelf >/dev/null 2>&1; then
    echo "error: missing patchelf" >&2
    exit 1
fi

if [ "$REUSE_GLIBC" = "0" ] && ! command -v jq >/dev/null 2>&1; then
    echo "error: missing jq" >&2
    exit 1
fi

if ! command -v infocmp >/dev/null 2>&1; then
    echo "error: missing infocmp" >&2
    exit 1
fi

if ! command -v tic >/dev/null 2>&1; then
    echo "error: missing tic" >&2
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

echo "relocating embedded paths..."
echo "  old: $OLD_EMBEDDED_PREFIX"
echo "  new: $NEW_EMBEDDED_PREFIX"

python3 - \
    "$OUT_ROOT" \
    "$OLD_EMBEDDED_PREFIX" \
    "$NEW_EMBEDDED_PREFIX" <<'PY'
import os
import shutil
import stat
import subprocess
import sys
import tempfile
from pathlib import Path

root = Path(sys.argv[1])
old_text = sys.argv[2]
new_text = sys.argv[3]

old_bytes = old_text.encode("utf-8")
new_bytes = new_text.encode("utf-8")

terminfo_root = root / "usr" / "share" / "terminfo"

patched_elfs = 0
patched_texts = 0
patched_terminfo_entries = 0
patched_occurrences = 0
unresolved = []


def display_path(path: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def regular_files(directory: Path):
    paths = []

    for current_root, directories, filenames in os.walk(
        directory,
        followlinks=False,
    ):
        directories.sort()
        filenames.sort()

        for filename in filenames:
            path = Path(current_root) / filename

            try:
                mode = os.lstat(path).st_mode
            except FileNotFoundError:
                continue

            if stat.S_ISREG(mode):
                paths.append(path)

    return paths


def command_output(command):
    result = subprocess.run(
        command,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )

    if result.returncode != 0:
        error = result.stderr.decode(
            "utf-8",
            errors="replace",
        ).strip()

        if not error:
            error = f"exit code {result.returncode}"

        raise RuntimeError(
            f"{' '.join(command)} failed: {error}"
        )

    return result.stdout


def patchelf_output(path: Path, option: str):
    result = subprocess.run(
        [
            "patchelf",
            option,
            os.fspath(path),
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        check=False,
    )

    if result.returncode != 0:
        return None

    return result.stdout.decode(
        "utf-8",
        errors="surrogateescape",
    ).rstrip("\n")


def run_patchelf(path: Path, *arguments: str):
    result = subprocess.run(
        [
            "patchelf",
            *arguments,
            os.fspath(path),
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
        check=False,
    )

    if result.returncode == 0:
        return

    error = result.stderr.decode(
        "utf-8",
        errors="replace",
    ).strip()

    if not error:
        error = f"exit code {result.returncode}"

    raise RuntimeError(
        f"patchelf {' '.join(arguments)} failed: {error}"
    )


def patch_elf(path: Path) -> int:
    changed_fields = 0

    interpreter = patchelf_output(
        path,
        "--print-interpreter",
    )

    if interpreter is not None and old_text in interpreter:
        run_patchelf(
            path,
            "--set-interpreter",
            interpreter.replace(old_text, new_text),
        )
        changed_fields += interpreter.count(old_text)

    rpath = patchelf_output(
        path,
        "--print-rpath",
    )

    if rpath is not None and old_text in rpath:
        run_patchelf(
            path,
            "--set-rpath",
            rpath.replace(old_text, new_text),
        )
        changed_fields += rpath.count(old_text)

    soname = patchelf_output(
        path,
        "--print-soname",
    )

    if soname is not None and old_text in soname:
        run_patchelf(
            path,
            "--set-soname",
            soname.replace(old_text, new_text),
        )
        changed_fields += soname.count(old_text)

    needed = patchelf_output(
        path,
        "--print-needed",
    )

    if needed:
        for dependency in needed.splitlines():
            if old_text not in dependency:
                continue

            run_patchelf(
                path,
                "--replace-needed",
                dependency,
                dependency.replace(old_text, new_text),
            )
            changed_fields += dependency.count(old_text)

    return changed_fields


def is_terminfo_entry(path: Path) -> bool:
    try:
        relative = path.relative_to(terminfo_root)
    except ValueError:
        return False

    return len(relative.parts) == 2


terminfo_lookup_temporary = tempfile.TemporaryDirectory(
    prefix="cosmic-terminfo-lookup-"
)
terminfo_lookup_root = Path(
    terminfo_lookup_temporary.name
)


def create_terminfo_lookup_database():
    """
    Make the bundled terminfo database readable by both layouts:

        w/wsvt25m
        77/wsvt25m

    macOS commonly looks for the hexadecimal form, while the packaged
    glibc database currently uses character directories.
    """

    for source in sorted(terminfo_root.glob("*/*")):
        if not source.is_file():
            continue

        terminal_name = source.name

        if not terminal_name:
            continue

        first_byte = terminal_name.encode(
            "ascii",
            errors="strict",
        )[0]

        buckets = {
            source.parent.name,
            terminal_name[0],
            f"{first_byte:02x}",
        }

        for bucket in buckets:
            destination = (
                terminfo_lookup_root
                / bucket
                / terminal_name
            )

            destination.parent.mkdir(
                parents=True,
                exist_ok=True,
            )

            if os.path.lexists(destination):
                continue

            os.symlink(
                os.path.abspath(source),
                destination,
            )


create_terminfo_lookup_database()


def find_generated_terminfo_entries(
    compiled_root: Path,
):
    entries = {}

    for generated in sorted(
        compiled_root.glob("*/*")
    ):
        if not generated.is_file():
            continue

        entries.setdefault(
            generated.name,
            generated,
        )

    return entries


def existing_terminfo_destinations(
    terminal_name: str,
):
    destinations = []

    for candidate in sorted(
        terminfo_root.glob(f"*/{terminal_name}")
    ):
        if candidate.is_file() or candidate.is_symlink():
            destinations.append(candidate)

    return destinations


def install_rebuilt_terminfo_entries(
    compiled_root: Path,
):
    generated_entries = find_generated_terminfo_entries(
        compiled_root
    )

    if not generated_entries:
        raise RuntimeError(
            "tic generated no compiled terminfo entries"
        )

    installed = []

    for terminal_name, generated in generated_entries.items():
        destinations = existing_terminfo_destinations(
            terminal_name
        )

        if not destinations:
            # Preserve the bundled database's character-directory
            # layout for newly generated aliases.
            destination = (
                terminfo_root
                / terminal_name[0]
                / terminal_name
            )

            destination.parent.mkdir(
                parents=True,
                exist_ok=True,
            )

            destinations = [destination]

        generated_data = generated.read_bytes()

        if old_bytes in generated_data:
            raise RuntimeError(
                f"rebuilt entry {terminal_name} still "
                "contains the old prefix"
            )

        for destination in destinations:
            # Writing through the existing file preserves hard-link
            # relationships where possible.
            if destination.is_symlink():
                destination.write_bytes(
                    generated_data
                )
            else:
                destination.parent.mkdir(
                    parents=True,
                    exist_ok=True,
                )

                with destination.open("wb") as output:
                    output.write(generated_data)

            installed.append(destination)

    return installed


def patch_terminfo(path: Path) -> int:
    terminal_name = path.name

    source = command_output(
        [
            "infocmp",
            "-x",
            "-A",
            os.fspath(terminfo_lookup_root),
            terminal_name,
        ]
    )

    occurrence_count = source.count(old_bytes)

    if occurrence_count == 0:
        raise RuntimeError(
            "infocmp output does not contain "
            "the old prefix"
        )

    updated_source = source.replace(
        old_bytes,
        new_bytes,
    )

    with tempfile.TemporaryDirectory(
        prefix="cosmic-terminfo-build-"
    ) as temporary_directory:
        temporary_root = Path(temporary_directory)
        source_path = temporary_root / "entry.src"
        compiled_root = temporary_root / "compiled"

        source_path.write_bytes(updated_source)
        compiled_root.mkdir()

        environment = os.environ.copy()

        # tic may need to resolve use= references. Point it at the
        # compatibility lookup database so either bucket layout works.
        environment["TERMINFO"] = os.fspath(
            terminfo_lookup_root
        )
        environment["TERMINFO_DIRS"] = os.fspath(
            terminfo_lookup_root
        )

        result = subprocess.run(
            [
                "tic",
                "-x",
                "-o",
                os.fspath(compiled_root),
                os.fspath(source_path),
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env=environment,
            check=False,
        )

        if result.returncode != 0:
            error = result.stderr.decode(
                "utf-8",
                errors="replace",
            ).strip()

            if not error:
                error = (
                    f"exit code {result.returncode}"
                )

            raise RuntimeError(
                f"tic failed for {terminal_name}: "
                f"{error}"
            )

        installed = install_rebuilt_terminfo_entries(
            compiled_root
        )

        if not installed:
            raise RuntimeError(
                f"tic generated no usable entries "
                f"for {terminal_name}"
            )

    destinations = existing_terminfo_destinations(
        terminal_name
    )

    if not destinations:
        raise RuntimeError(
            f"rebuilt entry disappeared: "
            f"{terminal_name}"
        )

    for destination in destinations:
        remaining = destination.read_bytes().count(
            old_bytes
        )

        if remaining:
            raise RuntimeError(
                f"{destination} still contains "
                f"{remaining} old-prefix occurrence(s)"
            )

    return occurrence_count


for path in regular_files(root):
    try:
        original = path.read_bytes()
    except OSError as error:
        unresolved.append(
            f"{display_path(path)}: could not read file: {error}"
        )
        continue

    original_count = original.count(old_bytes)

    if original_count == 0:
        continue

    shown_path = display_path(path)
    is_elf = original.startswith(b"\x7fELF")

    if is_elf:
        try:
            changed_fields = patch_elf(path)
        except Exception as error:
            unresolved.append(
                f"{shown_path}: {error}"
            )
            continue

        try:
            updated = path.read_bytes()
        except OSError as error:
            unresolved.append(
                f"{shown_path}: could not verify patched ELF: {error}"
            )
            continue

        remaining = updated.count(old_bytes)

        if remaining:
            unresolved.append(
                f"{shown_path}: {remaining} old-prefix occurrence(s) "
                "remain outside ELF interpreter/RPATH/RUNPATH/"
                "SONAME/DT_NEEDED metadata"
            )
            continue

        if changed_fields == 0:
            unresolved.append(
                f"{shown_path}: contains the old prefix, but patchelf "
                "found no supported field containing it"
            )
            continue

        patched_elfs += 1
        patched_occurrences += original_count

        print(
            f"patched ELF:     {shown_path} "
            f"({original_count} occurrence(s))"
        )
        continue

    if is_terminfo_entry(path):
        try:
            occurrence_count = patch_terminfo(path)
        except Exception as error:
            unresolved.append(
                f"{shown_path}: could not rebuild terminfo entry: {error}"
            )
            continue

        patched_terminfo_entries += 1
        patched_occurrences += original_count

        print(
            f"patched terminfo: {shown_path} "
            f"({occurrence_count} source occurrence(s))"
        )
        continue

    if b"\0" in original:
        unresolved.append(
            f"{shown_path}: non-ELF binary contains "
            f"{original_count} old-prefix occurrence(s)"
        )
        continue

    try:
        original.decode("utf-8")
    except UnicodeDecodeError:
        unresolved.append(
            f"{shown_path}: non-UTF-8 file contains "
            f"{original_count} old-prefix occurrence(s)"
        )
        continue

    updated = original.replace(
        old_bytes,
        new_bytes,
    )

    try:
        with path.open("r+b") as output:
            output.write(updated)
            output.truncate()
    except OSError as error:
        unresolved.append(
            f"{shown_path}: could not write replacement: {error}"
        )
        continue

    patched_texts += 1
    patched_occurrences += original_count

    print(
        f"patched text:    {shown_path} "
        f"({original_count} occurrence(s))"
    )


if unresolved:
    print(
        "",
        file=sys.stderr,
    )
    print(
        "error: some embedded paths could not be safely relocated:",
        file=sys.stderr,
    )

    for entry in unresolved:
        print(
            f"  {entry}",
            file=sys.stderr,
        )

    print(
        "",
        file=sys.stderr,
    )
    print(
        "ELF occurrences must be in fields supported by patchelf. "
        "Terminfo entries are rebuilt using infocmp and tic. "
        "Other binary occurrences must be fixed at build time.",
        file=sys.stderr,
    )

    raise SystemExit(1)


print(
    "embedded path relocation complete: "
    f"{patched_occurrences} occurrence(s) in "
    f"{patched_elfs} ELF file(s), "
    f"{patched_terminfo_entries} terminfo entry/entries and "
    f"{patched_texts} text file(s)"
)
PY

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
mv \
    "$OUT_DIR/lib/ld-linux-aarch64.so.1" \
    "$ROOT/app/src/main/jniLibs/arm64-v8a/libld_linux.so"

trap - EXIT HUP INT TERM
rm -f "$TAR_LIST"

echo "done: $ASSETS_TAR_ZST"
