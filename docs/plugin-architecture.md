# Cosmic IDE Extension Architecture

## Purpose

Cosmic IDE uses typed extension points for built-in features and third-party plugins to contribute
behavior without relying on app implementation classes. The architecture separates contracts,
runtime loading, application integration, and user configuration so each part can evolve without
turning the plugin API into a collection of hooks.

The supported extension surfaces are:

- editor language providers (`LspServerProvider`, `EditorLanguageProvider`);
- editor formatter providers (`EditorFormatterProvider`);
- editor theme providers (`EditorThemeProvider`);
- editor preview providers (`EditorPreviewProvider`);
- editor action providers (`EditorActionProvider`);
- plugin screen providers (`PluginScreenProvider`);
- settings UI providers (`SettingsUiProvider`);
- project creation providers (`ProjectCreationProvider`);
- project action providers (`ProjectActionProvider`);
- project command providers (`ProjectCommandProvider`).

Built-in providers use the same registry and resolution rules as plugin contributions.

## Module boundaries

### `:plugin-api`

This is the stable plugin contract. The module is an Android library (it also re-exports the Pine
agent API), so plugins build against Android, not a plain JVM target. It owns:

- `CosmicPlugin` activation and deactivation;
- `PluginDescriptor` and dependency metadata;
- `PluginContext`;
- typed extension and service registries;
- registration disposal and owner-based cleanup;
- `ConfigurableExtension`, the common identity and settings metadata contract.

Plugin artifacts should depend on this module and, only when they contribute IDE behavior, on
`:ide-api`. They must not compile against `:app` or `:plugin-runtime`.

### `:ide-api`

This module defines IDE-facing extension contracts. It currently owns:

- `EditorLanguageProvider` for advanced editor integrations;
- `LspServerProvider` for standard LSP-based language support;
- `EditorFormatterProvider` for document and range formatting;
- `EditorThemeProvider` for TextMate editor themes;
- request, result, connection, and server-definition data types;
- `EditorExtensionPoints`, the canonical extension point identifiers;
- declarative project forms, progress events, terminal setup actions, and command execution;
- `ProjectExtensionPoints`, the canonical project contribution identifiers.

An LSP plugin should normally use `LspServerProvider`. Direct `EditorLanguageProvider`
implementations are for integrations that cannot be represented by the LSP adapter.

### `:plugin-runtime`

The Android runtime owns discovery, manifest parsing, class loading, activation, unload, and
cleanup. It also owns low-level runtime hooks. Hooks are an implementation mechanism, not a public
extension model, and should not be used where a typed extension point can express the same behavior.

### `:app`

The app owns composition:

- creation of the global extension registry;
- registration of built-in extensions;
- loading installed plugins;
- persisted extension enablement policy;
- routing editor requests to enabled providers;
- the Sora Editor LSP adapter;
- custom user-defined LSP configuration and process startup;
- settings UI.

### `:util`

General utilities belong here. Plugin contracts and runtime lifecycle code do not.

## Core model

An extension contribution has three identities:

| Identity           | Meaning                             | Example                                  |
|--------------------|-------------------------------------|------------------------------------------|
| Extension point id | The contract being implemented      | `org.cosmicide.editor.lspServerProvider` |
| Extension id       | Stable identity of one contribution | `org.cosmicide.editor.java`              |
| Owner plugin id    | Lifecycle owner of the registration | `org.cosmicide.core`                     |

The extension id is used for persisted settings. It must remain stable across releases. Renaming a
class or moving its package is harmless if its extension id does not change. Changing the id creates
a new settings identity and loses the user's previous enablement override.

The owner plugin id is used for cleanup. Unloading a plugin removes all registrations owned by that
plugin, regardless of extension point.

## Configurable extensions

Every editor extension contract implements `ConfigurableExtension`:

```kotlin
interface ConfigurableExtension {
    val id: String
    val displayName: String
        get() = id
    val description: String
        get() = ""
    val enabledByDefault: Boolean
        get() = true
    val canDisable: Boolean
        get() = true
}
```

`displayName` and `description` are shown in Settings > Extensions. Providers should supply concise
user-facing values. `enabledByDefault` is consulted only when no user override exists.

Infrastructure adapters and terminal fallbacks should set `canDisable` to `false`. They remain in
the registry but are not shown as independent switches. For example, the generic LSP editor adapter
cannot be disabled because disabling individual LSP providers is the meaningful user operation.

