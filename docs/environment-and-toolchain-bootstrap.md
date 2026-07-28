# Environment and toolchain bootstrap

## Purpose

Bootstrap prepares three independent layers in app-private storage:

1. the bundled glibc runtime used to execute Linux binaries;
2. a user-selected Linux/aarch64 JDK;
3. optional language servers and Android SDK tools.

`IDENavigation` checks those layers in order and opens the first incomplete setup screen.

## Runtime deployment

The APK contains `assets/glibc.tar.zst`. `InstallResourcesScreen` extracts it to `files/glibc` and
restores validated symlinks recorded in the archive. That directory contains the loader, runtime
libraries, base commands, and preload shims; it is not a package-managed Linux installation.

The readiness check currently tests for `files/glibc`. If deployment becomes updatable or
recoverable in place, use a version/completion marker and an atomic temporary-directory rename so
an interrupted extraction cannot appear ready.

Runtime production and the meaning of each shim are documented in
[glibc runtime and compatibility shims](glibc-runtime-and-shims.md).

## JDK installation

The JDK settings UI resolves builds through Foojay using Linux, aarch64, and glibc as hard
compatibility constraints. Archives are installed below:

```text
files/jdks/<distribution>-<version>
```

The archive's leading directory is removed, executable permissions are repaired, and an
installation is accepted only when `bin/java` is executable. `Prefs.currentJDK` stores the chosen
directory name; `LinuxProcessRunner` derives `JAVA_HOME` from it for normal child processes.

Foojay metadata includes checksums, but the current installer does not verify them. Checksum
verification and stricter canonical-path/link validation are the main integrity gaps in this path.

## Development-tool setup

`ResourceUtil.prepareLanguageServerSetupScript` refreshes `files/setup.sh` from the APK and launches
it interactively in the terminal:

```text
bash <files>/setup.sh <filesDir> <cacheDir>
```

The script downloads and installs tools directly into `filesDir`; it does not create or enter a
separate Linux filesystem tree.

| Component                  | Installed path                      | Readiness evidence                    |
|----------------------------|-------------------------------------|---------------------------------------|
| Kotlin Language Server     | `files/kotlin-lsp`                  | `bin/intellij-server` is a file       |
| Eclipse JDT LS             | `files/jdtls`                       | Equinox launcher JAR under `plugins`  |
| Coursier and Metals        | `files/coursier`, `files/scala/bin` | `scala/bin/metals` is a file          |
| Android command-line tools | `files/Android/sdk`                 | `cmdline-tools/latest/bin/sdkmanager` |

Kotlin and JDT LS are extracted from upstream archives. Coursier installs the Scala toolchain and
Metals. Android setup selects the latest stable platform and build tools, then uses the external
Android ARM build-tools installer and writes the `aapt2` override to the user's Gradle properties.

The setup prompts are optional, while `isLanguageServerSetupIncomplete` currently requires all
three bundled language servers. Declining one therefore leaves the setup navigation incomplete.
If optional installation is the intended product behavior, readiness should follow enabled
capabilities rather than require every launcher.

## Consumption

Language servers and SDK commands must run through `ProcessExecutor` or `PtyProcessExecutor`.
Those facades supply the selected JDK and glibc environment; successfully downloading a launcher
does not prove it can be started correctly.

Language-server stdout is reserved for LSP framing. Providers drain stderr separately. Details of
the provider lifecycle are in [Editor and language services](editor-and-language-services.md).

## Trust and update rules

Bootstrap consumes executable artifacts from Foojay-selected JDK vendors, JetBrains, Eclipse,
Coursier/GitHub, and Google. The Android ARM build-tools installer is fetched and piped to Bash,
which is an explicit remote-code-execution boundary and should be pinned and verified before the
flow is considered reproducible.

When updating a tool:

1. confirm Linux/aarch64/glibc compatibility and archive layout;
2. update the readiness path if its launcher moves;
3. install once into an empty directory and once over an existing installation;
4. start the resulting tool through the normal process facade;
5. verify that protocol tools emit no diagnostics on stdout;
6. add or update checksum verification where available.

Local edits to `files/setup.sh` do not persist: the asset is copied again before setup.
