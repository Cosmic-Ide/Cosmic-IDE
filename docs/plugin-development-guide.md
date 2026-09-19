# Plugin Development Guide

This guide provides a step-by-step walkthrough for building, testing, packaging, and publishing
plugins for Cosmic IDE.

---

## Overview

Cosmic IDE features a modular, in-process extension architecture. Plugins run directly in the
application runtime and interact with the IDE through typed extension points defined in
`:plugin-api` and `:ide-api`.

A plugin can contribute:

- **Language Support & LSP**: Connect any Language Server Protocol (LSP) server with syntax
  highlighting.
- **Custom UI Screens**: Render full-screen Compose UI views (e.g., version control, database tools,
  API clients).
- **Settings UI**: Add custom settings sections under **Settings > Extensions**.
- **Editor Actions**: Insert menu options into the editor top toolbar.
- **Editor Themes**: Provide custom TextMate color themes.
- **Editor Previews**: Render visual preview panels (e.g., PDF, Markdown, images).
- **Code Formatters**: Provide range or document code formatting.
- **Project Creators & Actions**: Add new project wizards and project-level context actions.
- **Project Commands**: Expose build, sync, run, and utility commands executed in editor PTY tabs.
- **Terminal Setup Actions**: Request system toolchain installation via `pacman`.

---

## 1. Project Setup

A Cosmic IDE plugin is packaged as a `.zip` archive containing a `plugin.json` descriptor and an
`.apk` or `.jar` compiled artifact.

### Gradle Configuration (`build.gradle.kts`)