Enablement does not unregister or unload an extension. It is a persisted resolution policy:

1. all registrations remain available for inspection and lifecycle cleanup;
2. the resolver reads the provider's current setting;
3. disabled providers are removed before `supports` is called;
4. the remaining providers are evaluated by priority.

This distinction matters for plugin lifecycle. Toggling one contribution does not deactivate its
owner plugin or disable unrelated contributions from that plugin.

Settings are stored under `extension_enabled.<extension-id>`. A missing preference means
`enabledByDefault`. Changes affect new routing and formatting requests immediately. Existing editor
sessions and already-running server processes are not forcefully terminated.

## Registration and ordering

Registrations are synchronous and in-memory. The registry orders them by descending priority, then
by owner plugin id, preserving insertion order for equal ties and allowing duplicate values. Each
registration has an identity token: a stale disposable cannot remove an equal-valued replacement.
`DefaultExtensionRegistry` indexes by point id and caches immutable ordered snapshots by requested
runtime type. Mutating an unrelated point does not invalidate another point's snapshot. Lookup keeps
legacy id-plus-`isInstance` matching, including values registered through a broader point type. The
additive `ExtensionRegistryRevision` exposes a mutation stamp; the existing registry interfaces and
method signatures are unchanged. Old snapshots remain stable after mutations.

```kotlin
class MyPlugin : CosmicPlugin {
    override fun activate(context: PluginContext) {
        val registration = context.extensions.register(
            point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
            extension = MyLspProvider(),
            ownerPluginId = context.descriptor.id,
            priority = 350
        )
        context.registerDisposable(registration)
    }
}
```

The context facade automatically tracks returned registrations. Explicit `registerDisposable` calls
remain supported and idempotent. Omitted ownership (legacy `PluginIds.CORE`) maps to the actual
context plugin, including for `unregisterOwner(CORE)`. Explicit foreign-owner registration/removal
throws; removal after context close also throws. The host's raw registry retains global mutation for
host cleanup. These supported-API boundaries are not a sandbox for trusted in-process code.

Priority is a selection mechanism, not a load order. A provider should use the lowest priority that
expresses its precedence:

- `500`: user-defined custom LSP provider;
- `300`: bundled language servers;
- `200`: generic LSP editor adapter;
- `Int.MIN_VALUE`: plain-text fallback.

Third-party providers may choose their own value. A provider with a higher priority wins only when
its `supports` method returns `true` for the current request.

## Editor routing

Opening a file creates an `EditorLanguageRequest`. Routing proceeds as follows:

```text
file opened
  -> enabled EditorLanguageProvider registrations
  -> provider.supports(request), in priority order
  -> provider.configure(request)
  -> stop at the first provider returning true
  -> plain text fallback when none succeeds
```

Provider exceptions are logged and treated as a failed attempt, allowing the next matching provider
to run. `supports` should be fast, deterministic, and free of side effects. Expensive startup
belongs in `configure` or in the connection factory used by an LSP definition.

## LSP architecture

All bundled and custom language servers implement `LspServerProvider`. Java, Kotlin, and Scala own
their process launchers and server definitions in their respective language provider objects. There
is no second JDT-specific launcher in the generic LSP package.

The generic `LspEditorLanguageProvider` performs this flow:

```text
EditorLanguageRequest
  -> enabled LspServerProvider registrations
  -> first provider supporting the file
  -> LspServerDefinition
  -> session-owned Sora LSP adapter
  -> LspServerConnection
  -> server process stdin/stdout
```

The session-owned adapter manages editor mutability during connection, initialization timeouts,
TextMate wrapper selection, capability overrides, configuration dispatch, inlay hints, signature
help, and optional protocol tracing. Plugins do not depend on Sora's `LspEditor` classes.

### Implementing an LSP provider

```kotlin
class RustLspProvider : LspServerProvider {
    override val id = "com.example.rust-analyzer"
    override val displayName = "Rust language support"
    override val description = "Rust editing powered by rust-analyzer"
    override val priority = 350

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension.equals("rs", ignoreCase = true)
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtensions = setOf("rs"),
            displayName = "rust-analyzer",
            grammarScopeName = "source.rust",
            connectionFactory = RustConnectionFactory(),
            initializationTimeoutMillis = 120_000
        )
    }
}
```

