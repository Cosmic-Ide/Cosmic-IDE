# Cosmic IDE Extension Architecture

## Purpose

Cosmic IDE uses typed extension points to let built-in features and third-party plugins contribute
behavior without depending on app implementation classes. The architecture separates contracts,
runtime loading, application integration, and user configuration so each part can evolve without
turning the plugin API into a collection of hooks.

The first supported extension surfaces are:

- editor language providers;
- Language Server Protocol (LSP) server providers;
- editor formatter providers;
- project creation providers;
- project action providers.

Built-in providers use exactly the same registry and resolution rules as plugin contributions.

## Module boundaries

### `:plugin-api`

This is the stable, platform-neutral plugin contract. It owns:

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
- request, result, connection, and server-definition data types;
- `EditorExtensionPoints`, the canonical extension point identifiers;
- declarative project forms, progress events, terminal setup actions, and command execution;
- `ProjectExtensionPoints`, the canonical project contribution identifiers.

An LSP plugin should normally use `LspServerProvider`. Direct `EditorLanguageProvider`
implementations are intended for integrations that cannot be represented by the LSP adapter.

### `:plugin-runtime`

The Android runtime owns discovery, manifest parsing, class loading, activation, unload, and
cleanup.
It also owns low-level runtime hooks. Hooks are an implementation mechanism, not a public extension
model, and should not be used where a typed extension point can express the same behavior.

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
by owner plugin id for deterministic ties.

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

Always register the returned `Disposable` with `PluginContext`. The runtime also unregisters by
owner id during unload, but explicit disposal keeps resource ownership clear and handles partial
activation failures.

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
belongs
in `configure` or in the connection factory used by an LSP definition.

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
  -> app-owned Sora LSP adapter
  -> LspServerConnection
  -> server process stdin/stdout
```

The app-owned adapter is responsible for editor mutability during connection, initialization
timeouts, TextMate wrapper selection, capability overrides, configuration dispatch, inlay hints,
signature help, and optional protocol tracing. Plugins do not depend on Sora's `LspEditor` classes.

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
            fileExtension = "rs",
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

Server diagnostics must go to stderr. Writing logs to stdout corrupts LSP framing.

`LspServerDefinition` validates non-empty ids, file extensions, display names, and positive
timeouts.
Optional initialization settings are deliberately data-oriented so plugins do not need app classes.

## Custom language servers

Settings > Extensions contains a built-in Custom language servers extension. Users can add a server
without building a plugin by supplying:

- a display name;
- one file extension, without a leading dot;
- shell starter code that launches an LSP server using standard input and output;
- optionally, a direct TextMate grammar URL, Android document URI, file URI, or absolute path.

Example:

```sh
rust-analyzer
```

Starter code runs through `bash -c` with the project root as its working directory. The process
receives
Cosmic IDE's toolchain environment and these additional variables:

| Variable              | Value                                            |
|-----------------------|--------------------------------------------------|
| `COSMIC_PROJECT_ROOT` | Absolute path of the open project root           |
| `COSMIC_FILE`         | Absolute path of the file that triggered startup |
| `BASH_ENV`            | Cosmic's non-interactive Bash environment file   |

The server must speak LSP over stdin/stdout and must remain attached to the shell process. For a
multi-step script, use `exec` for the final server command so closing the editor connection also
terminates the server cleanly:

```sh
export RUST_LOG=warn
exec rust-analyzer
```

Custom entries have their own enabled switch. The Custom language servers provider also has a global
switch. Both must be enabled for an entry to match. Entries are persisted as JSON in application
preferences and are read on each routing request, so add, edit, delete, and enable operations do not
require an app restart.

The custom provider has priority `500`. A custom entry for `java`, for example, takes precedence
over
the bundled Java provider while that entry is enabled. Disable the entry to restore bundled routing.
Only one custom entry can be enabled for a file extension. Saving or enabling another entry for the
same normalized extension disables its peers. This rule applies to custom entries; the normal
priority router chooses the single runtime winner among custom, bundled, and plugin providers.

Linked grammars do not change LSP semantics; they provide TextMate syntax highlighting and editing
pairs around the LSP-backed editor. HTTPS grammar content is limited to 5 MB, cached by full URL,
refreshed after seven days, and replaced only after successful parsing. When refresh fails, the last
valid stale cache is used. `content://` permissions selected through the Android picker are
retained;
local sources are read directly on subsequent editor configuration.

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
failures,
and uses the first successful result. A result can replace the whole document or a specific
`TextRange`.

An LSP plugin may also register a formatter provider, but that provider should be a separate class
and registration. Keeping formatting separate prevents a local formatter from accidentally coupling
its lifecycle to a language server process.

## Project creation and action providers