Create a standard Android Library or Kotlin JVM project. Add dependencies on `:plugin-api` and
`:ide-api`:

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.myplugin"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Depend on Cosmic IDE extension API contracts
    compileOnly(project(":plugin-api"))
    compileOnly(project(":ide-api"))

    // Compose support (if contributing UI screens or settings)
    compileOnly(platform("androidx.compose:compose-bom:2026.09.00"))
    compileOnly("androidx.compose.runtime:runtime")
    compileOnly("androidx.compose.material3:material3")
}
```

> **Note**: Always use `compileOnly` for `:plugin-api`, `:ide-api`, and standard Android/Compose
> dependencies so they are provided by the Cosmic IDE runtime at runtime and not duplicated inside
> your plugin artifact.

---

## 2. Plugin Manifest (`plugin.json`)

Place `plugin.json` at the root of your plugin directory or ZIP package:

```json
{
  "schemaVersion": 1,
  "id": "com.example.myplugin",
  "name": "Sample Extension",
  "version": "1.0.0",
  "description": "Adds custom language, UI, and build support to Cosmic IDE.",
  "author": "Developer Name",
  "entryClass": "com.example.myplugin.MyPlugin",
  "classPath": ["plugin.apk"],
  "compatibility": {
    "pluginApi": { "minInclusive": "1.0.0", "maxExclusive": "2.0.0" },
    "ideApi": { "minInclusive": "1.0.0", "maxExclusive": "2.0.0" }
  },
  "capabilities": [
    "editor.lsp",
    "editor.formatter",
    "editor.theme",
    "ui.screen",
    "ui.settings",
    "editor.action"
  ],
  "enabledByDefault": true
}
```

### Manifest Fields

- `schemaVersion`: Manifest envelope version. The only supported value is `1`; manifests without
  this field run in the legacy unchecked mode described below.
- `id`: Permanent, reverse-domain identifier (e.g. `com.example.myplugin`). It must match the
  installation directory name.
- `name`: User-facing name shown in the Plugin Marketplace.
- `version`: Your plugin's own display version. The host accepts any non-empty string; API
  compatibility is declared through `compatibility`, not through this field.
- `entryClass`: Fully-qualified class name implementing `CosmicPlugin`.
- `classPath`: Compiled DEX/APK/JAR artifact filenames. `classPath`, `classpath`, and `artifact`
  are combined, with at most 64 entries counted before de-duplication.
- `compatibility`: Optional declaration of the host API ranges your plugin supports. Each entry is a
  half-open interval `[minInclusive, maxExclusive)` over strict `major.minor.patch` versions (no
  prerelease suffixes, operators, or wildcards). `pluginApi` covers
  `org.cosmicide.plugin.api`; `ideApi` covers the IDE extension contracts. The host checks its
  actual contract versions against these ranges before any of your code runs; a range that excludes
  the current host fails activation with an explicit reason.
- `capabilities`: Array of capability strings declared by your plugin (128 entries maximum).
- `enabledByDefault`: Whether the plugin activates upon initial installation.

### Dependencies and lifecycle threading

Declare dependencies as legacy id strings or objects, for example:

```json
"dependencies": [
  "com.example.legacy",
  { "id": "com.example.tools", "minVersion": "1.2.0-rc.1" },
  { "id": "com.example.extra", "minVersion": "2.0.0", "optional": true }
]
```

Unconstrained edges preserve arbitrary legacy version strings. With `minVersion`, both the minimum
and provider version must be strict SemVer 2.0 (`major.minor.patch`, optional prerelease/build
suffixes). Prereleases precede the corresponding release; numeric prerelease identifiers compare
numerically and reject leading zeros; build metadata is ignored. This dependency rule is separate
from the simpler host API compatibility bounds above.

Required missing, disabled, version-mismatched, duplicate, cyclic, or failed providers block your
activation before dependent code runs. Compatible dependencies start first in deterministic id
order. Optional edges are best effort; missing/incompatible/cyclic optional subtrees or activation
failures do not block you. Check availability yourself. The host neither downloads dependencies nor
shares implementation classes between plugin loaders.

Lifecycle callbacks remain synchronous on the requesting thread, serialized by a dedicated gate. Do
not call load/unload/install recursively from activation, deactivation, or cleanup callbacks:
these mutations fail explicitly. Do not block waiting for another thread's lifecycle request.
Concurrent successful loads activate once; a failed request can be explicitly retried. Unloading a
provider with active required dependents throws before deactivation or package mutation; unload the
dependents first. Optional dependents must tolerate the provider disappearing.

Register all resources with `PluginContext`. Close runs in reverse registration order; registration
after close immediately disposes the supplied resource. Contributions through `context.extensions`
are also tracked and reject registration after close. Manual disposable tracking remains supported.
Owned rollback cannot undo arbitrary external effects, untracked hooks, or detached jobs. For
cooperative background work, new hosts expose an optional typed service without changing
`PluginContext`:

```kotlin
val lifecycle = context.services.get(PluginCoroutineScope.KEY)
lifecycle?.scope?.launch {
    // Use cancellable APIs; release coroutine-owned resources in finally.
}
```

Import `org.cosmicide.plugin.api.PluginCoroutineScope` and `kotlinx.coroutines.launch`. The scope
uses
`Dispatchers.Default` plus a `SupervisorJob`: one failed child is logged without cancelling
siblings. Do not replace its Job or detach work if you need host cancellation. On close the host
withdraws this service, cancels the scope, and disposes registered resources in reverse order. New
launches on a retained scope are cancelled; finalizers must tolerate resources already being
disposed.

Hosts can opt into `AndroidPluginManager`'s suspending load/bulk-load/unload methods and observe its
immutable `states` flow. Those callbacks execute on `Dispatchers.IO`, not the requesting UI thread;
legacy synchronous calls remain unchanged. Async failure/unload waits for cooperative child cleanup,
whereas synchronous unload cancels without joining. A queued cancelled request does not mutate; once
admitted it finishes its transaction even if cancelled, and a successful load may remain ACTIVE. Do
not await lifecycle operations from callbacks or job finalizers. Blocking or non-cooperative code
cannot be forcibly interrupted and may prevent completion. These APIs do not migrate existing UI
callers or make lazy activation available. The owned-service API below supplies inter-plugin service
leases; automatic release of open editor/project sessions remains deferred. Close those sessions
before unloading their providers.

### Manifest limits and legacy mode

Manifest parsing is bounded: 64 KiB of text, at most 64 classpath entries (counted across the
aliases before de-duplication), and at most 128 dependency and capability entries. Unsupported
`schemaVersion` values and malformed compatibility ranges are rejected before your code loads.

Manifests without `schemaVersion` run in the **legacy unchecked mode**: unknown fields are ignored
and no API-range validation happens, so plugins built for older hosts keep loading. That also means
they get no protection against host API changes. Declare `schemaVersion` and
`compatibility` to opt into checked compatibility.

During discovery, one malformed or incompatible package is isolated and skipped; it does not stop
other plugins from loading.

---

## 3. Implementing `CosmicPlugin`

The plugin entry class must implement `CosmicPlugin` from `org.cosmicide.plugin.api`:

```kotlin
package com.example.myplugin

