package org.cosmicide.exec.linux

import android.annotation.SuppressLint
import android.content.Context
import org.cosmicide.util.repairJdkExecutablePermissions
import java.io.File
import java.io.RandomAccessFile
import java.lang.reflect.Field
import java.nio.ByteBuffer
import java.nio.ByteOrder

object LinuxProcessRunner {

    private const val JSTAT_SAMPLE_INTERVAL_MS = 1000
    private const val EXECUTABLE_IDENTITY_ENV = "COSMIC_EXECUTABLE"

    data class Configuration(
        val binary: File,
        val arguments: List<String>,
        val workingDir: File,
        val environmentOverrides: Map<String, String> = emptyMap(),
        val pathEntries: List<File> = emptyList(),
        val redirectErrorStream: Boolean = true,
        val setup: Boolean = false,
        val usePty: Boolean = false,
        val loadShellStartupFiles: Boolean = usePty,
        val terminalRows: Int = 24,
        val terminalColumns: Int = 80
    )

    private data class WrappedCommand(
        val command: List<String>, val executableIdentity: String
    )

    private data class GlibcRuntime(
        val appDir: File,
        val glibcPath: String,
        val customLinker: String,
        val combinedPreload: String
    )

    private enum class ExecutableKind {
        ELF, SCRIPT, UNSUPPORTED
    }

    /**
     * Spawns an arbitrary GNU/Linux binary inside the custom glibc environment layer.
     */
    fun start(context: Context, config: Configuration): Process {
        return createProcessBuilder(context, config).start()
    }

    /**
     * Spawns a process with PTY support for interactive terminal I/O
     * Uses the exec module native PTY implementation to fork/exec and attach the child process.
     */
    fun startWithPty(context: Context, config: Configuration): PtyProcess {
        if (!config.usePty) {
            throw IllegalArgumentException("Configuration must have usePty=true")
        }

        val runtime = prepareGlibcRuntime(context, config.setup)
        val shellArguments = runtime.applyShellStartupArguments(
            binary = config.binary,
            arguments = config.arguments,
            interactive = true,
            enabled = config.loadShellStartupFiles
        )
        val environment = buildMap {
            putCommonGlibcEnvironment(runtime)
            put("PATH", buildPath(runtime, config.binary, config.pathEntries))
            put("TERM", "xterm-256color")
            putAll(config.environmentOverrides)

            // ld-linux resolves the initial executable before libpath_redirect is
            // loaded, so virtual /usr and /lib entries are useless here. Normalize
            // them to the physical app-private root while retaining mandatory
            // runtime directories.
            put("LD_LIBRARY_PATH", runtime.normalizeLibraryPath(get("LD_LIBRARY_PATH")))
        }
        val wrappedCommand = runtime.wrapCommand(
            binary = config.binary,
            arguments = shellArguments,
            inheritedLibraryPath = environment.getValue("LD_LIBRARY_PATH")
        )
        val launchEnvironment = environment + mapOf(
            // The initial process is launched through ld-linux directly, before
            // exec_wrap can intercept anything. Preserve the real executable
            // identity for /proc/self/exe and AT_EXECFN virtualization.
            EXECUTABLE_IDENTITY_ENV to wrappedCommand.executableIdentity
        )

        // IMPORTANT: config.workingDir is passed to the native layer for chdir().
        val pty = PtyTerminal.allocateAndSpawn(
            workingDir = config.workingDir,
            executable = File(wrappedCommand.command[0]),
            arguments = wrappedCommand.command.drop(1),
            environment = launchEnvironment
        )

        pty.setWindowSize(config.terminalRows, config.terminalColumns)
        return PtyProcess.create(pty)
    }

    fun toolchainPathEntries(
        context: Context, jdkDir: File, setup: Boolean = false
    ): List<File> {
        return buildList {
            add(jdkDir.resolve("bin"))
            add(context.filesDir.resolve("jdtls/bin"))
            add(runtimeDir(context, setup).resolve("usr/bin"))
        }
    }

