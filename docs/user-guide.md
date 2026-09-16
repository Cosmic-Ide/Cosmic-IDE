# User guide

## What Cosmic IDE is

Cosmic IDE is an Android-hosted development environment. The app owns the UI, editor, files, and
project state. Compilers, shells, Gradle, and language servers run as Linux/aarch64 processes inside
an app-private glibc compatibility environment.

The current development build targets Android 8 or newer. The packaged Linux environment and
first-party toolchains are built for arm64 devices. Projects run with the permissions and resource
limits of an Android application. This is not a virtual machine, container, or root shell.

## First launch

Cosmic checks required capabilities in this order:

```text
glibc runtime -> selected JDK -> bundled language-server setup -> project home
```

1. **Environment Init** extracts the packaged glibc runtime into application storage.
2. **Toolchains** downloads and installs a Linux/aarch64 JDK selected through Foojay.
3. **Language-server setup** opens an interactive terminal script for Kotlin Language Server,
   Eclipse JDT LS, Metals/Coursier, and optional Android SDK components.
4. The **Projects** screen appears when readiness checks pass.

The setup terminal can download substantial archives. Keep the app in the foreground, use a stable
network, and leave enough storage for a JDK, language servers, Gradle distributions, dependencies,
and project outputs. Settings > About > Setup reopens the language-server setup flow later.

## Project home

The Projects screen lists directories below Cosmic's project root, ordered by last modification
time. Pull down to rescan after changing files externally.

Available actions:

- tap a project card to open the workspace;
- use the add button to create a Gradle project;
- use the folder button to choose a plugin-provided project workflow, such as **Clone Git
  repository**;
- use **Import ZIP** to extract an archive as a project;
- open a project's overflow menu for plugin actions, ZIP backup, or permanent deletion;
- open Settings from the top app bar.

Import uses the ZIP filename. Cosmic refuses the import if a directory with that name already
exists. ZIP files are treated as project content—only import archives you trust, and inspect build
scripts before running them.

On first use, the home screen asks whether to collect anonymous analytics. Change this later under
Settings > About.

## Creating a project

New projects use Gradle's `init` task, executed in a temporary project terminal. The form supports:

| Choice           | Current options                                                                |
|------------------|--------------------------------------------------------------------------------|
| Language         | Java, Kotlin, Scala                                                            |
| Build script DSL | Kotlin, Groovy                                                                 |
| Structure        | Single module, split project                                                   |
| Tests            | JUnit or TestNG for Java; JUnit or Kotlin Test for Kotlin; ScalaTest for Scala |

Project titles may contain letters, numbers, dots, underscores, and hyphens. Package names must
follow Java-style dotted identifiers. Creation output is shown in the form. The workspace opens
after Gradle succeeds.

### Cloning and managing with Git

Choose the folder button on Projects, then **Clone Git repository**. Enter an HTTPS or SSH remote.
The destination defaults to the final repository path component. Cosmic supports an optional
branch/tag and single-revision shallow clone. Progress shows in the dialog, and the cloned project
opens when the command succeeds.

If Git is not installed, Cosmic prompts to open a terminal running `pacman -S --needed git`. Confirm
the package transaction, wait for a successful exit, return to Projects, and retry the clone.

Cosmic includes a full-screen Git management screen accessible from the Editor toolbar menu, Home
project cards, or Settings > Extensions > Git. It features four tabs:

- **Changes**: Branch status, working tree changes, file staging, commit execution, and sync
  operations (Pull, Push, Fetch);
- **Branches & Remotes**: Local and remote branch lists with 1-click checkout, branch creation, and
  a remote URLs manager;
- **History**: Visual commit log with hashes, authors, timestamps, and messages;
- **Settings**: Author identity, configuration scope, default branch name, safe directories, and
  `git` binary status.

Cosmic detects an existing project's main language from `src/main/<language>` or
`app/src/main/<language>`. If no Java, Kotlin, or Scala directory exists, the list falls back to
Kotlin metadata. This does not prevent custom language-server routing for individual file
extensions.

## Workspace layout

The workspace has four cooperating areas:

```text
top app bar: current file, run, undo, redo, actions
left drawer: project explorer
center: open file tabs and editor
bottom tool window: Gradle sync, builds, project commands, and terminal sessions
```

### Project explorer

Open the drawer from the menu icon. Selecting a source file opens or focuses its tab. Context
actions include:

- create a Kotlin class, Java class, generic file, or folder;
- execute a selected supported source target;
- open a file through an external Android application;
- rename or delete an entry.