import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.launch
import org.cosmicide.plugin.api.onDispose
import org.cosmicide.plugin.api.register

class MyPlugin : CosmicPlugin {

    override fun activate(context: PluginContext) {
        context.register(
            point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
            extension = MyLspProvider(),
            priority = 300
        )
        context.onDispose { context.logger.info("MyPlugin resources released") }
        context.launch {
            // Do cooperative background work; release coroutine-owned resources in finally.
            context.logger.debug("MyPlugin background work started")
        }
    }
}
```

`MyLspProvider` is your implementation and `EditorExtensionPoints` comes from the IDE SDK. The
Kotlin `register` helper supplies `context.descriptor.id` and tracks the registration, including on
contexts without the runtime's scoped registry facade. Priority defaults to zero. Both `register`
and `onDispose` return a `Disposable` for early removal/cleanup. `onDispose` is synchronous and uses
the host's cleanup policy; keep it short and non-blocking.

`context.launch` returns a `Job` and uses the existing `PluginCoroutineScope` service; it creates no
new scope and inherits the host's dispatcher and cancellation. It throws `IllegalStateException` if
the service is absent (older hosts or a closed context). For optional support, use the nullable
service lookup shown above instead. Current hosts use `Dispatchers.Default` with supervised
children; use `withContext(Dispatchers.IO)` for blocking I/O, but cancellation is still cooperative.
On close, resources may be disposed before coroutine finalizers finish. Synchronous unload does not
join jobs; the suspending unload path waits for cooperative cleanup. No detached fallback or
automatic UI thread switching is provided.

---

### Owned registration and service lifetimes (Phase 3)

`context.extensions.register(point, value)` now assigns your plugin id automatically: the legacy
CORE default maps to `context.descriptor.id`. Explicit own ids still work; foreign owner
registration or `unregisterOwner` throws. `unregisterOwner(CORE)` removes only your contributions.
Registrations are tracked automatically, so manual wrapping remains valid but is unnecessary for
this facade. Equal-valued duplicates are independent registrations; disposing an old token cannot
remove a newer one. Snapshot ordering is priority descending, owner ascending, then insertion order
for ties.

Host services are live: each `context.services.get` observes the latest host replacement/removal,
with runtime locals such as plugin directory overriding the host. Do not retain a lookup expecting
it to track replacements automatically. Legacy host-map mutation is not observable through a flow.

New hosts also expose `org.cosmicide.plugin.api.OwnedServices.KEY`. This optional API is separate
from legacy host lookup and publication. For a public interface/key already shared by the host SDK:

```kotlin
val owned = context.services.get(OwnedServices.KEY) // null on older hosts
val publication = owned?.publish(SHARED_SERVICE_KEY, implementation)
// Publication is context-tracked; dispose early to withdraw. Duplicate live keys throw.

val lease = owned?.lease(SHARED_SERVICE_KEY)
val value = lease?.get() // null after withdrawal; old leases never bind to replacements

