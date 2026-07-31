# App module refactoring

This page tracks the staged conversion of `:app` from a mixed implementation module into the
application composition root. Each stage must remain buildable and preserve user-visible behavior.

## Dependency rules

- `:app` owns application startup, the activity, root navigation, dependency composition, and
  Android adapters.
- Feature UI may depend on public contracts and injected services, but must not read app-wide
  singletons directly.
- Composables render state and emit intents. Filesystem access, preference writes, network access,
  and process launches belong in repositories, services, or lifecycle controllers.
- Plugin-facing implementations depend on `:plugin-api` and `:ide-api`; they do not depend on UI.
- External Linux processes are launched through `:exec`.
- No library or feature module may depend on `:app`.

## Migration stages

1. Establish reliable compile and unit-test gates, typed navigation, and characterization tests for
   sync, LSP, grammar-cache, and plugin-selection behavior.
2. Split the editor, settings, home, project creation, terminal, and project-monitor screens into
   UI, state, and operation owners while they remain in `:app`.
3. Introduce an application container and replace direct access to `FileUtil`, `CosmicPluginHost`,
   and `ToolingServerManager` from feature UI.
4. Extract terminal, Gradle client, environment, home, project creation, settings, bundled plugins,
   and editor modules in that order.
5. Reduce `:app` to startup, navigation, composition, and Android-specific adapters.

## Current progress

- Settings routes use typed destinations rather than display-name strings.
- Project sync selection is isolated behind a pure strategy: a Gradle wrapper takes precedence,
  otherwise the first plugin `SYNC` command is used, otherwise sync is unavailable.
- Project routes and editor language configuration now carry `Project` explicitly; the legacy
  global `ProjectHandler` has been removed.
- The editor workspace coordinator has been separated from project-tree UI, toolbar/actions,
  dialogs, and editor-content/status rendering. Those implementations remain package-internal
  while their state and filesystem responsibilities are extracted in subsequent stages. Each open
  text tab now retains its own `CodeEditor` and LSP document session.
- Project-tree create, rename, and delete operations now run through a root-confined filesystem
  service on the IO dispatcher. Tool-window build, terminal, and sync tab transitions are owned by
  a pure state reducer rather than being implemented inline in Compose.
- Extensions settings now has a small route scaffold with separate provider, custom LSP, custom
  project-type, installed-plugin, and repository sections. Compose depends on an
  `ExtensionsSettingsRepository` instead of constructing stores or reading the plugin host directly,
  and form-to-configuration conversion is pure and tested.
- Home project discovery/deletion, archive import/export, and plugin contribution lookup now sit
  behind repositories. New Project delegates Gradle Tooling API execution and rollback to a
  `GradleProjectCreator`, while form validation and Gradle init argument construction are pure and
  tested.
- `MainActivity` now creates one activity-scoped `AppContainer` and publishes it through Compose.
  Home, New Project, Extensions settings, editor command/task discovery, and project-session
  cleanup consume contracts from that container instead of constructing Android stores or reaching
  into `FileUtil`, `CosmicPluginHost`, or `ToolingServerManager` from their Composables.
- New Project is split into a route coordinator, immutable `NewProjectFormState`, form validation,
  form rendering, and the Gradle creator. Language changes choose a compatible test-framework
  default, and request conversion is pure and covered by unit tests.
- Obsolete tests for removed chat and formatter implementations have been removed from the active
  app test source set.
- `ProjectOutput` and its 2,064-line runtime/profiling implementation have been removed because no
  active workflow navigated to them. `GradleTask` remains a legacy route, while workspace builds
  use embedded editor tool-window sessions.

## Extraction gate

A feature is ready to become a Gradle module when it has one public entry point, receives its
dependencies through parameters or a feature-level factory, performs no lookup through `:app`, and
has tests for its state and operation-selection rules.

## Current app package map

| Package                     | Current responsibility                                                      | Next boundary                                                                 |
|-----------------------------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `org.cosmicide.app`         | Activity-scoped service composition and adapters for plugin/tooling globals | Remains in `:app`; move feature contracts out as their modules are extracted  |
| `org.cosmicide.ui.home`     | Project list UI, archive workflow, and project contribution contracts       | Extract after its repository APIs no longer depend on app-local types         |
| `org.cosmicide.ui.project`  | New-project UI/state plus Gradle project-creation service                   | Extract creation state/UI first; keep Android Tooling adapter in `:app`       |
| `org.cosmicide.ui.editor`   | Editor workspace, file tree, toolbar, dialogs, and tool windows             | Split editor core from project/tool-window integration                        |
| `org.cosmicide.ui.settings` | Settings routes and section UI                                              | Group preferences into typed stores before extraction                         |
| `org.cosmicide.ui.terminal` | Terminal screen/controller and PTY lifecycle                                | Move behind one public terminal-session entry point                           |
| `org.cosmicide.tooling`     | App-side Gradle provider protocol and process lifecycle                     | Extract after legacy Gradle screens are removed or restored                   |
| `org.cosmicide.plugin`      | Built-in extension registration and app-side plugin integration             | Keep APIs in `:plugin-api`; move bundled implementations to dedicated modules |

The next route decision is whether the legacy full-screen `GradleTask` route still needs to coexist
with embedded editor tool-window builds. After that, `JDKSettingsPanel` is the largest active UI
boundary and should be separated into toolchain state, operations, and rendering.
