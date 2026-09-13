/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.git

import org.cosmicide.project.CommandExecutionService
import org.cosmicide.project.CommandRequest
import org.cosmicide.project.CommandResult
import java.io.File

data class GitFileStatus(
    val path: String,
    val stagedState: Char,
    val unstagedState: Char
) {
    val isStaged: Boolean
        get() = stagedState != ' ' && stagedState != '?'

    val isUntracked: Boolean
        get() = stagedState == '?' || unstagedState == '?'

    val isModified: Boolean
        get() = unstagedState == 'M' || stagedState == 'M'

    val isAdded: Boolean
        get() = stagedState == 'A'

    val isDeleted: Boolean
        get() = stagedState == 'D' || unstagedState == 'D'

    val displayName: String
        get() = path.substringAfterLast('/')
}

data class GitStatus(
    val branch: String = "",
    val tracking: String? = null,
    val ahead: Int = 0,
    val behind: Int = 0,
    val staged: List<GitFileStatus> = emptyList(),
    val unstaged: List<GitFileStatus> = emptyList(),
    val untracked: List<GitFileStatus> = emptyList()
) {
    val isClean: Boolean
        get() = staged.isEmpty() && unstaged.isEmpty() && untracked.isEmpty()

    val totalChangedCount: Int
        get() = staged.size + unstaged.size + untracked.size
}

data class GitBranch(
    val name: String,
    val isCurrent: Boolean,
    val isRemote: Boolean,
    val hash: String = "",
    val upstream: String? = null
) {
    val displayName: String
        get() = if (isRemote) name.removePrefix("remotes/") else name
}

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val author: String,
    val date: String,
    val message: String
)

data class GitRemote(
    val name: String,
    val fetchUrl: String,
    val pushUrl: String
)

data class GitUserConfig(
    val name: String = "",
    val email: String = "",
    val defaultBranch: String = ""
)