context.services.get(PluginCoroutineScope.KEY)?.scope?.launch {
    owned?.observe(SHARED_SERVICE_KEY)?.collect { current ->
        // Reacquire from current?.get(); tolerate absence and missed intermediate states.
    }
}
```

The sample's `SHARED_SERVICE_KEY` and `implementation` stand for a shared SDK interface and your
implementation; they are not built-in service names. Use `compileOnly` shared contracts with public
API/data types in their method signatures. Concrete-class keys and plugin-loader-only interfaces are
rejected. No sibling-loader linking or arbitrary shared-contract installation is provided.

You can see your own publications and those of direct declared dependencies with matching version
constraints. Optional providers may arrive/disappear; transitive dependencies are not visible.
Declare every provider you consume. Publication keys are globally unique, even across invisible
owners, and are separate from legacy host-map keys. Observe owned services through this API, not
`context.services.get(SHARED_SERVICE_KEY)`. Collection is cold and conflated and should use your
lifecycle scope; requester close ends collection, while provider withdrawal reports absence. Close
invalidates all leases and publications before tracked resources drain in reverse order. A retained
raw instance or already-running call cannot be revoked: reacquire before use and arrange safe
cancellation/coordination in the shared contract. No automatic editor-session cleanup is implied.
Direct hooks remain supported with unchanged APIs and their existing manual cleanup obligations.

## 4. Extension Point Examples

### A. Language Support & LSP (`LspServerProvider`)

To connect a stdio Language Server (e.g., `python-lsp-server`, `rust-analyzer`, `clangd`):

```kotlin
import org.cosmicide.editor.LspServerProvider
import org.cosmicide.editor.LspServerRequest
import org.cosmicide.editor.LspServerDefinition
import org.cosmicide.editor.LspServerConnectionFactory
import org.cosmicide.editor.ProcessLspServerConnection
import org.cosmicide.process.ProcessExecutor

class PythonLspProvider : LspServerProvider {
    override val id = "com.example.python.lsp"
    override val displayName = "Python Language Server"
    override val description = "Python code completion and diagnostics using pylsp"
    override val priority = 300

    override fun supports(request: LspServerRequest): Boolean {
        return request.extension.equals("py", ignoreCase = true)
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        return LspServerDefinition(
            id = id,
            fileExtensions = setOf("py"),
            displayName = "Python LSP",
            grammarScopeName = "source.python",
            connectionFactory = LspServerConnectionFactory { req ->
                val process = ProcessExecutor.start(
                    command = listOf("pylsp"),
                    directory = req.project.root
                )
                ProcessLspServerConnection(process)
            },
            initializationTimeoutMillis = 60_000
        )
    }
}
```

### B. Custom Full-Screen UI (`PluginScreenProvider`)

Render full Jetpack Compose screens accessible within the IDE navigation:

```kotlin
import androidx.compose.runtime.Composable
import org.cosmicide.ui.PluginScreenProvider

class MyToolScreenProvider : PluginScreenProvider {
    override val id = "com.example.mytool.screen"
    override val screenId = "mytool"
    override val title = "My Tool"
    override val displayName = "My Custom Tool"
    override val description = "Full screen tool for Cosmic IDE"

    @Composable
    override fun Content(args: Map<String, String>) {
        MyToolScreenView(args = args)
    }

    @Composable
    override fun Content() {
        MyToolScreenView(args = emptyMap())
    }
}
```

### C. Settings UI (`SettingsUiProvider`)

Contribute custom configuration sections to **Settings > Extensions**:

```kotlin
import androidx.compose.runtime.Composable
import org.cosmicide.ui.SettingsUiProvider

class MySettingsUiProvider : SettingsUiProvider {
    override val id = "com.example.mytool.settings"
    override val label = "My Tool"
    override val displayName = "My Tool Settings"
    override val description = "Configure API keys and defaults for My Tool"

    @Composable
    override fun Content() {
        MyToolSettingsView()
    }
}
```

### D. Editor Toolbar Actions (`EditorActionProvider`)

Add custom action items to the editor options menu:

```kotlin
import org.cosmicide.editor.EditorActionProvider
import org.cosmicide.editor.EditorAction
import org.cosmicide.project.Project
import java.io.File

