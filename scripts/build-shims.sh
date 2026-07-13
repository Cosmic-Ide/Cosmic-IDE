#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/app/src/main/jniLibs/arm64-v8a"

mkdir -p "$OUT"

zig cc \
  -target aarch64-linux-gnu \
  -shared \
  -fPIC \
  -O2 \
  -Wall \
  -Wextra \
  -Wl,-soname,libpath_redirect.so \
  -o "$OUT/libpath_redirect.so" \
  "$ROOT/scripts/glibc-shims/libpath_redirect.c" \
  "$ROOT/scripts/glibc-shims/dns_fallback.c" \
  "$ROOT/scripts/glibc-shims/exec_wrap.c" \
  "$ROOT/scripts/glibc-shims/pacman_fake_root.c" \
  -ldl \
  -pthread