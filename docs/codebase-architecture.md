# Codebase architecture

## System shape

Cosmic IDE is an Android modular monolith with two process boundaries:

```text
Android app
  +-- editor, UI, project state, and plugins
  +-- glibc process runner
       +-- JDKs, compilers, shells, and language servers
       +-- Gradle tooling provider
            +-- Gradle connection and daemon
```

The editor and plugin host run in ART. Desktop-oriented tools run as glibc Linux/aarch64
subprocesses. Gradle gets an additional boundary because its real Tooling API implementation runs
in a JDK subprocess and communicates with the app over JSON.

## Module boundaries

| Module                     | Owns                                                                                         |
|----------------------------|----------------------------------------------------------------------------------------------|
| `:app`                     | Compose UI, editor composition, built-in extensions, toolchain setup, and the Gradle client. |
| `:exec`                    | The only supported way to launch GNU/Linux tools, with plain-process and PTY transports.     |
| `:feature:tooling`         | Standalone JAR containing the real Gradle Tooling API provider.                              |
| `:plugin-api`              | Stable plugin lifecycle, registry, service, and descriptor contracts.                        |
| `:ide-api`                 | Editor, formatter, and LSP extension contracts.                                              |
| `:plugin-runtime`          | Android plugin discovery, dex loading, activation, and cleanup.                              |
| `:feature:project`         | Serializable project/language model and templates.                                           |
| `:feature:sdk-manager`     | JDK artifact discovery and download metadata.                                                |
| `:feature:code-navigation` | Compiler-backed code-navigation support.                                                     |
| `:common`, `:util`         | Shared preferences/data types and low-level filesystem/archive helpers.                      |

Plugin-facing code must depend on `:plugin-api` and `:ide-api`, not `:app` or
`:plugin-runtime`. External-process callers must use `:exec`; direct `ProcessBuilder` calls omit the
glibc compatibility environment.

`:app` is being reduced to the application composition root through buildable, behavior-preserving
steps. The target dependency rules and extraction sequence are tracked in
[App module refactoring](app-module-refactoring.md).

## Startup

`PreferencesInitializer` establishes persistent external storage through `FileUtil` and initializes
preferences. `App.onCreate` then loads TextMate assets, runtime hooks, and the plugin host. Built-in
editor extensions are registered before installed plugins so both use the same registries and
selection rules. `MainActivity` creates the activity-scoped `AppContainer`, which owns the concrete
repositories, project creator, ViewModel factory, and project-session adapter supplied to Compose.
Screens consume those contracts through `LocalAppContainer`; Android/global implementations remain
at the composition boundary.

Navigation chooses the first missing capability:

```text
glibc runtime -> selected JDK -> bundled language servers -> home
```

The navigation back stack owns the active Gradle tooling session. Leaving all project-related
screens stops the provider; application shutdown repeats the cleanup defensively.

## Storage ownership

| Location                                         | Contents                                                                                                             |
|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `files/glibc`                                    | Deployed glibc runtime and its home directory.                                                                       |
| `files/jdks`                                     | Installed JDK distributions.                                                                                         |
| `files/kotlin-lsp`, `files/jdtls`, `files/scala` | Language-server installations.                                                                                       |
| `files/Android/sdk`                              | Optional Android SDK.                                                                                                |
| `files/gradle-tooling.jar`                       | Runtime copy of the bundled Gradle provider.                                                                         |
| `cacheDir`                                       | Resolver files, language-server workspaces, downloads, Java temp files, and validated HTTPS TextMate grammar copies. |
| app-specific external files                      | User projects, plugins, and persistent IDE data managed by `FileUtil`.                                               |

## Main ownership rules

- `EditorViewModel` owns open documents and saves; editor providers own language configuration;
  LSP providers own server creation.
- `CosmicPluginHost` owns the extension registry; registrations are owned and removed by plugin id.
- `LinuxProcessRunner` owns the glibc environment and command wrapping for every external tool.
- `ToolingServerManager` owns at most one fixed-project Gradle provider process.
- Project screens request command contributions and tooling cleanup through
  `ProjectSessionServices`; the default adapter is the only Compose-facing bridge to the plugin and
  tooling globals.
- The app-side and provider-side Gradle protocol must change together.
- HTTPS TextMate grammar cache entries are URL-keyed, parser-validated before replacement, and
  refreshed after seven days. Cache storage is disposable; user configuration remains in
  preferences.

## Generated runtime artifacts

The sources alone do not determine runtime behavior. Changes must regenerate the relevant packaged
artifact:

| Artifact                                | Producer                                  |
|-----------------------------------------|-------------------------------------------|
| `assets/glibc.tar.zst`                  | `scripts/build-glibc.sh`                  |
| `jniLibs/arm64-v8a/libpath_redirect.so` | `scripts/build-shims.sh`                  |
| `assets/gradle-tooling.jar`             | `:feature:tooling:copyToolingJarToAssets` |

## Detailed references

- [glibc runtime and shims](glibc-runtime-and-shims.md)
- [Application UI and project lifecycle](app-ui-and-project-lifecycle.md)
- [Process execution and terminal](process-execution-and-terminal.md)
- [Gradle tooling bridge](gradle-tooling-bridge.md)
- [Editor and language services](editor-and-language-services.md)
- [Plugin architecture](plugin-architecture.md)
