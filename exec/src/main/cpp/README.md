# PTY (Pseudo-Terminal) Implementation for Cosmic IDE

This directory contains the implementation for pseudo-terminal support in Cosmic IDE, enabling
interactive terminal features for proper terminal emulation.

## Components

### Kotlin Files

#### `PtyTerminal.kt`

Core PTY management class providing:

- PTY allocation and child process spawning through the native layer
- Terminal window size configuration
- I/O operations through file descriptors
- Atomic PTY master FD lifecycle management

**Key Methods:**

- `allocateAndSpawn(...)` - Allocates a PTY and spawns the child process attached to it
- `setWindowSize(rows, columns)` - Sets terminal dimensions
- `getMasterInputStream()/getMasterOutputStream()` - I/O access
- `close()` - Closes PTY resources

#### `PtyProcess.kt`

Process wrapper with PTY capabilities:

- Wraps `PtyTerminal` with process lifecycle helpers
- Terminal control methods
- Signal handling
- Cached `waitFor()` result so the native child is reaped at most once

**Key Methods:**

- `setTerminalSize(rows, columns)` - Resize terminal
- `sendSignal(signal)` - Sends a signal and returns whether native delivery succeeded
- `terminate()` / `kill()` - Sends SIGTERM/SIGKILL and returns whether native delivery succeeded
- `waitFor()` - Blocks until exit on first call, then returns the cached exit code
- `close()` - Closes the PTY master FD

### Native Code

#### `pty_native.h`

JNI function declarations for:

- PTY allocation and process spawning (`openpty`, `fork`, `execve`)
- Terminal control (`ioctl` with `TIOCSWINSZ`)
- File descriptor I/O
- Process group signal delivery and child reaping

#### `pty_native.c`

JNI implementation providing:

- Android/Linux PTY operations
- Conversion of Java strings into owned C strings before `fork()`
- Local JNI reference cleanup during argument/environment conversion
- Sane initial terminal modes, including `ISIG` and `VINTR`/`VEOF` control characters
- Foreground process group setup for terminal-generated signals such as Ctrl+C and Ctrl+Z
- Error handling with proper errno reporting
- Blocking master FD I/O with `EINTR` retry and PTY EOF handling
- Foreground process group signal lookup via `TIOCGPGRP`
- Foreground-control fallback that signals descendants discovered through
  `/proc/<pid>/task/<pid>/children`
- Explicit signal delivery to the child process group with single-process fallback

### Build Configuration

#### `CMakeLists.txt`

CMake build configuration for:

- Building `libpty_native.so`
- Native include setup
- Optional util library linking when available

## Integration with LinuxProcessRunner

The `LinuxProcessRunner` has been enhanced with:

### New Configuration Options

```kotlin
data class Configuration(
    // ... existing fields ...
    val usePty: Boolean = false,
    val terminalRows: Int = 24,
    val terminalColumns: Int = 80
)
```

### New Methods

```kotlin
fun startWithPty(context: Context, config: Configuration): PtyProcess
```

Creates a process with full PTY support for interactive terminal I/O.

## Usage Example

```kotlin
// Create configuration with PTY enabled
val config = LinuxProcessRunner.Configuration(
    binary = File("/path/to/bash"),
    arguments = emptyList(),
    workingDir = File("/home/user"),
    usePty = true,
    terminalRows = 30,
    terminalColumns = 120
)

// Start process with PTY
val ptyProcess = LinuxProcessRunner.startWithPty(context, config)

// Configure terminal
ptyProcess.setTerminalSize(25, 100)

// I/O operations
val input = ptyProcess.getInputStream()
val output = ptyProcess.getOutputStream()

// Interactive control
output.write("echo Hello\n".toByteArray())
val response = input.read()

// Cleanup
ptyProcess.terminate()
val exitCode = ptyProcess.waitFor()
ptyProcess.close()
```

## Features

### Terminal Control

- **Window Sizing**: TIOCSWINSZ ioctl support
- **Controlling Terminal**: Child process is attached to the PTY slave after `setsid()`
- **Foreground Process Group**: The child process group is made foreground with `tcsetpgrp()` so
  terminal control bytes like Ctrl+C are delivered by the kernel to the active foreground job
- **Signal Handling**: Terminal controls can target the current PTY foreground process group, then
  foreground descendants, then the original child process group; explicit lifecycle signals still
  target the child process group with a single-process fallback

### Process Management

- **Blocking I/O**: Master FD remains blocking for standard `InputStream` behavior
- **Graceful Shutdown**: SIGTERM and SIGKILL helpers report native delivery success
- **Exit Tracking**: `waitFor()` reaps once and returns a cached exit code afterward
- **Owned Native Strings**: JNI strings are copied with `strdup()` before `fork()`

### Platform Support

- **Android**: Full support with Android NDK
- **Linux**: CMake file keeps util-library linking optional for compatible hosts
- **POSIX PTY APIs**: Uses standard `<pty.h>`, `setsid`, `ioctl`, `dup2`, and `execve`

## Compilation

### Android Build

The PTY library is compiled via gradle's native build system:

```gradle
android {
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }
}
```

## Terminal Emulation Support

The implementation supports:

- Interactive shells (bash, zsh, sh, ksh)
- Text editors (vim, nano, emacs)
- Terminal multiplexers (tmux, screen)
- Command-line tools with interactive modes (less, more, python REPL)

## Environment Configuration

When using PTY processes, the environment is automatically configured with:

- `TERM=xterm-256color` - Standard 256-color terminal capability
- Proper terminal file descriptors (stdin/stdout/stderr)
- All standard glibc environment variables

## Performance Considerations

- **Blocking Reads**: Matches `InputStream` expectations and treats PTY `EIO` as EOF
- **Large Writes**: Native writes loop until all requested bytes are written or an error occurs
- **Interrupted Syscalls**: Native read/write paths retry on `EINTR`
- **JNI Local Reference Cleanup**: Argument and environment conversion deletes local refs during
  loops

## Error Handling

All native operations include proper error reporting:

- JNI exceptions for allocation failures
- Boolean return values for control operations
- Errno values provided through exception messages where process setup fails
- Invalid file descriptors and invalid array bounds are rejected before syscall access

## Future Enhancements

Potential improvements for future versions:

1. Session recording/playback
2. Terminal color palette configuration
3. Mouse event support (MOUSE_EVENT)
4. Xterm extensions (title bar control, etc.)
5. Performance profiling for I/O operations
6. Async I/O through epoll/select

## Testing

Recommended test cases:

- Interactive shell invocation
- Terminal resize during process execution
- Terminal-generated signal delivery (Ctrl+C, Ctrl+Z)
- Explicit signal delivery (SIGINT, SIGTERM)
- Large I/O buffer handling
- PTY exhaustion scenarios
- Repeated `waitFor()` calls return the same cached exit code

## References

- POSIX PTY: https://pubs.opengroup.org/onlinepubs/9699919799/
- Linux PTY documentation: `man 7 pty`
- Android NDK JNI: https://developer.android.com/training/articles/on-device-debugging