Delete operations are permanent. Keep a ZIP backup or source-control history for important work.

### Tabs and saving

Each open text tab retains its own `CodeEditor` instance and LSP document session. Open document
content is retained by the editor view model, and active edits are written to disk as content
changes. Files save automatically; there is no explicit save action.

Tabs have close controls. Double-click-to-close behavior is configured under Code editor settings.
When changing files, Cosmic replaces the editor language, formatter, and LSP configuration for the
new extension.

### Editor commands

The top app bar exposes Run, Undo, Redo, and an actions menu. The menu provides:

- **Git**: open the full-screen Git screen for the project;
- **Find & Replace**: toggle the inline document search and replace bar;
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
and can use pipes, redirects, variables, and other Bash features.

Editor appearance and behavior are described in [Settings reference](settings-reference.md).

## Gradle sync and builds

Gradle support is plugin-provided. Opening a workspace triggers a project sync command.

The resizable bottom tool window contains:

- a **Sync** tab with command output;
- one tab per launched Gradle task, plugin command, or interactive project terminal, including
  status, output, close, and rerun controls.

Without a matching plugin run command, the Run button starts the Gradle `run` task. The task chooser
can start any task advertised by the project's task providers. Sync, build, and run commands execute
in a PTY terminal within the editor tool window. Gradle wrapper configuration and project build
scripts still determine the actual distribution, repositories, dependencies, and tasks.

Projects using Cargo, CMake, or another build system can register their workflow as a custom project
type. Those systems do not currently receive a dedicated task model UI.

## Custom project types

Settings > Extensions > Custom project types lets users register project templates without writing
an APK plugin. Each type defines marker files, creation code, sync code, build code, run code, and
additional named commands using `Label :: shell code`. Creation scripts run in a new project
directory and receive `COSMIC_PROJECT_ROOT`, `COSMIC_PROJECT_NAME`, and `COSMIC_PROJECT_TYPE`.

After enabling at least one type, choose the folder action on Projects and **Create custom
project**. Pick a type and project name. Cosmic runs its optional creation code, records the
selected type under `.cosmic/project-type`, then opens the project. Existing projects match a type
when any configured relative marker path exists—for example, `Cargo.toml` or `package.json`.

Matching sync, build, run, and additional commands appear under **Project Commands** in the editor.
The toolbar Run button invokes the first matching run command. Commands execute through Bash in a
real PTY tab. Interactive input, colors, progress, Ctrl+C, and normal terminal output work.

The fixed Sync tab uses Gradle only when the project root contains `gradlew`. Without a wrapper, the
first matching custom Sync command replaces Gradle sync in that tab and runs automatically. Rerun it
from the tab header or Execution menu. If neither source exists, the tab reports that no project
sync command is configured.

These scripts are executable configuration with the same project and process access as Cosmic IDE.
Only add commands you trust. See [Custom project types](custom-project-types.md) for details.

## Terminal

Settings > Terminal opens an interactive Bash session in the glibc environment. It uses a real PTY,
supports foreground jobs and resize events, and includes an extra-key row for Escape, Ctrl, Alt,
Ctrl+C, navigation, and common terminal keys. Pinch to change terminal text size for the current
screen.

The selected JDK is exposed through `JAVA_HOME` and the toolchain `PATH`. Cosmic's home and common
Linux paths are redirected into app-private storage. Commands run with the app UID and cannot bypass
Android or SELinux permissions.

Use the terminal for package/tool installation, compilers, REPLs, build systems, and diagnostics.
Use the close action to terminate a command's process group before leaving.

## Language and Intelligence

Cosmic IDE provides code intelligence through the Language Server Protocol (LSP). Features like
completion, diagnostics, hover information, navigation, and signature help are powered by
language-specific servers running in the background.

| Category            | Example Tools                                                 |
|---------------------|---------------------------------------------------------------|
| **JVM**             | Eclipse JDT LS (Java), Kotlin Language Server, Metals (Scala) |
| **Native**          | rust-analyzer (Rust), clangd (C/C++), Go, Gleam               |
| **Web & Scripting** | Python, LuaLS (Lua)                                           |

### Syntax Highlighting

Syntax highlighting is managed by TextMate grammars. Most languages come with high-quality packaged
grammars. For unsupported languages, link a custom TextMate grammar from a URL or local file in
**Settings > Extensions**.

### Routing and Priority

Language support is modular. When you open a file, Cosmic IDE routes the request to the
highest-priority enabled provider. Toggle specific providers in **Settings > Extensions**.

