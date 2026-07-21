# Application UI and project lifecycle

## Scope

This page describes the Android-side product flow: initialization gates, Navigation 3 routes,
project discovery and creation, workspace state, editor/file ownership, Gradle UI, settings, and
Android document integration. It complements the subsystem pages rather than repeating their
process internals.

## Application startup

AndroidX Startup runs `PreferencesInitializer` before `App.onCreate`. The initializer:

1. sets `FileUtil.dataDir` to app-specific external files;
2. creates `projects`, `classpath`, and `plugins` directories;
3. initializes the shared `Prefs` facade.

`App.onCreate` then initializes analytics, hidden-API/runtime hooks, bundled TextMate assets, and
the
plugin host. If `FileUtil` is unavailable, application initialization returns early; later code
assumes these roots exist.

`MainActivity` installs the current theme and hosts `IDENavigation`.

## Navigation graph

Navigation uses typed `Screen` keys and a saveable Navigation 3 back stack.

```text
InstallResourceScreen
  -> JDKSettingsScreen
  -> LanguageServerSetupScreen
  -> Home
       +-> NewProject -> Editor
       +-> Editor
       +-> Settings -> category destinations
```

The initial key is chosen from runtime readiness checks, not a stored onboarding flag. If a required
directory or launcher disappears, a later launch returns to the corresponding setup screen.

Project session ownership is inferred from whether the back stack contains `Editor`, `GradleTask`,
or `ProjectOutput`. When none remain, `ToolingServerManager.stopCurrent()` tears down the
fixed-project
Gradle provider. A `DisposableEffect` repeats shutdown when navigation leaves composition.

### Current route status

| Route                | Status                                                                                 |
|----------------------|----------------------------------------------------------------------------------------|
| Home                 | active                                                                                 |
| NewProject           | active                                                                                 |
| Editor               | active                                                                                 |
| GradleTask           | supported, while most workspace builds now use embedded tabs                           |
| ProjectOutput        | implemented runtime/profiling screen; automatic post-build navigation is commented out |
| Settings             | active                                                                                 |
| Code editor settings | active                                                                                 |
| Compiler settings    | active                                                                                 |
| Extensions settings  | active                                                                                 |
| Terminal category    | opens terminal directly                                                                |
| Toolchains           | active                                                                                 |
| About                | active                                                                                 |
| Formatter category   | listed, but no dedicated route implementation                                          |

Do not document a route as user-reachable merely because its `Screen` type and composable exist.

## Project discovery and home state

`ProjectViewModel` scans direct child directories of `FileUtil.projectDir` on IO and publishes a
`StateFlow`. It sorts projects by descending `lastModified` and infers language by checking both:

```text
src/main/{java,kotlin,scala}
app/src/main/{java,kotlin,scala}
```

Unknown layouts default to Kotlin metadata. Project identity is the absolute root path; there is no
separate project database.

Home owns import, backup, delete confirmation, refresh, navigation, and initial analytics consent:

- import creates a directory named from the selected ZIP and extracts into it;
- backup streams the project tree to a document-picker ZIP destination;
- delete invokes `Project.delete`, which recursively removes its root;
- pull-to-refresh repeats directory discovery.

Home also resolves enabled `ProjectCreationProvider` and `ProjectActionProvider` contributions.
Creation providers appear under the folder action in the floating toolbar. Actions returned for a
specific project are inserted into that project's overflow menu. The app renders their declarative
fields, output, and progress; plugin code never receives Compose or navigation objects. A
`TerminalAction` becomes a serializable `TerminalSession` route so setup commands retain a real PTY
and interactive package-manager prompts.

Archive extraction and project deletion are material filesystem operations. They belong on IO and
need traversal-safe archive helpers and explicit confirmation.

## Project creation

`NewProjectScreen` validates title/package input and translates UI choices into Gradle `init`
options. It creates the target directory, starts a temporary remote Tooling API connection, and runs
the `init` task with non-interactive options for language, DSL, project structure, package, and test
framework.

The provider is project-bound even when the target begins empty. Output is streamed into the form.
On success the new `Project` enters editor navigation; temporary tooling ownership is closed in the
creation flow.

The supported language model is intentionally small (`Java`, `Kotlin`, `Scala`). Custom file/LSP
support does not require extending this model unless project creation and source-root inference also
need to understand that language.

Plugin creators receive the canonical projects directory and return a `Project`. They must validate
the destination remains a direct child, reject an existing target, and clean up only a directory
they created when an operation fails. The bundled Git clone provider follows these rules and infers
Java/Scala source layouts, falling back to Kotlin metadata consistently with project discovery.

## Workspace state

`EditorScreen` coordinates three owners:

| Owner                    | State                                                         |
|--------------------------|---------------------------------------------------------------|
| `EditorViewModel`        | open file order, active file, cached document content, saving |
| `EditorToolingViewModel` | Gradle connection, task list, sync state/error/output         |
| Compose screen state     | drawer/dialogs, tool-window size/tab, build-session tabs      |

The `Project` value is serialized in the navigation key and also assigned to legacy
`ProjectHandler` state for Gradle-task and output screens. New code should prefer explicit route or
view-model ownership over expanding the global handler.

### Files and tabs

One Sora `CodeEditor` instance is reused. Opening another file first saves the active content,
creates or focuses a tab, loads cached or disk text, and routes language configuration for the new
request. Content change events update the active `EditorDocument` and immediately call
`File.writeText`.

`EditorDocument.savedContentHash` records whether the current cache matches the last write, but the
normal editing path autosaves each update. Closing a tab saves it once more, removes its cached
document, and activates the last remaining tab when necessary.

Because a single editor changes document identity, integrations must detach old language analysis,
LSP connections, subscriptions, and file-specific UI before attaching replacements. Retaining the
editor as if it permanently belongs to one file causes cross-tab diagnostics and leaked processes.

### Project explorer

The drawer recursively presents project files and opens selected files. Context menus can create
typed or generic entries, execute, open externally, rename, and delete. Dialog state carries the
target `File`; confirm handlers perform the mutation and refresh tree state.

Filesystem actions must remain confined to the project root. Add canonical-path validation when
accepting names or paths from new external inputs.

### Editor commands

The app bar delegates undo/redo to Sora. Run prefers the first matching plugin-provided run command
and falls back to the Gradle `run` task. The cascading menu owns program/runtime argument
persistence, project terminal creation, contributed commands, formatter routing, line navigation,
statistics, task selection, and resync.

Program and runtime arguments are stored as newline-separated files below `project.build/cache`.
They are project-local generated state rather than global preferences.

Editor Terminal and contributed project commands create bottom tool-window PTY sessions. Interactive
Terminal uses `bash -i`; declarative project commands pass shell code as an exact `bash -lc`
argument instead of reparsing it as a command line.

## Gradle workspace UI

`EditorToolingViewModel.initialize` starts sync once for a project root. It obtains
`BuildInvocations`,
combines task paths and selectors, and caps accumulated Sync output at 100,000 characters.

`EditorToolWindow` is a vertically resizable bottom surface. Its fixed Sync tab shows model output;
each task or project-command launch creates an `EditorBuildSession` with its own embedded terminal,
status, rerun counter, and close action. Closing a tab releases its composed terminal/controller.

Sync ownership is selected from project structure. A root `gradlew` keeps Gradle model sync and
initializes `EditorToolingViewModel`. Without that wrapper, the first enabled plugin `SYNC` command
replaces the fixed tab with a PTY session and Gradle tooling is not initialized. A wrapperless
project with no sync contribution receives an explanatory empty state.

The older full-screen `GradleTaskScreen` remains a valid route and shares the same terminal bridge.
`ProjectOutputScreen` can execute a resolved JVM main class and sample JVM process, heap, GC, and
runtime metrics, but the current Gradle success callback does not navigate to it automatically.

## Settings state

Most settings screens hold a Compose copy of a `SharedPreferences` value and write on change. The
consumer may observe the value immediately, on the next editor configuration, or only on the next
process launch. Extension switches are evaluated for new routing requests and do not unload plugin
code or terminate existing processes.

Preference keys and value types must be shared between UI and `Prefs`; current legacy mismatches are
listed in [Settings reference](settings-reference.md). A migration should read both old and new
representations safely before writing one canonical form.

## Android file surfaces

The home import/export flows use Android Activity Result document contracts, avoiding raw external
paths. Custom TextMate grammar selection uses `OpenDocument` and retains read permission for its
`content://` URI.

`FilesDocumentsProvider` separately publishes an internal-data document root. It maps document ids
to files and supports create, open, rename, copy, move, and delete. Its root differs from the
external
`FileUtil.dataDir` used for projects. Keep path resolution confined to the provider root and test
with the system DocumentsUI after changing flags, MIME types, or file operations.

## Lifecycle checks for UI changes

When changing application flow, test:

1. fresh launch through every setup gate;
2. process death/state restoration on Home, New Project, Editor, and Settings;
3. project switch and Gradle provider shutdown;
4. two open editor tabs with different extensions;
5. file rename/delete while the target is open;
6. build tab start, rerun, close, cancellation, and back navigation;
7. document import/export cancellation and duplicate names;
8. settings changes before and after recreating the destination;
9. terminal close versus back behavior;
10. clearing cache without clearing preferences or projects.