    fun toolchainEnvironment(jdkDir: File, runtimeDir: File): Map<String, String> {
        repairJdkExecutablePermissions(jdkDir)
        return buildMap {
            if (!jdkDir.absolutePath.endsWith("system")) {
                put("JAVA_HOME", jdkDir.absolutePath)
            } else {
                val javaHome = runtimeDir.resolve("usr/lib/jvm").listFiles()
                    ?.firstOrNull { it.name.startsWith("java-") }?.absolutePath

                if (javaHome != null)
                    put("JAVA_HOME", javaHome)
            }
        }
    }

    fun parseCommandLine(commandLine: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaping = false
        var tokenStarted = false

        commandLine.forEach { char ->
            when {
                escaping -> {
                    current.append(char)
                    escaping = false
                    tokenStarted = true
                }

                char == '\\' -> {
                    escaping = true
                    tokenStarted = true
                }

                quote != null -> {
                    if (char == quote) {
                        quote = null
                    } else {
                        current.append(char)
                    }
                }

                char == '\'' || char == '"' -> {
                    quote = char
                    tokenStarted = true
                }

                char.isWhitespace() -> {
                    if (tokenStarted) {
                        result.add(current.toString())
                        current.clear()
                        tokenStarted = false
                    }
                }

                else -> {
                    current.append(char)
                    tokenStarted = true
                }
            }
        }

        if (escaping) current.append('\\')
        if (quote != null) {
            throw IllegalArgumentException("Unclosed quote in command")
        }
        if (tokenStarted) result.add(current.toString())

        return result
    }

    fun resolveExecutable(commandName: String, workingDir: File, pathEntries: List<File>): File {
        val commandFile = File(commandName)

        if (commandFile.isAbsolute) return commandFile

        if (commandName.contains(File.separatorChar)) {
            return workingDir.resolve(commandName).canonicalFile
        }

        return pathEntries.plus(File("/system/bin")).asSequence().map { it.resolve(commandName) }
            .firstOrNull { it.isFile }
            ?: throw IllegalArgumentException("Command not found: $commandName")
    }

    fun startJstatGcSampler(
        context: Context, jdkDir: File, option: String = "-gccause", pid: Int, workingDir: File
    ): Process {
        return startJstatSampler(
            context = context, jdkDir = jdkDir, option = option, pid = pid, workingDir = workingDir
        )
    }

    fun startJstatClassSampler(
        context: Context, jdkDir: File, pid: Int, workingDir: File
    ): Process {
        return startJstatSampler(
            context = context,
            jdkDir = jdkDir,
            option = "-class",
            pid = pid,
            workingDir = workingDir
        )
    }

    private fun startJstatSampler(
        context: Context, jdkDir: File, option: String, pid: Int, workingDir: File
    ): Process {
        return start(
            context = context, config = Configuration(
                binary = jdkDir.resolve("bin/jstat"),
                arguments = listOf(
                    "-J-Djava.io.tmpdir=${context.cacheDir.absolutePath}",
                    option,
                    pid.toString(),
                    JSTAT_SAMPLE_INTERVAL_MS.toString()
                ),
                workingDir = workingDir,
                environmentOverrides = if (jdkDir.name.equals("system")) emptyMap() else mapOf("JAVA_HOME" to jdkDir.absolutePath)
            )
        )
    }

    private fun createProcessBuilder(context: Context, config: Configuration): ProcessBuilder {
        val runtime = prepareGlibcRuntime(context, config.setup)
        val shellArguments = runtime.applyShellStartupArguments(
            binary = config.binary,
            arguments = config.arguments,
            interactive = false,
            enabled = config.loadShellStartupFiles
        )
        val launchEnvironment = buildMap {
            putCommonGlibcEnvironment(runtime)
            put("PATH", buildPath(runtime, config.binary, config.pathEntries))

            // Keep caller overrides late so special tool invocations can replace
            // JAVA_HOME, LD_LIBRARY_PATH, PATH, DNS_TRACE, etc.
            putAll(config.environmentOverrides)
            put("LD_LIBRARY_PATH", runtime.normalizeLibraryPath(get("LD_LIBRARY_PATH")))
        }
        val wrappedCommand = runtime.wrapCommand(
            binary = config.binary,
            arguments = shellArguments,
            inheritedLibraryPath = launchEnvironment.getValue("LD_LIBRARY_PATH")
        )

        return ProcessBuilder(wrappedCommand.command).apply {
            directory(config.workingDir)
            redirectErrorStream(config.redirectErrorStream)

            environment().apply {
                clear()
                putAll(launchEnvironment)

                // This is internal launch metadata, not a caller-facing switch.
                put(EXECUTABLE_IDENTITY_ENV, wrappedCommand.executableIdentity)
            }
        }
    }

