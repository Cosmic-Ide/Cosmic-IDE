# Process execution and terminal

## Purpose

All JDK, Gradle, compiler, shell, and language-server processes must pass through
`LinuxProcessRunner`. It supplies the custom glibc loader, preload shims, virtual filesystem,
selected JDK, and tool paths. A direct `ProcessBuilder` launch is not equivalent.

Use:

- `ProcessExecutor` for captured output or stdio protocols;
- `PtyProcessExecutor` for interactive commands;
- `LinuxProcessRunner` directly only when a facade does not expose a required option.

Plugins use the `CommandExecutionService` registered under `IdeServices.COMMAND_EXECUTION`. Its
`CommandRequest` keeps executable and arguments separate, runs in the same toolchain environment,
merges output for finite user-facing operations, returns the exit code and a bounded output tail,
and streams chunks as they arrive. Plugin environment installation must instead be declared through
`CosmicPlugin.setupActions`; the marketplace opens the chosen `PluginSetupAction` through
`TerminalScreen` and the PTY path.

Editor `ProjectCommand` contributions use the same PTY bridge inside the resizable bottom tool
window. The terminal controller can accept an explicit argument list, allowing custom project shell
code to reach `bash -lc` without string re-tokenization. **Execution > Terminal** opens `bash -i` in
the project root; each command or terminal receives its own closable/rerunnable tab.

## Launch flow

`resolveExecutable` accepts an absolute path, a working-directory-relative path containing `/`, or
a command found in the toolchain PATH. `LinuxProcessRunner` then accepts only:

- a readable ELF, loaded by `libld_linux.so`;
- a shell script with a supported `sh`/`bash`-family shebang, passed to a relocated shell.

Android system executables cannot be loaded with glibc. The runner substitutes a same-named
relocated tool or rejects the initial launch.

The child environment is built from scratch:

1. canonical glibc environment;
2. computed `PATH` and PTY `TERM`;
3. caller overrides;
4. internal logical executable identity.

Overrides are intentionally last. Use them for a single tool, not as a second global environment
builder.

## Plain processes

Plain execution uses `ProcessBuilder` around the custom-loader command. Use it for LSP and JSON
protocols, builds with captured output, and other non-interactive work.

Protocol processes must keep stderr separate. Language servers and the Gradle provider reserve
stdout for framed messages and drain stderr independently.

`parseCommandLine` handles quotes and backslash escapes only. It does not implement pipes,
redirection, globbing, variables, or command substitution. Invoke `sh -c` explicitly only when shell
semantics are intended and the command is trusted.

## PTY processes

The JNI PTY layer uses `openpty` and `fork`, makes the child a session leader with a controlling
terminal, assigns its process group to the foreground, connects the slave to standard streams, and
executes the same wrapped glibc command used by the plain path.

Only the master FD and child PID return to Kotlin. Reads are blocking, retry `EINTR`, and treat PTY
`EIO` as EOF. Writes handle partial results. Closing a stream does not close the shared master FD.

### Signals

- `interrupt()` targets the current foreground process group and is correct for Ctrl+C.
- `terminate()` and `kill()` target the original child process group, with a process fallback.
- resizing calls `TIOCSWINSZ`, allowing the foreground job to receive normal `SIGWINCH` behavior.

`waitFor()` reaps the child once and caches the exit code. It is blocking and belongs on a
background thread. Closing the PTY does not terminate or reap the child.

Recommended shutdown order:

```text
SIGTERM -> optional grace period -> SIGKILL if needed -> waitFor -> close PTY
```

## Bash startup files

PTY Bash launches default to login and interactive mode. The runner creates a `.bash_profile`
bridge only when the user has not supplied `.bash_profile` or `.bash_login`; the bridge sources
`.profile` and `.bashrc`. Non-Bash and non-PTY processes do not receive shell startup behavior.

## Terminal UI bridge

`TerminalController` attaches the custom PTY to Termux's emulator/view without creating a Termux
`TerminalSession`. It translates Android/Termux key input to PTY bytes, appends output to the
emulator, resizes the PTY from view geometry, and owns clipboard/title behavior.

Pine hooks adapt Termux view methods that otherwise assume a `TerminalSession`. These hooks are
sensitive to Termux implementation changes and require interactive input/selection tests after a
dependency update.

## Choosing the path

| Need                                                 | Use                                  |
|------------------------------------------------------|--------------------------------------|
| LSP or JSON over stdio                               | Plain process, stderr separate       |
| Finite captured build output                         | Plain process                        |
| Shell, installer prompt, REPL, pager, or job control | PTY                                  |
| Shell operators                                      | Explicit trusted `sh -c`             |
| Literal arguments                                    | Direct executable plus argument list |

## Important checks

- A child process failing after its parent succeeds points to exec wrapping or lost preload state.
- Garbled protocol JSON/LSP usually means stderr or launcher logs reached stdout.
- Ctrl+C must target the foreground group, not only the original shell.
- Every PTY owner must handle signal, reap, and FD-close lifecycle separately.
