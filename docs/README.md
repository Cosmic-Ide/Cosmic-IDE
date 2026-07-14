# Engineering documentation

These documents describe boundaries that are difficult to recover from a single class. Routine UI
behavior, dependency versions, and self-explanatory build settings belong in code or build files.

| Document                                                                      | Scope                                                           |
|-------------------------------------------------------------------------------|-----------------------------------------------------------------|
| [Codebase architecture](codebase-architecture.md)                             | Modules, startup, storage, and ownership                        |
| [glibc runtime and compatibility shims](glibc-runtime-and-shims.md)           | Why each shim exists and how Linux binaries run on Android      |
| [Process execution and terminal](process-execution-and-terminal.md)           | Process facade, environment, PTY, signals, and terminal bridge  |
| [Environment and toolchain bootstrap](environment-and-toolchain-bootstrap.md) | Runtime deployment, JDKs, language servers, and SDK setup       |
| [Gradle tooling bridge](gradle-tooling-bridge.md)                             | Out-of-process Tooling API protocol and model snapshots         |
| [Editor and language services](editor-and-language-services.md)               | Language routing, LSP ownership, custom servers, and formatting |
| [Plugin architecture](plugin-architecture.md)                                 | Extension contracts, loading, enablement, and lifecycle         |

Keep these pages focused on purpose, invariants, ownership, and non-obvious failure modes. Update a
document when its boundary changes; do not expand it merely to mirror source structure.