class GitService(
    private val commands: CommandExecutionService
) {
    suspend fun isGitAvailable(workingDirectory: File): Boolean {
        return runCatching {
            val result = executeGit(workingDirectory, listOf("--version"))
            result.successful
        }.getOrDefault(false)
    }

    suspend fun getGitVersion(workingDirectory: File): String {
        return runCatching {
            val result = executeGit(workingDirectory, listOf("--version"))
            if (result.successful) result.output.trim() else "Git not found"
        }.getOrDefault("Git not found")
    }

    suspend fun isRepository(directory: File): Boolean {
        return directory.resolve(".git").exists()
    }

    suspend fun initRepository(directory: File, initialBranch: String = "main"): CommandResult {
        require(isSafeRef(initialBranch)) { "Invalid initial branch name" }
        return executeGit(directory, listOf("init", "-b", initialBranch))
    }

    suspend fun getStatus(workingDirectory: File): Result<GitStatus> = runCatching {
        val result = executeGit(workingDirectory, listOf("status", "--porcelain=v1", "-b"))
        check(result.successful) { result.output.ifBlank { "Failed to get git status" } }
        parseGitStatus(result.output)
    }

    suspend fun stageFiles(workingDirectory: File, paths: List<String>): CommandResult {
        val args = buildList {
            add("add")
            addAll(paths)
        }
        return executeGit(workingDirectory, args)
    }

    suspend fun stageAll(workingDirectory: File): CommandResult {
        return executeGit(workingDirectory, listOf("add", "--all"))
    }

    suspend fun unstageFiles(workingDirectory: File, paths: List<String>): CommandResult {
        val args = buildList {
            add("restore")
            add("--staged")
            addAll(paths)
        }
        return executeGit(workingDirectory, args)
    }

    suspend fun unstageAll(workingDirectory: File): CommandResult {
        return executeGit(workingDirectory, listOf("restore", "--staged", "."))
    }

    suspend fun commit(workingDirectory: File, message: String): CommandResult {
        require(message.isNotBlank()) { "Commit message cannot be empty" }
        return executeGit(workingDirectory, listOf("commit", "-m", message.trim()))
    }

    suspend fun fetch(workingDirectory: File): CommandResult {
        return executeGit(workingDirectory, listOf("fetch", "--all", "--prune", "--progress"))
    }

    suspend fun pull(workingDirectory: File): CommandResult {
        return executeGit(workingDirectory, listOf("pull", "--progress"))
    }

    suspend fun push(
        workingDirectory: File,
        setUpstream: Boolean = false,
        branch: String? = null
    ): CommandResult {
        val args = buildList {
            add("push")
            add("--progress")
            if (setUpstream && branch != null && isSafeRef(branch)) {
                add("-u")
                add("origin")
                add(branch)
            }
        }
        return executeGit(workingDirectory, args)
    }

    suspend fun getBranches(workingDirectory: File): Result<List<GitBranch>> = runCatching {
        val result = executeGit(workingDirectory, listOf("branch", "-a", "-v", "--no-color"))
        check(result.successful) { result.output.ifBlank { "Failed to list branches" } }
        parseGitBranches(result.output)
    }

    suspend fun checkoutBranch(workingDirectory: File, ref: String): CommandResult {
        require(isSafeRef(ref)) { "Invalid branch or tag name: $ref" }
        return executeGit(workingDirectory, listOf("checkout", ref))
    }

    suspend fun createBranch(workingDirectory: File, branchName: String): CommandResult {
        require(isSafeRef(branchName)) { "Invalid branch name: $branchName" }
        return executeGit(workingDirectory, listOf("checkout", "-b", branchName))
    }

    suspend fun deleteBranch(
        workingDirectory: File,
        branchName: String,
        force: Boolean = false
    ): CommandResult {
        require(isSafeRef(branchName)) { "Invalid branch name: $branchName" }
        return executeGit(workingDirectory, listOf("branch", if (force) "-D" else "-d", branchName))
    }

    suspend fun getLog(workingDirectory: File, maxCount: Int = 50): Result<List<GitCommit>> =
        runCatching {
            val format = "%H%x00%h%x00%an%x00%ad%x00%s"
            val result = executeGit(
                workingDirectory,
                listOf(
                    "log",
                    "-n",
                    maxCount.toString(),
                    "--pretty=format:$format",
                    "--date=relative"
                )
            )
            if (!result.successful) {
                return@runCatching emptyList()
            }
            parseGitLog(result.output)
        }

    suspend fun getRemotes(workingDirectory: File): Result<List<GitRemote>> = runCatching {
        val result = executeGit(workingDirectory, listOf("remote", "-v"))
        check(result.successful) { result.output.ifBlank { "Failed to list remotes" } }
        parseGitRemotes(result.output)
    }

    suspend fun addRemote(workingDirectory: File, name: String, url: String): CommandResult {
        require(name.isNotBlank() && isSafeRef(name)) { "Invalid remote name" }
        require(url.isNotBlank() && !url.startsWith("-")) { "Invalid remote URL" }
        return executeGit(workingDirectory, listOf("remote", "add", name.trim(), url.trim()))
    }

    suspend fun removeRemote(workingDirectory: File, name: String): CommandResult {
        require(name.isNotBlank() && isSafeRef(name)) { "Invalid remote name" }
        return executeGit(workingDirectory, listOf("remote", "remove", name.trim()))
    }

    suspend fun getUserConfig(workingDirectory: File, global: Boolean = false): GitUserConfig {
        val flag = if (global) listOf("--global") else emptyList()
        val nameResult = executeGit(workingDirectory, listOf("config") + flag + listOf("user.name"))
        val emailResult =
            executeGit(workingDirectory, listOf("config") + flag + listOf("user.email"))
        val branchResult =
            executeGit(workingDirectory, listOf("config") + flag + listOf("init.defaultBranch"))
        return GitUserConfig(
            name = if (nameResult.successful) nameResult.output.trim() else "",
            email = if (emailResult.successful) emailResult.output.trim() else "",
            defaultBranch = if (branchResult.successful) branchResult.output.trim() else "main"
        )
    }

    suspend fun setUserConfig(
        workingDirectory: File,
        name: String,
        email: String,
        defaultBranch: String = "",
        global: Boolean = false
    ): CommandResult {
        val flag = if (global) listOf("--global") else emptyList()
        if (name.isNotBlank()) {
            executeGit(workingDirectory, listOf("config") + flag + listOf("user.name", name.trim()))
        }
        if (email.isNotBlank()) {
            executeGit(
                workingDirectory,
                listOf("config") + flag + listOf("user.email", email.trim())
            )
        }
        if (defaultBranch.isNotBlank() && isSafeRef(defaultBranch)) {
            executeGit(
                workingDirectory,
                listOf("config") + flag + listOf("init.defaultBranch", defaultBranch.trim())
            )
        }
        return CommandResult(0, "Configuration updated successfully")
    }

    suspend fun addSafeDirectory(workingDirectory: File, directoryPath: String): CommandResult {
        return executeGit(
            workingDirectory,
            listOf("config", "--global", "--add", "safe.directory", directoryPath)
        )
    }

    suspend fun saveAuthToken(
        workingDirectory: File,
        host: String = "github.com",
        username: String = "",
        token: String,
        global: Boolean = true
    ): CommandResult {
        require(token.isNotBlank()) { "Token cannot be empty" }
        val cleanHost = host.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        val cleanUser = username.trim().ifBlank { "x-access-token" }
        val flag = if (global) listOf("--global") else emptyList()
        val rewriteUrl = "https://$cleanUser:${token.trim()}@$cleanHost/"
        val targetUrl = "https://$cleanHost/"

        return executeGit(
            workingDirectory,
            listOf("config") + flag + listOf("url.$rewriteUrl.insteadOf", targetUrl)
        )
    }

    private suspend fun executeGit(
        workingDirectory: File,
        arguments: List<String>,
        onOutput: (String) -> Unit = {}
    ): CommandResult {
        return commands.execute(
            CommandRequest(
                command = "git",
                arguments = arguments,
                workingDirectory = workingDirectory,
                environment = mapOf("GIT_TERMINAL_PROMPT" to "0")
            ),
            onOutput = onOutput
        )
    }
}

