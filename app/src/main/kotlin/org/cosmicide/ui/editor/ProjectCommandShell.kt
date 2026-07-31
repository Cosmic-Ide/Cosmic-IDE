package org.cosmicide.ui.editor

/**
 * Runs contributed project commands with the same environment as the interactive terminal.
 *
 * LinuxProcessRunner promotes PTY Bash processes to login shells. Keeping -i explicit here
 * matches TerminalScreen's "bash -i" launch and lets the runner's login bridge source .bashrc.
 */
internal fun projectCommandShellArguments(command: String): List<String> = listOf(
    "-i",
    "-c",
    command
)
