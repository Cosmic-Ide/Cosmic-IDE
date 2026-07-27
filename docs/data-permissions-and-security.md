# Data, permissions, and trust

## Security model

Cosmic IDE executes developer tools on a phone. Projects, Gradle build scripts, custom commands,
language-server startup code, plugins, and downloaded toolchains can all execute code with the
application's Android UID. The glibc layer changes binary compatibility; it does not create a
container, separate account, root environment, or security boundary.

Treat an unknown project the same way you would on a desktop IDE: inspect scripts and configuration
before syncing, building, running, or opening a tool that auto-starts for it.

## Android permissions

The manifest currently requests:

| Permission                  | Why the app may use it                                           |
|-----------------------------|------------------------------------------------------------------|
| Internet                    | JDK/tool downloads, Gradle, repositories, remote grammars, links |
| Manage external storage     | Developer workflows involving paths outside app-owned storage    |
| Read/write external storage | Legacy/shared-storage project and file interoperability          |
| Query all packages          | Discover applications able to open or handle project files       |

Android version and distribution policy determine which legacy storage permissions have effect.
Normal Cosmic project storage is app-specific external storage and does not require broad access on
modern Android. Settings > About > Manage storage permission opens the system all-files-access page;
grant it only when a workflow needs arbitrary shared paths.

## Persistent storage

### App-specific external files

`PreferencesInitializer` sets `FileUtil.dataDir` to `getExternalFilesDir(null)`. It owns:

| Directory    | Contents                                             |
|--------------|------------------------------------------------------|
| `projects/`  | Created and imported user projects                   |
| `plugins/`   | Installed plugin directories and classpath artifacts |
| `classpath/` | Persistent IDE classpath data                        |

Uninstalling the app normally removes app-specific external files, but device/OEM backup behavior
and manual file copies can differ. Project ZIP export is the explicit portable backup path.

### Internal files

The app's private `filesDir` owns executable environment state:

| Location                  | Contents                                      |
|---------------------------|-----------------------------------------------|
| `glibc/`                  | deployed compatibility runtime and home       |
| `jdks/`                   | installed JDK distributions                   |
| `kotlin-language-server/` | Kotlin Language Server                        |
| `jdtls/`                  | Eclipse JDT LS                                |
| `scala/`, `coursier/`     | Metals and Scala/Coursier tools               |
| `Android/sdk/`            | optional Android command-line SDK             |
| `gradle-tooling.jar`      | runtime copy of the packaged tooling provider |
| `setup.sh`                | refreshed language/tool setup script          |

These directories are implementation state, not project backups. Removing one may trigger setup or
break the corresponding tool until it is reinstalled.

### Cache

`cacheDir` contains disposable downloads, resolver data, language-server workspaces, Gradle/JVM
temporary files, and URL-linked TextMate grammars.

HTTPS grammar cache entries live under `textmate-grammar-cache/`. The filename is a SHA-256 hash of
the full URL rather than the URL itself. The full link remains persisted in the custom LSP
preference
JSON. A downloaded grammar is limited to 5 MB and becomes the cached copy only after successful
TextMate parsing. Entries are considered fresh for seven days. Android or the user may clear cache
at any time; the next editor configuration downloads the source again.

## Android DocumentsProvider

The app declares a DocumentsProvider titled **Cosmic IDE Files** rooted at the internal application
data directory. It supports browsing, search/recents flags, create, read/write, rename, copy, move,
and recursive delete according to each document row.

Document ids are resolved relative to the application data directory. Any change to this provider
must retain canonical-path confinement; accepting traversal outside the root would turn a document
id into arbitrary filesystem access. External applications receive access through Android's
document permission model.

The provider root is distinct from `FileUtil.dataDir`, which is app-specific external storage. Use
the project ZIP flow for portable project backup rather than assuming the provider exposes every
project directory.

## Network sources

Cosmic can contact:

- Foojay and selected JDK vendors for distribution metadata and archives;
- JetBrains/Kotlin, Eclipse JDT LS, Coursier/GitHub, Google SDK, and Android ARM tool sources during
  setup;
