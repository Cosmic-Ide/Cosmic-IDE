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
- Android SDK components matching the repository's compile SDK (currently API 37, minor 2);
- the Android build tools accepted by AGP;
- Git and a network connection for Gradle dependencies.

Native/runtime regeneration additionally needs:

- Zig for the aarch64 glibc preload library;
- `tar` and `zstd`;
- `jq` to download or rebuild the glibc package tree;
- `curl` or `wget`;
- Android NDK 30 and CMake 4.1.2 for the `:exec` native build.

`gradle.properties` disables Java installation auto-detection and auto-download. Ensure the Gradle
launcher uses the intended JDK and provide explicit installation paths if a task requests a Gradle
Java toolchain.

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

The output APKs are written below `app/build/outputs/apk/prod/debug`. ABI splitting is enabled for
`arm64-v8a` only, and the universal APK is disabled.

The `dev` and `prod` product flavors select different analytics implementations in `:common`. The
dev implementation imports Firebase Analytics, but its dependency is currently commented out in
`common/build.gradle.kts`. Running `compileDevDebugKotlin` requires restoring the matching
dependency and configuration. The prod flavor builds without that Firebase dependency.

## Module map

| Module                 | Type            | Responsibility                                                   |
|------------------------|-----------------|------------------------------------------------------------------|
| `:app`                 | Android app     | Compose UI, navigation, editor, built-in providers, setup        |
| `:common`              | Android library | Shared preferences, analytics flavor boundary, editor data types |
| `:util`                | Android library | Persistent data roots and archive/filesystem helpers             |
| `:exec`                | Android/native  | glibc command runner, plain process facade, PTY JNI transport    |
| `:feature:sdk-manager` | JVM library     | Foojay JDK metadata/download client                              |
| `:plugin-api`          | Android library | Plugin descriptors, lifecycle, registries, services              |
| `:plugin-runtime`      | Android library | Plugin discovery, dex loading, activation, and cleanup           |
| `:ide-api`             | Android library | Editor-language, LSP, and formatter extension contracts          |

Dependencies exposed to plugin authors belong in `:plugin-api` or `:ide-api`. Application and Sora
implementation types stay in `:app`. Launch external Linux tools through `:exec`, never a bare
`ProcessBuilder` in feature code.

## Plugin API baseline (Phase 1)

Plugin-facing contracts are versioned independently of the app release version. The initial host
contract baseline is `1.0.0` for both `pluginApi` (`org.cosmicide.plugin.api`) and `ideApi` (IDE
extension contracts), tracked in `PluginCompatibility`. Versioned manifests (`"schemaVersion": 1`)
declare the half-open host ranges they support, and the runtime validates its actual contract
versions against those ranges before any plugin code loads — during installed discovery, direct
load, and marketplace staging alike. Manifests without `schemaVersion` keep the documented legacy
unchecked mode and are not range-validated.

`PluginDescriptor`'s canonical constructor, `copy`, and property getters are pinned by a unit test
so legacy compiled plugins stay linkable. Full jar-level binary compatibility against older
published artifacts is not yet proven; adding a binary-compatibility validator remains open work, so
treat this as a signature pin rather than a compatibility guarantee.

`:app` is being reduced to the application composition root. Target dependency rules and the
extraction sequence are tracked in [App module refactoring](app-module-refactoring.md).

## Useful Gradle commands

```sh
# Compile the currently buildable production debug application
./gradlew :app:compileProdDebugKotlin

# Assemble APKs
./gradlew :app:assembleProdDebug

# Run every host-testable production variant
./gradlew :app:testProdDebugUnitTest :common:testProdDebugUnitTest \
  :exec:testProdDebugUnitTest :ide-api:testProdDebugUnitTest \
  :plugin-runtime:testProdDebugUnitTest :util:testDebugUnitTest \
  :plugin-api:testDebugUnitTest :feature:sdk-manager:test

# Run app production unit tests only
./gradlew :app:testProdDebugUnitTest

# Publish snapshot artifacts for API modules to Maven repository
./gradlew publishSnapshots

# Inspect available tasks
./gradlew tasks
```

