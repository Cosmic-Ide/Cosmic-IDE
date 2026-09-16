# Cosmic IDE

<div align="center">

### Complete Software Development on Android

Write, build, and run real applications directly on your Android phone, tablet, or desktop setup.

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
        <img src="docs/images/scala-gradle-build.jpeg" width="220" alt="A successful build in the Cosmic IDE terminal">
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
        <img src="docs/images/java-code-completion.jpeg" width="220" alt="Java code completion in Cosmic IDE while an application is running">
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
        <img src="docs/images/project-explorer.jpeg" width="220" alt="A repository open in the Cosmic IDE project explorer">
      </a>
      <br>
      <sub><strong>Touch-friendly project explorer</strong></sub>
    </td>
  </tr>
</table>

## What You Can Do

Cosmic IDE turns your Android device into a complete development environment. It combines an
intelligent editor, real compilers and build tools, full Git integration, and an interactive
terminal.

### Code Intelligence

- **Autocomplete & Diagnostics**: Real-time error checking, type inspection, and signature help via
  Language Server Protocol (LSP) integrations.
- **Modern Code Editor**: Multi-tab editing, sticky scroll, Code Minimap, inline Find & Replace,
  bracket auto-completion, and custom font support.
- **Themes & Styling**: Built-in syntax themes (One Dark, Nord, Dracula, Monokai, Solarized) with
  support for custom TextMate themes and grammars.
- **Side-by-Side Previews**: Live visual previews for Markdown, HTML, and images alongside your
  source code.

### Build & Run

- **Plugin-Driven Build System**: Automatic project sync, task runner, and streaming build logs in
  bottom tool-windows for installed build plugins and project templates.
- **Custom Project Workflows**: Support for Gradle, Cargo, CMake, Maven, npm, and custom shell-based
  project creation, sync, build, and run commands.
- **JDK Management**: Download and switch between Java Development Kits directly in settings via
  Foojay.
- **Background Task Protection**: Runs builds, long-running processes, and background servers safely
  without OS process termination.

### Full Version Control & Terminal

- **Dedicated Git Interface**: Manage changes, stage files, review commit history, switch branches,
  and push/pull from remotes.
- **Built-in Linux Terminal**: Interactive terminal shell with job control and `pacman` package
  manager to install compilers and tools.

---

## Supported Languages & Build Systems

Cosmic IDE supports languages and build tools through language servers and installable marketplace
plugins:

| Category                    | Languages & Tools Supported                               |
|-----------------------------|-----------------------------------------------------------|
| **JVM Stack**               | Java (JDT LS), Kotlin (Kotlin LS), Scala (Metals)         |
| **System & Native**         | Rust (`rust-analyzer`), C/C++ (`clangd`), Go, Gleam       |
| **Scripting & Web**         | Python (`pyright`), Lua (`LuaLS`), HTML, Markdown         |
| **Build Tools**             | Gradle, Maven, Cargo, CMake, npm, custom project types    |
| **Custom Language Support** | Add any stdio Language Server with custom starter scripts |

---

## Quick Start

1. **Download**: Get
   the [latest nightly APK](https://nightly.link/Cosmic-Ide/Cosmic-IDE/workflows/android/main/app-arm64-v8a.zip).
2. **Setup**: Follow the quick initial setup to install a JDK and language toolchain.
3. **Start Coding**: Create a new project, clone a Git repository, or open local files.

For detailed instructions and tips, check the [User Guide](docs/user-guide.md).

---

## Documentation

- **[User Guide](docs/user-guide.md)**: Workspace overview, Git workflows, and build systems.
- **[Settings Reference](docs/settings-reference.md)**: Personalizing themes, fonts, and controls.
- **[Plugin Development Guide](docs/plugin-development-guide.md)**: Creating and publishing plugins.
- **[Codebase Architecture](docs/codebase-architecture.md)**: Technical details for contributors
  (glibc layer, process execution, runtime shims).
- **[Development Guide](docs/development-guide.md)**: Building Cosmic IDE from source.

---

## Support the Project

Cosmic IDE is free and open source under GPLv3. If you find it useful, consider supporting its
development.

<p align="center">
  <a href="https://opencollective.com/invokevirtual">
    <img src="https://img.shields.io/badge/Support_Cosmic_IDE-Open_Collective-7FADF2?style=for-the-badge&logo=opencollective&logoColor=white" alt="Support Cosmic IDE on Open Collective">
  </a>
</p>

## License

Cosmic IDE is licensed under the [GNU General Public License v3.0](LICENSE).
