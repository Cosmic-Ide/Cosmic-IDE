# Building and contributing

## Supported development shape

Cosmic IDE is a Gradle multi-project Android application written primarily in Kotlin and Jetpack
Compose, with Java for the standalone Gradle provider and C/C++ for PTY and glibc compatibility
pieces. Application bytecode targets Java 17; some lower-level modules deliberately retain Java 11
compatibility.

The runtime product has three build layers:

```text
Android APK
  +-- packaged glibc runtime and native loader/shims
  +-- packaged standalone Gradle tooling JAR
  +-- Kotlin/Compose application and library modules
```

A normal Kotlin/UI edit needs only the Android build. Changes to a generated runtime layer require
its explicit producer command before the APK represents the new source.

## Prerequisites

For a standard application build, install:

- a JDK 17 Gradle runtime;
- Android SDK components matching the repository's compile SDK (currently API 37 with the configured
  minor SDK level);
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

| Module                     | Type            | Responsibility                                                            |
|----------------------------|-----------------|---------------------------------------------------------------------------|
| `:app`                     | Android app     | Compose UI, navigation, editor, built-in providers, setup, tooling client |
| `:common`                  | Android library | Shared preferences, analytics flavor boundary, editor data types          |
| `:util`                    | Android library | Persistent data roots and archive/filesystem helpers                      |
| `:exec`                    | Android/native  | glibc command runner, plain process facade, PTY JNI transport             |
| `:feature:project`         | JVM library     | Serializable project and language model                                   |
| `:feature:sdk-manager`     | JVM library     | Foojay JDK metadata/download client                                       |
| `:feature:tooling`         | JVM application | Real Gradle Tooling API provider packaged as a standalone JAR             |
| `:feature:code-navigation` | Android library | Compiler-backed code-navigation support                                   |
| `:plugin-api`              | JVM library     | Plugin descriptors, lifecycle, registries, services                       |
| `:plugin-runtime`          | Android library | Plugin discovery, dex loading, activation, and cleanup                    |
| `:ide-api`                 | Android library | Editor-language, LSP, and formatter extension contracts                   |

Dependencies exposed to plugin authors belong in `:plugin-api` or `:ide-api`. Application and Sora
implementation types should remain in `:app`. External Linux tools must be launched through
`:exec`, never a bare `ProcessBuilder` in feature code.

## Useful Gradle commands

```sh
# Compile the currently buildable production debug application
./gradlew :app:compileProdDebugKotlin

# Assemble APKs
./gradlew :app:assembleProdDebug

# Run unit tests across modules and continue after failures
./gradlew test --continue

# Run app production unit tests only
./gradlew :app:testProdDebugUnitTest

# Build and copy the standalone tooling provider into app assets
./gradlew :feature:tooling:copyToolingJarToAssets

# Inspect available tasks
./gradlew tasks
```

The CI Android workflow currently requests `assembleDevDebug`, builds the glibc asset first, and
uploads ABI-specific plus universal APKs. Keep workflow flavor assumptions synchronized with the
analytics dependencies.

## Generated and packaged artifacts

### Gradle tooling provider

`feature/tooling` creates a dependency-inclusive JAR whose main class is
`org.cosmicide.gradle.Main`:

```sh
./gradlew :feature:tooling:copyToolingJarToAssets
```

The resulting `app/src/main/assets/gradle-tooling.jar` is copied to private app storage at runtime.
Changes to provider source, JSON protocol, or its dependencies are incomplete until this asset is
regenerated. App-side protocol and provider-side protocol changes must land together.

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

This compiles the redirect, DNS, exec, and fake-root sources into
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
| `ui/compile`, `ui/output`, `ui/terminal`            | build/run surfaces and PTY terminal            |
| `editor/language`, `editor/lsp`, `editor/formatter` | built-in extension implementations             |
| `tooling/`                                          | app side of the Gradle JSON bridge             |
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
| Gradle protocol            | ping, models, build, streamed output, input, cancel, shutdown                       |
| Plugin runtime/API         | load, dependency failure, activation rollback, unload cleanup                       |
| Runtime/shims              | packaged APK on arm64 hardware, subprocess descendants, DNS                         |

Android local unit tests cannot prove glibc, loader, PTY, or DocumentsProvider behavior. Those need
an emulator/device where applicable, and glibc/aarch64 behavior ultimately needs compatible arm64
hardware or virtualization.

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