`LspServerConnectionFactory` creates a connection for a request. A connection must:

- start lazily when `start()` is called;
- expose server stdin as `outputStream`;
- expose server stdout as `inputStream`;
- report whether it is closed;
- stop and release resources from `close()`.

The app shares one connection for each definition id and project. All open files whose extensions
belong to that definition attach to the same server process. Each tab maintains its own
`EditorTabSession` and `CodeEditor` instance, which connects to the shared server connection. The
adapter keeps the connection alive until its final editor disconnects.

Server diagnostics must go to stderr. Writing logs to stdout corrupts LSP framing.

`LspServerDefinition` validates non-empty ids, file extensions, display names, and positive
timeouts. Optional initialization settings are deliberately data-oriented so plugins do not need app
classes.

## Custom language servers

Settings > Extensions contains a built-in Custom language servers extension. Users can add a server
without building a plugin by supplying:

- a display name;
- one or more file extensions, without leading dots;
- shell starter code that launches an LSP server using standard input and output;
- optionally, a direct TextMate grammar URL, Android document URI, file URI, or absolute path.

Example:

```sh
rust-analyzer
```

Starter code runs through `bash -c` with the project root as its working directory. The process
receives Cosmic IDE's toolchain environment and these additional variables:

| Variable              | Value                                            |
|-----------------------|--------------------------------------------------|
| `COSMIC_PROJECT_ROOT` | Absolute path of the open project root           |
| `COSMIC_FILE`         | Absolute path of the file that triggered startup |
| `BASH_ENV`            | Cosmic's non-interactive Bash environment file   |
| `APP_FILES_DIR`       | Cosmic's app-private Arch runtime root           |

The server must speak LSP over stdin/stdout and must remain attached to the shell process. For a
multi-step script, use `exec` for the final server command so closing the editor connection
terminates the server cleanly:

```sh
export RUST_LOG=warn
exec rust-analyzer
```

Custom entries have their own enabled switch. The Custom language servers provider also has a global
switch. Both must be enabled for an entry to match. Entries are persisted as JSON in application
preferences and are read on each routing request. Add, edit, delete, and enable operations do not
require an app restart.

The custom provider has priority `500`. A custom entry for `java`, for example, takes precedence
over the bundled Java provider while that entry is enabled. Disable the entry to restore bundled
routing. Only one custom entry can be enabled for a file extension. Saving or enabling another entry
with an overlapping normalized extension disables its peers. This rule applies to custom entries;
the normal priority router chooses the single runtime winner among custom, bundled, and plugin
providers.

Linked grammars do not change LSP semantics; they provide TextMate syntax highlighting and editing
pairs around the LSP-backed editor. HTTPS grammar content is limited to 5 MB, cached by full URL,
refreshed after seven days, and replaced only after successful parsing. When refresh fails, the last
valid stale cache is used. `content://` permissions selected through the Android picker are
retained; local sources are read directly on subsequent editor configuration.

Starter code is executable user configuration. It has the same filesystem and process permissions as
Cosmic IDE. Remote plugin repositories must never populate or execute custom starter code without an
explicit trust and confirmation flow.

## Formatter providers

Formatters are independent contributions. They do not need an LSP server and do not start one.

```kotlin
class RustfmtProvider : EditorFormatterProvider {
    override val id = "com.example.rustfmt"
    override val displayName = "rustfmt"
    override val description = "Formats Rust source files"
    override val priority = 200

    override fun supports(request: EditorFormatterRequest): Boolean {
        return request.file.extension == "rs"
    }

    override fun format(request: EditorFormatterRequest): EditorFormatterResult {
        return EditorFormatterResult(text = formatRust(request.text))
    }
}
```

The formatter router filters disabled providers, calls matching providers by priority, logs
failures, and uses the first successful result. A result can replace the whole document or a
specific `TextRange`.

An LSP plugin may also register a formatter provider, but that provider should be a separate class
and registration. Keeping formatting separate prevents a local formatter from accidentally coupling
its lifecycle to a language server process.

## Editor preview providers

Preview providers contribute either a code/preview mode or a preview-only editor surface. They use
Android `View` instances so plugins do not need the Compose compiler.

