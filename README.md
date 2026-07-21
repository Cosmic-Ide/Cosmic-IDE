# Cosmic IDE

[![Android CI](https://github.com/Cosmic-Ide/Cosmic-IDE/actions/workflows/android.yml/badge.svg)](https://github.com/Cosmic-Ide/Cosmic-IDE/actions/workflows/android.yml)
[![License](https://img.shields.io/github/license/Cosmic-Ide/Cosmic-IDE)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/Cosmic-Ide/Cosmic-IDE/total)](https://github.com/Cosmic-Ide/Cosmic-IDE/releases)
[![F-Droid](https://img.shields.io/f-droid/v/org.cosmicide)](https://f-droid.org/packages/org.cosmicide)
[![Discord](https://img.shields.io/discord/867985135931383809)](https://discord.gg/8Gu6YCq2eS)

Cosmic IDE is an extensible development environment that runs directly on Android. It combines a
modern code editor, Language Server Protocol support, an interactive terminal, a Linux/aarch64
toolchain environment, and Gradle integration without requiring a desktop computer.

> [!IMPORTANT]
> The current `main` branch is substantially ahead of the latest public release. This README
> describes development builds; release and F-Droid builds may not yet contain these features.

## Language support

Cosmic IDE is not limited to JVM languages. Java, Kotlin, and Scala are the first-party experience
provided by default, with integrations for JDT LS, Kotlin Language Server, and Metals. They are the
starting toolset, not a hard-coded language boundary.

For other languages, users can install a Linux/aarch64 toolchain or language server in Cosmic's
glibc environment and register any standard-input/output language server for a file extension.
The same environment is available to the terminal, custom commands, compilers, build tools, and
Gradle processes.

This has been tested with Rust, C++, and XML tooling. In practice, other languages can work when
their compiler or language server is available for Linux/aarch64 and can operate within Android's
application sandbox. Editor capabilities depend on what the configured language server provides.

Gradle is fully integrated for projects that use it, including model and task discovery, builds,
tests, progress, cancellation, and interactive input. Projects using Cargo, CMake, or another build
system can run those tools through the terminal or project commands instead.

## Highlights

- Custom LSP configurations with a file extension and shell startup command
- Per-LSP TextMate grammars from HTTPS links, Android documents, or local files
- First-party Java, Kotlin, and Scala language-server integrations
- App-private glibc environment for Linux/aarch64 compilers and developer tools
- Interactive PTY terminal with normal shell, signal, and process-group behavior
- Gradle Tooling API bridge for models, tasks, builds, and tests
- Installable and selectable glibc-compatible JDK toolchains
- Optional Android SDK and ARM build-tools setup
- LSP completion, diagnostics, navigation, hover information, and other advertised capabilities
- Sora Editor with TextMate grammars, themes, and configurable editing behavior
- Plugin APIs for editor languages, language servers, formatters, project creation, and project
  actions
- Bundled Git plugin with clone, status, fetch, pull, push, stage, commit, branch, and checkout
- Bundled custom-project plugin for user-defined templates and sync/build/run/utility commands
- Project creation, import, backup, file management, and bottom-panel PTY command sessions
- Material You interface with light and dark themes

## How custom language support works

1. Install the compiler, runtime, and language server from the integrated terminal.
2. Open **Settings → Extensions → Custom language servers**.
3. Associate a file extension with the shell command that starts the server over stdio.
4. Optionally attach a TextMate grammar through a direct link or the Android file picker.
5. Use Gradle integration, a project command, or the terminal for build and run workflows.

Only one custom server can be active for a file extension. Enabling another entry for the same
extension disables the previous entry. HTTPS grammars are cached after they parse successfully and
refreshed after seven days; a stale valid copy is used if refresh fails.

Custom startup code executes with the app's permissions. Review commands from projects or third
parties before adding them.

## Installation

Current development APKs are produced by the
[Android CI workflow](https://github.com/Cosmic-Ide/Cosmic-IDE/actions/workflows/android.yml). The
[universal development artifact](https://nightly.link/Cosmic-Ide/Cosmic-IDE/workflows/android/main/app-universal.zip)
tracks successful builds from `main`.

Stable builds are available
from [GitHub Releases](https://github.com/Cosmic-Ide/Cosmic-IDE/releases)
and [F-Droid](https://f-droid.org/packages/org.cosmicide). Check the version before installing if
you need features documented for the development branch.

The current development environment targets Android 13 or later and arm64 devices. Initial setup
downloads the selected JDK and any requested language servers or Android SDK components.

## Documentation

The complete index is in [docs/README.md](docs/README.md). Main entry points:

- [User guide](docs/user-guide.md) — setup, projects, editor, builds, terminal, and language support
- [Settings reference](docs/settings-reference.md) — every current settings category and known gaps
- [Building and contributing](docs/development-guide.md) — local builds, modules, tests, and
  generated assets
- [Data, permissions, and trust](docs/data-permissions-and-security.md) — storage, network access,
  and executable configuration
- [Codebase architecture](docs/codebase-architecture.md) — module and process boundaries
- [Editor and language services](docs/editor-and-language-services.md) — LSP, TextMate, routing, and
  formatting
- [Plugin architecture](docs/plugin-architecture.md) — extension contracts and lifecycle

## Contributing

Issues and pull requests are welcome. For substantial changes, open an issue first so the design
and Android/runtime constraints can be discussed before implementation.

When changing the glibc runtime, native shims, or Gradle tooling provider, remember that their
generated artifacts are packaged separately from their sources. The linked engineering
documentation identifies the required rebuild step for each artifact.

## Community and support

- [Discord](https://discord.gg/8Gu6YCq2eS)
- [Telegram](https://t.me/cosmicide)
- [GitHub Issues](https://github.com/Cosmic-Ide/Cosmic-IDE/issues)
- [Email](mailto:purwarpranav80@gmail.com)

## Donate

If Cosmic IDE is useful to you, you can support its development through
[Patreon](https://patreon.com/cosmicide).

## Acknowledgments

Cosmic IDE builds on projects including [Sora Editor](https://github.com/Rosemoe/sora-editor),
[Termux](https://github.com/termux/termux-app), the
[Gradle Tooling API](https://docs.gradle.org/current/userguide/tooling_api.html),
[Eclipse JDT LS](https://github.com/eclipse-jdtls/eclipse.jdt.ls),
[Kotlin Language Server](https://github.com/Kotlin/kotlin-lsp), and
[Metals](https://scalameta.org/metals/). Thanks to
[Iyxan23](https://github.com/Iyxan23) for the application icon and to every contributor who has
helped develop and test the project.

[![Contributors](https://contrib.rocks/image?repo=Cosmic-Ide/Cosmic-IDE)](https://github.com/Cosmic-Ide/Cosmic-IDE/graphs/contributors)

## License

Cosmic IDE is licensed under the [GNU General Public License v3.0](LICENSE).
