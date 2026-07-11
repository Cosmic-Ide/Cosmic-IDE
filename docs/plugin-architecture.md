# Plugin Architecture

## Problem framing

Cosmic IDE needs extension points similar to VS Code or IntelliJ without making plugins depend on
app internals. The old plugin pieces lived in `util` and exposed low-level method hooks as the
primary API. That makes plugins hard to reason about, hard to unload, and tightly coupled to
implementation details.

Primary use cases:

- Load installed plugins from the app plugin directory.
- Let plugins contribute IDE behavior through typed extension points.
- Let plugins contribute LSP-backed language support without reimplementing app/editor wiring.
- Keep low-level runtime hooks available for app-owned integrations, but separate from the public
  plugin API.
- Allow built-in features and third-party plugins to use the same registration path.

Open constraints:

- Plugin signing, trust policy, repository metadata, and user enable/disable persistence still need
  product decisions.
- Runtime isolation is classloader-level, not process-level.
- Current first extension surfaces are editor language selection and LSP language server
  definitions; formatting has a stable contract but is not fully routed through the editor yet.

## Proposed boundaries

- `:plugin-api`: pure JVM contracts for descriptors, lifecycle, extension registry, service
  registry, and plugin manager results. This is the stable API plugin authors compile against.
- `:ide-api`: Android/editor-facing API for IDE extension contracts such as
  `EditorLanguageProvider`, `LspServerProvider`, and `EditorFormatterProvider`.
- `:plugin-runtime`: Android runtime implementation for manifest parsing, dex/apk/jar loading,
  plugin activation, plugin cleanup, and low-level Pine hooks.
- `:app`: owns app startup, built-in extension registration, installed plugin loading, and concrete
  editor integrations.
- `:util`: general utilities only. It no longer owns plugin API or hook runtime classes.

## Consistency map

- Extension registration is in-memory and synchronous. Priority determines deterministic provider
  ordering.
- Plugin activation is all-or-nothing for registered extensions: if activation fails, runtime
  disposes plugin resources and unregisters that plugin owner.
- Plugin unload calls `deactivate`, disposes registered resources, and unregisters all extensions
  owned by that plugin id.
- Installed plugin discovery is read-only from `FileUtil.pluginDir`; install/update/delete flows
  should be added as separate commands with explicit validation.

## API or event contracts

Plugin entry point:

```kotlin
class MyPlugin : CosmicPlugin {
    override fun activate(context: PluginContext) {
        val disposable = context.extensions.register(
            point = EditorExtensionPoints.LANGUAGE_PROVIDER,
            extension = MyLanguageProvider(),
            ownerPluginId = context.descriptor.id,
            priority = 300
        )
        context.registerDisposable(disposable)
    }
}
```

Plugin manifest:

```json
{
  "id": "com.example.cosmic.myplugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "entryClass": "com.example.MyPlugin",
  "classPath": ["plugin.apk"],
  "capabilities": ["editor.language"]
}
```

Editor language providers receive `EditorLanguageRequest` with `CodeEditor`, `Project`, and `File`,
then return `true` when they configured the editor. Providers are ordered by descending priority.

LSP plugins should usually register `EditorExtensionPoints.LSP_SERVER_PROVIDER` instead of
`LANGUAGE_PROVIDER`. The app owns the Sora `LspEditor` adapter, TextMate wrapper setup,
configuration dispatch, and editor enable/disable state.

```kotlin
class MyLspPlugin : CosmicPlugin {
    override fun activate(context: PluginContext) {
        context.registerDisposable(
            context.extensions.register(
                point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
                extension = MyLspProvider(),
                ownerPluginId = context.descriptor.id,
                priority = 300
            )
        )
    }
}

class MyLspProvider : LspServerProvider {
    override val id = "com.example.my-language.lsp"

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension == "my"
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtension = "my",
            displayName = "My Language Server",
            grammarScopeName = "source.my",
            connectionFactory = MyServerConnectionFactory()
        )
    }
}
```

## Tradeoffs

- A modular monolith is the right boundary now. Separate services would add deployment and IPC
  complexity without solving the IDE extension problem.
- The public API is small and typed. That is less flexible than arbitrary hooks, but safer to evolve
  and unload.
- Runtime hooks remain available in `:plugin-runtime` because current app behavior uses them, but
  they are no longer the extension model.
- Generic editor language providers still expose Sora editor types for advanced integrations. LSP
  plugins can use `LspServerProvider`, which avoids direct `CodeEditor` and `LspEditor` coupling.

## ADR outline

Decision: introduce dedicated `plugin-api`, `ide-api`, and `plugin-runtime` modules, route editor
language selection through a typed extension registry, and add a dedicated LSP server provider API
for LSP-backed languages.

Accepted option: modular monolith with explicit API/runtime boundaries.

Rejected option: keep plugin/hook classes in `util`. It preserves current coupling and makes every
plugin depend on internal runtime mechanics.

Rejected option: full VS Code-style extension host process now. It improves isolation but requires
IPC, process lifecycle policy, and a much larger API surface before the app has stable extension
points.

Follow-up risks:

- Add plugin signing and repository trust before enabling remote install.
- Persist user enable/disable state instead of relying on `enabledByDefault`.
- Formatter providers are separate from LSP providers. An LSP integration can also register an
  `EditorFormatterProvider`, but local formatters such as ktfmt and google-java-format should not be
  coupled to LSP server startup.
- Route commands, project import, diagnostics, and terminal contributions through typed extension
  points.
- Add lifecycle ownership for starting/stopping plugin-provided LSP server processes.
- Add compatibility/version checks for `PluginDescriptor.dependencies`.
