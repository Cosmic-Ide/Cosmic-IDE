# Cosmic IDE documentation

This directory documents using Cosmic IDE and maintaining its implementation. The current `main`
branch can be ahead of published APKs. Confirm the installed version when a screen or setting is
missing.

## Using the app

| Document                                                         | Scope                                                                   |
|------------------------------------------------------------------|-------------------------------------------------------------------------|
| [User guide](user-guide.md)                                      | First run, projects, editor, Gradle, terminal, and language support     |
| [Settings reference](settings-reference.md)                      | Current settings categories, controls, effects, and unfinished entries  |
| [Data, permissions, and trust](data-permissions-and-security.md) | Storage locations, Android permissions, downloads, scripts, and privacy |

## Developing the app

| Document                                                                      | Scope                                                                                        |
|-------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| [Building and contributing](development-guide.md)                             | Checkout, toolchain, variants, builds, tests, and generated assets                           |
| [Codebase architecture](codebase-architecture.md)                             | Modules, startup, navigation, storage, and ownership                                         |
| [App module refactoring](app-module-refactoring.md)                           | Target boundaries, dependency rules, migration stages, and current progress                  |
| [Application UI and project lifecycle](app-ui-and-project-lifecycle.md)       | Navigation, projects, editor state, Gradle UI, settings, and document surfaces               |
| [glibc runtime and compatibility shims](glibc-runtime-and-shims.md)           | Why each shim exists and how Linux binaries run on Android                                   |
| [Process execution and terminal](process-execution-and-terminal.md)           | Process facade, environment, PTY, signals, and terminal bridge                               |
| [Environment and toolchain bootstrap](environment-and-toolchain-bootstrap.md) | Runtime deployment, JDKs, language servers, and SDK setup                                    |
| [Editor and language services](editor-and-language-services.md)               | Language routing, LSP, TextMate grammars, caching, and formatting                            |
| [Plugin architecture](plugin-architecture.md)                                 | Extension contracts, loading, enablement, and lifecycle                                      |
| [Plugin development guide](plugin-development-guide.md)                       | Step-by-step guide for creating, building, testing, and publishing plugins                   |
| [Git plugin and project APIs](git-plugin-and-project-apis.md)                 | Git workflows, project contributions, forms, progress, terminal setup, and command execution |
| [Custom project types](custom-project-types.md)                               | User-defined project templates, matching, commands, and editor terminals                     |

## Coverage map

The user guide covers the visible product flow from environment initialization through project
creation, import, editing, builds, and terminal use. The settings reference covers every category
listed by `SettingsScreen`, including those with unfinished UI. The engineering pages cover every
Gradle module and non-trivial process boundary.

Small helper classes and routine Compose layouts are intentionally documented through their owning
feature rather than mirrored class by class. Update the relevant page when modifying a user
workflow, storage contract, extension API, generated artifact, or process boundary.