Project UI extensions are declarative. `ProjectCreationProvider.fields` and
`ProjectAction.fields` contain text, password, boolean, or choice `PluginFormField` values. Cosmic
renders
the form, validates required values, owns coroutine cancellation, caps visible output, and displays
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

Finite tools run through `CommandExecutionService`. Pass the executable and arguments separately;
do not concatenate user values into a shell command. Output callbacks may arrive from a background
thread. Check `CommandResult.exitCode` and treat non-zero exit as failure. Commands use Cosmic's
selected JDK, glibc runtime, and app-private Arch tool paths.

Package installation, authentication prompts, pagers, and other interactive tasks must use a
`TerminalAction`. Cosmic turns it into a PTY-backed terminal route. Terminal command strings are a
trusted-code surface, so plugins must not interpolate untrusted form values into them.

The bundled `GitPlugin` is the reference implementation. It contributes clone plus project-scoped
Git operations, reports progress parsed from `git --progress`, requests installation through
`pacman -S --needed git`, disables Git's hidden credential prompt for captured commands, and deletes
only a newly created partial clone when clone fails.

`ProjectCommandProvider` is the editor-facing command surface. Its `commands(project)` method is
side-effect free and returns sync, build, run, or other `ProjectCommand` values for matching
projects.
The editor gives a contributed run command precedence over the Gradle `run` fallback and opens all
contributed commands in bottom PTY tabs. Command text is intentionally shell code and is passed as
an exact argument to `bash -lc`; providers must never place untrusted values into it.

The fixed Sync tab has an additional ownership rule: `gradlew` takes precedence and retains Gradle
sync. If the wrapper is absent, the first enabled `SYNC` command replaces Gradle in that tab and
Gradle tooling startup is skipped. Providers should therefore return sync commands only for project
layouts they positively recognize.

The bundled `CustomProjectTypePlugin` is a user-configurable implementation. It contributes one
dynamic project creator plus a command provider backed by application preferences. Configuration
can associate existing projects through relative marker paths, while created projects persist the
type id in `.cosmic/project-type`.

## Plugin manifest

Installed plugins live in their own directory under the app plugin root and include a manifest:

```json
{
  "id": "com.example.cosmic.rust",
  "name": "Rust Support",
  "version": "1.0.0",
  "entryClass": "com.example.rust.RustPlugin",
  "classPath": ["plugin.apk"],
  "capabilities": ["editor.lsp", "editor.formatter"],
  "enabledByDefault": true
}
```

Manifest `enabledByDefault` controls initial plugin activation. It is separate from
`ConfigurableExtension.enabledByDefault`, which controls an individual contribution after the plugin
has activated.

Plugin activation is atomic with respect to owned registrations. If activation fails, the runtime
disposes collected resources and unregisters the owner. Unload calls `deactivate`, disposes plugin
resources, and unregisters all contributions owned by the plugin id.

## Failure behavior

- A failing plugin activation leaves no active registrations from that owner.
- A provider throwing from `supports`, `configure`, or `format` is logged; routing continues where
  the
  caller can safely try another provider.
- An LSP process that cannot start causes connection failure and leaves the editor adapter
  responsible
  for reporting and recovery.
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

The app compilation task used for integration verification is:

```sh
./gradlew :app:compileDevDebugKotlin
```

## Architectural decisions

Cosmic IDE uses a modular monolith. Separate extension-host processes would provide stronger
isolation, but require IPC, process supervision, API serialization, compatibility negotiation, and a
permission model. Those costs are not justified until the typed in-process API stabilizes.

Typed extension points were chosen over arbitrary method hooks. Hooks remain in `:plugin-runtime`
for app-owned compatibility work, but are not a supported plugin contract because they couple
plugins
to implementation details and cannot provide reliable compatibility or cleanup.

Enablement is a resolution policy instead of registry mutation. This preserves plugin ownership,
makes settings reversible, avoids reactivation for a single contribution, and keeps registry
inspection truthful.

Bundled language servers implement the same `LspServerProvider` contract as plugins and custom user
configuration. This prevents language-specific editor wiring from diverging and gives precedence,
enablement, failure handling, and connection lifecycle one implementation path.

## Remaining work

- Define plugin signing, repository trust, and install/update/delete transactions before remote
  installation is enabled.
- Add explicit plugin-level persisted enable/disable and reload controls in addition to contribution
  switches.
- Add dependency and host API compatibility checks before plugin activation.
- Introduce process-group ownership so custom shell scripts with child processes are always stopped.
- Add typed extension points for general commands, diagnostics, custom terminal panels, and settings
  pages. Project creation/actions and terminal setup requests are now typed.
- Consider an out-of-process extension host when the API and permission model are mature enough to
  serialize safely.