```kotlin
class PdfPreviewProvider : EditorPreviewProvider {
    override val id = "com.example.pdf-preview"
    override val displayName = "PDF preview"
    override val presentation = EditorPreviewPresentation.PREVIEW_ONLY
    override val priority = 200

    override fun supports(request: EditorPreviewMatchRequest): Boolean {
        return request.file.extension.equals("pdf", ignoreCase = true)
    }

    override fun createView(request: EditorPreviewRenderRequest): View {
        return PdfPreviewView(request.context)
    }

    override fun updateView(view: View, request: EditorPreviewRenderRequest) {
        (view as PdfPreviewView).open(request.file)
    }
}
```

Register it at `EditorExtensionPoints.PREVIEW_PROVIDER`. Higher-priority providers win. For
`CODE_AND_PREVIEW`, `content` contains the current editor text; for `PREVIEW_ONLY`, it is `null` and
Cosmic IDE never loads or saves the file through the text editor. Providers should release WebViews,
decoders, or other owned resources in `releaseView`.

## Editor theme providers

Theme providers contribute TextMate themes to the Sora editor. Register them at
`EditorExtensionPoints.THEME_PROVIDER`. Each provider builds a `ThemeModel` whose model name must
exactly match the `name` field inside the TextMate theme JSON. Cosmic IDE keys themes by that name:
a mismatch means the theme loads under one name but is selected under another.

```kotlin
class SolarizedDarkThemeProvider : EditorThemeProvider {
    override val id = "com.example.theme.solarized-dark"
    override val displayName = "Solarized Dark"
    override val description = "A dark Solarized color scheme"
    override val isDark = true

    override fun createTheme(): ThemeModel = ThemeModel(
        IThemeSource.fromString(IThemeSource.ContentType.JSON, themeJson),
        "Solarized Dark"
    )
}
```

`isDark` tells Cosmic IDE which side of the automatic (match system) choice the theme belongs to.
The editor applies the theme selected in Settings > Editor > Theme; the automatic option keeps the
bundled darcula/Quiet Light pair. Themes are looked up by their model name, so it must stay stable.

## Editor action providers

Editor action providers contribute custom action items to the editor toolbar options menu. Register
them at `EditorExtensionPoints.EDITOR_ACTION_PROVIDER`.

```kotlin
class GitEditorActionProvider : EditorActionProvider {
    override val id = "org.cosmicide.git.editorAction"
    override val displayName = "Git"
    override val description = "Open Git version control screen from the editor"

    override fun actions(project: Project, file: File?): List<EditorAction> {
        return listOf(
            EditorAction(
                id = "git",
                label = "Git",
                description = "Open Git version control for ${project.name}",
                onClick = { /* navigate to Git screen */ }
            )
        )
    }
}
```

## Plugin screen providers

Plugin screen providers allow plugins to render entire full-screen Compose UI surfaces (e.g. Git
Screen). Register them at `UiExtensionPoints.PLUGIN_SCREEN`.

```kotlin
class GitScreenProvider(
    private val gitService: GitService
) : PluginScreenProvider {
    override val id = "org.cosmicide.git.screen"
    override val screenId = "git"
    override val title = "Git"
    override val displayName = "Git"
    override val description = "Version control and Git management screen"

    @Composable
    override fun Content(args: Map<String, String>) {
        GitScreen(gitService = gitService, args = args)
    }

    @Composable
    override fun Content() {
        GitScreen(gitService = gitService, args = emptyMap())
    }
}
```

## Settings UI providers

Settings UI providers contribute custom settings sections to Settings > Extensions > Plugin
Settings. Register them at `UiExtensionPoints.SETTINGS_UI`.

```kotlin
class GitSettingsUiProvider(
    private val gitService: GitService
) : SettingsUiProvider {
    override val id = "org.cosmicide.git.settings"
    override val label = "Git"
    override val displayName = "Git Settings"
    override val description = "Configure Git user identity, safe directories, and defaults"

    @Composable
    override fun Content() {
        GitScreen(gitService = gitService, args = mapOf("initial_tab" to "settings"))
    }
}
```

## Project creation and action providers

Project UI extensions are declarative. `ProjectCreationProvider.fields` and `ProjectAction.fields`
contain text, password, boolean, or choice `PluginFormField` values. Cosmic renders the form,
validates required values, owns coroutine cancellation, caps visible output, and displays
determinate progress when the provider reports a normalized value. Providers receive a string map
and an `OperationReporter`; they never depend on Compose classes.

Register project contributions like any other extension:

