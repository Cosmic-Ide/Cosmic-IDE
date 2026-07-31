# Cosmic IDE

<div align="center">

### Desktop-class development on Android

A modular development environment for Android. Edit, build, and run projects using genuine Linux
toolchains, an integrated terminal, and a plugin-based architecture.

[![Android CI](https://github.com/Cosmic-Ide/Cosmic-IDE/actions/workflows/android.yml/badge.svg)](https://github.com/Cosmic-Ide/Cosmic-IDE/actions/workflows/android.yml)
[![License](https://img.shields.io/github/license/Cosmic-Ide/Cosmic-IDE)](LICENSE)
[![Discord](https://img.shields.io/discord/867985135931383809)](https://discord.gg/46wCMRVAre)

[Download Nightly](https://nightly.link/Cosmic-Ide/Cosmic-IDE/workflows/android/main/app-arm64-v8a.zip) · [User Guide](docs/user-guide.md) · [Discord](https://discord.gg/46wCMRVAre)

</div>

<table>
  <tr>
    <td align="center" width="50%">
      <a href="docs/images/scala-code-completion.jpeg">
        <img src="docs/images/scala-code-completion.jpeg" width="220" alt="Scala code completion in Cosmic IDE">
      </a>
      <br>
      <sub><strong>Scala completion powered by Metals</strong></sub>
    </td>
    <td align="center" width="50%">
      <a href="docs/images/scala-gradle-build.jpeg">
        <img src="docs/images/scala-gradle-build.jpeg" width="220" alt="A successful Scala Gradle build in the Cosmic IDE terminal">
      </a>
      <br>
      <sub><strong>Integrated build output and task execution</strong></sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <a href="docs/images/java-diagnostics-quick-fixes.jpeg">
        <img src="docs/images/java-diagnostics-quick-fixes.jpeg" width="220" alt="Java error diagnostics and quick fixes in Cosmic IDE">
      </a>
      <br>
      <sub><strong>Inline diagnostics and quick fixes</strong></sub>
    </td>
    <td align="center" width="50%">
      <a href="docs/images/java-code-completion.jpeg">
        <img src="docs/images/java-code-completion.jpeg" width="220" alt="Java code completion in Cosmic IDE while a Gradle application is running">
      </a>
      <br>
      <sub><strong>Java completion backed by JDT LS</strong></sub>
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <a href="docs/images/java-symbol-information.jpeg">
        <img src="docs/images/java-symbol-information.jpeg" width="220" alt="Java symbol information displayed over the Cosmic IDE editor">
      </a>
      <br>
      <sub><strong>Types and documentation in the editor</strong></sub>
    </td>
    <td align="center" width="50%">
      <a href="docs/images/project-explorer.jpeg">
        <img src="docs/images/project-explorer.jpeg" width="220" alt="A Java repository open in the Cosmic IDE project explorer">
      </a>
      <br>
      <sub><strong>Touch-friendly project explorer</strong></sub>
    </td>
  </tr>
</table>

## Core architecture

Cosmic IDE provides a complete Linux-based development environment inside an Android app. It isn't
just a text editor; it's a runtime that hosts genuine compilers, build systems, and language
servers.

- **Intelligent Workspace**: Multi-tab editor with LSP support, inline diagnostics, and Material 3
  design.
- **Linux Environment**: Run compilers and shells within an app-private environment compatible with
  Arch Linux ARM.
- **Integrated Terminal**: A full-featured PTY terminal. Install and run tools from the official
  Arch Linux ARM repositories using `pacman`.
- **Responsive UI**: Optimized for touch, keyboard, and Android desktop environments like Samsung
  DeX.

---

## Language & Tooling Ecosystem

The IDE is built on a modular plugin system, allowing support for various languages and build tools
to be added or updated independently.

|                  |                                                                     |
|------------------|---------------------------------------------------------------------|
| **JVM Stack**    | Java, Kotlin, Scala, Gradle, Maven                                  |
| **System & Web** | Rust (`rust-analyzer`), C/C++ (`clangd`), Go, Gleam                 |
| **Scripting**    | Python, Lua (`LuaLS`)                                               |
| **Workflows**    | Git integration, custom project templates, and shell-based commands |

*Manage and install language support directly from the in-app **Plugin Marketplace**.*

---

## Features

- **Editor**: Syntax highlighting, auto-completion, navigation, and customizable themes via
  TextMate.
- **Build System**: Execute tasks (Gradle, Cargo, Maven, etc.) with real-time output in integrated
  tool windows.
- **Package Management**: Use `pacman` to install CLI tools, compilers, and utilities from Arch
  Linux ARM.
- **Git Workflows**: Built-in support for cloning, branching, committing, and syncing repositories.
- **Local Control**: Your projects remain in app-private storage or shared storage via Android's
  Document Picker.
- **Extensible**: Add custom language servers, TextMate grammars, and project templates without
  modifying the app's source.

---

## Quick Start

1. **Install**: Download
   the [latest nightly APK](https://nightly.link/Cosmic-Ide/Cosmic-IDE/workflows/android/main/app-arm64-v8a.zip).
2. **Bootstrap**: Follow the guided setup to install a JDK and the base Linux environment.
3. **Coding**: Create a new project or clone a repository.

Detailed documentation is available in the [User Guide](docs/user-guide.md).

---

## Documentation

- **[User Guide](docs/user-guide.md)**: Projects, builds, Git, and terminal workflows.
- **[Settings Reference](docs/settings-reference.md)**: Personalizing the workspace.
- **[Plugin Architecture](docs/plugin-architecture.md)**: Building and contributing extensions.
- **[Development Guide](docs/development-guide.md)**: Building Cosmic IDE from source.

## Support the project

Cosmic IDE is free and open source. If you find it useful, consider supporting its development.

<p align="center">
  <a href="https://opencollective.com/invokevirtual">
    <img src="https://img.shields.io/badge/Support_Cosmic_IDE-Open_Collective-7FADF2?style=for-the-badge&logo=opencollective&logoColor=white" alt="Support Cosmic IDE on Open Collective">
  </a>
</p>

## License

Cosmic IDE is licensed under the [GNU General Public License v3.0](LICENSE).
