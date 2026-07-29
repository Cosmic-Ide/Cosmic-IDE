# Settings reference

## Navigation

Settings is opened from the Projects screen. The current category list is:

| Category    | Current destination                                                              |
|-------------|----------------------------------------------------------------------------------|
| Code editor | Editor appearance and interaction controls                                       |
| Compiler    | Active installed JDK selector                                                    |
| Extensions  | Provider switches, custom LSP/project entries, installed plugins, repository URL |
| Terminal    | Interactive terminal session, not a preference form                              |
| Toolchains  | Foojay distribution/version browser and JDK install/uninstall queue              |
| About       | Version, setup, source, storage permission, donation, analytics                  |

Changes are stored in the app's default named `SharedPreferences` unless noted otherwise. Most
changes affect newly configured editors or future process launches; an existing LSP session is not
forcibly restarted when its provider switch changes.

## Code editor

| Control                      | Intended effect                                         |
|------------------------------|---------------------------------------------------------|
| Font size                    | Sora editor text size                                   |
| Tab size                     | Visual tab width                                        |
| JDT LS                       | Historical/experimental Java language-server preference |
| Editor font                  | Font file selected through Android's document picker    |
| Sticky scroll                | Keep structural context visible at the top              |
| Use spaces instead of tabs   | Indentation behavior                                    |
| Font ligatures               | Typeface ligature rendering                             |
| Word wrap                    | Wrap long visual lines                                  |
| Bracket pair auto-completion | Insert matching bracket/quote pairs                     |
| Scrollbar                    | Show editor scrollbars                                  |
| Fast delete blank lines      | Sora fast-delete behavior                               |
| Hardware acceleration        | Allow accelerated editor drawing                        |
| Non-printable characters     | Paint whitespace/control markers                        |
| Line numbers                 | Show the line-number gutter                             |

Provider selection is controlled under Extensions. In particular, disabling or enabling the Java
language-server provider there is the authoritative routing operation; the separate JDT LS toggle
is currently stored but is not consulted by the provider router.

Legacy editor preference aliases and numeric value types are migrated to the canonical schema when
preferences initialize. Editor changes apply the next time a file editor is configured.

## Compiler

**Selected Active JDK** lists directories currently installed below the app's JDK root and writes
the selected directory name to `current_jdk`. Future terminal, Gradle, language-server, and compiler
launches derive `JAVA_HOME` from that selection.

This screen does not download JDKs. Use Toolchains to install or remove distributions. Avoid
uninstalling the selected JDK while a build, terminal, or language server is running.

## Extensions

The Extensions screen is divided into four tabs so unrelated configuration does not share one
long settings page:

- **Providers** enables or disables registered editor and project extension providers.
- **Languages** manages custom language servers and linked TextMate grammars.
- **Projects** manages user-defined project types and their commands.
- **Plugins** lists installed plugins and configures the plugin index repository.

### Extension providers

Configurable contributions are grouped as editor languages, language servers, formatters, project
creation, project actions, and project commands.
Switching one contribution persists `extension_enabled.<extension-id>`. A missing value uses that
extension's `enabledByDefault` value.

The switch filters future routing requests. It does not unload the owning plugin or stop an already
running process immediately. Core routing and plain-text fallback providers that declare
`canDisable = false` do not appear here.

### Custom language servers

Each row shows its name, extension, first starter-code line, optional grammar link, active switch,
edit action, and delete action.

The editor accepts:

- a name;
- one or more extensions without leading dots, separated by commas or spaces;
- non-empty Bash starter code;
- an optional HTTP(S), `content://`, or `file://` grammar link, or an absolute path.

The file picker retains read access to the selected Android document URI. Only one custom entry is
active per extension; enabling or saving another entry for that normalized extension disables the
others. Disabling the sole entry leaves that extension available to lower-priority built-in or
plugin providers.

HTTPS grammars are limited to 5 MB, cached by full URL after successful parsing, refreshed after
seven days, and served stale when refresh fails. See
[User guide: Linked TextMate grammars](user-guide.md#linked-textmate-grammars).

### Custom project types

Each entry defines a name, zero or more relative marker paths, optional creation/sync/build/run
shell
code, and additional commands written as `Label :: shell code`. A type can be disabled, edited, or
deleted independently. Any configured marker can match an existing project; projects created from a
type also retain its id in `.cosmic/project-type`.

Creation code runs in the new project directory. Build, run, and additional commands appear in the
editor's Project Commands menu and open in bottom PTY tabs. These fields are trusted executable
configuration, not escaped literal arguments.

### Plugin marketplace

The Plugins tab provides search plus Marketplace and Installed filters. Extension cards use
user-facing Installed or Update labels instead of exposing internal runtime states. Tapping a card
opens a full-screen details sheet with Markdown documentation and install, update, setup, or
uninstall actions.

The settings button beside the filters edits the HTTPS plugin-index URL. Saving a repository URL
refreshes the marketplace immediately.

## Terminal

Selecting Terminal opens an interactive `bash -i` PTY rooted in app-private files. Its controls are
session controls rather than persisted settings:

- Back leaves the screen and releases its controller;
- Close terminates the session process;
- the extra-key bar provides Escape, Ctrl, Alt, Ctrl+C, navigation, and common symbols;
- zoom gestures adjust text size for the current screen.

The session uses the active JDK and Cosmic toolchain environment.

## Toolchains

Toolchains queries Foojay for maintained JDK distributions compatible with Linux/aarch64/glibc.
Select a distribution, mark desired versions, then apply the install/uninstall queue. Installed
archives live below `files/jdks` and are accepted only when `bin/java` is executable.

Installing a JDK does not automatically make it active in every code path. Confirm the selection in
Compiler after installation. Checksum verification is not currently enforced by the installer; the
integrity implications are documented in
[Environment and toolchain bootstrap](environment-and-toolchain-bootstrap.md).

## About

| Item                      | Behavior                                               |
|---------------------------|--------------------------------------------------------|
| About                     | Product and GPLv3 description                          |
| Donate                    | Opens the project donation page                        |
| App version               | Shows version name and debug commit when available     |
| Setup                     | Reopens the language-server/tool setup terminal        |
| Source code               | Opens the Cosmic IDE repository                        |
| Manage storage permission | Opens Android's all-files-access settings for this app |
| Analytics                 | Enables or disables analytics collection preference    |

All-files access is broader than Cosmic's normal app-owned project and toolchain directories.
Grant it only for workflows that need arbitrary shared-storage paths.

## Settings ownership for developers

New settings should use one constant in `PreferenceKeys`, one value type, and one default shared by
the UI and `Prefs`. Document whether a change affects existing sessions or only future routing. For
extension switches, keep enablement separate from plugin activation. For executable values such as
custom LSP starter code, preserve explicit user confirmation and never populate them silently from
a remote source.