```kotlin
class ExamplePlugin : CosmicPlugin {
    override fun activate(context: PluginContext) {
        val commands = context.services.require(IdeServices.COMMAND_EXECUTION)
        context.registerDisposable(
            context.extensions.register(
                point = ProjectExtensionPoints.CREATION_PROVIDER,
                extension = ExampleProjectCreator(commands),
                ownerPluginId = context.descriptor.id,
                priority = 200
            )
        )
    }
}
```

A creator receives the canonical projects directory in `ProjectCreationRequest`. It must validate
that its destination remains inside that directory, must not overwrite an existing project, and
returns `ProjectCreationResult` only after usable project content exists. An action provider's
`actions(project)` should be fast and side-effect free; return an empty list when it does not apply.
Execution belongs in the suspending `create` or `execute` method.

Finite tools run through `CommandExecutionService`. Pass the executable and arguments separately; do
not concatenate user values into a shell command. Output callbacks may arrive from a background
thread. Check `CommandResult.exitCode` and treat non-zero exit as failure. Commands use Cosmic's
selected JDK, glibc runtime, and app-private Arch tool paths.

Long-lived tools such as language servers use `IdeServices.TOOL_PROCESS`. Its
`ToolProcessService.start` method returns a live `Process` with the same environment and forces
`APP_FILES_DIR` to Cosmic's app-private Arch runtime root. LSP plugins must request separate stderr,
drain it away from the protocol stream, and close all three streams when the connection stops.

Plugin environment installation must use `CosmicPlugin.setupActions`. Cosmic turns each
`PluginSetupAction` into a PTY-backed terminal handoff from the marketplace. Setup command strings
are a trusted-code surface, so plugins must not interpolate untrusted form values into them.

The bundled `GitPlugin` is the reference implementation. It contributes clone plus project-scoped
Git operations, reports progress parsed from `git --progress`, requests installation through
`pacman -S --needed git`, disables Git's hidden credential prompt for captured commands, and deletes
only a newly created partial clone when clone fails.

`ProjectCommandProvider` is the editor-facing command surface. Its `commands(project)` method is
side-effect free and returns a tree of `ProjectCommand` values for matching projects. A leaf
provides shell command text and can be classified as sync, build, run, or other. A branch leaves the
command text blank and provides `children`, which the editor renders as a nested submenu. The editor
gives a contributed run command precedence over the Gradle `run` fallback and opens all contributed
commands in bottom PTY tabs. Command text is intentionally shell code and is passed as an exact
argument to `bash -lc`; providers must never place untrusted values into it.

The fixed Sync tab has an additional ownership rule: `gradlew` takes precedence for Gradle sync. If
the wrapper is absent, the first enabled `SYNC` command replaces Gradle in that tab. In both cases,
sync is executed as a command in a PTY terminal.

The bundled `CustomProjectTypePlugin` is a user-configurable implementation. It contributes one
dynamic project creator plus a command provider backed by application preferences. Configuration can
associate existing projects through relative marker paths, while created projects persist the type
id in `.cosmic/project-type`.

Installed language plugins should also register `ProjectExtensionPoints.TYPE_PROVIDER`.
`ProjectTypeProvider.supports` recognizes a project root, while `languageName` and `fileExtension`
create a serializable `Language.Custom` value. Cosmic consults enabled type providers before its
built-in Java/Kotlin/Scala directory heuristics whenever it scans the Projects screen.

## Plugin manifest

Installed plugins live in their own directory under the app plugin root and include a manifest:

```json
{
  "schemaVersion": 1,
  "id": "com.example.cosmic.rust",
  "name": "Rust Support",
  "version": "1.0.0",
  "entryClass": "com.example.rust.RustPlugin",
  "classPath": ["plugin.apk"],
  "compatibility": {
    "pluginApi": { "minInclusive": "1.0.0", "maxExclusive": "2.0.0" },
    "ideApi": { "minInclusive": "1.0.0", "maxExclusive": "2.0.0" }
  },
  "capabilities": ["editor.lsp", "editor.formatter"],
  "enabledByDefault": true
}
```

Manifest `enabledByDefault` controls initial plugin activation. It is separate from
`ConfigurableExtension.enabledByDefault`, which controls an individual contribution after the plugin
has activated.