    private fun GlibcRuntime.applyShellStartupArguments(
        binary: File, arguments: List<String>, interactive: Boolean, enabled: Boolean
    ): List<String> {
        if (!enabled) return arguments

        val target = resolveInitialGlibcTarget(binary.absoluteFile)
        if (target.name != "bash") return arguments

        val alreadyLogin = arguments.any { argument ->
            argument == "--login" || argument == "-l" || (argument.startsWith("-") && !argument.startsWith(
                "--"
            ) && argument.drop(1).contains('l'))
        }
        val alreadyInteractive = arguments.any { argument ->
            argument == "-i" || (argument.startsWith("-") && !argument.startsWith("--") && argument.drop(
                1
            ).contains('i'))
        }

        return buildList {
            if (!alreadyLogin) add("--login")
            if (interactive && !alreadyInteractive) add("-i")
            addAll(arguments)
        }
    }

    private fun GlibcRuntime.wrapCommand(
        binary: File, arguments: List<String>, inheritedLibraryPath: String
    ): WrappedCommand {
        val target = resolveInitialGlibcTarget(binary.absoluteFile)

        return when (target.executableKind()) {
            ExecutableKind.ELF -> linkerCommand(target, arguments, inheritedLibraryPath)

            ExecutableKind.SCRIPT -> {
                val interpreter = resolveInterpreterForScript(target)
                linkerCommand(
                    interpreter, listOf(target.absolutePath) + arguments, inheritedLibraryPath
                )
            }

            ExecutableKind.UNSUPPORTED -> {
                throw IllegalArgumentException(
                    "Unsupported executable: ${target.absolutePath}. " + "Expected a readable ELF binary or a readable script with a valid shebang (e.g. sh, bash, node)."
                )
            }
        }
    }

    private fun GlibcRuntime.linkerCommand(
        program: File, arguments: List<String>, inheritedLibraryPath: String
    ): WrappedCommand {
        val programPath = program.canonicalOrAbsolutePath()
        val loaderLibraryPath = loaderLibraryPath(program, inheritedLibraryPath)

        return WrappedCommand(
            command = buildList {
                add(customLinker)
                add("--argv0")
                add(programPath)
                add("--library-path")
                add(loaderLibraryPath)
                add("--preload")
                add(combinedPreload)
                add(programPath)
                addAll(arguments)
            }, executableIdentity = programPath
        )
    }

