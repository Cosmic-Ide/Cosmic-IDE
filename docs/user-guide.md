# User guide

## What Cosmic IDE is

Cosmic IDE is an Android-hosted development environment. The Android application owns the user
interface, editor, files, and project state. Compilers, shells, Gradle, and language servers run as
Linux/aarch64 processes inside an app-private glibc compatibility environment.

The current development build targets Android 8 or newer. The packaged Linux environment and
first-party toolchains are intended for arm64 devices. A project still has the permissions and
resource limits of an Android application; this is not a virtual machine, container, or root shell.

## First launch

Cosmic checks required capabilities in this order:

```text
glibc runtime -> selected JDK -> bundled language-server setup -> project home
```

1. **Environment Init** extracts the packaged glibc runtime into application storage.
2. **Toolchains** downloads and installs a Linux/aarch64 JDK selected through Foojay.
3. **Language-server setup** opens an interactive terminal script for Kotlin Language Server,
   Eclipse JDT LS, Metals/Coursier, and optional Android SDK components.
4. The **Projects** screen appears when the readiness checks pass.

The setup terminal can download substantial archives. Keep the app in the foreground, use a stable
network, and leave enough storage for a JDK, language servers, Gradle distributions, dependencies,
and project outputs. Settings > About > Setup reopens the language-server setup flow later.

## Project home

The Projects screen discovers directories below Cosmic's project root and orders them by last
modification time. Pull down to rescan after changing files externally.

Available actions are:

- tap a project card to open the workspace;
- use the add button to create a Gradle project;
- use the folder button to choose a plugin-provided project workflow such as **Clone Git repository
  **;
- use Import ZIP to extract an archive as a project;
- open a project's overflow menu for plugin actions, ZIP backup, or permanent deletion;
- open Settings from the top app bar.

An imported archive is named from the ZIP filename. Import is refused when a project directory with
that name already exists. ZIP files are treated as project content, so only import archives you
trust and inspect build scripts before running them.

On first use, the home screen asks whether anonymous analytics may be collected. The decision can
be changed under Settings > About.

## Creating a project

New projects are produced through Gradle's `init` task, executed by the out-of-process Gradle
tooling bridge. The form supports:

| Choice           | Current options                                                                |
|------------------|--------------------------------------------------------------------------------|
| Language         | Java, Kotlin, Scala                                                            |
| Build script DSL | Kotlin, Groovy                                                                 |
| Structure        | Single module, split project                                                   |
| Tests            | JUnit or TestNG for Java; JUnit or Kotlin Test for Kotlin; ScalaTest for Scala |

Project titles may contain letters, numbers, dots, underscores, and hyphens. Package names must
follow Java-style dotted identifiers. Creation output is shown in the form; the workspace opens
after Gradle completes successfully.

### Cloning with Git

Choose the folder button on Projects, then **Clone Git repository**. Enter an HTTPS or SSH remote;
the destination defaults to the final repository path component. An optional branch/tag and
single-revision shallow clone are supported. Clone output and percentage-bearing Git progress are
shown in the dialog, and the cloned project opens when the command succeeds.

interactive terminal running `pacman -S --needed git`. Confirm the package transaction, wait for the
terminal to report a successful exit, return to Projects, and retry the clone. This installation is
stored in Cosmic's app-private Arch/glibc environment and does not modify Android's system image.

For a project containing `.git`, its overflow menu includes status, fetch, pull, push, stage all,
commit, branch listing, and checkout. A non-Git project instead offers repository initialization.
Operations stream their output and progress into a generic plugin dialog. Network authentication is
non-interactive in captured operations; configure credentials or SSH keys in the terminal first.

Cosmic detects an existing project's main language from `src/main/<language>` or
`app/src/main/<language>`. If no Java, Kotlin, or Scala source directory exists, the project list
currently falls back to Kotlin metadata. This does not prevent custom language-server routing for
individual file extensions.

## Workspace layout

The workspace consists of four cooperating areas:

```text
top app bar: current file, run, undo, redo, actions
left drawer: project explorer
center: open file tabs and editor
bottom tool window: Gradle sync, builds, project commands, and terminal sessions
```

### Project explorer

Open the drawer from the menu icon. Selecting a source file opens or focuses its tab. File and
directory context actions currently include:

- create a Kotlin class, Java class, generic file, or folder;
- execute a selected supported source target;
- open a file through an external Android application;
- rename or delete an entry.

Delete operations are permanent inside the project directory. Keep a ZIP backup or source-control
history for important work.

### Tabs and saving

One editor view is reused while tabs change. Open document content is retained by the editor view
model, and active edits are written to disk as content changes. This behaves as autosave rather
than a separate dirty-buffer and explicit-save workflow.

Tabs have close controls. The optional double-click-to-close behavior is configured under Code
editor settings. When changing files, Cosmic replaces the editor language, formatter, and LSP
configuration for the new extension.

### Editor commands

The top app bar exposes Run, Undo, Redo, and an actions menu. The menu currently provides:

- **Execution > Program Arguments**: arguments passed to the project program;
- **Execution > Runtime Arguments**: JVM/runtime arguments;
- **Execution > Terminal**: open an interactive Bash PTY in the project root below the editor;
- **Project Commands**: run commands contributed by matching project-type plugins in bottom PTY
  tabs;
- **Editor > Format**: request the first matching enabled formatter;
- **Editor > Go To Line**: jump to a validated one-based line number;
- **Editor > View Statistics**: show byte, character, word, and line counts;
- **Gradle > Tasks**: choose a task discovered from the Gradle model;
- **Gradle > Resync Gradle**: discard the current model view and synchronize again.

The Run button prefers a matching plugin-provided run command. If none exists, it starts Gradle's
`run` task. Both paths open a terminal tab below the editor. Project commands are trusted shell code
and can use pipes, redirects, variables, and other Bash behavior.

Editor appearance and behavior are described in [Settings reference](settings-reference.md).

## Gradle sync and builds

Gradle integration runs in the selected JDK, outside ART, and is bound to one project at a time.
Opening a workspace starts or reuses that project's tooling provider. Leaving all project screens
stops it.

The resizable bottom tool window contains:

- a **Sync** tab with model-discovery output;
- one tab per launched Gradle task, plugin command, or interactive project terminal, including
  status, output, close, and rerun controls.

Without a matching plugin run command, the Run button starts the Gradle `run` task. The task chooser
can start any task advertised by the current Gradle model. Builds support streamed output, progress,
cancellation, and interactive input through
the tooling bridge. Gradle wrapper configuration and project build scripts still determine the
actual distribution, repositories, dependencies, and tasks.

Projects using Cargo, CMake, or another build system can register their workflow as a custom project
type. Those systems do not currently receive a dedicated task model UI.

## Custom project types

Settings > Extensions > Custom project types lets users register project templates without writing
an APK plugin. Each type can define marker files, creation code, sync code, build code, run code,
and additional
named commands using `Label :: shell code`. Creation scripts run in a new project directory and
receive `COSMIC_PROJECT_ROOT`, `COSMIC_PROJECT_NAME`, and `COSMIC_PROJECT_TYPE`.

After at least one type is enabled, choose the folder action on Projects and **Create custom
project**. Pick a type and project name. Cosmic runs its optional creation code, records the
selected
type under `.cosmic/project-type`, then opens the project. Existing projects can match a type when
any configured relative marker path exists—for example `Cargo.toml` or `package.json`.

Matching sync, build, run, and additional commands appear under **Project Commands** in the editor.
The
toolbar Run button invokes the first matching run command. Commands execute through Bash in a real
PTY tab, so interactive input, colors, progress, Ctrl+C, and normal terminal output work.

The fixed Sync tab uses Gradle only when the project root contains `gradlew`. Without a wrapper, the
first matching custom Sync command replaces Gradle sync in that tab and runs automatically. It can
be rerun from the tab header or Execution menu. If neither source exists, the tab reports that no
project sync command is configured.

These scripts are executable configuration with the same project and process access as Cosmic IDE.
Only add commands you trust. See [Custom project types](custom-project-types.md) for details.

## Terminal

Settings > Terminal opens an interactive Bash session in the glibc environment. It uses a real PTY,
supports foreground jobs and resize events, and includes an extra-key row for Escape, Ctrl, Alt,
Ctrl+C, navigation, and common terminal keys. Pinch/zoom changes terminal text size for the current
screen.

The selected JDK is exposed through `JAVA_HOME` and the toolchain `PATH`. Cosmic's home and common
Linux paths are redirected into app-private storage. Commands run with the app UID and cannot
bypass Android or SELinux permissions.

Use the terminal for package/tool installation, compilers, REPLs, build systems, and diagnostics.
Use the close action to terminate its process group before leaving a long-running command.

## Built-in language support

| File extensions                  | Language server        | Packaged grammar scope |
|----------------------------------|------------------------|------------------------|
| `.java`                          | Eclipse JDT LS         | `source.java`          |
| `.kt`                            | Kotlin Language Server | `source.kotlin`        |
| `.scala`, `.sc`, `.sbt`, `.mill` | Metals                 | `source.scala`         |

The LSP adapter exposes completion, diagnostics, hover, navigation, signature help, inlay hints,
and other capabilities advertised by the selected server. A server can connect without a grammar;
in that case semantic features may work while syntax highlighting remains plain.

Language and formatter providers can be enabled or disabled under Settings > Extensions. Routing
uses the highest-priority enabled provider that supports the current file.

## Custom language servers

Open Settings > Extensions > Custom language servers and choose **Add language server**. Each entry
contains:

| Field                 | Meaning                                                        |
|-----------------------|----------------------------------------------------------------|
| Name                  | Label shown in settings and connection messages                |
| File types            | Extensions without leading dots, separated by commas or spaces |
| Starter code          | Bash code that starts a standard-input/output LSP server       |
| TextMate grammar link | Optional syntax grammar URL, document URI, file URI, or path   |

For example, after installing `rust-analyzer` and making it available on the Cosmic toolchain
`PATH`:

```sh
exec rust-analyzer
```