A manifest may declare `"schemaVersion": 1` with a `compatibility` envelope. Each entry is a
half-open host range (`pluginApi` for `org.cosmicide.plugin.api`, `ideApi` for the IDE extension
contracts) written as strict `major.minor.patch` bounds. The host validates its actual contract
versions against these ranges before creating class loaders or running plugin code — during
installed discovery, direct load, and marketplace staging alike. Manifests without a schema version
keep the legacy unchecked mode: they parse and load exactly as before, without API validation.
Manifest parsing is bounded (64 KiB text, 64 classpath entries, 128 dependency and capability
entries), and a malformed or incompatible package is isolated during discovery instead of preventing
the remaining plugins from loading.

### Dependency planning and lifecycle (Phase 2 core)

Built-in and installed plugins use one Android-free lifecycle core. Installed metadata is discovered
before planning; bundled plugins are registered as a batch. Activation follows deterministic,
dependency-first order (dependency ids break ties). Required missing, disabled,
incompatible-version, duplicate-id, cyclic, and failed dependencies block dependent code with
distinct structured causes. There is one installed version per id, with no automatic downloads or
uninstall cascades.

`dependencies` accepts legacy id strings and objects with `id`, `minVersion`, and `optional`. An
unconstrained edge accepts any nonblank legacy provider version. A constrained edge requires both
versions to be strict SemVer 2.0, including prerelease precedence; build metadata does not change
precedence. Numeric components have no machine-integer size limit. Optional dependencies are best
effort: unavailable/incompatible subtrees and cycle-forming edges are skipped, and an optional
activation failure does not block the requester. Required edges take priority. Dependencies order
lifecycle and now restrict the optional owned-service surface described below; implementation
classes are not shared between plugin loaders.

The synchronous manager preserves caller-thread callbacks. A dedicated transition gate serializes
loads, unloads, and package transactions; no registry lock is held for lifecycle callbacks.
Concurrent successful loads reuse the same active instance. Snapshot reads are nonblocking and show
the last published handle. Callback-triggered lifecycle mutations are explicitly rejected with a
`REENTRANT` cause. Callbacks must not synchronously wait for another thread's lifecycle operation,
which waits for the current callback. Failed requests may be retried by later calls.

Failed activation closes the context and removes owned registrations. Rollback stops only plugins
newly started by that transaction and not reachable from its successful root or previously active
plugins. Previously active dependencies are never unwound by an unrelated failure. Unload refuses
while active required dependents exist; optional dependents do not prevent unload. Marketplace
replacement/removal uses the same gate and checks refusal before mutating the installed package.
Failed replacement still restores/reactivates the previous directory; failure to prepare its backup
also attempts reactivation. This does not undo arbitrary plugin side effects or journal process
death.

Context close atomically seals disposable/contribution registration, disposes tracked resources in
reverse order, and immediately disposes late registrations exactly once. Contributions made through
`context.extensions` are owner-scoped, tracked automatically, and rejected after close. Raw registry
references and untracked services, jobs, listeners, and hooks remain the author's cleanup
responsibility.

`AndroidPluginManager` also offers opt-in suspending `loadAsync`, `loadInstalledPluginsAsync`,
`loadBuiltinsAsync`, and `unloadAsync` entry points. These run the same core on `Dispatchers.IO`;
the synchronous APIs retain their caller-thread callbacks. `states` is a conflated `StateFlow` of
immutable diagnostic snapshots (id, version, stable state, error text, structured failure kind, and
optional ACTIVATING/DEACTIVATING transition). It contains no plugin instances or mutable descriptor
collections. It reflects synchronous operations too; it is current state, not a durable event log.

Async requests serialize through a coroutine mutex and the existing transition gate. Cancellation
while queued skips mutation. Once admitted, a transaction completes even if its requester cancels;
an activated plugin may remain ACTIVE, so inspect `states` and explicitly unload if required.
Successful duplicate loads reuse the active instance; failed requests remain retryable. Direct
same-thread async callback reentry is rejected before dispatch. Callbacks and plugin job finalizers
must never await another lifecycle request, including by launching work on another thread.

