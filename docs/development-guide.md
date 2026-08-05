# Building and contributing

## Supported development shape

Cosmic IDE is a Gradle multi-project Android application written primarily in Kotlin and Jetpack
Compose, with C/C++ for PTY and glibc compatibility pieces. Application bytecode targets Java 17;
some lower-level modules deliberately retain Java 11 compatibility.

The runtime product has two build layers:

```text
Android APK
  +-- packaged glibc runtime and native loader/shims
  +-- Kotlin/Compose application and library modules
```

A normal Kotlin/UI edit needs only the Android build. Changes to a generated runtime layer require
its explicit producer command before the APK represents the new source.

## Prerequisites

For a standard application build, install:

- a JDK 17 Gradle runtime;
- Android SDK components matching the repository's compile SDK (currently API 37.1);
- the Android build tools accepted by AGP;
- Git and a network connection for Gradle dependencies.

Native/runtime regeneration additionally needs:

- Zig for the aarch64 glibc preload library;
- `tar` and `zstd`;
- `jq` when downloading/rebuilding the glibc package tree;
- `curl` or `wget`;
- Android NDK 30 and CMake 4.1.2 for the `:exec` native build.

`gradle.properties` disables Java installation auto-detection and auto-download. Ensure the Gradle
launcher itself uses the intended JDK and provide explicit installation paths if a task requests a
Gradle Java toolchain.

## Checkout and first build

From the repository root:

```sh
chmod +x gradlew
./gradlew :app:assembleProdDebug
```

For a faster source check:

```sh
./gradlew :app:compileProdDebugKotlin
```

The output APKs are written below `app/build/outputs/apk/prod/debug`. ABI splitting is enabled and a
universal artifact may also be produced by the Android plugin.

The `dev` and `prod` product flavors select different analytics implementations in `:common`.
At the time of writing, the dev implementation imports Firebase Analytics while its dependency is
commented out in `common/build.gradle.kts`; `compileDevDebugKotlin` therefore requires restoring the
matching dependency/configuration. The prod flavor builds without that Firebase dependency.

## Module map

| Module                     | Type            | Responsibility                                                   |
|----------------------------|-----------------|------------------------------------------------------------------|
| `:app`                     | Android app     | Compose UI, navigation, editor, built-in providers, setup        |
| `:common`                  | Android library | Shared preferences, analytics flavor boundary, editor data types |
| `:util`                    | Android library | Persistent data roots and archive/filesystem helpers             |
| `:exec`                    | Android/native  | glibc command runner, plain process facade, PTY JNI transport    |
| `:feature:project`         | JVM library     | Serializable project and language model                          |
| `:feature:sdk-manager`     | JVM library     | Foojay JDK metadata/download client                              |
| `:feature:code-navigation` | Android library | Compiler-backed code-navigation support                          |
| `:plugin-api`              | JVM library     | Plugin descriptors, lifecycle, registries, services              |
| `:plugin-runtime`          | Android library | Plugin discovery, dex loading, activation, and cleanup           |
| `:ide-api`                 | Android library | Editor-language, LSP, and formatter extension contracts          |

Dependencies exposed to plugin authors belong in `:plugin-api` or `:ide-api`. Application and Sora
implementation types should remain in `:app`. External Linux tools must be launched through
`:exec`, never a bare `ProcessBuilder` in feature code.

## Useful Gradle commands

```sh
# Compile the currently buildable production debug application
./gradlew :app:compileProdDebugKotlin

# Assemble APKs
./gradlew :app:assembleProdDebug

# Run every host-testable production variant
./gradlew :app:testProdDebugUnitTest :common:testProdDebugUnitTest \
  :exec:testProdDebugUnitTest :ide-api:testProdDebugUnitTest \
  :plugin-runtime:testDebugUnitTest :util:testDebugUnitTest \
  :plugin-api:test :feature:project:test :feature:sdk-manager:test \
  :feature:code-navigation:testDebugUnitTest

# Run app production unit tests only
./gradlew :app:testProdDebugUnitTest

# Inspect available tasks
./gradlew tasks
```

The CI Android workflow currently requests `assembleDevDebug`, builds the glibc asset first, and
uploads ABI-specific plus universal APKs. Keep workflow flavor assumptions synchronized with the
analytics dependencies.

## Generated and packaged artifacts

### glibc runtime

The full runtime build downloads/extracts packages, builds the preload shim, records safe symlinks,
and creates `app/src/main/assets/glibc.tar.zst`:

```sh
./scripts/build-glibc.sh
```

To rebuild the archive and shim using an existing `./glibc` tree:

```sh
./scripts/build-glibc.sh --reuse-glibc
```

This flow requires network access unless the package tree and cache are already populated. Review
[glibc runtime and compatibility shims](glibc-runtime-and-shims.md) before changing package layout,
loader paths, redirects, or symlink handling.

### Preload shim only

With Zig installed:

```sh
./scripts/build-shims.sh
```