Starter code runs as `bash -c` in the project root. It receives:

| Variable              | Value                                                   |
|-----------------------|---------------------------------------------------------|
| `COSMIC_PROJECT_ROOT` | Absolute project root                                   |
| `COSMIC_FILE`         | File that caused this server to be selected             |
| `BASH_ENV`            | Cosmic's non-interactive Bash environment configuration |

Use `exec` for the final process so connection shutdown reaches the language server rather than
only a parent shell. Server stdout must contain only `Content-Length`-framed LSP messages; send
diagnostics to stderr.

Only one custom server can be enabled per normalized extension. Enabling or saving another entry
with any overlapping file type disables the previous one. A custom entry has higher routing priority
than
the bundled Java, Kotlin, or Scala provider; disable it to restore bundled behavior.

### Linked TextMate grammars

A grammar is optional and independent from the server. It controls syntax scopes and editor pairs;
it does not add completion, diagnostics, or navigation.

Accepted sources are:

- direct `https://` or `http://` grammar URLs;
- Android `content://` URIs selected with **Choose grammar file**;
- `file://` URIs;
- absolute filesystem paths.

JSON, XML/plist, and YAML TextMate grammars are detected from their content. The grammar file must
declare its own `scopeName` and must be no larger than 5 MB. For GitHub, use a raw-content URL, not
the HTML `blob` page.

HTTPS behavior is deliberately offline-friendly:

1. Cosmic hashes the full URL and checks `cacheDir/textmate-grammar-cache`.
2. A successfully parsed cached grammar is reused for seven days.
3. After seven days, Cosmic downloads and parses a replacement.
4. The cache is replaced atomically only when the candidate grammar is valid.
5. A failed download or invalid update falls back to the last valid stale grammar.

Changing the URL produces a different cache key and therefore fetches immediately. Android
document and local-file sources are not copied into this cache; they are read again when the
language is configured so local edits can take effect.

## Plugins and extensions

Cosmic loads installed plugins from its plugin directory during application startup. Settings >
Extensions lists configurable editor-language, language-server, formatter, project-creation, and
project-action contributions and
shows installed plugin state/version information.

The **Plugins** tab is a searchable marketplace with separate marketplace and installed views.
Tap an extension to open its full-screen details, including its Markdown documentation and
install, update, setup, or uninstall actions. Cosmic verifies each published SHA-256 checksum,
safely stages the ZIP, validates its manifest, and activates it without adding the plugin to the
app APK. If an update cannot activate, Cosmic restores the previous plugin.

Some plugins need command-line tools. On a first install, Cosmic shows the exact setup command and
asks before opening it in the interactive terminal. You can defer this step and later choose
**Run setup** in the plugin details. Plugin code still runs in the application process and should be
treated as trusted code.

The Rust Support plugin installs Rust and rust-analyzer through
`pacman -S --needed rust rust-analyzer`, provides `.rs` LSP editing, recognizes `Cargo.toml`
projects, creates Cargo binary/library projects, and contributes Cargo fetch/check/build/run/test
commands.

See [Plugin architecture](plugin-architecture.md) for the developer contract and manifest format.
See [Git plugin and project APIs](git-plugin-and-project-apis.md) for the project UI contracts.

## Backups and Android file access

Project backup writes a ZIP through Android's document picker. Import reads a selected ZIP and
creates a project under Cosmic's project root. Cosmic also exposes an Android DocumentsProvider
named **Cosmic IDE Files**, allowing compatible system file pickers to browse application data.

Settings > About > Manage storage permission opens Android's all-files-access control. Grant it only
when workflows require paths outside app-owned storage. See
[Data, permissions, and trust](data-permissions-and-security.md) for exact storage boundaries.

## Troubleshooting

| Symptom                                   | First checks                                                                           |
|-------------------------------------------|----------------------------------------------------------------------------------------|
| Setup returns on every launch             | glibc directory, selected JDK, and all expected language-server launchers              |
| Project creation fails                    | selected JDK, network, Gradle output, package/title validation                         |
| Git network operation cannot authenticate | configure credentials or SSH keys from the terminal; dialog operations disable prompts |
| Gradle tasks never appear                 | Sync output, wrapper availability, provider process, build-script errors               |
| File remains plain text                   | extension, provider switch, active custom entry, grammar validity                      |
| LSP connection fails                      | starter command, executable `PATH`, stderr logs, stdout contamination                  |
| HTTPS grammar does not update             | cache is valid for seven days; change URL or clear app cache                           |
| HTTPS grammar fails while offline         | a valid copy must have been parsed and cached at least once                            |
| Terminal command is not found             | install arm64/glibc-compatible tool and verify the Cosmic `PATH`                       |
| Custom command rejects shell syntax       | it is an argument parser; invoke trusted `bash -c` explicitly                          |
| Formatting makes no change                | built-in Java/Kotlin formatter implementations are currently pass-through              |

For implementation-level diagnosis, continue with
[Codebase architecture](codebase-architecture.md),
[Process execution and terminal](process-execution-and-terminal.md), and
[Editor and language services](editor-and-language-services.md).