The CI Android workflow currently requests `assembleDevDebug`, builds the glibc asset first, and
uploads ABI-specific and universal APKs. Keep workflow flavor assumptions synchronized with the
analytics dependencies.

## Generated and packaged artifacts

### glibc runtime

The full runtime build downloads and extracts packages, builds the preload shim, records safe
symlinks, and creates `app/src/main/assets/glibc.tar.zst`:

```sh
./scripts/build-glibc.sh
```

To rebuild the archive and shim using an existing `./glibc` tree:

```sh
./scripts/build-glibc.sh --reuse-glibc
```

This flow requires network access unless the package tree and cache are already populated.
Review [glibc runtime and compatibility shims](glibc-runtime-and-shims.md) before changing package
layout, loader paths, redirects, or symlink handling.

### Preload shim only

With Zig installed:

```sh
./scripts/build-shims.sh
```

This compiles the redirect, DNS, exec, fake-root, and syscall-compatibility sources into
`app/src/main/jniLibs/arm64-v8a/libpath_redirect.so`. Rebuilding only the `.so` does not update the
copy inside an existing glibc archive. Verify both packaging paths.

### PTY native library

The `:exec` module builds JNI code using its configured CMake and NDK versions. Android Gradle tasks
invoke CMake as needed. Test process launch, interactive input, Ctrl+C, resize, termination,
reaping, and repeated open/close after native changes.

## Source layout and ownership

Important application entry points:

| Source area                                         | Ownership                                      |
|-----------------------------------------------------|------------------------------------------------|
| `App.kt`                                            | application initialization and TextMate assets |
| `startup/`                                          | persistent roots and preference initialization |
| `ui/IDENavigation.kt`                               | setup gates and screen routing                 |
| `ui/home`, `ui/project`, `ui/editor`                | project lifecycle and editing workspace        |
| `ui/compile`, `ui/terminal`                         | build surfaces and PTY terminal                |
| `editor/language`, `editor/lsp`, `editor/formatter` | built-in extension implementations             |
| `plugin/` and `plugin-runtime/`                     | extension host and installed-plugin runtime    |

When working in a dirty checkout, preserve unrelated modifications. Many runtime artifacts are large
or generated. Verify whether they are intentionally tracked before replacing them.

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
an emulator/device. Ultimately, glibc/aarch64 behavior needs compatible arm64 hardware or
virtualization.

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

`:feature:code-navigation` remains an integration boundary around Kotlin/Java compiler PSI.
Meaningful tests require a compiler analysis environment and representative parsed source; testing
only its navigation data classes would mirror constructors without protecting behavior. `:common`
editor widgets and `:plugin-runtime` dex/hook adapters similarly require Android/Sora or runtime
integration tests beyond the host suites above.

### Plugin lifecycle host gates

The plugin runtime host tests cover deterministic dependency planning, rollback, synchronous and
asynchronous serialization/reentry, immutable diagnostic snapshots, queued versus admitted request
cancellation, supervised context jobs, late resource disposal, and cooperative finalizer draining.
The optional scope service uses the existing coroutines catalog dependency; no version upgrade is
required. Existing public interface members and synchronous callback threads remain unchanged.

```sh
./gradlew :plugin-api:testDebugUnitTest :plugin-runtime:testProdDebugUnitTest
./gradlew :app:compileProdDebugKotlin
./gradlew :app:assembleProdDebug
```

Run these sequentially. Async cleanup proof covers cooperative child jobs and tracked disposables,
not arbitrary blocking callbacks, detached work, provider-session leases, or native/process cleanup.
Editor/project owner-aware leases remain deferred and require real session-boundary integration.
Device/arm64 validation remains necessary for ART loading and provider-backed editor resources.

## Documentation and compatibility

Update documentation when modifying:

- a visible workflow or settings category;
- storage or Android permission behavior;
- an extension interface or routing priority;
- custom command/LSP trust boundaries;
- a generated artifact or regeneration command;
- process ownership, protocol framing, or shutdown behavior.

Public plugin contracts require stable ids and data-oriented types. If an incompatible API change is
necessary, document the migration in [Plugin architecture](plugin-architecture.md) and update sample
code. Limit README claims to behavior present on the documented branch.

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