## Custom language servers

Open Settings > Extensions > Custom language servers and choose **Add language server**. Each entry
contains:

| Field                 | Meaning                                                        |
|-----------------------|----------------------------------------------------------------|
| Name                  | Label shown in settings and connection messages                |
| File types            | Extensions without leading dots, separated by commas or spaces |
| Starter code          | Bash code that starts a standard-input/output LSP server       |
| TextMate grammar link | Optional syntax grammar URL, document URI, file URI, or path   |

For example, after installing `rust-analyzer` and making it available on the toolchain `PATH`:

```sh
exec rust-analyzer
```

Starter code runs as `bash -c` in the project root. It receives:

| Variable              | Value                                                   |
|-----------------------|---------------------------------------------------------|
| `COSMIC_PROJECT_ROOT` | Absolute project root                                   |
| `COSMIC_FILE`         | File that caused this server to be selected             |
| `BASH_ENV`            | Cosmic's non-interactive Bash environment configuration |

Use `exec` for the final process so connection shutdown reaches the language server instead of a
parent shell. Server stdout must contain only `Content-Length`-framed LSP messages. Send diagnostics
to stderr.

Only one custom server can be enabled per normalized extension. Enabling or saving an entry with any
overlapping file type disables the previous one. A custom entry takes priority over the bundled
Java, Kotlin, or Scala provider. Disable it to restore bundled behavior.

### Linked TextMate grammars

A grammar is optional and independent from the server. It controls syntax scopes and editor pairs.
It does not add completion, diagnostics, or navigation.

Accepted sources:

- direct `https://` or `http://` URLs;
- Android `content://` URIs selected with **Choose grammar file**;
- `file://` URIs;
- absolute filesystem paths.

JSON, XML/plist, and YAML grammars are detected from their content. The grammar file must declare
its own `scopeName` and must be no larger than 5 MB. For GitHub, use a raw-content URL, not the HTML
`blob` page.

HTTPS grammars are cached for offline use:

1. Cosmic hashes the full URL and checks `cacheDir/textmate-grammar-cache`.
2. A parsed grammar is reused for seven days.
3. After seven days, Cosmic downloads and parses a replacement.
4. The cache is replaced atomically only when the candidate grammar is valid.
5. A failed download or invalid update falls back to the last valid stale grammar.

Changing the URL produces a new cache key and fetches immediately. Android document and local-file
sources bypass this cache; they are read again when the language is configured so local edits take
effect.

## Plugins and extensions

Cosmic loads installed plugins from its plugin directory during application startup. Settings >
Extensions lists editor, language-server, formatter, project-creation, and project-action
contributions and shows installed plugin versions.

The **Plugins** tab is a searchable marketplace. Tap an extension to open its details,
documentation, and install/update/uninstall actions. Cosmic verifies each published SHA-256
checksum, stages the ZIP, validates its manifest, and activates it without adding the plugin to the
app APK. If an update fails to activate, Cosmic restores the previous plugin.

Some plugins need command-line tools. On a first install, Cosmic shows the exact setup command and
asks before opening it in the interactive terminal. You can defer this and choose **Run setup**
later. Plugin code still runs in the application process and should be treated as trusted code.

Cosmic supports plugins for **Rust, Gleam, Go, Gradle, Maven, Python, Lua, C/C++ (Clangd & CMake)**,
and more. For example, the Rust Support plugin installs Rust and rust-analyzer through
`pacman -S --needed rust rust-analyzer`, provides `.rs` LSP editing, recognizes `Cargo.toml`
projects, creates Cargo projects, and contributes Cargo fetch/check/build/run/test commands.

See [Plugin architecture](plugin-architecture.md) for the developer contract and manifest format.
See [Git plugin and project APIs](git-plugin-and-project-apis.md) for project UI contracts.

## Backups and Android file access

Project backup writes a ZIP through Android's document picker. Import reads a selected ZIP and
creates a project under Cosmic's project root. Cosmic also exposes an Android DocumentsProvider
named **Cosmic IDE Files**, allowing system file pickers to browse application data.

Settings > About > Manage storage permission opens Android's all-files-access control. Grant it only
when workflows require paths outside app-owned storage.
See [Data, permissions, and trust](data-permissions-and-security.md) for exact storage boundaries.

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

For implementation-level diagnosis,
check [Codebase architecture](codebase-architecture.md), [Process execution and terminal](process-execution-and-terminal.md),
and [Editor and language services](editor-and-language-services.md).