    private fun GlibcRuntime.resolveInitialGlibcTarget(binary: File): File {
        if (!binary.isAndroidSystemPath()) return binary

        val replacement = glibcBinReplacementFor(binary)
        if (replacement != null) return replacement

        throw IllegalArgumentException(
            "Refusing to launch Android system executable through glibc ld-linux: ${binary.absolutePath}. " + "No readable glibc replacement exists at ${
                File(
                    glibcPath
                ).resolve("bin").resolve(binary.name).absolutePath
            }."
        )
    }

    private fun GlibcRuntime.glibcBinReplacementFor(binary: File): File? {
        val replacement = File(glibcPath).resolve("bin").resolve(binary.name).absoluteFile

        return replacement.takeIf { it.executableKind() != ExecutableKind.UNSUPPORTED }
    }

    private fun GlibcRuntime.gccFrontendDirs(): List<File> {
        val gccRoot = File(glibcPath).resolve("lib/gcc")
        if (!gccRoot.isDirectory) return emptyList()

        return try {
            gccRoot.walkTopDown().maxDepth(4).filter { dir ->
                dir.isDirectory && (dir.resolve("cc1").exists() || dir.resolve("cc1plus")
                    .exists() || dir.resolve("f951").exists() || dir.resolve("lto1").exists())
            }.map { it.absoluteFile }.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildPath(runtime: GlibcRuntime, binary: File, extraEntries: List<File>): String {
        val glibcBin = File(runtime.glibcPath).resolve("bin")

        return buildList {
            // Keep the target directory first for project-local tools like ./gradlew,
            // but do not put /system/bin before the relocated GNU/Linux tools.
            binary.parentFile?.takeUnless { it.isAndroidSystemPath() }?.let(::add)

            addAll(extraEntries)
            add(glibcBin)
            addAll(runtime.gccFrontendDirs())
            add(File("/system/bin"))
        }.distinctBy { it.absolutePath }.joinToString(":") { it.absolutePath }
    }

    private fun GlibcRuntime.defaultShell(): File {
        return firstUsableShell(
            listOf(
                File(glibcPath).resolve("bin/bash"), File(glibcPath).resolve("bin/sh")
            )
        ) ?: File(glibcPath).resolve("bin/bash")
    }

    private fun GlibcRuntime.resolveInterpreterForScript(script: File): File {
        val requestedInterpreter = script.readShebangLine()?.interpreterFromShebang()
        val binDir = File(glibcPath).resolve("bin")

        val candidates = buildList {
            if (requestedInterpreter != null) {
                add(binDir.resolve(requestedInterpreter))
                if (requestedInterpreter.isKnownShellName()) {
                    add(binDir.resolve("sh"))
                    add(binDir.resolve("bash"))
                }
            }
            add(binDir.resolve("sh"))
            add(binDir.resolve("bash"))
        }

        return firstUsableShell(candidates) ?: throw IllegalArgumentException(
            "Script ${script.absolutePath} requires interpreter '${requestedInterpreter ?: "sh"}', " + "but no valid ELF binary was found in ${binDir.absolutePath}."
        )
    }

    private fun firstUsableShell(candidates: List<File>): File? {
        return candidates.asSequence().map { it.absoluteFile }.distinctBy { it.absolutePath }
            .firstOrNull { it.isElfFile() }
    }

    private fun File.executableKind(): ExecutableKind {
        return when {
            isElfFile() -> ExecutableKind.ELF
            isSupportedScriptFile() -> ExecutableKind.SCRIPT
            else -> ExecutableKind.UNSUPPORTED
        }
    }

    private fun File.isElfFile(): Boolean {
        // Do not check executable permission here. The kernel executes customLinker;
        // ld-linux only needs to read/map the target ELF.
        return try {
            inputStream().use { input ->
                val magic = ByteArray(4)
                input.read(magic) == 4 && magic[0] == 0x7F.toByte() && magic[1] == 'E'.code.toByte() && magic[2] == 'L'.code.toByte() && magic[3] == 'F'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun File.isSupportedScriptFile(): Boolean {
        // Do not check executable permission here either. We run scripts through
        // customLinker + glibc interpreter + <script>, so the script only needs to be readable.
        return readShebangLine()?.interpreterFromShebang() != null
    }

    private fun File.readShebangLine(maxBytes: Int = 512): String? {
        return try {
            inputStream().use { input ->
                val buffer = ByteArray(maxBytes)
                val count = input.read(buffer)
                if (count <= 0) return null

                val text = buffer.copyOf(count).toString(Charsets.UTF_8)

                text.lineSequence().firstOrNull()?.takeIf { it.startsWith("#!") }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun String.interpreterFromShebang(): String? {
        if (!startsWith("#!")) return null

        val parts = removePrefix("#!").trim().split(Regex("\\s+")).filter { it.isNotBlank() }

        if (parts.isEmpty()) return null

        val interpreter = File(parts[0]).name

        if (interpreter != "env") return interpreter

        var index = 1
        while (index < parts.size) {
            val part = parts[index]

            if (part == "-S" || part.startsWith("-")) {
                index++
                continue
            }

            return File(part).name
        }

        return null
    }

    private fun String.isKnownShellName(): Boolean {
        return this == "sh" || this == "bash" || this == "dash" || this == "ash" || this == "ksh" || this == "zsh"
    }

    private fun File.canonicalOrAbsolutePath(): String {
        return try {
            canonicalPath
        } catch (_: Exception) {
            absolutePath
        }
    }

    private fun File.isAndroidSystemPath(): Boolean {
        val path = absolutePath
        return path.startsWith("/system/") || path.startsWith("/apex/") || path.startsWith("/vendor/") || path.startsWith(
            "/odm/"
        ) || path.startsWith("/product/")
    }

    private fun prepareGlibcRuntime(context: Context, setup: Boolean): GlibcRuntime {
        val appDir = runtimeDir(context, setup)
        val homeDir = appDir.resolve("home").apply { mkdirs() }
        ensureBashStartupBridge(homeDir)
        val glibcRoot = appDir.resolve("usr")
        val glibcPath = glibcRoot.absolutePath
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        val pathRedirect = "$nativeLibDir/libpath_redirect.so"
        val nssWrapper = glibcRoot.resolve("lib/libnss_wrapper.so").absolutePath

        return GlibcRuntime(
            appDir = appDir,
            glibcPath = glibcPath,
            customLinker = "$nativeLibDir/libld_linux.so",
            combinedPreload = listOfNotNull(
                pathRedirect, if (setup) nssWrapper else null
            ).joinToString(":")
        )
    }

    private fun runtimeDir(context: Context, setup: Boolean): File {
        return context.filesDir.resolve(if (setup) "glibc" else "arch")
    }

    private fun ensureBashStartupBridge(homeDir: File) {
        val bashProfile = homeDir.resolve(".bash_profile")
        val bashLogin = homeDir.resolve(".bash_login")

        // Bash login shells read only the first existing file from:
        // .bash_profile, .bash_login, .profile. Create the conventional bridge
        // only when the user has not already supplied a Bash-specific login file.
        if (bashProfile.exists() || bashLogin.exists()) return

        bashProfile.writeTextIfChanged(
            $$"""
            # Generated by Cosmic IDE.
            # Keep login-shell environment in .profile and interactive setup in .bashrc.
            if [ -r "$HOME/.profile" ]; then
                . "$HOME/.profile"
            fi

            if [ -r "$HOME/.bashrc" ]; then
                . "$HOME/.bashrc"
            fi
            """.trimIndent() + "\n"
        )
    }

    private fun File.writeTextIfChanged(content: String) {
        parentFile?.mkdirs()

        val oldContent = try {
            if (isFile) readText() else null
        } catch (_: Exception) {
            null
        }

        if (oldContent != content) {
            writeText(content)
        }
    }


    /**
     * Converts virtual Linux library paths to their physical app-private paths.
     * Mandatory runtime directories are appended so a caller-provided
     * LD_LIBRARY_PATH cannot accidentally hide libc, libstdc++, or GCC helpers.
     */
    private fun GlibcRuntime.normalizeLibraryPath(value: String?): String {
        return buildList {
            value.orEmpty().split(':').filter { it.isNotBlank() }.forEach { entry ->
                add(relocateVirtualPath(entry))
            }
            addAll(linkLibraryDirs().map { it.absolutePath })
        }.distinct().joinToString(":")
    }

    /**
     * The preload layer cannot participate in dependency lookup for the initial
     * ELF. Translate its DT_RUNPATH/DT_RPATH entries and feed them directly to
     * ld-linux through --library-path.
     */
    private fun GlibcRuntime.loaderLibraryPath(
        program: File, inheritedLibraryPath: String
    ): String {
        val origin = program.parentFile?.canonicalOrAbsolutePath().orEmpty()
        val embedded = program.readElfDynamicSearchPath().flatMap { pathList ->
            pathList.split(':').filter { it.isNotBlank() }
        }.map { entry ->
            relocateVirtualPath(
                entry.replace($$"${ORIGIN}", origin).replace($$"$ORIGIN", origin)
            )
        }

        return (inheritedLibraryPath.split(':').filter { it.isNotBlank() } + embedded).distinct()
            .joinToString(":")
    }

    @SuppressLint("SdCardPath")
    private fun GlibcRuntime.relocateVirtualPath(path: String): String {
        if (!path.startsWith('/')) return path

        val appRoot = appDir.canonicalOrAbsolutePath()
        if (path == appRoot || path.startsWith("$appRoot/")) return path

        val termuxFiles = "/data/data/com.termux/files"
        val termuxGlibc = "$termuxFiles/usr/glibc"
        return when {
            path == termuxGlibc || path.startsWith("$termuxGlibc/") -> {
                appDir.resolve("usr" + path.removePrefix(termuxGlibc)).absolutePath
            }

            path == termuxFiles || path.startsWith("$termuxFiles/") -> {
                appDir.resolve(path.removePrefix(termuxFiles).removePrefix("/")).absolutePath
            }

            VIRTUAL_ROOTS.any { path == it || path.startsWith("$it/") } -> {
                appDir.resolve(path.removePrefix("/")).absolutePath
            }

            else -> path
        }
    }

    /**
     * Reads DT_RUNPATH (preferred) or DT_RPATH from a little-endian ELF64 file.
     * Cosmic currently supports aarch64 only, so rejecting other ELF layouts is
     * deliberate rather than silently mis-parsing them.
     */
    private fun File.readElfDynamicSearchPath(): List<String> {
        data class LoadSegment(
            val fileOffset: Long, val virtualAddress: Long, val fileSize: Long
        )

        return try {
            RandomAccessFile(this, "r").use { file ->
                fun read(offset: Long, size: Int): ByteBuffer {
                    require(offset >= 0 && size >= 0 && offset + size <= file.length())
                    val bytes = ByteArray(size)
                    file.seek(offset)
                    file.readFully(bytes)
                    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                }

                val ident = read(0, 16)
                if (ident.get(0) != 0x7F.toByte() || ident.get(1) != 'E'.code.toByte() || ident.get(
                        2
                    ) != 'L'.code.toByte() || ident.get(3) != 'F'.code.toByte() || ident.get(4) != ELF_CLASS_64 || ident.get(
                        5
                    ) != ELF_DATA_LSB
                ) return emptyList()

                val header = read(0, ELF64_HEADER_SIZE)
                val programHeaderOffset = header.getLong(32)
                val programHeaderEntrySize = header.getShort(54).toInt() and 0xFFFF
                val programHeaderCount = header.getShort(56).toInt() and 0xFFFF
                if (programHeaderEntrySize < ELF64_PROGRAM_HEADER_SIZE || programHeaderCount == 0) {
                    return emptyList()
                }

                val loadSegments = mutableListOf<LoadSegment>()
                var dynamicOffset = -1L
                var dynamicSize = 0L
                repeat(programHeaderCount) { index ->
                    val offset = programHeaderOffset + index.toLong() * programHeaderEntrySize
                    val headerEntry = read(offset, ELF64_PROGRAM_HEADER_SIZE)
                    val type = headerEntry.getInt(0)
                    val fileOffset = headerEntry.getLong(8)
                    val virtualAddress = headerEntry.getLong(16)
                    val fileSize = headerEntry.getLong(32)
                    when (type) {
                        ELF_PT_LOAD -> loadSegments += LoadSegment(
                            fileOffset = fileOffset,
                            virtualAddress = virtualAddress,
                            fileSize = fileSize
                        )

                        ELF_PT_DYNAMIC -> {
                            dynamicOffset = fileOffset
                            dynamicSize = fileSize
                        }
                    }
                }
                if (dynamicOffset < 0 || dynamicSize < ELF64_DYNAMIC_ENTRY_SIZE) return emptyList()

                var stringTableAddress = -1L
                var stringTableSize = 0L
                var runPathOffset = -1L
                var rPathOffset = -1L
                val entryCount =
                    (dynamicSize / ELF64_DYNAMIC_ENTRY_SIZE).coerceAtMost(MAX_DYNAMIC_ENTRIES.toLong())
                        .toInt()
                for (index in 0 until entryCount) {
                    val entry = read(
                        dynamicOffset + index.toLong() * ELF64_DYNAMIC_ENTRY_SIZE,
                        ELF64_DYNAMIC_ENTRY_SIZE
                    )
                    val tag = entry.getLong(0)
                    val value = entry.getLong(8)
                    when (tag) {
                        ELF_DT_NULL -> break
                        ELF_DT_STRTAB -> stringTableAddress = value
                        ELF_DT_STRSZ -> stringTableSize = value
                        ELF_DT_RPATH -> rPathOffset = value
                        ELF_DT_RUNPATH -> runPathOffset = value
                    }
                }

                val selectedOffset = if (runPathOffset >= 0) runPathOffset else rPathOffset
                if (stringTableAddress < 0 || stringTableSize <= 0 || selectedOffset < 0) {
                    return emptyList()
                }

                val segment = loadSegments.firstOrNull { segment ->
                    stringTableAddress >= segment.virtualAddress && stringTableAddress < segment.virtualAddress + segment.fileSize
                } ?: return emptyList()
                val tableFileOffset =
                    segment.fileOffset + (stringTableAddress - segment.virtualAddress)
                if (selectedOffset >= stringTableSize) return emptyList()

                val maxLength =
                    (stringTableSize - selectedOffset).coerceAtMost(MAX_DYNAMIC_STRING_BYTES.toLong())
                        .toInt()
                val bytes = read(tableFileOffset + selectedOffset, maxLength).array()
                val length = bytes.indexOf(0).let { if (it >= 0) it else bytes.size }
                listOf(bytes.copyOf(length).toString(Charsets.UTF_8))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun GlibcRuntime.linkLibraryDirs(): List<File> {
        val glibcRoot = File(glibcPath)

        return buildList {
            add(glibcRoot.resolve("lib"))
            add(glibcRoot)
            addAll(gccFrontendDirs())
        }.filter { it.exists() }.distinctBy { it.absolutePath }
    }

    private fun MutableMap<String, String>.putCommonGlibcEnvironment(runtime: GlibcRuntime) {
        put(
            "LD_LIBRARY_PATH", runtime.linkLibraryDirs().joinToString(":") { it.absolutePath })
        // --preload handles the initial custom-linker exec. LD_PRELOAD makes
        // Java/Gradle child processes inherit the same compatibility layer.
        put("LD_PRELOAD", runtime.combinedPreload)

        // Only path-bearing value exec_wrap.c cannot infer by itself.
        // Behavior toggles live as compile-time constants at the top of exec_wrap.c.
        put("HOME", runtime.appDir.resolve("home").absolutePath)
        put("SHELL", runtime.defaultShell().absolutePath)

        if (runtime.appDir.absolutePath.endsWith("arch")) {
            put("SSL_CERT_FILE", "${runtime.appDir}/etc/ssl/certs/ca-certificates.crt")
        }

        put("RES_OPTIONS", "attempts:1 timeout:1")

        // The app-private glibc runtime never has a booted systemd as PID 1.
        // Avoid systemd package hooks probing Android's restricted /proc and
        // attempting to contact a non-existent system manager.
        put("SYSTEMD_OFFLINE", "1")
        put("SYSTEMD_IGNORE_CHROOT", "1")

        // Make paths baked into Termux/gpkg binaries resolve against the app-private
        // files directory. runtime.appDir is the fake Termux files root: <app>/files/glibc.
        put("APP_FILES_DIR", runtime.appDir.absolutePath)
        put("TERMUX_PREFIX", File(runtime.glibcPath).absolutePath)

        val gccFrontendPath = runtime.gccFrontendDirs().joinToString(":") { it.absolutePath }

        if (gccFrontendPath.isNotEmpty()) {
            put("COMPILER_PATH", gccFrontendPath)
        }

        put(
            "LIBRARY_PATH", runtime.linkLibraryDirs().joinToString(":") { it.absolutePath })

        put("TMPDIR", runtime.appDir.resolve("tmp").absolutePath)
        put("TEMP", runtime.appDir.resolve("tmp").absolutePath)
        put("TMP", runtime.appDir.resolve("tmp").absolutePath)
    }

    fun getResidentMemoryKb(pid: Int): Long {
        return try {
            val fields = File("/proc/$pid/statm").readText().split(' ')
            val residentPages = fields.getOrNull(1)?.toLongOrNull() ?: return 0L
            residentPages * PAGE_SIZE_BYTES / BYTES_PER_KB
        } catch (_: Exception) {
            0L
        }
    }

    fun getJvmPid(process: Process): Int {
        return getJvmPidCandidates(process).firstOrNull() ?: -1
    }

    fun getBestProcessMemoryPid(process: Process): Int {
        return getRuntimePidCandidates(process).firstOrNull() ?: -1
    }

    fun getRuntimePidCandidates(process: Process): List<Int> {
        val launcherPid = getNativePid(process)
        if (launcherPid == -1) return emptyList()

        val descendants = findDescendantPids(launcherPid)
        return buildList {
            addAll(jvmPidCandidates(launcherPid, descendants))
            addAll(descendants)
            add(launcherPid)
        }.distinct()
    }

    fun getJvmPidCandidates(process: Process): List<Int> {
        val launcherPid = getNativePid(process)
        if (launcherPid == -1) return emptyList()

        return jvmPidCandidates(launcherPid, findDescendantPids(launcherPid))
    }

    fun getNativePid(process: Process): Int {
        return try {
            val field: Field = (process as Any).javaClass.getDeclaredField("pid").apply {
                isAccessible = true
            }
            field.get(process) as Int
        } catch (_: Exception) {
            -1
        }
    }

    private fun findDescendantPids(rootPid: Int): List<Int> {
        val result = mutableListOf<Int>()
        val queue = ArrayDeque<Int>()
        val seen = mutableSetOf(rootPid)

        queue.add(rootPid)
        while (queue.isNotEmpty()) {
            val parentPid = queue.removeFirst()
            childPidsOf(parentPid).forEach { childPid ->
                if (seen.add(childPid)) {
                    result.add(childPid)
                    queue.add(childPid)
                }
            }
        }

        return result
    }

    private fun childPidsOf(pid: Int): List<Int> {
        val directChildren = try {
            File("/proc/$pid/task/$pid/children").readText().trim().split(Regex("\\s+"))
                .mapNotNull { it.toIntOrNull() }
        } catch (_: Exception) {
            emptyList()
        }

        if (directChildren.isNotEmpty()) return directChildren

        return try {
            File("/proc").listFiles()?.mapNotNull { it.name.toIntOrNull() }
                ?.filter { childPid -> getParentPid(childPid) == pid }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getParentPid(pid: Int): Int? {
        return try {
            File("/proc/$pid/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("PPid:") }?.substringAfter(':')?.trim()
                    ?.toIntOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun jvmPidCandidates(launcherPid: Int, descendants: List<Int>): List<Int> {
        return buildList {
            addAll(descendants.filter { isJvmProcess(it) })
            if (isJvmProcess(launcherPid)) add(launcherPid)
        }.distinct()
    }

    private fun isJvmProcess(pid: Int): Boolean {
        val commandName = try {
            File("/proc/$pid/comm").readText().trim()
        } catch (_: Exception) {
            ""
        }
        if (commandName == "java") return true

        val executableName = try {
            File("/proc/$pid/exe").canonicalFile.name
        } catch (_: Exception) {
            ""
        }
        if (executableName == "java") return true

        val command = readCommandLine(pid).firstOrNull().orEmpty()
        return command == "java" || command.endsWith("/bin/java") || hasLoadedJvm(pid)
    }

    private fun readCommandLine(pid: Int): List<String> {
        return try {
            File("/proc/$pid/cmdline").readText().split('\u0000').filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun hasLoadedJvm(pid: Int): Boolean {
        return try {
            File("/proc/$pid/maps").useLines { lines ->
                lines.any { line -> line.contains("libjvm.so") }
            }
        } catch (_: Exception) {
            false
        }
    }

    private val VIRTUAL_ROOTS = listOf("/usr", "/lib", "/lib64", "/home")

    private const val ELF_CLASS_64: Byte = 2
    private const val ELF_DATA_LSB: Byte = 1
    private const val ELF64_HEADER_SIZE = 64
    private const val ELF64_PROGRAM_HEADER_SIZE = 56
    private const val ELF64_DYNAMIC_ENTRY_SIZE = 16
    private const val ELF_PT_LOAD = 1
    private const val ELF_PT_DYNAMIC = 2
    private const val ELF_DT_NULL = 0L
    private const val ELF_DT_STRTAB = 5L
    private const val ELF_DT_STRSZ = 10L
    private const val ELF_DT_RPATH = 15L
    private const val ELF_DT_RUNPATH = 29L
    private const val MAX_DYNAMIC_ENTRIES = 16_384
    private const val MAX_DYNAMIC_STRING_BYTES = 1 shl 20

    private const val PAGE_SIZE_BYTES = 4096L
    private const val BYTES_PER_KB = 1024L
}