internal fun parseGitStatus(output: String): GitStatus {
    var branch = ""
    var tracking: String? = null
    var ahead = 0
    var behind = 0
    val staged = mutableListOf<GitFileStatus>()
    val unstaged = mutableListOf<GitFileStatus>()
    val untracked = mutableListOf<GitFileStatus>()

    for (line in output.lines()) {
        if (line.isBlank()) continue
        if (line.startsWith("## ")) {
            val header = line.removePrefix("## ").trim()
            val branchPart = header.substringBefore(" [")
            if (branchPart.contains("...")) {
                val split = branchPart.split("...")
                branch = split[0].trim()
                tracking = split.getOrNull(1)?.trim()
            } else if (branchPart.startsWith("No commits yet on ")) {
                branch = branchPart.removePrefix("No commits yet on ").trim()
            } else if (branchPart.startsWith("Initial commit on ")) {
                branch = branchPart.removePrefix("Initial commit on ").trim()
            } else {
                branch = branchPart.trim()
            }

            if (header.contains("[") && header.contains("]")) {
                val bracketContent = header.substringAfter("[").substringBefore("]")
                for (part in bracketContent.split(",")) {
                    val trimmed = part.trim()
                    if (trimmed.startsWith("ahead ")) {
                        ahead = trimmed.removePrefix("ahead ").trim().toIntOrNull() ?: 0
                    } else if (trimmed.startsWith("behind ")) {
                        behind = trimmed.removePrefix("behind ").trim().toIntOrNull() ?: 0
                    }
                }
            }
            continue
        }

        if (line.length < 3) continue
        val stagedState = line[0]
        val unstagedState = line[1]
        val path = line.substring(3).trim().removePrefix("\"").removeSuffix("\"")

        val fileStatus = GitFileStatus(path, stagedState, unstagedState)

        if (stagedState == '?' && unstagedState == '?') {
            untracked.add(fileStatus)
        } else {
            if (stagedState != ' ' && stagedState != '?') {
                staged.add(fileStatus)
            }
            if (unstagedState != ' ' && unstagedState != '?') {
                unstaged.add(fileStatus)
            }
        }
    }

    return GitStatus(
        branch = branch.ifBlank { "HEAD" },
        tracking = tracking,
        ahead = ahead,
        behind = behind,
        staged = staged,
        unstaged = unstaged,
        untracked = untracked
    )
}

internal fun parseGitBranches(output: String): List<GitBranch> {
    val branches = mutableListOf<GitBranch>()
    for (line in output.lines()) {
        if (line.isBlank()) continue
        val isCurrent = line.startsWith("* ")
        val content = line.substring(2).trim()
        val tokens = content.split("\\s+".toRegex())
        if (tokens.isEmpty()) continue
        val name = tokens[0]
        if (name == "->") continue
        val isRemote = name.startsWith("remotes/")
        val hash = tokens.getOrNull(1) ?: ""
        var upstream: String? = null
        if (content.contains("[") && content.contains("]")) {
            upstream = content.substringAfter("[").substringBefore("]").substringBefore(":")
                .substringBefore(" ")
        }
        branches.add(
            GitBranch(
                name = name,
                isCurrent = isCurrent,
                isRemote = isRemote,
                hash = hash,
                upstream = upstream
            )
        )
    }
    return branches
}

internal fun parseGitLog(output: String): List<GitCommit> {
    val commits = mutableListOf<GitCommit>()
    for (line in output.lines()) {
        if (line.isBlank()) continue
        val parts = line.split('\u0000')
        if (parts.size >= 5) {
            commits.add(
                GitCommit(
                    hash = parts[0],
                    shortHash = parts[1],
                    author = parts[2],
                    date = parts[3],
                    message = parts[4]
                )
            )
        }
    }
    return commits
}

internal fun parseGitRemotes(output: String): List<GitRemote> {
    val remotesMap = mutableMapOf<String, Pair<String, String>>()
    for (line in output.lines()) {
        if (line.isBlank()) continue
        val tokens = line.split("\\s+".toRegex())
        if (tokens.size >= 3) {
            val name = tokens[0]
            val url = tokens[1]
            val type = tokens[2]
            val existing = remotesMap[name] ?: ("" to "")
            if (type.contains("fetch")) {
                remotesMap[name] = url to existing.second.ifBlank { url }
            } else if (type.contains("push")) {
                remotesMap[name] = existing.first.ifBlank { url } to url
            }
        }
    }
    return remotesMap.map { (name, urls) ->
        GitRemote(name = name, fetchUrl = urls.first, pushUrl = urls.second)
    }
}