class MyEditorActionProvider : EditorActionProvider {
    override val id = "com.example.mytool.editorAction"
    override val displayName = "My Tool Action"

    override fun actions(project: Project, file: File?): List<EditorAction> {
        return listOf(
            EditorAction(
                id = "run_my_tool",
                label = "Run My Tool",
                description = "Execute custom analysis on ${file?.name}",
                onClick = { /* trigger tool execution */ }
            )
        )
    }
}
```

### E. Code Formatter (`EditorFormatterProvider`)

Provide code formatting for specific file types:

```kotlin
import org.cosmicide.editor.EditorFormatterProvider
import org.cosmicide.editor.EditorFormatterRequest
import org.cosmicide.editor.EditorFormatterResult

class PythonFormatterProvider : EditorFormatterProvider {
    override val id = "com.example.python.formatter"
    override val displayName = "Python Formatter (autopep8)"
    override val priority = 200

    override fun supports(request: EditorFormatterRequest): Boolean {
        return request.file.extension.equals("py", ignoreCase = true)
    }

    override fun format(request: EditorFormatterRequest): EditorFormatterResult {
        val formatted = runAutopep8(request.text)
        return EditorFormatterResult(text = formatted)
    }
}
```

---

## 5. Building and Packaging

1. **Build the APK/JAR**:
   Run `./gradlew assembleRelease` to compile your plugin module into an `.apk` file.

2. **Assemble the ZIP package**:
   Create a ZIP archive containing `plugin.json` and your compiled `.apk`:
   ```text
   my-plugin-1.0.0.zip
   ├── plugin.json
   └── plugin.apk
   ```

---

## 6. Testing Plugins Locally

To test your plugin on a device or emulator without setting up a remote repository:

1. Locate Cosmic IDE's internal plugin directory on the target device:
   `/data/data/org.cosmicide/files/plugins/<your-plugin-id>/`
2. Extract `plugin.json` and `plugin.apk` into that directory.
3. Restart Cosmic IDE. The plugin will be discovered, verified, and activated automatically.

---

## 7. Publishing to a Plugin Marketplace

Cosmic IDE downloads plugin marketplace data from a JSON repository index.

### Repository Index Schema (`plugins.json`)

Host a JSON index file at an HTTPS URL:

```json
{
  "plugins": [
    {
      "id": "com.example.myplugin",
      "name": "Sample Extension",
      "version": "1.0.0",
      "description": "Adds custom language and UI tools to Cosmic IDE.",
      "detailedDescription": "## Sample Extension\n\nFull **Markdown** description of features and usage.",
      "author": "Developer Name",
      "downloadUrl": "https://example.com/downloads/my-plugin-1.0.0.zip",
      "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
  ]
}
```

### Marketplace Verification & Security Rules

- **SHA-256 Checksum**: The downloaded ZIP package SHA-256 hash **must** match the `sha256` field in
  `plugins.json`.
- **Package Limits**: Downloaded ZIP archives are capped at 25 MB, with expanded data capped at 50
  MB and 256 files.
- **Atomic Rollback**: If an update or new installation fails during activation, Cosmic IDE
  automatically restores the previously working plugin version.
- **Repository Configuration**: Users can add or update repository URLs in **Settings > Extensions >
  Plugins > Settings**.

---

## 8. Best Practices & Safety

1. **Never Contaminate Standard Output**: Stdio language servers must write protocol messages only
   to stdout. All debug logs, traces, or diagnostic output must go to stderr.
2. **Dispose Resources**: Always register returned `Disposable` instances with `PluginContext` to
   prevent memory leaks and orphaned process leaks.
3. **Use `compileOnly`**: Keep your plugin APK size small by marking core IDE API and Compose
   libraries as `compileOnly`.
4. **Stable Extension IDs**: Keep `id` values stable across releases to preserve user settings and
   overrides.