- Gradle distributions and project-declared artifact repositories;
- user-supplied plugin repository URLs;
- user-supplied HTTP(S) TextMate grammar links;
- community/source/donation links selected in the UI.

The plugin repository value is currently stored but not fetched by an installer UI. Project Gradle
configuration and terminal commands may contact arbitrary hosts.

Remote TextMate content is data parsed by the grammar engine, not executed as a shell command, but
it can still consume parser resources. The app applies a 5 MB limit, timeouts, isolated grammar
registries, and validated cache replacement. Prefer HTTPS and a source under your control. A GitHub
link should target raw content rather than an HTML page. Do not embed secrets in a grammar URL:
the complete link is stored in preferences and can appear in diagnostic logs when refresh fails.

## Executable trust boundaries

### Projects and Gradle

Gradle settings, init scripts, plugins, build scripts, compiler plugins, annotation processors, test
code, and application code can execute during sync/build/run. Importing a ZIP does not sanitize its
contents. Inspect unfamiliar projects before invoking Gradle.

### Custom project types and commands

Custom project creation, build, run, and utility fields are trusted Bash code. Creation runs through
`bash -lc` in a newly created directory; editor commands run through `bash -lc` in a bottom PTY tab.
They inherit the Cosmic toolchain environment and the app's filesystem/network authority. Review
every command before saving it. **Execution > Terminal** opens an interactive project shell with
the same authority.

### Custom language-server starter code

Custom LSP entries run stored starter code through `bash -c` with project paths in the environment.
They have the app's filesystem and network permissions and start automatically when a matching file
is configured. Never import or enable starter code from a project, plugin index, or web page without
clear review and confirmation.

Only one custom entry is active for an extension, reducing ambiguous routing but not reducing the
entry's authority. Disabling an entry affects future routing; close existing editor sessions if its
process must stop immediately.

### Plugins

Plugins are dex/classpath code loaded into the Android application process. They can access services
and extension registries and have no OS isolation from Cosmic itself. Plugin manifests describe
identity and capabilities but are not a sandbox. Install only trusted plugin artifacts and verify
their source and integrity.

### Toolchains and setup scripts

JDKs and language tools are native/JVM executables. The current JDK installer receives checksum
metadata but does not enforce it. The setup script also consumes upstream archives and, for Android
ARM build tools, remote installer logic. Pinning versions and verifying checksums are required
before
these flows can be considered reproducible or supply-chain hardened.

## Process and protocol isolation

LSP and Gradle provider stdout are protocol channels. Diagnostic text on stdout can corrupt framing
and cause the app to parse attacker-controlled or accidental output as protocol data. Providers must
send logs to stderr, which Cosmic drains separately.

PTY terminal processes intentionally receive interactive input and output and are not protocol
isolated. Ctrl+C targets the foreground process group. Closing UI resources and terminating/reaping
the child are separate lifecycle responsibilities.

## Analytics and privacy

The home screen asks for analytics consent and Settings > About exposes the stored analytics toggle.
The exact analytics backend is selected by the build flavor. Development and production source sets
must both respect the same preference contract.

Do not place secrets in analytics events, logs, custom LSP names/commands, project paths, or
protocol
traces. Incoming LSP tracing can include source text and should remain a diagnostic-only option.

## Backups and deletion

- Project backup creates a ZIP through Android's document picker.
- Project delete recursively removes the project directory and is not an undoable trash operation.
- Toolchain uninstall removes the selected JDK installation.
- Clearing app cache removes grammar copies and other disposable data, not their stored URLs.
- Clearing app storage or uninstalling can remove projects, plugins, preferences, and installed
  tools.

Keep external source-control or ZIP backups. Do not treat Gradle caches, build outputs, LSP
workspaces, or TextMate caches as authoritative data.

## Reporting security issues

Avoid posting secrets, private source files, API keys, or full protocol traces in public issues.
Provide the app version/commit, Android version, device ABI, minimal reproduction, relevant redacted
logs, and whether broad storage access was granted. Use the project contact channels in the root
README when a disclosure should not begin in a public issue.