This compiles the redirect, DNS, exec, fake-root, and syscall-compatibility sources into
`app/src/main/jniLibs/arm64-v8a/libpath_redirect.so`. Rebuilding only the `.so` does not update the
copy inside an existing glibc archive if that archive also carries it; verify both packaging paths.

### PTY native library

The `:exec` module builds JNI code through its configured CMake and NDK versions. Android Gradle
tasks invoke CMake as needed. Test process launch, interactive input, Ctrl+C, resize, termination,
reaping, and repeated open/close after native changes.

## Source layout and ownership

Important application entry points are:

| Source area                                         | Ownership                                      |
|-----------------------------------------------------|------------------------------------------------|
| `App.kt`                                            | application initialization and TextMate assets |
| `startup/`                                          | persistent roots and preference initialization |
| `ui/IDENavigation.kt`                               | setup gates and screen routing                 |
| `ui/home`, `ui/project`, `ui/editor`                | project lifecycle and editing workspace        |
| `ui/compile`, `ui/terminal`                         | build surfaces and PTY terminal                |
| `editor/language`, `editor/lsp`, `editor/formatter` | built-in extension implementations             |
| `plugin/` and `plugin-runtime/`                     | extension host and installed-plugin runtime    |

When working in a dirty checkout, preserve unrelated modifications. Many runtime artifacts are
large or generated; verify whether they are intentionally tracked before replacing them.

## Testing strategy

Run verification in proportion to the changed boundary:

| Change                     | Minimum useful checks                                                               |
|----------------------------|-------------------------------------------------------------------------------------|
| Pure Kotlin model/helper   | owning module unit tests                                                            |
| Compose screen or settings | app Kotlin compile plus device navigation/state restoration                         |
| Editor/LSP provider        | two files, provider precedence, startup failure, close/reopen                       |
| Linked TextMate grammar    | JSON/XML/YAML, fresh cache, seven-day stale path, invalid refresh, offline fallback |
| Process facade             | captured stdout/stderr, exit status, environment, child exec                        |
| PTY/terminal               | input, Unicode, resize, Ctrl+C, termination, FD/process leak                        |
| Gradle support             | sync and tasks contributed by plugin providers                                      |
| Plugin runtime/API         | load, dependency failure, activation rollback, unload cleanup                       |
| Runtime/shims              | packaged APK on arm64 hardware, subprocess descendants, DNS                         |

Android local unit tests cannot prove glibc, loader, PTY, or DocumentsProvider behavior. Those need
an emulator/device where applicable, and glibc/aarch64 behavior ultimately needs compatible arm64
hardware or virtualization.

### Current automated coverage

Tests live with the module that owns the behavior:

| Module                 | Host test focus                                                                                                                                                                                                                                                             |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `:app`                 | LSP normalization and selection, grammar-cache age, editor documents and tool-window state, project repositories/tree confinement, Git helpers, project creation, extension forms, terminal control codes, Gradle client parameter encoding, and model proxy reconstruction |
| `:common`              | Defensive parsing and bounds for persisted numeric preferences                                                                                                                                                                                                              |
| `:util`                | Persistent directory initialization, ZIP round trips/traversal/prefix/overwrite rules, and JDK executable repair                                                                                                                                                            |
| `:exec`                | Shell-like command tokenization, empty arguments, executable resolution, and missing-command failures                                                                                                                                                                       |
| `:feature:project`     | Language lookup/serialization, project paths, source-set precedence, argument persistence, and project serialization                                                                                                                                                        |
| `:feature:sdk-manager` | Foojay platform aliases, request parameters, response filtering/mapping, failures, downloads, and progress contract using a mock HTTP engine                                                                                                                                |
| `:ide-api`             | Validation and semantics of plugin forms, commands, progress, actions, and LSP definitions                                                                                                                                                                                  |

`:feature:code-navigation` remains an integration boundary around Kotlin/Java compiler PSI. Its
meaningful tests require a compiler analysis environment and representative parsed source; testing
only its navigation data classes would mirror constructors without protecting behavior. `:common`
editor widgets and `:plugin-runtime` dex/hook adapters similarly require Android/Sora or runtime
integration tests beyond the host suites above.

## Documentation and compatibility

Update documentation in the same change when modifying:

- a visible workflow or settings category;
- storage or Android permission behavior;
- an extension interface or routing priority;
- custom command/LSP trust boundaries;
- a generated artifact or regeneration command;
- process ownership, protocol framing, or shutdown behavior.

Public plugin contracts require stable ids and data-oriented types. If an incompatible API change is
necessary, document the migration in [Plugin architecture](plugin-architecture.md) and update sample
code. Keep README claims limited to behavior present on the documented branch.

## Pull requests

Before handing off a change:

1. inspect the focused diff and run `git diff --check`;
2. compile the relevant flavor/module;
3. run focused tests and record any unrelated blocker;
4. regenerate packaged artifacts when their sources changed;
5. update user and engineering documentation;
6. describe device/manual checks required after merge.

Do not commit secrets, signing credentials, downloaded JDKs, local Android SDK paths, application
data, Gradle caches, or user project content.
