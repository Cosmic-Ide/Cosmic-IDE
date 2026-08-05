# glibc runtime and compatibility shims

## Why this exists

Android uses bionic, but Cosmic IDE runs Linux/aarch64 JDKs and build tools compiled for glibc.
`LinuxProcessRunner` starts those binaries with the packaged glibc loader instead of Android's
linker:

```text
libld_linux.so
  --library-path <files/glibc/usr/lib:...>
  --preload libpath_redirect.so:libnss_wrapper.so
  <program> <arguments...>
```

This is a compatibility layer, not a container. Processes keep the app UID, SELinux restrictions,
and filesystem permissions.

## Runtime pieces

| Piece                 | Purpose                                                                      |
|-----------------------|------------------------------------------------------------------------------|
| `glibc.tar.zst`       | Relocated glibc, shells, compilers, and GNU tools deployed to `files/glibc`. |
| `libld_linux.so`      | Loads a glibc ELF without involving Android's linker.                        |
| `libpath_redirect.so` | One preload library containing all five shim components below.               |
| `libnss_wrapper.so`   | Makes glibc user/group lookup use app-generated passwd and group files.      |

`scripts/build-glibc.sh` assembles and archives the runtime. `scripts/build-shims.sh` compiles the
five C sources into `app/src/main/jniLibs/arm64-v8a/libpath_redirect.so`. Neither script builds
`libld_linux.so`; the loader is a separately packaged binary.

## What each shim is for

### `libpath_redirect.c`: give Linux paths an app-private home

Linux tools contain absolute paths such as `/usr/bin`, `/etc/resolv.conf`, `/var`, and `/tmp`.
Those paths are unavailable or inappropriate inside an Android sandbox. The path shim rewrites
them to the deployed runtime:

| Requested path      | Effective location                       |
|---------------------|------------------------------------------|
| `/usr/...`          | `$APP_FILES_DIR/usr/...`                 |
| `/bin/...`          | `$APP_FILES_DIR/usr/bin/...`             |
| `/sbin/...`         | `$APP_FILES_DIR/usr/sbin/...`            |
| `/etc/...`          | `$APP_FILES_DIR/usr/etc/...`             |
| `/var/...`          | `$APP_FILES_DIR/usr/var/...`             |
| `/run/...`          | `$APP_FILES_DIR/usr/run/...`             |
| `/tmp/...`          | `$TMPDIR/...`                            |
| old Termux prefixes | the matching path below `$APP_FILES_DIR` |

`APP_FILES_DIR` is normally `files/glibc`. Paths already inside it are not redirected again.
Resolver files have explicit environment overrides and take precedence over the general `/etc`
mapping.

The shim covers normal libc path APIs, their `*at` variants, and selected direct syscalls. It also
redirects filesystem Unix sockets, preserves virtual symlink targets, and falls back to copying
when Android rejects a requested hardlink. That fallback preserves contents, not hardlink identity.

Two less obvious responsibilities belong here because they are path/identity observations:

- `/proc/self/exe` and `AT_EXECFN` are made to report the logical program instead of
  `libld_linux.so`;
- HotSpot `hsperfdata` metadata is adjusted so tools such as `jstat` recognize JVM performance
  files in app-private storage.

When a tool still reaches the real `/usr` or `/etc`, it is usually using an unwrapped libc symbol or
direct syscall. Add the narrowest missing interposer rather than special-casing the tool.

### `exec_wrap.c`: keep child processes on glibc

Loading the first JVM correctly is not enough. Gradle, Java, shells, and compilers continuously call
`exec*` and `posix_spawn*`; without interception their children would use the interpreter embedded
in the ELF or Android's process environment.

The exec shim resolves the child, identifies ELF files and supported shell scripts, and rebuilds
the launch through the sibling `libld_linux.so`. It preserves `LD_LIBRARY_PATH`, `LD_PRELOAD`, and
the logical executable identity for every generation.

It also:

- runs `sh`/`bash`-style scripts through a relocated glibc shell;
- prefers a relocated GNU tool when a child requests an Android system tool of the same name;
- injects `-Djava.io.tmpdir=$TMPDIR` for Java children when absent;
- removes glibc preload/library variables before delegating a genuine Android executable.

The last rule is safety-critical: a bionic executable must never receive the glibc preload or be
passed to the glibc loader.

### `dns_fallback.c`: resolve names when glibc NSS cannot

glibc's resolver expects its normal `/etc` files and NSS environment, which do not naturally match
an Android app. The DNS shim always calls the real `getaddrinfo` first and returns successful
results unchanged. Only a failed hostname lookup enters the fallback.

The fallback reads the generated hosts, nsswitch, and resolv files. It supports `files`/`dns`
ordering, A/AAAA queries, search domains, bounded retries, CNAME following, and TCP retry for
truncated UDP replies.