Each context supplies the optional typed `PluginCoroutineScope.KEY` service through a local overlay,
without adding `PluginContext` members or publishing the scope to the host registry. Its
`SupervisorJob` and `Dispatchers.Default` scope isolate ordinary child failures and log them.
Context close seals registration, withdraws scope lookup, cancels the scope, then drains disposables
in reverse order. Existing scope references remain cancelled. Async failure/unload additionally
joins cooperative child finalizers outside the synchronous transition gate before returning or
admitting the next async request. Resources may already be disposed while finalizers run; finalizers
must tolerate that ordering. Synchronous unload requests cancellation and disposes synchronously,
but deliberately does not join jobs or change legacy blocking/thread semantics. Mixing synchronous
reload/package operations with async cleanup does not provide a job-drain barrier.

There is no forced interruption or timeout for arbitrary blocking callbacks, detached jobs,
non-cooperative finalizers, or native code. Such work can prevent async completion. Raw host service
publication and untracked resources still require explicit disposal. Provider-session leases and
automatic editor/project-resource release before provider disposal remain deferred: project routing
currently returns bare providers/commands without owner-aware session lifetimes. Close
provider-backed sessions before unload. Phase 2 is not fully complete; host tests do not prove ART
loading or device resource cleanup.

## Owned services and live host lookup (Phase 3)

`AndroidPluginManager` supplies installed contexts with runtime-local overrides (such as plugin
directory) above a live read-through host service view, not `serviceRegistry.copy()`. Built-ins also
read the live host. Host replacement, removal, and direct mutation of
`DefaultServiceRegistry.services`
are visible on the next lookup. Context close withdraws its service view. Legacy `register` still
replaces, `copy()` still produces a detached snapshot, and the public mutable map is unchanged.
Registration tokens protect replacements, including equal/same-instance re-registration through the
API. Direct remove/reinsert of the exact same object cannot be distinguished as a new lifetime.
There is deliberately no host-map revision, observation, or cache-coherence guarantee.

The additive `OwnedServices.KEY` service provides `publish`, `lease`, and `observe`; it adds no
mandatory `PluginContext` or registry members. One private hub per runtime is shared by built-in and
installed contexts. Publication is separate from legacy `context.services.get(key)` lookup. A key
may have only one live owned publication globally; duplicates throw, never silently replace. Tokens
and subscriptions cannot remove another publisher. Publication disposables are automatically
tracked.

Keys must name public interfaces resolved to the identical class by the host API class loader. Use
shared public SDK contracts (`compileOnly`), never private plugin classes or copied API classes. The
runtime does not link sibling loaders or install arbitrary shared-contract packages. Contract
methods must themselves use shared public data/API types; the runtime does not recursively validate
method signatures or sandbox returned objects.

A session sees its own publications and direct declared dependencies only, including optional edges
whose `minVersion` matches the provider. Visibility is not transitive. Publications become visible
when published during activation; failed activation withdraws them. Compatible optional providers
may appear later or disappear without stopping consumers. Required-provider unload still refuses
while required dependents remain active. The runtime rechecks version visibility against the actual
publishing session, including after reload.

`ServiceLease.get()` returns null after withdrawal or requester close. Old leases never rebind to a
new publication. Withdrawal drops the hub's instance reference; it cannot revoke an object already
returned or cancel an in-flight call. Reacquire before use and arrange application-level
coordination for long operations. `observe` is a cold, conflated current-state flow (initial absence
included), not an event bus or durable log; it may emit unchanged availability after unrelated
publication changes. Collect in `PluginCoroutineScope` and keep collectors cooperative. Provider
withdrawal notifies live collectors; requester close ends collection, without guaranteeing delivery
of a final null. All owned leases/publications are invalidated before reverse-order resource
disposal begins. This does not introduce leases for legacy extension providers or automatically
close editor/project sessions.

Existing direct hooks remain supported and untouched; their untracked registrations still require
explicit cleanup and implementation-target compatibility checks.

## Plugin repository and installation

Settings > Extensions > Plugins downloads the configured HTTPS JSON index. The index is either an
array or an object containing a `plugins` array. Every installable entry must declare:

```json
{
  "id": "com.example.cosmic.rust",
  "name": "Rust Support",
  "version": "1.0.0",
  "description": "Rust language support powered by rust-analyzer.",
  "detailedDescription": "## Rust support\n\nDetailed **Markdown** content.",
  "downloadUrl": "https://example.com/rust-support-1.0.0.zip",
  "sha256": "64 lowercase hexadecimal characters"
}
```

