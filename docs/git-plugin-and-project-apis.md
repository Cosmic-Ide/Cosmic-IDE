# Git plugin and project APIs

## What ships

Cosmic bundles `GitPlugin` and activates it through the same `PluginContext`, extension registry,
service registry, settings, and disposal path used by installed plugins. The plugin contributes:

- **Clone Git repository** on the Projects floating toolbar;
- **Initialize Git repository** for directories without `.git`;
- status, fetch, pull, push, stage-all, commit, branch-list, and checkout actions for repositories;
- streamed command output and progress parsed from Git's percentage messages.

Git itself is not embedded in the APK as a separate implementation. Commands resolve `git` from
Cosmic's glibc/Arch toolchain `PATH`. Installation runs:

```sh
pacman -S --needed git
```

The command intentionally uses a PTY and does not force `--noconfirm`, so pacman can show package,
download, verification, installation progress, and confirmation normally. The resulting files live
under the app's private runtime root. Uninstalling the Android app removes that environment.

## UI flow

```text
ProjectCreationProvider / ProjectActionProvider
  -> declarative project-specific fields
  -> Home renders a generic operation dialog
  -> provider calls CommandExecutionService
  -> LinuxProcessRunner starts the glibc executable
  -> output chunks become OperationUpdate values
  -> dialog shows logs, progress, completion, failure, or cancellation
Editor Toolbar Menu / Home Project Card / Extensions Settings
  -> navigates to PluginScreen("org.cosmicide.git", "git", args)
  -> GitScreen renders with tabs (Changes, Branches, History, Settings)
  -> user triggers Git actions (stage, commit, branch checkout, pull/push, config update)
  -> GitService calls CommandExecutionService
  -> LinuxProcessRunner starts glibc git executable
  -> output and status are parsed into typed models (GitStatus, GitBranch, GitCommit, GitRemote)
  -> UI updates dynamically with snackbar feedback and progress indicators
```

Clone accepts a remote URL, optional directory, optional branch/tag, and shallow-clone switch. It
uses argument lists rather than shell interpolation, rejects option-like URLs and unsafe refs,
requires the destination to be a new direct child of the projects directory, and removes a partial
destination after a failed clone. Existing directories are never overwritten or removed.

Captured Git network commands set `GIT_TERMINAL_PROMPT=0`. This avoids an invisible password prompt
behind a non-PTY dialog. Users should configure an SSH key, credential helper, or authenticated
remote from the terminal before retrying a private remote operation. remote from the terminal or Git
settings before retrying a private remote operation.

## Public contracts

All project-facing contracts live in `:ide-api` under `org.cosmicide.project`:
Project-facing and UI extension contracts live in `:ide-api`:

| Contract                                       | Role                                                                        |
|------------------------------------------------|-----------------------------------------------------------------------------|
| `ProjectExtensionPoints.CREATION_PROVIDER`     | Adds a create/import choice to Projects                                     |
| `ProjectCreationProvider`                      | Describes a form and creates a project                                      |
| `ProjectExtensionPoints.ACTION_PROVIDER`       | Adds applicable operations to project menus                                 |
| `ProjectActionProvider`                        | Describes and executes project-scoped actions                               |
| `ProjectExtensionPoints.COMMAND_PROVIDER`      | Adds build/run/utility commands to the editor                               |
| `ProjectCommandProvider` / `ProjectCommand`    | Selects commands for a project and classifies their role                    |
| `PluginFormField`                              | Text, password, boolean, or choice input with static/conditional visibility |
| `OperationReporter` / `OperationUpdate`        | Streams status, output, warning/error, and optional 0–1 progress            |
| `PluginSetupAction`                            | Declares plugin-level setup through `CosmicPlugin.setupActions`             |
| `UiExtensionPoints.PLUGIN_SCREEN`              | Adds full-screen UI for plugins (`PluginScreenProvider`)                    |
| `UiExtensionPoints.SETTINGS_UI`                | Adds settings UI for plugins (`SettingsUiProvider`)                         |
| `EditorExtensionPoints.EDITOR_ACTION_PROVIDER` | Adds actions to the editor toolbar options menu (`EditorActionProvider`)    |
| `ProjectExtensionPoints.CREATION_PROVIDER`     | Adds a create/import choice to Projects (`ProjectCreationProvider`)         |
| `ProjectExtensionPoints.ACTION_PROVIDER`       | Adds applicable operations to project menus (`ProjectActionProvider`)       |
| `ProjectExtensionPoints.COMMAND_PROVIDER`      | Adds build/run/utility commands to the editor (`ProjectCommandProvider`)    |
| `IdeServices.COMMAND_EXECUTION`                | Looks up the host process service from `PluginContext.services`             |
| `CommandExecutionService`                      | Runs finite commands in Cosmic's toolchain environment                      |

Both provider types implement `ConfigurableExtension`. Their stable ids therefore appear under
Settings > Extensions and can be enabled or disabled independently without unloading their owner
plugin. Home evaluates enabled providers when composed; reopen Projects after changing a switch.
Both screen and creation providers implement `ConfigurableExtension`. Their stable ids appear under
Settings > Extensions and can be enabled or disabled independently without unloading their owner
plugin.

## Minimal creator

```kotlin
class MyCreator(
    private val commands: CommandExecutionService
) : ProjectCreationProvider {
    override val id = "com.example.project.creator"
    override val displayName = "Create example project"
    override val fields = listOf(
        PluginFormField("name", "Project name", required = true)
    )

    override suspend fun create(
        request: ProjectCreationRequest,
        reporter: OperationReporter
    ): ProjectCreationResult {
        val root = request.projectsDirectory.resolve(request.values.getValue("name")).canonicalFile
        require(root.parentFile == request.projectsDirectory.canonicalFile)
        require(!root.exists())
        check(root.mkdir())

        reporter.report(OperationUpdate("Creating files…"))
        // Create the project or use commands.execute(CommandRequest(...)).
        return ProjectCreationResult(Project(root, Language.Kotlin))
    }
}
```

Register the provider during `CosmicPlugin.activate` and register the returned `Disposable` with the
context. Project actions use the same pattern with `ProjectExtensionPoints.ACTION_PROVIDER`.

## Failure and lifecycle rules

- Throw a descriptive exception for invalid input, unavailable tools, or non-zero command results;
  the app presents its message in the dialog.
- Report raw output with `OperationMessageKind.OUTPUT`; include normalized progress when available.
- Keep `actions(project)` free of network and process work.
- Throw a descriptive exception for invalid input, unavailable tools, or non-zero command results.
- Keep `actions(project, file)` free of network and process work; execute operations in coroutines
  on background dispatchers.
- Do not launch an interactive program through `CommandExecutionService`; declare environment
  setup on the owning `CosmicPlugin` through `PluginSetupAction`.
- Do not retain screen, activity, or Compose references. Dispose plugin-owned resources through
  `PluginContext`.
- Do not retain screen, activity, or Compose references across disposal boundaries.
- Treat command execution and terminal actions as trusted-code capabilities. Installed plugins run
  in Cosmic's application process and have the app's filesystem access.

The bundled implementation is in `app/src/main/kotlin/org/cosmicide/plugin/git`; generic rendering
is in `app/src/main/kotlin/org/cosmicide/ui/plugin`; process adaptation is in The bundled
implementation is in `app/src/main/kotlin/org/cosmicide/plugin/git`; UI rendering is in
`app/src/main/kotlin/org/cosmicide/plugin/git/ui`; process adaptation is in
`AndroidCommandExecutionService`.