It is deliberately not a complete resolver: there is no DNSSEC, mDNS, LLMNR, arbitrary NSS module,
full `gai.conf` policy, or textual `/etc/services` fallback.

### `fake_root.c`: satisfy package-manager and sudo identity checks, not gain root

Pacman and sudo refuse some operations when UID/GID queries do not report root. This shim returns
zero only when the current executable is identified as `sudo`, `pacman`, or `pacman-key`; every
other process sees the real app identity. The path shim likewise reports root ownership only for
sudo's `/etc/sudo.conf`, `/etc/sudoers`, and `/etc/sudoers.d` metadata checks in those processes.

It does not grant capabilities, ownership changes, mount access, or any other kernel permission.
Pacman can still fail when an operation genuinely requires privilege. Pacman remains packaged by
the runtime builder, although the current application setup script does not invoke it.

### `syscall.c`: make seccomp-trapped feature probes fall back

Android's app seccomp policy traps some newer Linux syscalls. The syscall shim converts those traps
to `ENOSYS`, allowing callers to use their existing fallback instead of terminating. For example,
Go retries `faccessat2` with legacy `faccessat`, which avoids Android's blocked `faccessat2` call.
It emulates `statx` with legacy `fstatat` metadata because systemd uses `statx` to compare
`/proc/1/root` with `/` when detecting a chroot.

## Environment owned by `LinuxProcessRunner`

`LinuxProcessRunner` is the single source of the runtime environment. Important values are:

| Variable                                                                | Purpose                                                                |
|-------------------------------------------------------------------------|------------------------------------------------------------------------|
| `LD_LIBRARY_PATH`                                                       | Relocated glibc libraries and discovered GCC frontend directories.     |
| `LD_PRELOAD`                                                            | Combined shim plus `libnss_wrapper.so`, inherited by descendants.      |
| `APP_FILES_DIR`                                                         | Physical root used by path redirection.                                |
| `SYSTEMD_OFFLINE`, `SYSTEMD_IGNORE_CHROOT`                              | Keep systemd package hooks offline; this runtime has no systemd PID 1. |
| `TMPDIR`, `TMP`, `TEMP`                                                 | Writable app cache and virtual `/tmp`.                                 |
| `RESOLV_CONF_PATH`, `HOSTS_PATH`, `NSSWITCH_CONF_PATH`, `GAI_CONF_PATH` | Generated resolver configuration.                                      |
| `NSS_WRAPPER_PASSWD`, `NSS_WRAPPER_GROUP`                               | Generated records for the real app UID/GID.                            |
| `COSMIC_EXECUTABLE`                                                     | Logical target hidden behind the explicit loader launch.               |

The runner also sets `HOME`, `SHELL`, compiler tool paths, and the selected `JAVA_HOME`. Caller
overrides are applied last, so they can intentionally replace defaults—and can also break the
environment.

## Deployment and symlinks

The runtime builder flattens Termux glibc packages into `glibc/usr` and rewrites baked-in Termux
symlinks. The asset tar contains regular files plus a `.symlinks` manifest. Android extraction
rejects escaping paths, skips link/device entries, then recreates only validated relative symlinks
from that manifest.

Changing C sources requires rebuilding the JNI `.so`. Changing runtime packages requires rebuilding
`glibc.tar.zst`. These are independent artifacts.

## Debugging

Enable tracing per process through environment overrides:

| Variable                | Shows                                                                           |
|-------------------------|---------------------------------------------------------------------------------|
| `PATH_REDIRECT_DEBUG=1` | Path rewrites and related filesystem compatibility decisions.                   |
| `EXEC_WRAP_TRACE=1`     | Target resolution, child wrapping, Java temp injection, and Android delegation. |
| `DNS_TRACE=1`           | Resolver fallback configuration, queries, and results.                          |

Start diagnosis at the boundary matching the symptom:

- initial ELF fails: loader, architecture, or library path;
- first process works but a child fails: exec wrapping or inherited preload;
- absolute Linux path fails: missing path/syscall interposer;
- hostname fails: generated resolver files and DNS trace;
- Android binary crashes: it received glibc loader/preload state;
- `jstat` cannot find a JVM: temp path, executable identity, or hsperfdata metadata.

## Invariants

1. Initial and descendant launches use the same loader, libraries, and preloads.
2. Android executables never receive glibc preload state.
3. Physical paths below `APP_FILES_DIR` are never redirected twice.
4. Real `getaddrinfo` success always wins over the fallback.
5. Fake root identity is restricted to sudo and package-manager checks and does not imply privilege.
6. Packaged native/runtime artifacts are rebuilt when their sources change.