`description` is plain text for marketplace cards. `detailedDescription` supports Markdown in the
full details sheet. Older indexes using `shortDescription`, or only `description`, remain
compatible. `author` and the HTTPS `source` link are optional metadata. The downloaded ZIP is capped
at 25 MB and its SHA-256 must match before extraction. Extraction rejects traversal paths, more than
256 entries, and more than 50 MB of expanded data. The package is staged under the plugin root, its
manifest and loadable artifact are verified, and its APK, DEX, or JAR artifacts are made read-only
before Android's dynamic class loader sees them. Only then is it swapped into place. An update
preserves the previous directory and restores/reactivates it if the new plugin fails.

Plugins declare environment setup commands on their main `CosmicPlugin` implementation through
`setupActions`. These actions belong to the plugin lifecycle, not to project creation or project
actions. Cosmic never runs them in the background. After a fresh install, it shows the exact
commands and requires a choice before opening an interactive terminal. The marketplace retains a
manual **Run setup** action in the plugin details sheet.

## Failure behavior

- A failing plugin activation leaves no active registrations from that owner.
- A checksum, extraction, manifest, or activation failure does not replace a working installed
  plugin.
- A provider throwing from `supports`, `configure`, or `format` is logged; routing continues where
  the caller can safely try another provider.
- An LSP process that cannot start causes connection failure and leaves the editor adapter
  responsible for reporting and recovery.
- Malformed custom LSP JSON is ignored and logged instead of crashing settings or editor startup.
- Invalid custom names, file extensions, and empty starter code are rejected before persistence.
- Disabling a provider affects future requests. It does not kill existing process connections.

## Compatibility rules for plugin authors

1. Treat extension ids as permanent persisted API.
2. Depend only on `:plugin-api` and the extension contracts needed from `:ide-api`.
3. Do not cast registry, request, or service objects to app implementation types.
4. Keep `supports` side-effect free.
5. Start processes and allocate resources lazily.
6. Close every process, stream, listener, and registration through plugin lifecycle disposal.
7. Use additive manifest and API evolution; older hosts may not understand new capabilities.
8. Keep server logs off stdout when using LSP stdio transport.

## Testing guidance

Extension tests should cover:

- stable id and metadata;
- supported and unsupported file extensions;
- priority conflicts with another matching provider;
- default enablement and persisted overrides;
- disabled providers never receiving `supports` calls;
- process startup failure and connection close;
- whole-document and range formatter results;
- plugin activation rollback and unload cleanup;
- malformed custom configuration persistence;
- custom LSP precedence over bundled providers.

The app compilation task for integration verification is:

```sh
./gradlew :app:compileProdDebugKotlin
```

## Architectural decisions

Cosmic IDE uses a modular monolith. Separate extension-host processes would provide stronger
isolation, but require IPC, process supervision, API serialization, compatibility negotiation, and a
permission model. Those costs are not justified until the typed in-process API stabilizes.

Typed extension points are the primary plugin contract. Hooks remain supported in `:plugin-runtime`
as a trusted escape hatch for app-owned compatibility work, but they are version-sensitive, provide
no compatibility guarantees or automatic cleanup, and are not sandboxed.

Enablement is a resolution policy instead of registry mutation. This preserves plugin ownership,
makes settings reversible, avoids reactivation for a single contribution, and keeps registry
inspection truthful.

Bundled language servers implement the same `LspServerProvider` contract as plugins and custom user
configuration. This prevents language-specific editor wiring from diverging and gives precedence,
enablement, failure handling, and connection lifecycle one implementation path.

## Remaining work

- Add plugin signing and publisher trust on top of the current HTTPS plus SHA-256 repository model.
- Add explicit rollback controls to the marketplace UI.
- Add explicit plugin-level persisted enable/disable and reload controls in addition to contribution
  switches.
- Host API compatibility checks before activation are implemented: versioned manifests declare
  supported host ranges, validated against the host contract baselines; legacy unchecked mode
  remains documented. Plugin dependency planning and synchronous lifecycle serialization are
  implemented, with opt-in asynchronous state observation and context-owned cooperative
  cancellation. Provider-session leases and automatic editor/project release remain future work.
- Introduce process-group ownership so custom shell scripts with child processes are always stopped.
- Add typed extension points for general commands, diagnostics, custom terminal panels, and settings
  pages. Project creation/actions and terminal setup requests are now typed.
- Consider an out-of-process extension host when the API and permission model are mature enough to
  serialize safely.