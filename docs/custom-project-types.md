# Custom project types

## Purpose

The bundled **Custom project types** plugin lets users add project workflows without compiling a
plugin. It is activated through the normal plugin runtime and contributes both
`ProjectCreationProvider` and `ProjectCommandProvider` implementations.

Configure types under Settings > Extensions > Custom project types. Each entry supports:

| Field               | Behavior                                                      |
|---------------------|---------------------------------------------------------------|
| Name                | User-facing type and command name                             |
| Marker files        | Relative paths; any existing path matches an imported project |
| Creation code       | Optional Bash code run inside a new project directory         |
| Sync code           | Adds a dependency/setup sync command to matching editors      |
| Build code          | Adds a build command to matching editors                      |
| Run code            | Adds a run command and becomes the toolbar Run action         |
| Additional commands | One `Label :: shell code` entry per line                      |

An entry can be enabled, disabled, edited, or deleted. Deleting a type does not delete projects that
were created from it.

## Creation

When at least one type is enabled, Projects shows **Create custom project** under the folder action.
The user chooses a registered type and a new direct-child project name. Cosmic creates the
directory, then invokes creation code through `bash -lc` using a direct argument list.

Creation receives:

| Variable              | Value                          |
|-----------------------|--------------------------------|
| `COSMIC_PROJECT_ROOT` | Absolute new project directory |
| `COSMIC_PROJECT_NAME` | Directory name                 |
| `COSMIC_PROJECT_TYPE` | Configured type name           |

After success, Cosmic writes `.cosmic/project-type` containing the stable configuration id. A
non-zero exit or cancellation removes only the newly created partial directory. Existing targets
are rejected before execution.

## Matching existing projects

A project matches when its `.cosmic/project-type` id equals the configuration id or any marker path
exists. Markers must be relative and cannot traverse outside the project. Useful examples include:

```text
Cargo.toml
package.json
CMakeLists.txt
pubspec.yaml
```

When multiple types match, their commands are combined in provider order. The first matching run
command is used by the toolbar Run button.

## Editor terminals

Project commands appear in the editor's **Project Commands** submenu. Providers can group them into
further nested submenus with `ProjectCommand.children`. Each leaf launch creates a resizable bottom
PTY tab with status, rerun, close, normal terminal colors, interactive input, and Ctrl+C support.
**Execution > Terminal** creates an interactive `bash -i` tab in the same panel.
The PTY runner supplies login startup as well, so `.bash_profile` bridges to `.profile` and
`.bashrc`.

When `gradlew` exists, the fixed **Sync** tab remains owned by Gradle model synchronization. When it
does not exist and a matching project type supplies Sync code, the fixed tab instead runs that code
in a PTY and provides status and rerun controls. Cosmic does not initialize Gradle tooling for that
wrapperless project. Without either a wrapper or project Sync code, the tab reports that no sync
command is configured.

Gradle tasks continue to use the same bottom panel for wrapper projects. When no project plugin
supplies a run command, the toolbar Run action falls back to the Gradle `run` task.

## Security

Creation and project commands are trusted Bash code. They have Cosmic's application identity,
project access, glibc toolchain environment, and network access allowed to the app. Do not paste
commands from an untrusted source. Marker values are data, but command fields are executable.

Plugin authors can provide the same editor behavior directly through
`ProjectExtensionPoints.COMMAND_PROVIDER`; see [Plugin architecture](plugin-architecture.md).
