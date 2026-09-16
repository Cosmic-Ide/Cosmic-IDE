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

A Cosmic IDE plugin is packaged as a `.zip` archive containing a `manifest.json` descriptor and an
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

## 2. Plugin Manifest (`manifest.json`)

Place `manifest.json` at the root of your plugin directory or ZIP package:

```json
{
  "id": "com.example.myplugin",
  "name": "Sample Extension",
  "version": "1.0.0",
  "description": "Adds custom language, UI, and build support to Cosmic IDE.",
  "author": "Developer Name",
  "entryClass": "com.example.myplugin.MyPlugin",
  "classPath": ["plugin.apk"],
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

- `id`: Permanent, reverse-domain identifier (e.g. `com.example.myplugin`).
- `name`: User-facing name shown in the Plugin Marketplace.
- `version`: Semantic version string.
- `entryClass`: Fully-qualified class name implementing `CosmicPlugin`.
- `classPath`: Array containing the compiled DEX/APK/JAR artifact filenames.
- `capabilities`: Array of capability strings declared by your plugin.
- `enabledByDefault`: Whether the plugin activates upon initial installation.

---

## 3. Implementing `CosmicPlugin`

The plugin entry class must implement `CosmicPlugin` from `org.cosmicide.plugin.api`:

```kotlin
package com.example.myplugin

import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.PluginDescriptor

class MyPlugin : CosmicPlugin {

    override fun activate(context: PluginContext) {
        val owner = context.descriptor.id

        // 1. Access core IDE services
        val commands = context.services.require(IdeServices.COMMAND_EXECUTION)

        // 2. Register extensions
        val lspRegistration = context.extensions.register(
            point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
            extension = MyLspProvider(),
            ownerPluginId = owner,
            priority = 300
        )
        context.registerDisposable(lspRegistration)
    }

    override fun deactivate(context: PluginContext) {
        // Cleanup performed automatically via registered Disposables
    }
}
```

---

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
   Create a ZIP archive containing `manifest.json` and your compiled `.apk`:
   ```text
   my-plugin-1.0.0.zip
   ├── manifest.json
   └── plugin.apk
   ```

---

## 6. Testing Plugins Locally

To test your plugin on a device or emulator without setting up a remote repository:

1. Locate Cosmic IDE's internal plugin directory on the target device:
   `/data/data/org.cosmicide/files/plugins/<your-plugin-id>/`
2. Extract `manifest.json` and `plugin.apk` into that directory.
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
